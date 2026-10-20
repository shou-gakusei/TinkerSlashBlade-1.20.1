package com.whfwtf.tsb;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 锻造台界面：刀胚槽 + 三个材料槽 + 产物槽。 */
public class ForgeMenu extends AbstractContainerMenu {

    private final ForgeBlockEntity entity;

    public ForgeMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, (ForgeBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ForgeMenu(int id, Inventory inventory, ForgeBlockEntity entity) {
        super(TsbMenus.BLADE_FORGE.get(), id);
        this.entity = entity;

        addSlot(new Slot(entity, 0, 26, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return TsbItems.isBlank(stack);
            }
        });
        for (int i = 0; i < 3; i++) {
            addSlot(new Slot(entity, 1 + i, 62 + i * 18, 26) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return TsbAssembly.materialOfIngot(stack) != null;
                }
            });
        }
        addSlot(new Slot(entity, entity.resultSlot(), 140, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                entity.consumeInputs();
                ForgeMenu.this.slotsChanged(entity);
                super.onTake(player, stack);
            }
        });

        // 玩家背包 + 快捷栏
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
        } else {
            // 从背包往输入槽塞：先刀胚，再材料锭
            if (!moveItemStackTo(stack, 0, 1, false)
                    && !moveItemStackTo(stack, 1, inputs, false)) {
                return ItemStack.EMPTY;
            }
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
