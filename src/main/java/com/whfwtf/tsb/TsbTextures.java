package com.whfwtf.tsb;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * 刀身贴图路径约定。
 *
 * <p>每种材料一张刃纹贴图：{@code tinkersslashblade:model/blade/<material>.png}，
 * 由 {@code tools/gen_blade_textures.py} 从拔刀剑默认刀身贴图按材料色漂染生成（37 张）。
 *
 * <p>颜色只染「低饱和度的钢面」，刀鞘 / 刀柄的暖色部分保持原样，所以不同材料一眼能看出区别。
 */
public final class TsbTextures {

    private TsbTextures() {}

    @Nullable
    public static ResourceLocation forMaterial(String material) {
        return ResourceLocation.tryParse(TSB.MODID + ":model/blade/" + material + ".png");
    }
}
