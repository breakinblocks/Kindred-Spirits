package com.breakinblocks.kindredspirits.client.screen;

import com.breakinblocks.kindredspirits.menu.KindredStorageMenu;
import com.breakinblocks.kindredspirits.net.KindredNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class KindredStorageScreen extends AbstractContainerScreen<KindredStorageMenu> {
    private static final ResourceLocation CONTAINER_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int COLOUR_LOCKED = 0x80311D3D;
    private static final int COLOUR_LABEL = 0xFF404040;
    private static final int MOUNT_BUTTON_HEIGHT = 12;
    private static final int MOUNT_BUTTON_PADDING = 8;
    private static final int MOUNT_BUTTON_RIGHT = 7;
    private static final int MOUNT_BUTTON_TOP = 4;

    private final int rows;
    private int titleWidth;

    public KindredStorageScreen(KindredStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.rows = menu.getRowCount();
        this.imageWidth = 176;
        this.imageHeight = 114 + this.rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleWidth = this.imageWidth - this.titleLabelX * 2;

        if (this.menu.mountable()) {
            Component label = Component.translatable("screen.kindredspirits.mount");
            int width = this.font.width(label) + MOUNT_BUTTON_PADDING;
            this.titleWidth = this.imageWidth - MOUNT_BUTTON_RIGHT - width - MOUNT_BUTTON_PADDING - this.titleLabelX;
            this.addRenderableWidget(Button.builder(
                            label, button -> PacketDistributor.sendToServer(KindredNetworking.MountPayload.INSTANCE))
                    .bounds(
                            this.leftPos + this.imageWidth - MOUNT_BUTTON_RIGHT - width,
                            this.topPos + MOUNT_BUTTON_TOP,
                            width,
                            MOUNT_BUTTON_HEIGHT)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component title = this.title;
        if (this.font.width(title) > this.titleWidth) {
            String ellipsis = "...";
            title = Component.literal(
                    this.font.plainSubstrByWidth(title.getString(), this.titleWidth - this.font.width(ellipsis))
                            + ellipsis);
        }
        graphics.drawString(this.font, title, this.titleLabelX, this.titleLabelY, COLOUR_LABEL, false);
        graphics.drawString(
                this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, COLOUR_LABEL, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x0 = (this.width - this.imageWidth) / 2;
        int y0 = (this.height - this.imageHeight) / 2;

        graphics.blit(CONTAINER_BACKGROUND, x0, y0, 0, 0, this.imageWidth, this.rows * 18 + 17);
        graphics.blit(CONTAINER_BACKGROUND, x0, y0 + this.rows * 18 + 17, 0, 126, this.imageWidth, 96);

        for (int index = this.menu.accessibleSlots(); index < this.rows * 9; index++) {
            int slotX = x0 + 8 + (index % 9) * 18;
            int slotY = y0 + 18 + (index / 9) * 18;
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, COLOUR_LOCKED);
        }
    }
}
