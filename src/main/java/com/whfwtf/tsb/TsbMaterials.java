package com.whfwtf.tsb;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;

import javax.annotation.Nullable;

/**
 * 材料解析：TAIGA 的 37 种材料走本 mod 的表（有刀性/材料色），
 * 其它匠魂材料（木头、石头、燧石……）直接用匠魂注册表里的刀身属性兜底。
 *
 * <p>这样「部件」才能真正吃匠魂的材料体系：浇筑什么流体、部件制造台放什么材料，
 * 出来的刀就用什么材料的数值。
 */
public final class TsbMaterials {

    private TsbMaterials() {}

    /** {@code taiga:nihilite} / {@code nihilite} → {@code nihilite}。 */
    public static String path(String idOrPath) {
        int idx = idOrPath.indexOf(':');
        return idx < 0 ? idOrPath : idOrPath.substring(idx + 1);
    }

    /** 材料 id（可能不带命名空间）。 */
    public static MaterialId materialId(String idOrPath) {
        return MaterialId.tryParse(idOrPath.contains(":") ? idOrPath : "taiga:" + idOrPath);
    }

    /**
     * 解析成一条可用于计算的材料数据；解析不出来返回 null（例如该材料没有刀身属性）。
     */
    @Nullable
    public static TsbMaterial.Entry resolve(String idOrPath) {
        if (idOrPath == null || idOrPath.isEmpty()) {
            return null;
        }
        TsbMaterial.Entry known = TsbMaterial.ALL.get(path(idOrPath));
        if (known != null) {
            return known;
        }
        MaterialId id = materialId(idOrPath);
        if (id == null) {
            return null;
        }
        IMaterial material = MaterialRegistry.getMaterial(id);
        if (material == null || material == IMaterial.UNKNOWN) {
            return null;
        }
        HeadMaterialStats stats = MaterialRegistry.getInstance()
                .getMaterialStats(id, HeadMaterialStats.ID)
                .filter(HeadMaterialStats.class::isInstance)
                .map(HeadMaterialStats.class::cast)
                .orElse(null);
        if (stats == null) {
            return null;
        }
        return new TsbMaterial.Entry(path(idOrPath), stats.durability(), stats.attack(),
                "", 0xAAAAAA, material.getTier());
    }

    /** 材料显示名：TAIGA 的材料用「XX锭」的名字，其它材料用匠魂自带的材料名。 */
    public static Component displayName(String idOrPath) {
        String path = path(idOrPath);
        if (TsbMaterial.ALL.containsKey(path)) {
            return Component.translatable("item.taiga." + path + "_ingot");
        }
        MaterialId id = materialId(idOrPath);
        IMaterial material = id == null ? null : MaterialRegistry.getMaterial(id);
        return material == null ? Component.literal(path)
                : Component.translatable("material." + id.getNamespace() + "." + id.getPath());
    }

    /** 材料色（拿不到就用灰色）。 */
    public static int color(String idOrPath) {
        TsbMaterial.Entry entry = TsbMaterial.ALL.get(path(idOrPath));
        return entry == null ? 0xAAAAAA : entry.color();
    }

    /** 材料阶级（拿不到就 1）。 */
    public static int tier(String idOrPath) {
        TsbMaterial.Entry entry = TsbMaterial.ALL.get(path(idOrPath));
        return entry == null ? 1 : entry.tier();
    }

    /** 是否本 mod 认识的 TAIGA 材料（有刀性）。 */
    public static boolean isTaiga(String idOrPath) {
        return TsbMaterial.ALL.containsKey(path(idOrPath));
    }

    /** 材料名的颜色样式。 */
    public static ChatFormatting tierColor(String idOrPath) {
        return switch (Math.max(1, Math.min(4, tier(idOrPath)))) {
            case 4 -> ChatFormatting.LIGHT_PURPLE;
            case 3 -> ChatFormatting.AQUA;
            case 2 -> ChatFormatting.GREEN;
            default -> ChatFormatting.WHITE;
        };
    }

    /** 供 tooltip 用的一行材料名。 */
    public static Component coloredName(String idOrPath) {
        return displayName(idOrPath).copy().withStyle(style -> style.withColor(color(idOrPath)));
    }

    /** 材料对应的刀性（只有 TAIGA 材料有）。 */
    public static TsbTrait trait(String idOrPath) {
        TsbMaterial.Entry entry = TsbMaterial.ALL.get(path(idOrPath));
        return entry == null ? TsbTrait.NONE : TsbTrait.byId(entry.trait());
    }

    /** 刀柄材料的耐久倍率（匠魂 HandleMaterialStats），拿不到就用 1.0。 */
    public static float handleDurability(String idOrPath) {
        return handleStat(idOrPath, HandleMaterialStats::durability, 1.0F);
    }

    /** 刀柄材料的攻击倍率。 */
    public static float handleAttack(String idOrPath) {
        return handleStat(idOrPath, HandleMaterialStats::attackDamage, 1.0F);
    }

    /** 刀柄材料的攻击速度倍率。 */
    public static float handleMeleeSpeed(String idOrPath) {
        return handleStat(idOrPath, HandleMaterialStats::meleeSpeed, 1.0F);
    }

    private static float handleStat(String idOrPath,
                                    java.util.function.Function<HandleMaterialStats, Float> getter,
                                    float fallback) {
        MaterialId id = materialId(idOrPath);
        if (id == null) {
            return fallback;
        }
        return MaterialRegistry.getInstance()
                .getMaterialStats(id, HandleMaterialStats.ID)
                .filter(HandleMaterialStats.class::isInstance)
                .map(HandleMaterialStats.class::cast)
                .map(getter)
                .orElse(fallback);
    }

    /** 材料在物品上的资源路径（TAIGA 材料的锭 id）。 */
    public static ResourceLocation taigaIngot(String idOrPath) {
        return ResourceLocation.tryParse("taiga:" + path(idOrPath) + "_ingot");
    }
}
