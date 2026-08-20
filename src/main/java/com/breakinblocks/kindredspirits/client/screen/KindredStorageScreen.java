package com.breakinblocks.kindredspirits.client.screen;

import com.breakinblocks.kindredspirits.menu.KindredStorageMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class KindredStorageScreen extends AbstractContainerScreen<KindredStorageMenu> {
    private static final Identifier CONTAINER_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int COLOUR_LOCKED = 0x80311D3D;

    private final int rows;

    public KindredStorageScreen(KindredStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 114 + menu.getRowCount() * 18);
        this.rows = menu.getRowCount();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        int x0 = (this.width - this.imageWidth) / 2;
        int y0 = (this.height - this.imageHeight) / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x0, y0,
                0.0f, 0.0f, this.imageWidth, this.rows * 18 + 17, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x0, y0 + this.rows * 18 + 17,
                0.0f, 126.0f, this.imageWidth, 96, 256, 256);

        for (int index = this.menu.accessibleSlots(); index < this.rows * 9; index++) {
            int slotX = x0 + 8 + (index % 9) * 18;
            int slotY = y0 + 18 + (index / 9) * 18;
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, COLOUR_LOCKED);
        }
    }
}
