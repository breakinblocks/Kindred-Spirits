package com.breakinblocks.kindredspirits.client.screen;

import com.breakinblocks.kindredspirits.companion.CompanionAggression;
import com.breakinblocks.kindredspirits.companion.CompanionCommand;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.net.CharmView;
import com.breakinblocks.kindredspirits.net.KindredNetworking.CharmActionPayload;
import com.breakinblocks.kindredspirits.net.KindredNetworking.CharmActionPayload.Action;
import com.breakinblocks.kindredspirits.registry.KindredEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

public class KindredCharmScreen extends Screen {
    private static final int PANEL_WIDTH = 288;
    private static final int PANEL_HEIGHT = 236;
    private static final int PAD = 10;

    private static final int CONTENT_Y = 26;
    private static final int PORTRAIT_WIDTH = 104;
    private static final int PORTRAIT_BOTTOM = 192;
    private static final int NAME_BOX_Y = 198;
    private static final int NAME_BOX_HEIGHT = 14;
    private static final int STATE_Y = 218;

    private static final int COLUMN_X = PAD + PORTRAIT_WIDTH + 12;
    private static final int COLUMN_WIDTH = PANEL_WIDTH - COLUMN_X - PAD;

    private static final int LEVEL_Y = 26;
    private static final int EXPERIENCE_BAR_Y = 42;
    private static final int BOND_BAR_Y = 64;
    private static final int HEALTH_BAR_Y = 86;
    private static final int ATTRIBUTE_Y = 108;
    private static final int BAR_LABEL_OFFSET = 6;

    private static final int BUTTON_Y = 124;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_SPACING = 21;
    private static final int TEXT_HEIGHT = 9;

    private static final int COLOUR_BACKDROP = 0xE0101018;
    private static final int COLOUR_PANEL = 0xFF1B1B26;
    private static final int COLOUR_PORTRAIT = 0xFF10101A;
    private static final int COLOUR_EDGE = 0xFF3C3C58;
    private static final int COLOUR_ACCENT = 0xFF6FD7E8;
    private static final int COLOUR_TITLE = 0xFFF2E9C9;
    private static final int COLOUR_LABEL = 0xFF9A9AB4;
    private static final int COLOUR_VALUE = 0xFFE8E8F2;
    private static final int COLOUR_HEALTH = 0xFFE0556A;
    private static final int COLOUR_EXPERIENCE = 0xFF7FD46B;
    private static final int COLOUR_BOND = 0xFFD79BE8;

    private static final int REFRESH_INTERVAL = 20;

    private CharmView view;
    private int refreshTimer;
    private boolean nameDirty;
    private boolean nameChanged;
    private boolean nameWasFocused;
    private boolean settingName;
    private @Nullable EditBox nameBox;
    private @Nullable CompanionEntity display;
    private int left;
    private int top;

    private Button summonButton;
    private Button dismissButton;
    private Button commandButton;
    private Button aggressionButton;
    private Button releaseButton;

    public KindredCharmScreen(CharmView view) {
        super(Component.translatable("screen.kindredspirits.charm"));
        this.view = view;
    }

    public void updateView(CharmView view) {
        boolean sameCompanion = this.view.species().equals(view.species()) && this.view.name().equals(view.name());
        this.view = view;

        if (!sameCompanion) {
            this.display = null;
        }

        if (this.nameBox != null && !this.nameBox.isFocused() && !this.nameDirty) {
            this.setNameBoxValue(view.name().orElse(""));
            this.nameBox.setHint(this.speciesName().copy().withStyle(ChatFormatting.DARK_GRAY));
        }

        if (this.summonButton != null) {
            this.updateButtonState();
        }
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - PANEL_HEIGHT) / 2;

        int buttonX = this.left + COLUMN_X;
        int y = this.top + BUTTON_Y;

        this.summonButton = this.addRenderableWidget(Button.builder(this.summonLabel(),
                        button -> this.send(this.view.stored() ? Action.SUMMON : Action.RECALL, 0))
                .bounds(buttonX, y, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.dismissButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.kindredspirits.dismiss"),
                        button -> this.send(Action.DISMISS, 0))
                .bounds(buttonX, y + BUTTON_SPACING, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.commandButton = this.addRenderableWidget(Button.builder(this.commandLabel(),
                        button -> this.send(Action.SET_COMMAND, this.view.commandValue().next().ordinal()))
                .bounds(buttonX, y + BUTTON_SPACING * 2, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.aggressionButton = this.addRenderableWidget(Button.builder(this.aggressionLabel(),
                        button -> this.send(Action.SET_AGGRESSION, this.view.aggressionValue().next().ordinal()))
                .bounds(buttonX, y + BUTTON_SPACING * 3, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.releaseButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.kindredspirits.release").withStyle(ChatFormatting.RED),
                        button -> this.send(Action.RELEASE, 0))
                .bounds(buttonX, y + BUTTON_SPACING * 4, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.nameBox = new EditBox(this.font, this.left + PAD, this.top + NAME_BOX_Y,
                PORTRAIT_WIDTH, NAME_BOX_HEIGHT, Component.translatable("screen.kindredspirits.name"));
        this.nameBox.setMaxLength(CharmActionPayload.MAX_NAME_LENGTH);
        this.nameBox.setHint(this.speciesName().copy().withStyle(ChatFormatting.DARK_GRAY));
        this.nameBox.setValue(this.view.name().orElse(""));
        this.nameBox.setResponder(value -> {
            if (!this.settingName) {
                this.nameDirty = true;
            }
        });
        this.addRenderableWidget(this.nameBox);

        this.updateButtonState();
    }

    private void setNameBoxValue(String value) {
        if (this.nameBox == null || this.nameBox.getValue().equals(value)) {
            return;
        }

        this.settingName = true;
        this.nameBox.setValue(value);
        this.settingName = false;
    }

    private Component speciesName() {
        return this.view.resolveSpecies()
                .map(species -> (Component) Component.translatable(species.translationKey()))
                .orElse(Component.empty());
    }

    private void commitName(boolean announce) {
        if (this.nameBox == null || (!this.nameDirty && !announce)) {
            return;
        }

        if (!this.nameDirty && !this.nameChanged) {
            return;
        }

        this.nameDirty = false;
        this.nameChanged = true;
        ClientPacketDistributor.sendToServer(
                new CharmActionPayload(Action.SET_NAME, announce ? 1 : 0, this.nameBox.getValue()));
    }

    private void updateButtonState() {
        boolean bound = this.view.bound();
        boolean present = bound && this.view.present();

        this.summonButton.setMessage(this.summonLabel());
        this.summonButton.active = bound && (present || this.view.reviveSeconds() <= 0);

        this.dismissButton.active = present;

        this.commandButton.setMessage(this.commandLabel());
        this.commandButton.active = present;

        this.aggressionButton.setMessage(this.aggressionLabel());
        this.aggressionButton.active = present;

        this.releaseButton.active = bound;

        if (this.nameBox != null) {
            this.nameBox.visible = bound;
            this.nameBox.setEditable(bound);
        }
    }

    private Component summonLabel() {
        if (this.view.reviveSeconds() > 0) {
            return Component.translatable("screen.kindredspirits.recovering", this.view.reviveSeconds());
        }
        return Component.translatable(this.view.stored()
                ? "screen.kindredspirits.summon"
                : "screen.kindredspirits.recall");
    }

    private Component commandLabel() {
        return Component.translatable("screen.kindredspirits.orders", this.view.commandValue().displayName());
    }

    private Component aggressionLabel() {
        return Component.translatable("screen.kindredspirits.aggression", this.view.aggressionValue().displayName());
    }

    private void send(Action action, int value) {
        ClientPacketDistributor.sendToServer(new CharmActionPayload(action, value));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, COLOUR_BACKDROP);

        graphics.fill(this.left - 1, this.top - 1, this.left + PANEL_WIDTH + 1, this.top + PANEL_HEIGHT + 1, COLOUR_EDGE);
        graphics.fill(this.left, this.top, this.left + PANEL_WIDTH, this.top + PANEL_HEIGHT, COLOUR_PANEL);
        graphics.fill(this.left, this.top, this.left + PANEL_WIDTH, this.top + 2, COLOUR_ACCENT);

        graphics.text(this.font, this.title, this.left + 10, this.top + 9, COLOUR_TITLE);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (!this.view.bound()) {
            graphics.text(this.font, Component.translatable("screen.kindredspirits.unbound"),
                    this.left + PAD, this.top + CONTENT_Y, COLOUR_VALUE);
            graphics.textWithWordWrap(this.font, Component.translatable("screen.kindredspirits.unbound_hint"),
                    this.left + PAD, this.top + CONTENT_Y + TEXT_HEIGHT + 5, PANEL_WIDTH - PAD * 2, COLOUR_LABEL);
            return;
        }

        this.renderPortrait(graphics, mouseX, mouseY);
        this.renderStats(graphics);
    }

    private void renderPortrait(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x0 = this.left + PAD;
        int y0 = this.top + CONTENT_Y;
        int x1 = x0 + PORTRAIT_WIDTH;
        int y1 = this.top + PORTRAIT_BOTTOM;

        graphics.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, COLOUR_EDGE);
        graphics.fill(x0, y0, x1, y1, COLOUR_PORTRAIT);

        CompanionEntity entity = this.displayEntity();
        if (entity != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x0 + 2, y0 + 2, x1 - 2, y1 - 2,
                    42, 0.0f, mouseX, mouseY, entity);
        }

        graphics.text(this.font, Component.translatable(this.view.present()
                        ? "screen.kindredspirits.state_out"
                        : "screen.kindredspirits.state_resting"),
                x0, this.top + STATE_Y, COLOUR_LABEL);
    }

    private void renderStats(GuiGraphicsExtractor graphics) {
        int x = this.left + COLUMN_X;

        graphics.text(this.font, Component.translatable("screen.kindredspirits.level", this.view.level()),
                x, this.top + LEVEL_Y, COLOUR_VALUE);

        this.bar(graphics, x, this.top + EXPERIENCE_BAR_Y, this.view.experience(),
                this.view.experienceToNext(), COLOUR_EXPERIENCE);
        graphics.text(this.font, Component.literal(this.view.experience() + " / " + this.view.experienceToNext()),
                x, this.top + EXPERIENCE_BAR_Y + BAR_LABEL_OFFSET, COLOUR_LABEL);

        this.bar(graphics, x, this.top + BOND_BAR_Y, this.view.bond(), this.view.maxBond(), COLOUR_BOND);
        graphics.text(this.font, Component.translatable("screen.kindredspirits.bond",
                        this.view.bond(), this.view.maxBond()),
                x, this.top + BOND_BAR_Y + BAR_LABEL_OFFSET, COLOUR_LABEL);

        if (!this.view.present()) {
            graphics.text(this.font, Component.translatable("screen.kindredspirits.stats_unavailable"),
                    x, this.top + HEALTH_BAR_Y + BAR_LABEL_OFFSET, COLOUR_LABEL);
            return;
        }

        this.bar(graphics, x, this.top + HEALTH_BAR_Y, (int) this.view.health(), (int) this.view.maxHealth(),
                COLOUR_HEALTH);
        graphics.text(this.font, Component.translatable("screen.kindredspirits.health",
                        format(this.view.health()), format(this.view.maxHealth())),
                x, this.top + HEALTH_BAR_Y + BAR_LABEL_OFFSET, COLOUR_LABEL);

        graphics.text(this.font, Component.translatable("screen.kindredspirits.attack",
                format(this.view.attackDamage())), x, this.top + ATTRIBUTE_Y, COLOUR_LABEL);
        graphics.text(this.font, Component.translatable("screen.kindredspirits.armour",
                format(this.view.armour())), x + COLUMN_WIDTH / 2, this.top + ATTRIBUTE_Y, COLOUR_LABEL);
    }

    private void bar(GuiGraphicsExtractor graphics, int x, int y, int value, int max, int colour) {
        int filled = (int) (COLUMN_WIDTH * Math.clamp(value / (double) Math.max(1, max), 0.0, 1.0));

        graphics.fill(x - 1, y - 1, x + COLUMN_WIDTH + 1, y + 4, COLOUR_EDGE);
        graphics.fill(x, y, x + COLUMN_WIDTH, y + 3, COLOUR_PORTRAIT);

        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + 3, colour);
        }
    }

    private static String format(float value) {
        return value == Math.floor(value) ? String.valueOf((int) value) : String.format("%.1f", value);
    }

    private @Nullable CompanionEntity displayEntity() {
        if (this.display != null) {
            return this.display;
        }

        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }

        CompanionSpecies species = this.view.resolveSpecies().orElse(null);
        if (species == null) {
            return null;
        }

        this.display = KindredEntities.type(species).create(this.minecraft.level, EntitySpawnReason.LOAD);

        if (this.display != null) {
            this.view.name().ifPresent(name -> this.display.setCustomName(Component.literal(name)));
        }

        return this.display;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.nameBox != null && this.nameBox.isFocused() && (event.key() == 257 || event.key() == 335)) {
            this.commitName(false);
            this.nameBox.setFocused(false);
            this.nameWasFocused = false;
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.nameBox != null) {
            boolean focused = this.nameBox.isFocused();

            if (this.nameWasFocused && !focused) {
                this.commitName(false);
            }

            this.nameWasFocused = focused;
        }

        if (++this.refreshTimer >= REFRESH_INTERVAL) {
            this.refreshTimer = 0;
            this.send(Action.REFRESH, 0);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.commitName(true);
        this.display = null;
        super.onClose();
    }
}
