package com.breakinblocks.kindredspirits.client.screen;

import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies.Unlock;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import com.breakinblocks.kindredspirits.net.KindredNetworking;
import com.breakinblocks.kindredspirits.net.KindredNetworking.ToggleAbilityPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class CompanionAbilitiesScreen extends Screen {
    private static final int PAD = 10;
    private static final int TITLE_Y = 9;
    private static final int STATUS_Y = 24;
    private static final int STATUS_GAP = 8;
    private static final int LINE_HEIGHT = 10;
    private static final int MIN_CONTENT_WIDTH = 200;
    private static final int MARGIN = 20;
    private static final int COLUMN_GAP = 12;
    private static final int ROW_HEIGHT = 22;
    private static final int TOGGLE_WIDTH = 32;
    private static final int TOGGLE_HEIGHT = 14;
    private static final int TEXT_HEIGHT = 9;
    private static final int TOOLTIP_WIDTH = 200;
    private static final List<String> STATUS_KEYS = List.of("bonded", "staying", "idle");

    private static final int COLOUR_BACKDROP = 0xA0000000;
    private static final int COLOUR_PANEL = 0xFF1B1B26;
    private static final int COLOUR_EDGE = 0xFF3C3C58;
    private static final int COLOUR_ACCENT = 0xFF6FD7E8;
    private static final int COLOUR_TITLE = 0xFFF2E9C9;
    private static final int COLOUR_LABEL = 0xFF9A9AB4;
    private static final int COLOUR_VALUE = 0xFFE8E8F2;
    private static final int COLOUR_WORKING = 0xFF7FD46B;

    private final CompanionEntity companion;
    private final List<Unlock> unlocks;
    private final List<Button> toggles = new ArrayList<>();
    private int left;
    private int top;
    private int contentWidth;
    private int panelHeight;
    private int rowsTop;
    private int rowsBottom;
    private int scroll;

    public CompanionAbilitiesScreen(CompanionEntity companion) {
        super(Component.translatable("screen.kindredspirits.abilities"));
        this.companion = companion;
        this.unlocks = companion.species().unlocks();
    }

    private boolean unlocked(Unlock unlock) {
        return unlock.isMet(this.companion.getLevel(), this.companion.getBondLevel());
    }

    private Component requirement(Unlock unlock) {
        return this.unlocked(unlock)
                ? unlock.requirement()
                : unlock.requirement()
                        .copy()
                        .append(" ")
                        .append(Component.translatable("screen.kindredspirits.ability_locked"));
    }

    private String statusKey() {
        return this.companion.isBonded() ? "bonded" : this.companion.isStaying() ? "staying" : "idle";
    }

    private static Component status(String key) {
        return Component.translatable("screen.kindredspirits.abilities_" + key);
    }

    @Override
    protected void init() {
        int names = 0;
        int requirements = 0;
        for (Unlock unlock : this.unlocks) {
            names = Math.max(names, this.font.width(unlock.ability().displayName()));
            requirements = Math.max(requirements, this.font.width(this.requirement(unlock)));
        }
        this.contentWidth = Math.max(
                MIN_CONTENT_WIDTH,
                Math.max(
                        this.font.width(this.companion.getDisplayName()),
                        names + COLUMN_GAP + requirements + COLUMN_GAP + TOGGLE_WIDTH));

        int statusLines = STATUS_KEYS.stream()
                .mapToInt(key -> this.font.split(status(key), this.contentWidth).size())
                .max()
                .orElse(1);
        int header = STATUS_Y + statusLines * LINE_HEIGHT + STATUS_GAP;
        int panelWidth = this.contentWidth + PAD * 2;
        this.panelHeight = Math.min(header + ROW_HEIGHT * this.unlocks.size() + PAD, this.height - MARGIN * 2);
        this.left = (this.width - panelWidth) / 2;
        this.top = (this.height - this.panelHeight) / 2;
        this.rowsTop = this.top + header;
        this.rowsBottom = this.top + this.panelHeight - PAD;
        this.scroll = Math.clamp(this.scroll, 0, this.maxScroll());

        this.toggles.clear();
        for (Unlock unlock : this.unlocks) {
            String id = unlock.ability().id().getPath();
            this.toggles.add(this.addRenderableWidget(Button.builder(
                            Component.empty(),
                            button -> ClientPacketDistributor.sendToServer(
                                    new ToggleAbilityPayload(this.companion.getId(), id)))
                    .bounds(0, 0, TOGGLE_WIDTH, TOGGLE_HEIGHT)
                    .build()));
        }
        this.updateToggles();
    }

    private int maxScroll() {
        return Math.max(0, ROW_HEIGHT * this.unlocks.size() - (this.rowsBottom - this.rowsTop));
    }

    private void updateToggles() {
        int buttonX = this.left + PAD + this.contentWidth - TOGGLE_WIDTH;
        for (int i = 0; i < this.toggles.size(); i++) {
            Unlock unlock = this.unlocks.get(i);
            Button toggle = this.toggles.get(i);
            int rowY = this.rowsTop - this.scroll + i * ROW_HEIGHT + (ROW_HEIGHT - TOGGLE_HEIGHT) / 2;
            boolean enabled = !this.companion.isAbilityDisabled(unlock.ability());

            toggle.setX(buttonX);
            toggle.setY(rowY);
            toggle.visible = this.unlocked(unlock) && rowY >= this.rowsTop && rowY + TOGGLE_HEIGHT <= this.rowsBottom;
            toggle.active = toggle.visible;
            toggle.setMessage(Component.translatable(
                            enabled ? "screen.kindredspirits.ability_on" : "screen.kindredspirits.ability_off")
                    .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.minecraft == null
                || this.minecraft.player == null
                || this.companion.isRemoved()
                || !KindredNetworking.canEditAbilities(this.minecraft.player, this.companion)) {
            this.onClose();
            return;
        }

        this.updateToggles();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        this.scroll = Math.clamp((int) Math.round(this.scroll - scrollY * ROW_HEIGHT), 0, this.maxScroll());
        this.updateToggles();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = this.contentWidth + PAD * 2;
        graphics.fill(0, 0, this.width, this.height, COLOUR_BACKDROP);
        graphics.fill(
                this.left - 1, this.top - 1, this.left + panelWidth + 1, this.top + this.panelHeight + 1, COLOUR_EDGE);
        graphics.fill(this.left, this.top, this.left + panelWidth, this.top + this.panelHeight, COLOUR_PANEL);
        graphics.fill(this.left, this.top, this.left + panelWidth, this.top + 2, COLOUR_ACCENT);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int x = this.left + PAD;
        graphics.text(this.font, this.companion.getDisplayName(), x, this.top + TITLE_Y, COLOUR_TITLE);
        String key = this.statusKey();
        graphics.textWithWordWrap(
                this.font,
                status(key),
                x,
                this.top + STATUS_Y,
                this.contentWidth,
                key.equals("idle") ? COLOUR_LABEL : COLOUR_WORKING);

        boolean inside =
                mouseX >= x && mouseX < x + this.contentWidth && mouseY >= this.rowsTop && mouseY < this.rowsBottom;
        int requirementRight = x + this.contentWidth - TOGGLE_WIDTH - COLUMN_GAP;
        int textY = (ROW_HEIGHT - TEXT_HEIGHT) / 2;
        int y = this.rowsTop - this.scroll;

        graphics.enableScissor(x, this.rowsTop, x + this.contentWidth, this.rowsBottom);
        for (Unlock unlock : this.unlocks) {
            CompanionAbility ability = unlock.ability();
            boolean enabled = this.unlocked(unlock) && !this.companion.isAbilityDisabled(ability);

            graphics.text(this.font, ability.displayName(), x, y + textY, enabled ? COLOUR_VALUE : COLOUR_LABEL);
            Component requirement = this.requirement(unlock);
            graphics.text(
                    this.font, requirement, requirementRight - this.font.width(requirement), y + textY, COLOUR_LABEL);

            if (inside && mouseX < requirementRight && mouseY >= y && mouseY < y + ROW_HEIGHT) {
                graphics.setTooltipForNextFrame(
                        this.font, this.font.split(ability.description(), TOOLTIP_WIDTH), mouseX, mouseY);
            }

            y += ROW_HEIGHT;
        }
        graphics.disableScissor();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
