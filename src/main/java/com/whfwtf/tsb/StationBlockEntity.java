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

/** 装配台：刀身 + 刀柄 + 刀鞘 → 刀。 */
public class StationBlockEntity extends AbstractStationEntity implements MenuProvider {

    public static final int INPUT_SLOTS = 3; // 0 = 刀身，1 = 刀柄，2 = 刀鞘

    public StationBlockEntity(BlockPos pos, BlockState state) {
        super(TsbBlockEntities.BLADE_STATION.get(), pos, state, INPUT_SLOTS);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tinkersslashblade.blade_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new StationMenu(id, inventory, this);
    }

    @Override
    protected ItemStack computeResult() {
        if (!TsbParts.isPart(getItem(0), TsbPartType.BLADE)
                || !TsbParts.isPart(getItem(1), TsbPartType.HANDLE)
                || !TsbParts.isPart(getItem(2), TsbPartType.SAYA)) {
            return ItemStack.EMPTY;
        }
        return TsbAssembly.assemble(getItem(0), getItem(1), getItem(2));
    }

    public void consumeInputs() {
        removeItem(0, 1);
        removeItem(1, 1);
        removeItem(2, 1);
    }
}
