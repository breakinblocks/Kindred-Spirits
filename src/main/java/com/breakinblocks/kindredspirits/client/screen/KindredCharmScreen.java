package com.breakinblocks.kindredspirits.client.screen;

import com.breakinblocks.kindredspirits.companion.CompanionAggression;
import com.breakinblocks.kindredspirits.companion.CompanionCommand;
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
import net.minecraft.util.Mth;
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
    private static final int COLOUR_STARS = 0xFFF5C542;

    private static final int SIDE_WIDTH = 150;
    private static final int SIDE_TAB_WIDTH = 12;
    private static final int SIDE_TAB_HEIGHT = 20;
    private static final int SIDE_TAB_Y = 8;
    private static final int SIDE_SLIDE_TICKS = 8;
    private static final int SIDE_PAD = 8;
    private static final int SIDE_TITLE_Y = 9;
    private static final int SIDE_CONTENT_Y = 26;
    private static final int STAT_ROW_HEIGHT = 13;
    private static final int STAT_GROUP_GAP = 6;
    private static final int ABILITY_ROW_HEIGHT = 24;
    private static final int TOGGLE_WIDTH = 32;
    private static final int TOGGLE_HEIGHT = 14;
    private static final int COLOUR_BOOSTED = 0xFF7FD46B;

    private static final boolean[] SIDE_OPEN = {true, true};

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

    private Button summonButton;
    private Button dismissButton;
    private Button commandButton;
    private Button aggressionButton;
    private Button releaseButton;
    private Button prestigeButton;
    private Button abilityButton;
    private Button unequipButton;
    private SidePanel statsPanel;
    private SidePanel abilitiesPanel;

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
        return this.skinRow ? PORTRAIT_BOTTOM_WITH_SKIN : PORTRAIT_BOTTOM;
    }

    private int nameBoxY() {
        return this.skinRow ? NAME_BOX_Y_WITH_SKIN : NAME_BOX_Y;
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - PANEL_HEIGHT) / 2;
        this.skinRow = this.wantsSkinRow();

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

        this.prestigeButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.kindredspirits.prestige").withStyle(ChatFormatting.GOLD),
                        button -> this.send(Action.PRESTIGE, 0))
                .bounds(buttonX, y + BUTTON_SPACING * 5, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.abilityButton = this.addRenderableWidget(Button.builder(this.abilityLabel(),
                        button -> this.send(Action.USE_ABILITY, 0))
                .bounds(this.left + PAD, this.top + ABILITY_BUTTON_Y, PORTRAIT_WIDTH, BUTTON_HEIGHT).build());

        this.unequipButton = this.addRenderableWidget(Button.builder(Component.literal("x"),
                        button -> this.send(Action.UNEQUIP, 0))
                .bounds(this.left + PAD + PORTRAIT_WIDTH - UNEQUIP_WIDTH, this.top + EQUIPMENT_Y,
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
            EditBox skinBox = new EditBox(this.font, this.left + PAD, this.top + SKIN_BOX_Y,
                    PORTRAIT_WIDTH, NAME_BOX_HEIGHT, Component.translatable("screen.kindredspirits.skin"));
            skinBox.setMaxLength(MAX_SKIN_LENGTH);
            skinBox.setHint(Component.translatable("screen.kindredspirits.skin_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
            skinBox.setValue(this.view.skin());
            this.skinField = new TextField(Action.SET_SKIN, this.addRenderableWidget(skinBox));
        } else {
            this.skinField = null;
        }

        this.statsPanel = new StatsPanel();
        this.abilitiesPanel = new AbilitiesPanel();
        this.statsPanel.build();
        this.abilitiesPanel.build();

        this.updateButtonState();
    }

    private List<SidePanel> panels() {
        return List.of(this.statsPanel, this.abilitiesPanel);
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
        boolean present = bound && this.view.present();

        this.summonButton.setMessage(this.summonLabel());
        this.summonButton.active = bound && (present || this.view.reviveSeconds() <= 0);

        this.dismissButton.active = present;

        this.commandButton.setMessage(this.commandLabel());
        this.commandButton.active = bound;

        this.aggressionButton.setMessage(this.aggressionLabel());
        this.aggressionButton.active = bound;

        this.releaseButton.active = bound;
        this.prestigeButton.visible = bound && this.view.canPrestige();
        this.prestigeButton.active = this.prestigeButton.visible;

        this.abilityButton.setMessage(this.abilityLabel());
        this.abilityButton.visible = present && this.view.resolveActiveAbility().isPresent();
        this.abilityButton.active = this.abilityButton.visible && this.view.activeCooldownSeconds() <= 0;

        this.unequipButton.visible = bound && !this.view.equipment().isEmpty();
        this.unequipButton.active = this.unequipButton.visible;

        for (SidePanel panel : this.panels()) {
            panel.updateState();
        }

        if (this.nameField != null) {
            this.nameField.box.visible = bound;
            this.nameField.box.setEditable(bound);
        }

        if (this.skinField != null) {
            this.skinField.box.visible = bound;
            this.skinField.box.setEditable(bound);
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

        for (SidePanel panel : this.panels()) {
            panel.layout(partialTick);
            panel.render(graphics, mouseX, mouseY);
        }

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

            if (this.view.companionsBonded() > 0) {
                graphics.text(this.font, Component.translatable("screen.kindredspirits.stat_bonded",
                        this.view.companionsBonded()), this.left + PAD, this.top + STATE_Y - 12, COLOUR_LABEL);
            }
            if (this.view.highestLevel() > 0) {
                graphics.text(this.font, Component.translatable("screen.kindredspirits.stat_highest_level",
                        this.view.highestLevel()), this.left + PAD, this.top + STATE_Y, COLOUR_LABEL);
            }
            if (this.view.highestStars() > 0) {
                graphics.text(this.font, Component.translatable("screen.kindredspirits.stat_highest_stars",
                        CharmView.starText(this.view.highestStars())), this.left + PAD, this.top + STATE_Y + 12,
                        COLOUR_STARS);
            }
            return;
        }

        this.renderPortrait(graphics, mouseX, mouseY);
        this.renderStats(graphics);
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
                x0, this.top + STATE_Y, COLOUR_LABEL);

        this.renderEquipment(graphics, x0, this.top + EQUIPMENT_Y, mouseX, mouseY);
    }

    private void renderEquipment(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        ItemStack equipment = this.view.equipment();

        if (equipment.isEmpty()) {
            graphics.text(this.font, Component.translatable("screen.kindredspirits.no_equipment"),
                    x, y + (EQUIPMENT_ICON - TEXT_HEIGHT) / 2, COLOUR_LABEL);
            return;
        }

        graphics.item(equipment, x, y);
        graphics.text(this.font, equipment.getHoverName(), x + EQUIPMENT_ICON + 4,
                y + (EQUIPMENT_ICON - TEXT_HEIGHT) / 2, COLOUR_VALUE);

        if (mouseX >= x && mouseX < x + EQUIPMENT_ICON && mouseY >= y && mouseY < y + EQUIPMENT_ICON) {
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
    public boolean keyPressed(KeyEvent event) {
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

        for (SidePanel panel : this.panels()) {
            panel.tick();
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

    private abstract class SidePanel {
        private final int side;
        private final Component title;
        private final List<AbstractWidget> widgets = new ArrayList<>();
        private Button tab;
        private float progress;
        private float previous;
        private int x;

        private SidePanel(int side, Component title) {
            this.side = side;
            this.title = title;
            this.progress = this.isOpen() ? 1.0f : 0.0f;
            this.previous = this.progress;
        }

        private int index() {
            return this.side < 0 ? 0 : 1;
        }

        private boolean isOpen() {
            return SIDE_OPEN[this.index()];
        }

        private void build() {
            this.tab = KindredCharmScreen.this.addRenderableWidget(Button.builder(this.tabLabel(), button -> {
                SIDE_OPEN[this.index()] = !this.isOpen();
                this.tab.setMessage(this.tabLabel());
                this.tab.setTooltip(this.tabTooltip());
            }).bounds(0, 0, SIDE_TAB_WIDTH, SIDE_TAB_HEIGHT).build());
            this.tab.setTooltip(this.tabTooltip());
            this.buildWidgets();
        }

        protected <T extends AbstractWidget> T add(T widget) {
            this.widgets.add(widget);
            return KindredCharmScreen.this.addRenderableWidget(widget);
        }

        private Component tabLabel() {
            return Component.literal(this.side < 0 == this.isOpen() ? "<" : ">");
        }

        private Tooltip tabTooltip() {
            return Tooltip.create(Component.translatable(this.isOpen()
                    ? "screen.kindredspirits.panel_collapse" : "screen.kindredspirits.panel_expand"));
        }

        private void tick() {
            this.previous = this.progress;
            float step = 1.0f / SIDE_SLIDE_TICKS;
            this.progress = Mth.clamp(this.progress + (this.isOpen() ? step : -step), 0.0f, 1.0f);
        }

        private float offset(float partialTick) {
            float linear = Mth.lerp(partialTick, this.previous, this.progress);
            return linear * linear * (3.0f - 2.0f * linear);
        }

        private boolean fullyOpen() {
            return this.progress >= 1.0f && this.previous >= 1.0f;
        }

        private void layout(float partialTick) {
            KindredCharmScreen screen = KindredCharmScreen.this;
            int travel = Math.round(SIDE_WIDTH * this.offset(partialTick));

            if (this.side < 0) {
                this.x = Math.max(SIDE_TAB_WIDTH + 1, screen.left - travel);
                this.tab.setX(this.x - SIDE_TAB_WIDTH);
            } else {
                this.x = Math.min(screen.width - SIDE_WIDTH - SIDE_TAB_WIDTH - 1,
                        screen.left + PANEL_WIDTH - SIDE_WIDTH + travel);
                this.tab.setX(this.x + SIDE_WIDTH);
            }

            this.tab.setY(screen.top + SIDE_TAB_Y);
            this.tab.visible = screen.view.bound();
            this.tab.active = this.tab.visible;

            this.positionWidgets(this.x, screen.top, this.tab.visible && this.fullyOpen());
        }

        private void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            KindredCharmScreen screen = KindredCharmScreen.this;
            if (!screen.view.bound() || this.progress <= 0.0f && this.previous <= 0.0f) {
                return;
            }

            int top = screen.top;
            graphics.fill(this.x - 1, top - 1, this.x + SIDE_WIDTH + 1, top + PANEL_HEIGHT + 1, COLOUR_EDGE);
            graphics.fill(this.x, top, this.x + SIDE_WIDTH, top + PANEL_HEIGHT, COLOUR_PANEL);
            graphics.fill(this.x, top, this.x + SIDE_WIDTH, top + 2, COLOUR_ACCENT);
            graphics.text(screen.font, this.title, this.x + SIDE_PAD, top + SIDE_TITLE_Y, COLOUR_TITLE);

            this.renderContent(graphics, this.x, top + SIDE_CONTENT_Y, mouseX, mouseY);
        }

        protected void updateState() {
        }

        protected abstract void buildWidgets();

        protected abstract void positionWidgets(int x, int top, boolean visible);

        protected abstract void renderContent(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY);
    }

    private final class StatsPanel extends SidePanel {
        private StatsPanel() {
            super(-1, Component.translatable("screen.kindredspirits.panel_stats"));
        }

        @Override
        protected void buildWidgets() {
        }

        @Override
        protected void positionWidgets(int x, int top, boolean visible) {
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
            CompanionStats stats = KindredCharmScreen.this.view.stats();
            int right = x + SIDE_WIDTH - SIDE_PAD;

            for (int i = 0; i < CompanionStats.KEYS.size(); i++) {
                float base = stats.base().get(i);
                float modified = stats.modified().get(i);
                this.row(graphics, x, y, right,
                        Component.translatable("screen.kindredspirits.stat." + CompanionStats.KEYS.get(i)),
                        formatStat(i, modified), formatStat(i, base),
                        modified > base + 0.001f ? COLOUR_BOOSTED : COLOUR_VALUE);
                y += STAT_ROW_HEIGHT;
            }

            y += STAT_GROUP_GAP;
            this.row(graphics, x, y, right, Component.translatable("screen.kindredspirits.stat.xp_multiplier"),
                    String.format("x%.2f", stats.experienceMultiplier()), null,
                    stats.experienceMultiplier() > 1.001f ? COLOUR_BOOSTED : COLOUR_VALUE);
            y += STAT_ROW_HEIGHT;
            this.row(graphics, x, y, right, Component.translatable("screen.kindredspirits.stat.storage"),
                    stats.storageSlots() + " / " + stats.storageMax(), null, COLOUR_VALUE);
            y += STAT_ROW_HEIGHT;
            this.row(graphics, x, y, right, Component.translatable("screen.kindredspirits.stat.revive"),
                    formatDuration(stats.reviveSeconds()), null, COLOUR_VALUE);
            y += STAT_ROW_HEIGHT + STAT_GROUP_GAP;

            graphics.textWithWordWrap(KindredCharmScreen.this.font,
                    Component.translatable("screen.kindredspirits.stat_hint"),
                    x + SIDE_PAD, y, SIDE_WIDTH - SIDE_PAD * 2, COLOUR_LABEL);
        }

        private void row(GuiGraphicsExtractor graphics, int x, int y, int right, Component label,
                         String modified, @Nullable String base, int colour) {
            graphics.text(KindredCharmScreen.this.font, label, x + SIDE_PAD, y, COLOUR_LABEL);

            int cursor = right;
            if (base != null) {
                String bracket = " (" + base + ")";
                cursor -= KindredCharmScreen.this.font.width(bracket);
                graphics.text(KindredCharmScreen.this.font, Component.literal(bracket), cursor, y, COLOUR_LABEL);
            }
            cursor -= KindredCharmScreen.this.font.width(modified);
            graphics.text(KindredCharmScreen.this.font, Component.literal(modified), cursor, y, colour);
        }
    }

    private final class AbilitiesPanel extends SidePanel {
        private final List<Button> toggles = new ArrayList<>();
        private List<Unlock> unlocks = List.of();

        private AbilitiesPanel() {
            super(1, Component.translatable("screen.kindredspirits.panel_abilities"));
        }

        @Override
        protected void buildWidgets() {
            this.unlocks = KindredCharmScreen.this.view.resolveSpecies()
                    .map(CompanionSpecies::unlocks).orElse(List.of());
            this.toggles.clear();
            for (Unlock unlock : this.unlocks) {
                String id = unlock.ability().id().getPath();
                this.toggles.add(this.add(Button.builder(Component.empty(), button ->
                                ClientPacketDistributor.sendToServer(
                                        new CharmActionPayload(Action.TOGGLE_ABILITY, 0, id)))
                        .bounds(0, 0, TOGGLE_WIDTH, TOGGLE_HEIGHT).build()));
            }
        }

        private boolean unlocked(Unlock unlock) {
            CharmView view = KindredCharmScreen.this.view;
            return unlock.isMet(view.level(), view.bondLevel());
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
        protected void positionWidgets(int x, int top, boolean visible) {
            int y = top + SIDE_CONTENT_Y;
            for (int i = 0; i < this.toggles.size(); i++) {
                Button toggle = this.toggles.get(i);
                toggle.setX(x + SIDE_WIDTH - SIDE_PAD - TOGGLE_WIDTH);
                toggle.setY(y + (ABILITY_ROW_HEIGHT - TOGGLE_HEIGHT) / 2 - 2);
                toggle.visible = visible && this.unlocked(this.unlocks.get(i));
                toggle.active = toggle.visible;
                y += ABILITY_ROW_HEIGHT;
            }
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
            CharmView view = KindredCharmScreen.this.view;
            int textWidth = SIDE_WIDTH - SIDE_PAD * 2 - TOGGLE_WIDTH - 4;

            for (Unlock unlock : this.unlocks) {
                CompanionAbility ability = unlock.ability();
                boolean unlocked = this.unlocked(unlock);
                boolean enabled = unlocked && !view.isDisabled(ability);

                graphics.text(KindredCharmScreen.this.font, ability.displayName(), x + SIDE_PAD, y,
                        enabled ? COLOUR_VALUE : COLOUR_LABEL);
                Component requirement = unlocked ? unlock.requirement() : unlock.requirement().copy()
                        .append(" ").append(Component.translatable("screen.kindredspirits.ability_locked"));
                graphics.text(KindredCharmScreen.this.font, requirement, x + SIDE_PAD, y + TEXT_HEIGHT + 2, COLOUR_LABEL);

                if (mouseX >= x + SIDE_PAD && mouseX < x + SIDE_PAD + textWidth
                        && mouseY >= y && mouseY < y + ABILITY_ROW_HEIGHT - 2) {
                    graphics.setTooltipForNextFrame(KindredCharmScreen.this.font, ability.description(), mouseX, mouseY);
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
