package com.whfwtf.tsb.compat.jei;

import com.whfwtf.tsb.TsbBladeVariant;
import com.whfwtf.tsb.TsbMaterial;

/**
 * JEI 里展示的「刀匠锻造」配方条目：一种材料 × 一种刀型。
 *
 * <p>真正的锻造逻辑在 {@code BladeForgingRecipe}（CustomRecipe，按工作台里的实际物品动态算数值），
 * JEI 无法直接读它，所以这里把材料表展开成一条条可浏览的展示用配方。
 */
public record ForgingJeiRecipe(TsbBladeVariant variant, TsbMaterial.Entry material) {

    /** 材料锭。 */
    public net.minecraft.world.item.ItemStack ingot(int count) {
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(net.minecraft.resources.ResourceLocation.tryParse("taiga:" + material.id() + "_ingot"));
        return item == null
                ? net.minecraft.world.item.ItemStack.EMPTY
                : new net.minecraft.world.item.ItemStack(item, count);
    }
}
