package com.breakinblocks.kindredspirits.client.screen;

import com.breakinblocks.kindredspirits.menu.KindredStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class KindredStorageScreen extends AbstractContainerScreen<KindredStorageMenu> {
    private static final ResourceLocation CONTAINER_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int COLOUR_LOCKED = 0x80311D3D;

    private final int rows;

    public KindredStorageScreen(KindredStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.rows = menu.getRowCount();
        this.imageWidth = 176;
        this.imageHeight = 114 + this.rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
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
