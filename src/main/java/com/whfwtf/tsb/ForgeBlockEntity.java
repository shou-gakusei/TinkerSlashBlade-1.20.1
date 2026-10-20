package com.whfwtf.tsb;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** 锻造台：刀胚 + 1~3 锭同材料 → 刀。 */
public class ForgeBlockEntity extends AbstractStationEntity implements MenuProvider {

    public static final int INPUT_SLOTS = 4; // 0 = 刀胚，1~3 = 材料锭

    public ForgeBlockEntity(BlockPos pos, BlockState state) {
        super(TsbBlockEntities.BLADE_FORGE.get(), pos, state, INPUT_SLOTS);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tinkersslashblade.blade_forge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ForgeMenu(id, inventory, this);
    }

    /** 按当前输入算出产物；不合法返回空。 */
    @Override
    protected ItemStack computeResult() {
        TsbBladeVariant variant = null;
        ItemStack blank = getItem(0);
        for (TsbBladeVariant candidate : TsbBladeVariant.values()) {
            if (blank.is(TsbItems.blank(candidate)) && blank.getCount() == 1) {
                variant = candidate;
                break;
            }
        }
        if (variant == null) {
            return ItemStack.EMPTY;
        }

        String material = null;
        int ingots = 0;
        for (int i = 1; i <= 3; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            String mat = TsbAssembly.materialOfIngot(stack);
            if (mat == null || (material != null && !TsbMaterials.path(material).equals(TsbMaterials.path(mat)))) {
                return ItemStack.EMPTY;
            }
            material = mat;
            ingots += stack.getCount();
        }
        if (material == null || ingots < 1 || ingots > 3) {
            return ItemStack.EMPTY;
        }
        return TsbAssembly.forge(variant, material, ingots);
    }

    /** 取走产物时消耗输入（消耗份数与配方一致）。 */
    public void consumeInputs() {
        int ingots = 0;
        for (int i = 1; i <= 3; i++) {
            ingots += getItem(i).getCount();
        }
        removeItem(0, 1);
        int remaining = ingots;
        for (int i = 1; i <= 3 && remaining > 0; i++) {
            int take = Math.min(remaining, getItem(i).getCount());
            removeItem(i, take);
            remaining -= take;
        }
    }
}
