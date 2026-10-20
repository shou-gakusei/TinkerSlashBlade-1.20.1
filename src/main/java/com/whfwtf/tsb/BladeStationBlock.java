package com.whfwtf.tsb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 拔刀剑装配台：刀身 + 刀柄 + 刀鞘 → 刀。 */
public class BladeStationBlock extends AbstractStationBlock {

    public BladeStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StationBlockEntity(pos, state);
    }
}
