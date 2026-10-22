package com.whfwtf.tsb;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * 刀匠研磨：白鞘 + 3 份同材料匠魂锭 → 同材料的打刀。
 *
 * <p>白鞘是素刀（耐久 ×0.6、刀性只发挥一半、无刀技），把它的材料锭再喂给它是「开刃 + 研磨」：
 * 耐久回满、刀性全额、刀技显现。已有的附魔 / 耀魂 / 锻造数 / 击杀数全部保留，耐久按比例换算。
 *
 * <p>这是「白鞘 → 打刀」的妖刀化前置；再自己附魔（+本 mod 自带的刀名）就是拔刀剑本体的「妖刀」。
 */
public class BladeUpgradeRecipe extends CustomRecipe {

    /** 研磨需要的材料锭数量。 */
    private static final int REQUIRED_INGOTS = 3;

    public BladeUpgradeRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private record Plan(ItemStack blade, String material) {}

    @Nullable
    private static Plan plan(CraftingContainer inv) {
        ItemStack blade = ItemStack.EMPTY;
        String material = null;
        int ingots = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof TsbBladeItem item) {
                if (!blade.isEmpty() || item.variant() != TsbBladeVariant.SHIRASAYA
                        || stack.getCount() != 1) {
                    return null;
                }
                blade = stack;
                material = TsbItems.materialOf(stack);
                if (material.isEmpty() || TsbMaterials.resolve(material) == null) {
                    return null;
                }
                continue;
            }
            String mat = TsbAssembly.materialOfIngot(stack);
            if (mat == null || !TsbMaterials.path(mat).equals(TsbMaterials.path(material))) {
                return null;
            }
            ingots += stack.getCount();
        }

        if (blade.isEmpty() || ingots != REQUIRED_INGOTS) {
            return null;
        }
        return new Plan(blade, material);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        return plan(inv) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        Plan plan = plan(inv);
        if (plan == null) {
            return ItemStack.EMPTY;
        }
        return TsbAssembly.polish(plan.blade(), plan.material(), REQUIRED_INGOTS);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 4;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TsbRecipes.BLADE_UPGRADE.get();
    }
}
