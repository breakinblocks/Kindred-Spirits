package com.breakinblocks.kindredspirits.menu;

import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionLevels;
import com.breakinblocks.kindredspirits.registry.KindredMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class KindredStorageMenu extends AbstractContainerMenu {
    private static final double REACH_SQR = 64.0;

    private final Container storage;
    private final int rows;
    private final int accessibleSlots;
    private final @Nullable CompanionEntity companion;

    public KindredStorageMenu(
            int containerId,
            Inventory playerInventory,
            Container storage,
            int rows,
            int accessibleSlots,
            @Nullable CompanionEntity companion) {
        super(KindredMenus.COMPANION_STORAGE.get(), containerId);
        this.storage = storage;
        this.rows = rows;
        this.accessibleSlots = accessibleSlots;
        this.companion = companion;
        storage.startOpen(playerInventory.player);

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 9; column++) {
                int index = column + row * 9;
                this.addSlot(new StorageSlot(storage, index, 8 + column * 18, 18 + row * 18, index < accessibleSlots));
            }
        }

        int inventoryY = 18 + rows * 18 + 13;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, inventoryY + 58));
        }
    }

    public static KindredStorageMenu client(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        int rows = data.readVarInt();
        int accessible = data.readVarInt();
        return new KindredStorageMenu(
                containerId,
                playerInventory,
                new SimpleContainer(CompanionLevels.MAX_STORAGE_SLOTS),
                rows,
                accessible,
                null);
    }

    public int getRowCount() {
        return this.rows;
    }

    public int accessibleSlots() {
        return this.accessibleSlots;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            int containerSlots = this.rows * 9;

            if (slotIndex < containerSlots) {
                if (!this.moveItemStackTo(current, containerSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, Math.min(this.accessibleSlots, containerSlots), false)) {
                return ItemStack.EMPTY;
            }

            if (current.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.companion == null) {
            return true;
        }

        return this.companion.isAlive()
                && this.companion.isBonded()
                && player.distanceToSqr(this.companion) <= REACH_SQR;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.storage.stopOpen(player);
    }

    private static final class StorageSlot extends Slot {
        private final boolean accessible;

        private StorageSlot(Container container, int index, int x, int y, boolean accessible) {
            super(container, index, x, y);
            this.accessible = accessible;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.accessible;
        }
    }
}
