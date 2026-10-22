package com.whfwtf.tsb;

import com.mojang.logging.LogUtils;
import com.whfwtf.tsb.client.TsbBooks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * 刀锻冶匠魂（Tinker's SlashBlade）1.20.1 移植。
 *
 * <p>基于 1.12.2 的 TinkerSlashBlade（cn.mmf.slashblade_tic）重写：
 * 用匠魂的材料体系决定拔刀剑的数值，刀本体仍是真正的拔刀剑（BladeState 驱动）。
 *
 * <p>依赖：Minecraft 1.20.1 / Forge 47.x / Tinkers' Construct 3 / SlashBlade: Resharped / TAIGA。
 */
@Mod(TSB.MODID)
public class TSB {

    public static final String MODID = "tinkersslashblade";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tinkersslashblade"))
                    .icon(() -> new ItemStack(TsbItems.blade(TsbBladeVariant.UCHIGATANA)))
                    .displayItems((params, output) -> TsbItems.appendToTab(output))
                    .build());

    public TSB(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        if (!ModList.get().isLoaded("slashblade")) {
            LOGGER.error("未检测到拔刀剑（slashblade），刀锻冶匠魂已禁用");
            return;
        }
        if (!ModList.get().isLoaded("taiga")) {
            LOGGER.error("未检测到 TAIGA（taiga），刀锻冶匠魂已禁用");
            return;
        }

        TsbItems.ITEMS.register(modBus);
        TsbParts.ITEMS.register(modBus);
        TsbBlocks.BLOCKS.register(modBus);
        TsbBlocks.ITEMS.register(modBus);
        TsbBlockEntities.BLOCK_ENTITIES.register(modBus);
        TsbMenus.MENUS.register(modBus);
        TsbRecipes.SERIALIZERS.register(modBus);
        TABS.register(modBus);

        // 客户端：注册《刀匠标准手册》（Mantle 书）。与 TConstruct 的 TinkerBook 同时机（构造期）。
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> TsbBooks::register);

        LOGGER.info("Tinker's SlashBlade 1.20.1 loaded");
    }
}
