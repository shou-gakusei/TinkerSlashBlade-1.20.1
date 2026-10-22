package com.whfwtf.tsb.client;

import com.whfwtf.tsb.TSB;
import com.whfwtf.tsb.TsbMaterialColors;
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
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

/**
 * 客户端：刀匠台界面绑定 + 部件的材料染色。
 *
 * <p>部件贴图是「白膜」（纯灰阶明暗），这里用 {@code ItemColor} 按材料色相乘，
 * 因此同一张贴图能出各材料颜色——和参考模组运行时混色是同一个思路。
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

    /**
     * 白膜 × 材料色。
     *
     * <p>取色顺序（任何时候都能给出颜色，不依赖客户端资源加载是否成功）：
     * <ol>
     *   <li>{@link TsbMaterialColors} 静态表：TAIGA 37 种 + 匠魂自带 + 原版映射，全都有本色；</li>
     *   <li>{@link TsbMaterials#color(String)} 老表（TAIGA）；</li>
     *   <li>匠魂客户端的 {@link MaterialRenderInfo}（只是补充，取不到也不影响上面两步）；</li>
     *   <li>都没有才返回白色。</li>
     * </ol>
     */
    private static int partColor(ItemStack stack, int tintIndex) {
        MaterialVariantId material = null;
        if (stack.getItem() instanceof IMaterialItem mi) {
            material = mi.getMaterial(stack);
        }
        // 材料 id：匠魂的 MaterialVariantId（如 tconstruct:iron）与本 mod 存的 NBT 两条途径
        String id = material != null ? material.getId().toString() : "";
        if (id.isEmpty()) {
            id = TsbParts.materialOf(stack);
        }
        if (id.isEmpty()) {
            return 0xFFFFFFFF;
        }
        // 1) 静态颜色表（最全、最可靠）
        int color = TsbMaterialColors.colorOf(id);
        if (color >= 0) {
            return color;
        }
        // 2) TAIGA 老表
        color = TsbMaterials.color(id);
        if (color != 0xAAAAAA) {
            return 0xFF000000 | color;
        }
        // 3) 匠魂客户端渲染信息（补充手段，取不到就跳过）
        if (material != null) {
            color = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material)
                    .map(MaterialRenderInfo::vertexColor)
                    .orElse(-1);
            if (color >= 0) {
                return color;
            }
        }
        // 4) 兜底白色
        return 0xFFFFFFFF;
    }
}
