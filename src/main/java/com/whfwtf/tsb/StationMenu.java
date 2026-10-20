package com.whfwtf.tsb;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 装配台界面：左列（刀身 / 刀柄 / 刀鞘）= 打刀老三槽；右列（副刃 / 副鞘）补满即无上大快刀。 */
public class StationMenu extends AbstractContainerMenu {

    /** 输入槽在界面里的坐标（下标与 {@link StationBlockEntity#SLOT_TYPES} 对应）。 */
    private static final int[][] SLOT_POS = {
            {26, 20}, {44, 20}, {26, 40}, {26, 60}, {44, 60}
    };

    private final StationBlockEntity entity;

    public StationMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, (StationBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public StationMenu(int id, Inventory inventory, StationBlockEntity entity) {
        super(TsbMenus.BLADE_STATION.get(), id);
        this.entity = entity;

        for (int i = 0; i < SLOT_POS.length; i++) {
            TsbPartType type = StationBlockEntity.SLOT_TYPES[i];
            int[] pos = SLOT_POS[i];
            addSlot(new Slot(entity, i, pos[0], pos[1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return TsbParts.isPart(stack, type);
                }
            });
        }
        addSlot(new Slot(entity, entity.resultSlot(), 140, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                entity.consumeInputs();
                StationMenu.this.slotsChanged(entity);
                super.onTake(player, stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        // 打开界面时先把产物算一次（方块里本来就有东西的情况）
        slotsChanged(entity);
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == entity) {
            entity.setItem(entity.resultSlot(), entity.computeResult());
        }
        super.slotsChanged(container);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int inputs = entity.inputSlots();
        if (index == entity.resultSlot()) {
            if (!moveItemStackTo(stack, inputs + 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < inputs) {
            if (!moveItemStackTo(stack, inputs + 1, slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, inputs, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return entity.stillValid(player);
    }
}
