package com.whfwtf.tsb;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 刀匠台方块的注册。 */
public final class TsbBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, TSB.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TSB.MODID);

    public static final RegistryObject<Block> BLADE_FORGE = register("blade_forge",
            () -> new BladeForgeBlock(properties()));
    public static final RegistryObject<Block> BLADE_STATION = register("blade_station",
            () -> new BladeStationBlock(properties()));

    private TsbBlocks() {}

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0F, 6.0F)
                .sound(SoundType.WOOD)
                .noOcclusion();
    }

    private static RegistryObject<Block> register(String name, java.util.function.Supplier<Block> block) {
        RegistryObject<Block> object = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(object.get(), new Item.Properties()));
        return object;
    }
}
