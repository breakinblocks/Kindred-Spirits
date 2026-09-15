package com.breakinblocks.kindredspirits.client.screen;

import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.net.CharmView;
import com.breakinblocks.kindredspirits.net.KindredNetworking.CharmActionPayload;
import com.breakinblocks.kindredspirits.net.KindredNetworking.CharmActionPayload.Action;
import com.breakinblocks.kindredspirits.registry.KindredEntities;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies.Unlock;
import com.breakinblocks.kindredspirits.companion.CompanionStats;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class KindredCharmScreen extends Screen {
    private static final int PANEL_WIDTH = 288;
    private static final int PANEL_HEIGHT = 268;
    private static final int PAD = 10;

    private static final int CONTENT_Y = 26;
    private static final int PORTRAIT_WIDTH = 104;
    private static final int PORTRAIT_BOTTOM = 192;
    private static final int PORTRAIT_BOTTOM_WITH_SKIN = 174;
    private static final int NAME_BOX_Y = 198;
    private static final int NAME_BOX_Y_WITH_SKIN = 180;
    private static final int SKIN_BOX_Y = 198;
    private static final int NAME_BOX_HEIGHT = 14;
    private static final int STATE_Y = 254;
    private static final int ABILITY_BUTTON_Y = 216;
    private static final int EQUIPMENT_Y = 236;
    private static final int EQUIPMENT_ICON = 16;
    private static final int UNEQUIP_WIDTH = 14;

    private static final int COLUMN_X = PAD + PORTRAIT_WIDTH + 12;
    private static final int COLUMN_WIDTH = PANEL_WIDTH - COLUMN_X - PAD;

    private static final int LEVEL_Y = 26;
    private static final int EXPERIENCE_BAR_Y = 42;
    private static final int BOND_BAR_Y = 64;
    private static final int HEALTH_BAR_Y = 86;
    private static final int BAR_LABEL_OFFSET = 6;

    private static final int BUTTON_Y = 124;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_SPACING = 20;
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
    private static final int COLOUR_STARS = 0xFFF5C542;

    private static final int POPUP_PAD = 10;
    private static final int POPUP_TITLE_Y = 8;
    private static final int POPUP_CONTENT_Y = 24;
    private static final int POPUP_CLOSE_SIZE = 14;
    private static final int POPUP_MARGIN = 20;
    private static final int POPUP_COLUMN_GAP = 12;
    private static final int STAT_ROW_HEIGHT = 13;
    private static final int STAT_GROUP_GAP = 6;
    private static final int ABILITY_ROW_HEIGHT = 22;
    private static final int TOGGLE_WIDTH = 32;
    private static final int TOGGLE_HEIGHT = 14;
    private static final int SCROLL_STEP = ABILITY_ROW_HEIGHT;
    private static final int COLOUR_BOOSTED = 0xFF7FD46B;
    private static final int COLOUR_POPUP_BACKDROP = 0xA0000000;

    private static final int REFRESH_INTERVAL = 20;
    private static final int MAX_SKIN_LENGTH = 16;

    private CharmView view;
    private int refreshTimer;
    private boolean skinRow;
    private @Nullable TextField nameField;
    private @Nullable TextField skinField;
    private @Nullable CompanionEntity display;
    private int left;
    private int top;
    private int panelHeight;

    private Button summonButton;
    private Button dismissButton;
    private Button commandButton;
    private Button aggressionButton;
    private Button releaseButton;
    private Button prestigeButton;
    private Button abilityButton;
    private Button unequipButton;
    private Button statsButton;
    private Button abilitiesButton;
    private @Nullable Popup popup;

    public KindredCharmScreen(CharmView view) {
        super(Component.translatable("screen.kindredspirits.charm"));
        this.view = view;
    }

    public void updateView(CharmView view) {
        boolean sameSpecies = this.view.species().equals(view.species());
        boolean sameCompanion = sameSpecies
                && this.view.name().equals(view.name())
                && this.view.skin().equals(view.skin());
        this.view = view;

        if (!sameCompanion) {
            this.display = null;
        } else if (this.display != null) {
            this.display.showEquipment(view.equipment());
        }

        if (this.summonButton != null && (this.skinRow != this.wantsSkinRow() || !sameSpecies)) {
            this.rebuildWidgets();
            return;
        }

        if (this.nameField != null) {
            this.nameField.seed(view.name().orElse(""));
            this.nameField.box.setHint(this.speciesName().copy().withStyle(ChatFormatting.DARK_GRAY));
        }

        if (this.skinField != null) {
            this.skinField.seed(view.skin());
        }

        if (this.summonButton != null) {
            this.updateButtonState();
        }
    }

    private boolean wantsSkinRow() {
        return this.view.bound() && this.view.usesPlayerSkin();
    }

    private int portraitBottom() {
        return this.lowerY(this.skinRow ? PORTRAIT_BOTTOM_WITH_SKIN : PORTRAIT_BOTTOM);
    }

    private int nameBoxY() {
        return this.lowerY(this.skinRow ? NAME_BOX_Y_WITH_SKIN : NAME_BOX_Y);
    }

    private int lowerY(int y) {
        return y - (PANEL_HEIGHT - this.panelHeight);
    }

    private int buttonStart() {
        return BUTTON_Y - Math.min(16, PANEL_HEIGHT - this.panelHeight);
    }

    private int buttonSpacing() {
        return Math.min(BUTTON_SPACING, (this.panelHeight - this.buttonStart() - 8) / 7);
    }

    private int buttonHeight() {
        return Math.min(BUTTON_HEIGHT, this.buttonSpacing() - 2);
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.panelHeight = Math.min(PANEL_HEIGHT, this.height - 8);
        this.top = (this.height - this.panelHeight) / 2;
        this.skinRow = this.wantsSkinRow();

        int buttonX = this.left + COLUMN_X;
        int y = this.top + this.buttonStart();

        this.summonButton = this.columnButton(0, this.summonLabel(),
                () -> this.send(this.view.stored() ? Action.SUMMON : Action.RECALL, 0));
        this.dismissButton = this.columnButton(1, Component.translatable("screen.kindredspirits.dismiss"),
                () -> this.send(Action.DISMISS, 0));
        this.commandButton = this.columnButton(2, this.commandLabel(),
                () -> this.send(Action.SET_COMMAND, this.view.commandValue().next().ordinal()));
        this.aggressionButton = this.columnButton(3, this.aggressionLabel(),
                () -> this.send(Action.SET_AGGRESSION, this.view.aggressionValue().next().ordinal()));
        this.releaseButton = this.columnButton(4,
                Component.translatable("screen.kindredspirits.release").withStyle(ChatFormatting.RED),
                () -> this.send(Action.RELEASE, 0));
        this.prestigeButton = this.columnButton(5,
                Component.translatable("screen.kindredspirits.prestige").withStyle(ChatFormatting.GOLD),
                () -> this.send(Action.PRESTIGE, 0));

        this.abilityButton = this.addRenderableWidget(Button.builder(this.abilityLabel(),
                        button -> this.send(Action.USE_ABILITY, 0))
                .bounds(this.left + PAD, this.top + this.lowerY(ABILITY_BUTTON_Y), PORTRAIT_WIDTH, BUTTON_HEIGHT).build());

        this.unequipButton = this.addRenderableWidget(Button.builder(Component.literal("x"),
                        button -> this.send(Action.UNEQUIP, 0))
                .bounds(this.left + PAD + PORTRAIT_WIDTH - UNEQUIP_WIDTH, this.top + this.lowerY(EQUIPMENT_Y),
                        UNEQUIP_WIDTH, EQUIPMENT_ICON).build());
        this.unequipButton.setTooltip(Tooltip.create(
                Component.translatable("screen.kindredspirits.unequip")));

        EditBox nameBox = new EditBox(this.font, this.left + PAD, this.top + this.nameBoxY(),
                PORTRAIT_WIDTH, NAME_BOX_HEIGHT, Component.translatable("screen.kindredspirits.name"));
        nameBox.setMaxLength(CharmActionPayload.MAX_NAME_LENGTH);
        nameBox.setHint(this.speciesName().copy().withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(this.view.name().orElse(""));
        this.nameField = new TextField(Action.SET_NAME, this.addRenderableWidget(nameBox));

        if (this.skinRow) {
            EditBox skinBox = new EditBox(this.font, this.left + PAD, this.top + this.lowerY(SKIN_BOX_Y),
                    PORTRAIT_WIDTH, NAME_BOX_HEIGHT, Component.translatable("screen.kindredspirits.skin"));
            skinBox.setMaxLength(MAX_SKIN_LENGTH);
            skinBox.setHint(Component.translatable("screen.kindredspirits.skin_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
            skinBox.setValue(this.view.skin());
            this.skinField = new TextField(Action.SET_SKIN, this.addRenderableWidget(skinBox));
        } else {
            this.skinField = null;
        }

        int halfWidth = COLUMN_WIDTH / 2 - 2;
        this.statsButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.kindredspirits.stats"),
                        button -> this.openPopup(new StatsPopup()))
                .bounds(buttonX, y + this.buttonSpacing() * 6, halfWidth, this.buttonHeight()).build());
        this.abilitiesButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.kindredspirits.abilities"),
                        button -> this.openPopup(new AbilitiesPopup()))
                .bounds(buttonX + COLUMN_WIDTH - halfWidth, y + this.buttonSpacing() * 6, halfWidth, this.buttonHeight()).build());

        if (this.popup != null) {
            this.popup.open();
        }

        this.updateButtonState();
    }

    private Button columnButton(int row, Component label, Runnable onPress) {
        return this.addRenderableWidget(Button.builder(label, button -> onPress.run())
                .bounds(this.left + COLUMN_X, this.top + this.buttonStart() + this.buttonSpacing() * row, COLUMN_WIDTH, this.buttonHeight())
                .build());
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int left, int top, int width, int height,
                           Component title, int titleX, int titleY) {
        graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, COLOUR_EDGE);
        graphics.fill(left, top, left + width, top + height, COLOUR_PANEL);
        graphics.fill(left, top, left + width, top + 2, COLOUR_ACCENT);
        graphics.text(this.font, title, titleX, titleY, COLOUR_TITLE);
    }

    private void openPopup(Popup popup) {
        this.closePopup();
        this.popup = popup;
        popup.open();
        this.updateButtonState();
    }

    private void closePopup() {
        if (this.popup != null) {
            this.popup.close();
            this.popup = null;
            this.updateButtonState();
        }
    }

    private List<AbstractWidget> mainWidgets() {
        List<AbstractWidget> widgets = new ArrayList<>(List.of(this.summonButton, this.dismissButton,
                this.commandButton, this.aggressionButton, this.releaseButton, this.prestigeButton,
                this.abilityButton, this.unequipButton, this.statsButton, this.abilitiesButton));
        for (TextField field : this.fields()) {
            widgets.add(field.box);
        }
        return widgets;
    }

    private Component abilityLabel() {
        return this.view.resolveActiveAbility()
                .map(ability -> this.view.activeCooldownSeconds() > 0
                        ? Component.translatable("screen.kindredspirits.ability_cooldown",
                                ability.displayName(), this.view.activeCooldownSeconds())
                        : ability.displayName())
                .orElse(Component.empty());
    }

    private Component speciesName() {
        return this.view.resolveSpecies()
                .map(species -> (Component) Component.translatable(species.translationKey()))
                .orElse(Component.empty());
    }

    private void updateButtonState() {
        boolean bound = this.view.bound();
        boolean editable = bound && (this.view.stored() || this.view.present());
        boolean present = bound && this.view.present();

        this.summonButton.setMessage(this.summonLabel());
        this.summonButton.active = editable && (present || this.view.reviveSeconds() <= 0);

        this.dismissButton.active = present;

        this.commandButton.setMessage(this.commandLabel());
        this.commandButton.active = editable;

        this.aggressionButton.setMessage(this.aggressionLabel());
        this.aggressionButton.active = editable;

        this.releaseButton.active = editable;
        this.prestigeButton.visible = bound && this.view.canPrestige();
        this.prestigeButton.active = this.prestigeButton.visible && editable;

        this.abilityButton.setMessage(this.abilityLabel());
        this.abilityButton.visible = present && this.view.resolveActiveAbility().isPresent();
        this.abilityButton.active = this.abilityButton.visible && this.view.activeCooldownSeconds() <= 0;

        this.unequipButton.visible = bound && !this.view.equipment().isEmpty();
        this.unequipButton.active = this.unequipButton.visible && editable;

        this.statsButton.visible = bound;
        this.abilitiesButton.visible = bound;
        this.summonButton.visible = true;
        this.dismissButton.visible = true;
        this.commandButton.visible = true;
        this.aggressionButton.visible = true;
        this.releaseButton.visible = true;

        if (this.popup != null) {
            for (AbstractWidget widget : this.mainWidgets()) {
                widget.visible = false;
            }
            this.popup.updateState();
        }

        for (TextField field : this.fields()) {
            field.box.visible = bound && this.popup == null;
            field.box.setEditable(editable && this.popup == null);
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

        this.drawPanel(graphics, this.left, this.top, PANEL_WIDTH, this.panelHeight,
                this.title, this.left + 10, this.top + 9);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (!this.view.bound()) {
            graphics.text(this.font, Component.translatable("screen.kindredspirits.unbound"),
                    this.left + PAD, this.top + CONTENT_Y, COLOUR_VALUE);
            graphics.textWithWordWrap(this.font, Component.translatable("screen.kindredspirits.unbound_hint"),
                    this.left + PAD, this.top + CONTENT_Y + TEXT_HEIGHT + 5, PANEL_WIDTH - PAD * 2, COLOUR_LABEL);

            if (this.view.companionsBonded() > 0) {
                graphics.text(this.font, Component.translatable("screen.kindredspirits.stat_bonded",
                        this.view.companionsBonded()), this.left + PAD, this.top + this.lowerY(STATE_Y) - 12, COLOUR_LABEL);
            }
            if (this.view.highestLevel() > 0) {
                graphics.text(this.font, Component.translatable("screen.kindredspirits.stat_highest_level",
                        this.view.highestLevel()), this.left + PAD, this.top + this.lowerY(STATE_Y), COLOUR_LABEL);
            }
            if (this.view.highestStars() > 0) {
                graphics.text(this.font, Component.translatable("screen.kindredspirits.stat_highest_stars",
                        CharmView.starText(this.view.highestStars())), this.left + PAD, this.top + this.lowerY(STATE_Y) + 12,
                        COLOUR_STARS);
            }
            return;
        }

        this.renderPortrait(graphics, mouseX, mouseY);
        this.renderStats(graphics);

        if (this.popup != null) {
            this.popup.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderPortrait(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x0 = this.left + PAD;
        int y0 = this.top + CONTENT_Y;
        int x1 = x0 + PORTRAIT_WIDTH;
        int y1 = this.top + this.portraitBottom();

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
                x0, this.top + this.lowerY(STATE_Y), COLOUR_LABEL);

        this.renderEquipment(graphics, x0, this.top + this.lowerY(EQUIPMENT_Y), mouseX, mouseY);
    }

    private void renderEquipment(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        ItemStack equipment = this.view.equipment();

        if (equipment.isEmpty()) {
            graphics.text(this.font, Component.translatable("screen.kindredspirits.no_equipment"),
                    x, y + (EQUIPMENT_ICON - TEXT_HEIGHT) / 2, COLOUR_LABEL);
            return;
        }

        graphics.item(equipment, x, y);
        graphics.text(this.font, this.font.plainSubstrByWidth(equipment.getHoverName().getString(),
                        PORTRAIT_WIDTH - EQUIPMENT_ICON - UNEQUIP_WIDTH - 8), x + EQUIPMENT_ICON + 4,
                y + (EQUIPMENT_ICON - TEXT_HEIGHT) / 2, COLOUR_VALUE);

        if (mouseX >= x && mouseX < x + PORTRAIT_WIDTH - UNEQUIP_WIDTH && mouseY >= y && mouseY < y + EQUIPMENT_ICON) {
            graphics.setTooltipForNextFrame(this.font, equipment, mouseX, mouseY);
        }
    }

    private void renderStats(GuiGraphicsExtractor graphics) {
        int x = this.left + COLUMN_X;

        graphics.text(this.font, Component.translatable("screen.kindredspirits.level", this.view.level()),
                x, this.top + LEVEL_Y, COLOUR_VALUE);
        if (this.view.stars() > 0) {
            graphics.text(this.font, CharmView.starText(this.view.stars()),
                    x + COLUMN_WIDTH - this.font.width(CharmView.starText(this.view.stars())),
                    this.top + LEVEL_Y, COLOUR_STARS);
        }

        this.bar(graphics, x, this.top + EXPERIENCE_BAR_Y, this.view.experience(),
                this.view.experienceToNext(), COLOUR_EXPERIENCE);
        graphics.text(this.font, Component.literal(this.view.experience() + " / " + this.view.experienceToNext()),
                x, this.top + EXPERIENCE_BAR_Y + BAR_LABEL_OFFSET, COLOUR_LABEL);

        boolean maxBond = this.view.bondLevel() >= this.view.bondMaxLevel();
        this.bar(graphics, x, this.top + BOND_BAR_Y, maxBond ? 1 : this.view.bondProgress(),
                maxBond ? 1 : this.view.bondCost(), COLOUR_BOND);
        Component bondLabel = Component.translatable("screen.kindredspirits.bond",
                this.view.bondLevel(), this.view.bondMaxLevel());
        if (this.view.feedSeconds() > 0) {
            bondLabel = bondLabel.copy().append(Component.translatable("screen.kindredspirits.bond_feed_wait",
                    formatDuration(this.view.feedSeconds())));
        }
        graphics.text(this.font, bondLabel, x, this.top + BOND_BAR_Y + BAR_LABEL_OFFSET, COLOUR_LABEL);

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
    }

    private void bar(GuiGraphicsExtractor graphics, int x, int y, int value, int max, int colour) {
        int filled = (int) (COLUMN_WIDTH * Math.clamp(value / (double) Math.max(1, max), 0.0, 1.0));

        graphics.fill(x - 1, y - 1, x + COLUMN_WIDTH + 1, y + 4, COLOUR_EDGE);
        graphics.fill(x, y, x + COLUMN_WIDTH, y + 3, COLOUR_PORTRAIT);

        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + 3, colour);
        }
    }

    private static String formatDuration(int seconds) {
        return seconds >= 60 ? (seconds + 59) / 60 + "m" : seconds + "s";
    }

    private static String format(float value) {
        return value == Math.floor(value) ? String.valueOf((int) value) : String.format("%.1f", value);
    }

    private static String formatStat(int index, float value) {
        return CompanionStats.ATTRIBUTES.get(index).is(Attributes.MOVEMENT_SPEED)
                ? String.format("%.2f", value)
                : format(value);
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
            this.display.showEquipment(this.view.equipment());

            if (species.usesPlayerSkin()) {
                this.display.setSkinName(this.view.skin().isEmpty() ? this.localName() : this.view.skin());
            }
        }

        return this.display;
    }

    private String localName() {
        return this.minecraft == null ? "" : this.minecraft.getGameProfile().name();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.popup != null) {
            this.popup.scrollBy(scrollY);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE && this.popup != null) {
            this.closePopup();
            return true;
        }

        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            for (TextField field : this.fields()) {
                if (field.commitOnEnter()) {
                    return true;
                }
            }
        }

        return super.keyPressed(event);
    }

    private List<TextField> fields() {
        List<TextField> fields = new ArrayList<>(2);

        if (this.nameField != null) {
            fields.add(this.nameField);
        }
        if (this.skinField != null) {
            fields.add(this.skinField);
        }

        return fields;
    }

    @Override
    public void tick() {
        super.tick();

        for (TextField field : this.fields()) {
            field.tickFocus();
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
        for (TextField field : this.fields()) {
            field.commit(true);
        }

        this.display = null;
        super.onClose();
    }

    private abstract class Popup {
        private final Component title;
        private final List<AbstractWidget> widgets = new ArrayList<>();
        private Button closeButton;
        private int left;
        private int top;
    private int panelHeight;
        private int width;
        private int height;
        private int contentHeight;
        private int scroll;

        private Popup(Component title) {
            this.title = title;
        }

        private void open() {
            KindredCharmScreen screen = KindredCharmScreen.this;
            int titleWidth = screen.font.width(this.title) + POPUP_CLOSE_SIZE + POPUP_PAD;
            this.width = Math.max(this.contentWidth(), titleWidth) + POPUP_PAD * 2;
            this.contentHeight = this.contentHeight();
            this.height = Math.min(this.contentHeight + POPUP_CONTENT_Y + POPUP_PAD,
                    screen.height - POPUP_MARGIN * 2);
            this.left = (screen.width - this.width) / 2;
            this.top = (screen.height - this.height) / 2;
            this.scroll = 0;

            this.closeButton = this.add(Button.builder(Component.literal("x"), button -> screen.closePopup())
                    .bounds(this.left + this.width - POPUP_PAD - POPUP_CLOSE_SIZE + 4, this.top + 4,
                            POPUP_CLOSE_SIZE, POPUP_CLOSE_SIZE).build());
            this.buildWidgets();
            this.layout();
        }

        private void close() {
            for (AbstractWidget widget : this.widgets) {
                KindredCharmScreen.this.removeWidget(widget);
            }
            this.widgets.clear();
        }

        protected <T extends AbstractWidget> T add(T widget) {
            this.widgets.add(widget);
            return KindredCharmScreen.this.addWidget(widget);
        }

        private int contentLeft() {
            return this.left + POPUP_PAD;
        }

        private int contentTop() {
            return this.top + POPUP_CONTENT_Y;
        }

        private int contentBottom() {
            return this.top + this.height - POPUP_PAD;
        }

        private int innerWidth() {
            return this.width - POPUP_PAD * 2;
        }

        private int maxScroll() {
            return Math.max(0, this.contentHeight - (this.contentBottom() - this.contentTop()));
        }

        private void scrollBy(double amount) {
            this.scroll = Math.clamp((int) Math.round(this.scroll - amount * SCROLL_STEP), 0, this.maxScroll());
            this.layout();
        }

        private void layout() {
            this.layoutWidgets(this.contentLeft(), this.contentTop() - this.scroll,
                    this.contentTop(), this.contentBottom());
        }

        private void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            KindredCharmScreen screen = KindredCharmScreen.this;
            graphics.fill(0, 0, screen.width, screen.height, COLOUR_POPUP_BACKDROP);
            screen.drawPanel(graphics, this.left, this.top, this.width, this.height,
                    this.title, this.contentLeft(), this.top + POPUP_TITLE_Y);

            boolean inside = mouseX >= this.contentLeft() && mouseX < this.contentLeft() + this.innerWidth()
                    && mouseY >= this.contentTop() && mouseY < this.contentBottom();
            graphics.enableScissor(this.contentLeft(), this.contentTop(),
                    this.contentLeft() + this.innerWidth(), this.contentBottom());
            this.renderContent(graphics, this.contentLeft(), this.contentTop() - this.scroll,
                    inside ? mouseX : -1, inside ? mouseY : -1);
            graphics.disableScissor();

            for (AbstractWidget widget : this.widgets) {
                if (widget.visible) {
                    widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
                }
            }
        }

        protected void updateState() {
        }

        protected void buildWidgets() {
        }

        protected void layoutWidgets(int x, int y, int visibleTop, int visibleBottom) {
        }

        protected abstract int contentWidth();

        protected abstract int contentHeight();

        protected abstract void renderContent(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY);
    }

    private final class StatsPopup extends Popup {
        private StatsPopup() {
            super(Component.translatable("screen.kindredspirits.stats"));
        }

        private CompanionStats stats() {
            return KindredCharmScreen.this.view.stats();
        }

        private Component label(int index) {
            return Component.translatable("screen.kindredspirits.stat." + CompanionStats.KEYS.get(index));
        }

        private String value(int index) {
            CompanionStats stats = this.stats();
            return formatStat(index, stats.modified().get(index)) + " (" + formatStat(index, stats.base().get(index)) + ")";
        }

        private List<Component> derivedLabels() {
            return List.of(Component.translatable("screen.kindredspirits.stat.xp_multiplier"),
                    Component.translatable("screen.kindredspirits.stat.storage"),
                    Component.translatable("screen.kindredspirits.stat.revive"));
        }

        private List<String> derivedValues() {
            CompanionStats stats = this.stats();
            return List.of(String.format("x%.2f", stats.experienceMultiplier()),
                    stats.storageSlots() + " / " + stats.storageMax(),
                    formatDuration(stats.reviveSeconds()));
        }

        @Override
        protected int contentWidth() {
            var font = KindredCharmScreen.this.font;
            int widest = font.width(Component.translatable("screen.kindredspirits.stat_hint"));
            for (int i = 0; i < CompanionStats.KEYS.size(); i++) {
                widest = Math.max(widest, font.width(this.label(i)) + POPUP_COLUMN_GAP + font.width(this.value(i)));
            }
            List<Component> labels = this.derivedLabels();
            List<String> values = this.derivedValues();
            for (int i = 0; i < labels.size(); i++) {
                widest = Math.max(widest, font.width(labels.get(i)) + POPUP_COLUMN_GAP + font.width(values.get(i)));
            }
            return widest;
        }

        @Override
        protected int contentHeight() {
            return STAT_ROW_HEIGHT * (CompanionStats.KEYS.size() + this.derivedLabels().size())
                    + STAT_GROUP_GAP * 2 + TEXT_HEIGHT;
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
            CompanionStats stats = this.stats();
            int right = x + this.contentWidth();

            for (int i = 0; i < CompanionStats.KEYS.size(); i++) {
                boolean boosted = stats.modified().get(i) > stats.base().get(i) + 0.001f;
                this.row(graphics, x, y, right, this.label(i), this.value(i), boosted ? COLOUR_BOOSTED : COLOUR_VALUE);
                y += STAT_ROW_HEIGHT;
            }

            y += STAT_GROUP_GAP;
            List<Component> labels = this.derivedLabels();
            List<String> values = this.derivedValues();
            for (int i = 0; i < labels.size(); i++) {
                boolean boosted = i == 0 && stats.experienceMultiplier() > 1.001f;
                this.row(graphics, x, y, right, labels.get(i), values.get(i), boosted ? COLOUR_BOOSTED : COLOUR_VALUE);
                y += STAT_ROW_HEIGHT;
            }

            y += STAT_GROUP_GAP;
            graphics.text(KindredCharmScreen.this.font, Component.translatable("screen.kindredspirits.stat_hint"),
                    x, y, COLOUR_LABEL);
        }

        private void row(GuiGraphicsExtractor graphics, int x, int y, int right, Component label, String value, int colour) {
            graphics.text(KindredCharmScreen.this.font, label, x, y, COLOUR_LABEL);
            graphics.text(KindredCharmScreen.this.font, Component.literal(value),
                    right - KindredCharmScreen.this.font.width(value), y, colour);
        }
    }

    private final class AbilitiesPopup extends Popup {
        private final List<Unlock> unlocks;
        private final List<Button> toggles = new ArrayList<>();

        private AbilitiesPopup() {
            super(Component.translatable("screen.kindredspirits.abilities"));
            this.unlocks = KindredCharmScreen.this.view.resolveSpecies()
                    .map(CompanionSpecies::unlocks).orElse(List.of());
        }

        private boolean unlocked(Unlock unlock) {
            CharmView view = KindredCharmScreen.this.view;
            return unlock.isMet(view.level(), view.bondLevel());
        }

        private Component requirement(Unlock unlock) {
            return this.unlocked(unlock) ? unlock.requirement()
                    : unlock.requirement().copy().append(" ")
                            .append(Component.translatable("screen.kindredspirits.ability_locked"));
        }

        @Override
        protected int contentWidth() {
            var font = KindredCharmScreen.this.font;
            int names = 0;
            int requirements = 0;
            for (Unlock unlock : this.unlocks) {
                names = Math.max(names, font.width(unlock.ability().displayName()));
                requirements = Math.max(requirements, font.width(this.requirement(unlock)));
            }
            return names + POPUP_COLUMN_GAP + requirements + POPUP_COLUMN_GAP + TOGGLE_WIDTH;
        }

        @Override
        protected int contentHeight() {
            return ABILITY_ROW_HEIGHT * this.unlocks.size();
        }

        @Override
        protected void buildWidgets() {
            this.toggles.clear();
            for (Unlock unlock : this.unlocks) {
                String id = unlock.ability().id().getPath();
                this.toggles.add(this.add(Button.builder(Component.empty(), button ->
                                ClientPacketDistributor.sendToServer(
                                        new CharmActionPayload(Action.TOGGLE_ABILITY, 0, id)))
                        .bounds(0, 0, TOGGLE_WIDTH, TOGGLE_HEIGHT).build()));
            }
            this.updateState();
        }

        @Override
        protected void updateState() {
            CharmView view = KindredCharmScreen.this.view;
            for (int i = 0; i < this.unlocks.size(); i++) {
                boolean enabled = !view.isDisabled(this.unlocks.get(i).ability());
                this.toggles.get(i).setMessage(Component.translatable(enabled
                                ? "screen.kindredspirits.ability_on" : "screen.kindredspirits.ability_off")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
        }

        @Override
        protected void layoutWidgets(int x, int y, int visibleTop, int visibleBottom) {
            int buttonX = x + this.contentWidth() - TOGGLE_WIDTH;
            for (int i = 0; i < this.toggles.size(); i++) {
                Button toggle = this.toggles.get(i);
                int rowY = y + i * ABILITY_ROW_HEIGHT + (ABILITY_ROW_HEIGHT - TOGGLE_HEIGHT) / 2;
                toggle.setX(buttonX);
                toggle.setY(rowY);
                toggle.visible = this.unlocked(this.unlocks.get(i))
                        && rowY >= visibleTop && rowY + TOGGLE_HEIGHT <= visibleBottom;
                toggle.active = toggle.visible && (KindredCharmScreen.this.view.stored() || KindredCharmScreen.this.view.present());
            }
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
            CharmView view = KindredCharmScreen.this.view;
            var font = KindredCharmScreen.this.font;
            int requirementRight = x + this.contentWidth() - TOGGLE_WIDTH - POPUP_COLUMN_GAP;
            int textY = (ABILITY_ROW_HEIGHT - TEXT_HEIGHT) / 2;

            for (Unlock unlock : this.unlocks) {
                CompanionAbility ability = unlock.ability();
                boolean unlocked = this.unlocked(unlock);
                boolean enabled = unlocked && !view.isDisabled(ability);

                graphics.text(font, ability.displayName(), x, y + textY, enabled ? COLOUR_VALUE : COLOUR_LABEL);
                Component requirement = this.requirement(unlock);
                graphics.text(font, requirement, requirementRight - font.width(requirement), y + textY, COLOUR_LABEL);

                if (mouseX >= x && mouseX < requirementRight && mouseY >= y && mouseY < y + ABILITY_ROW_HEIGHT) {
                    graphics.setTooltipForNextFrame(font, ability.description(), mouseX, mouseY);
                }

                y += ABILITY_ROW_HEIGHT;
            }
        }
    }

    private final class TextField {
        private final Action action;
        private final EditBox box;
        private boolean dirty;
        private boolean changed;
        private boolean wasFocused;
        private boolean setting;

        private TextField(Action action, EditBox box) {
            this.action = action;
            this.box = box;
            this.box.setResponder(value -> {
                if (!this.setting) {
                    this.dirty = true;
                }
            });
        }

        private void seed(String value) {
            if (this.box.isFocused() || this.dirty || this.box.getValue().equals(value)) {
                return;
            }

            this.setting = true;
            this.box.setValue(value);
            this.setting = false;
        }

        private void commit(boolean announce) {
            if (!this.dirty && !announce) {
                return;
            }

            if (!this.dirty && !this.changed) {
                return;
            }

            this.dirty = false;
            this.changed = true;
            ClientPacketDistributor.sendToServer(
                    new CharmActionPayload(this.action, announce ? 1 : 0, this.box.getValue()));
        }

        private boolean commitOnEnter() {
            if (!this.box.isFocused()) {
                return false;
            }

            this.commit(false);
            this.box.setFocused(false);
            this.wasFocused = false;
            return true;
        }

        private void tickFocus() {
            boolean focused = this.box.isFocused();

            if (this.wasFocused && !focused) {
                this.commit(false);
            }

            this.wasFocused = focused;
        }
    }
}
