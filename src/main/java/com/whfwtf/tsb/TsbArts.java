package com.whfwtf.tsb;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * 刀技（SlashArts）分配。
 *
 * <p>拔刀剑本体只认「附了魔的刀」放 SA（{@code SwordType.ENCHANTED}），所以这里做的是
 * <b>给每把刀预设一个刀技</b>：材料不同 → 刀技不同，玩家把它附魔（+自定义名，即为「妖刀」）后就能放出来。
 *
 * <p>白鞘是素刀（{@code slashblade:none}），研磨成打刀后刀技才「显现」。
 */
public final class TsbArts {

    public static final ResourceLocation NONE = id("none");

    private TsbArts() {}

    @Nullable
    private static ResourceLocation id(String path) {
        return ResourceLocation.tryParse("slashblade:" + path);
    }

    /**
     * 按材料特性决定刀技：
     * 火系 → 横扫；穿刺 → 突刺；沉重 → 圆斩；噬魂 → 虚空斩；狂战/不稳 → 樱花落；
     * 锯齿/强化/抑界 → 竖直斩；其余 → 断罪。
     */
    public static ResourceLocation forMaterial(TsbMaterial.Entry entry) {
        ResourceLocation arts = switch (TsbTrait.byId(entry.trait())) {
            case SEARING, OVERBURN, FLAMESTANCE -> id("drive_horizontal");
            case PIERCE -> id("piercing");
            case HEAVY -> id("circle_slash");
            case SOUL_EATER -> id("void_slash");
            case BERSERKER, UNSTABLE -> id("sakura_end");
            case JAGGED, ENHANCED, ENDERFERENCE -> id("drive_vertical");
            case LIGHTWEIGHT, LOOTING, DUCTILE, GLOWING -> id("wave_edge");
            default -> id("judgement_cut");
        };
        return arts != null ? arts : NONE;
    }
}
