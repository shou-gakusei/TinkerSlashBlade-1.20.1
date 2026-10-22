package com.whfwtf.tsb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 拔刀剑锻造台：刀胚 + 材料锭 → 刀。 */
public class BladeForgeBlock extends AbstractStationBlock {

    public BladeForgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ForgeBlockEntity(pos, state);
    }
}
