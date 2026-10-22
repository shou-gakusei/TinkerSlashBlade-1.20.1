package com.whfwtf.tsb.client;

import com.whfwtf.tsb.TSB;
import com.whfwtf.tsb.TsbMaterials;
import com.whfwtf.tsb.TsbMenus;
import com.whfwtf.tsb.TsbPartType;
import com.whfwtf.tsb.TsbParts;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端：刀匠台界面绑定 + 部件的材料染色。
 *
 * <p>部件贴图是「白膜」（纯灰阶明暗），这里用 {@code ItemColor} 按材料色相乘，
 * 因此同一张贴图能出 37 种颜色——和参考模组运行时混色是同一个思路。
 */
@Mod.EventBusSubscriber(modid = TSB.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TsbClientSetup {

    private TsbClientSetup() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(TsbMenus.BLADE_FORGE.get(), ForgeScreen::new);
            MenuScreens.register(TsbMenus.BLADE_STATION.get(), StationScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(TsbClientSetup::partColor,
                TsbParts.item(TsbPartType.BLADE),
                TsbParts.item(TsbPartType.HANDLE),
                TsbParts.item(TsbPartType.SAYA));
    }

    /** 白膜 × 材料色（TAIGA 材料用本 mod 的颜色，其它匠魂材料用灰色）。 */
    private static int partColor(ItemStack stack, int tintIndex) {
        String material = TsbParts.materialOf(stack);
        return material.isEmpty() ? 0xFFFFFFFF : 0xFF000000 | TsbMaterials.color(material);
    }
}
