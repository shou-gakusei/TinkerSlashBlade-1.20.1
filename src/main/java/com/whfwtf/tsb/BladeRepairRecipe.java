package com.whfwtf.tsb;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.RegistryAccess;
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
 * 刀匠研磨：刀锻冶匠魂的刀 + 同材料匠魂锭 → 修复 25%（每锭）。
 *
 * <p>拔刀剑的耐久存在 BladeState 里，原版铁砧的修复路径写的是物品 NBT 的 Damage，
 * 对本 mod 的刀无效，所以这里单独给一条合成修复路线：把刀和它自己的材料锭一起放进工作台。
 */
public class BladeRepairRecipe extends CustomRecipe {

    /** 每锭回复的最大耐久的比例。 */
    private static final float RESTORE_PER_INGOT = 0.25F;
    private static final int MAX_INGOTS = 3;

    public BladeRepairRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private record Plan(ItemStack blade, String material, int ingots) {}

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
            if (stack.getItem() instanceof TsbBladeItem) {
                if (!blade.isEmpty()) {
                    return null;
                }
                blade = stack;
                material = TsbItems.materialOf(stack);
                if (material.isEmpty()) {
                    return null;
                }
                continue;
            }
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null || !"taiga".equals(id.getNamespace()) || !id.getPath().endsWith("_ingot")) {
                return null;
            }
            String mat = id.getPath().substring(0, id.getPath().length() - "_ingot".length());
            if (!mat.equals(TsbMaterials.path(material))) {
                return null;
            }
            ingots += stack.getCount();
        }

        if (blade.isEmpty() || ingots < 1 || ingots > MAX_INGOTS) {
            return null;
        }
        // 满耐久就不用修了，避免「修了个寂寞」还吃材料
        if (blade.getDamageValue() <= 0) {
            return null;
        }
        return new Plan(blade, material, ingots);
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
        return TsbAssembly.repair(plan.blade(), plan.ingots());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TsbRecipes.BLADE_REPAIR.get();
    }
}
