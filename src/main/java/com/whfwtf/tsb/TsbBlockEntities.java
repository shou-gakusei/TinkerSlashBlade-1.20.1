package com.whfwtf.tsb;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 刀匠台方块实体的注册。 */
public final class TsbBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TSB.MODID);

    public static final RegistryObject<BlockEntityType<ForgeBlockEntity>> BLADE_FORGE =
            BLOCK_ENTITIES.register("blade_forge", () -> BlockEntityType.Builder
                    .of(ForgeBlockEntity::new, TsbBlocks.BLADE_FORGE.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<StationBlockEntity>> BLADE_STATION =
            BLOCK_ENTITIES.register("blade_station", () -> BlockEntityType.Builder
                    .of(StationBlockEntity::new, TsbBlocks.BLADE_STATION.get())
                    .build(null));

    private TsbBlockEntities() {}
}
