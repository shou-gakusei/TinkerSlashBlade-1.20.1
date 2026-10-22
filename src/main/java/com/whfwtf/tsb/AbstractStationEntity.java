package com.whfwtf.tsb;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 刀匠台（锻造台 / 装配台）共用的方块实体：一个 {@link Container} 加一份持久化。
 *
 * <p>最后一个格子是「预览产物」格，只出不进。
 */
public abstract class AbstractStationEntity extends BlockEntity implements Container {

    protected final NonNullList<ItemStack> items;

    protected AbstractStationEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inputSlots) {
        super(type, pos, state);
        this.items = NonNullList.withSize(inputSlots + 1, ItemStack.EMPTY);
    }

    /** 正在刷新产物格（防止 setChanged → 重算 → setItem → setChanged 无限递归）。 */
    private boolean refreshing;

    /** 由子类实现：按当前输入算出产物。 */
    protected abstract ItemStack computeResult();

    /**
     * 每次输入格变动都会走这里（{@code Slot.setChanged()} → {@code Container.setChanged()}），
     * 顺便把产物格刷一遍——原来的实现只算了打开界面那一次，所以摆满材料也没产物。
     */
    @Override
    public void setChanged() {
        super.setChanged();
        refreshResult();
    }

    /** 重算产物格。 */
    public void refreshResult() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        try {
            ItemStack result = computeResult();
            if (!ItemStack.matches(result, getItem(resultSlot()))) {
                items.set(resultSlot(), result);
                super.setChanged();
            }
        } finally {
            refreshing = false;
        }
    }

    /** 输入格数量（不含产物格）。 */
    public int inputSlots() {
        return items.size() - 1;
    }

    public int resultSlot() {
        return items.size() - 1;
    }

    // ------------------------------------------------------------------ Container

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(items, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    /** 产物格不允许玩家塞东西。 */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < inputSlots();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
    }
}
