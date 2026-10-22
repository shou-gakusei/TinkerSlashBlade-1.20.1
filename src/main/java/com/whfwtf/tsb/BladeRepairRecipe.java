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
import java.util.HashSet;
import java.util.Set;

/**
 * 刀匠研磨：刀锻冶匠魂的刀 + 同材料匠魂锭 → 修复 25%（每锭）。
 *
 * <p>拔刀剑的耐久存在 BladeState 里，原版铁砧的修复路径写的是物品 NBT 的 Damage，
 * 对本 mod 的刀无效，所以这里单独给一条合成修复路线：把刀和它自己的材料锭一起放进工作台。
 *
 * <p>「同材料」指六槽里任意一件的材料（刀身 / 副刀身 / 主柄 / 副柄 / 刀鞘 / 副刀鞘），
 * 即做这把刀的任何一种材料锭都能修；修复同时清除刀的 broken 状态。
 */
public class BladeRepairRecipe extends CustomRecipe {

    /** 每锭回复的最大耐久的比例。 */
    private static final float RESTORE_PER_INGOT = 0.25F;
    private static final int MAX_INGOTS = 3;

    private static final String[] BLADE_MATERIAL_KEYS = {
            TsbItems.TAG_MATERIAL, TsbItems.TAG_BLADE2, TsbItems.TAG_HANDLE,
            TsbItems.TAG_HANDLE2, TsbItems.TAG_SAYA, TsbItems.TAG_SAYA2
    };

    public BladeRepairRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private record Plan(ItemStack blade, int ingots) {}

    /** 这把刀的六槽材料路径集合。 */
    private static Set<String> bladeMaterials(ItemStack blade) {
        Set<String> set = new HashSet<>();
        if (blade.hasTag()) {
            for (String key : BLADE_MATERIAL_KEYS) {
                String mat = blade.getTag().getString(key);
                if (!mat.isEmpty()) {
                    set.add(TsbMaterials.path(mat));
                }
            }
        }
        return set;
    }

    @Nullable
    private static Plan plan(CraftingContainer inv) {
        ItemStack blade = ItemStack.EMPTY;
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
                continue;
            }
            String mat = TsbAssembly.materialOfIngot(stack);
            if (mat == null) {
                return null;
            }
            Set<String> materials = bladeMaterials(blade);
            if (materials.isEmpty() || !materials.contains(TsbMaterials.path(mat))) {
                return null;
            }
            ingots += stack.getCount();
        }

        if (blade.isEmpty() || ingots < 1 || ingots > MAX_INGOTS) {
            return null;
        }
        // 满耐久且没坏就不用修了，避免「修了个寂寞」还吃材料
        ItemStack finalBlade = blade;
        if (blade.getDamageValue() <= 0
                && !blade.getCapability(ItemSlashBlade.BLADESTATE).resolve()
                    .map(mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState::isBroken)
                    .orElse(false)) {
            return null;
        }
        return new Plan(finalBlade, ingots);
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
