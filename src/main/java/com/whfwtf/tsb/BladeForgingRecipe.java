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
 * 刀匠锻造：刀胚 + 1~3 份同种匠魂材料锭 → 一把数值由材料决定的拔刀剑。
 *
 * <p>材料取 {@code taiga:<material>_ingot}，数值读 {@link TsbMaterial}（来自匠魂材料表），
 * 结果写入拔刀剑的 BladeState，因此后续 Refine / 耀魂 / SA 全部按拔刀剑原版逻辑走。
 *
 * <p>多放材料锭只提升耐久（每多 1 份 +10%），不改变特性。
 */
public class BladeForgingRecipe extends CustomRecipe {

    /** 最多吃几份材料锭。 */
    private static final int MAX_INGOTS = 3;

    public BladeForgingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    /**
     * 若该物品是可识别的材料锭，返回材料完整 ID，否则 null。
     *
     * <p>不止 TAIGA：原版（{@code minecraft:iron_ingot}）与匠魂（{@code tconstruct:manyullyn_ingot}）
     * 的锭也认——按「锭命名空间 + 同名材料 → taiga: → tconstruct:」顺序试探，
     * {@link TsbMaterials#resolve} 认得出（有匠魂刀身属性）就算数。
     */
    @Nullable
    private static String materialOf(ItemStack stack) {
        return TsbAssembly.materialOfIngot(stack);
    }

    /** 一次配方的解析结果。 */
    private record Plan(TsbBladeVariant variant, String material, int ingots) {}

    @Nullable
    private static Plan plan(CraftingContainer inv) {
        TsbBladeVariant variant = null;
        String material = null;
        int ingots = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            TsbBladeVariant blankVariant = null;
            for (TsbBladeVariant candidate : TsbBladeVariant.values()) {
                if (stack.is(TsbItems.blank(candidate))) {
                    blankVariant = candidate;
                    break;
                }
            }
            if (blankVariant != null) {
                if (variant != null || stack.getCount() != 1) {
                    return null;
                }
                variant = blankVariant;
                continue;
            }

            String mat = materialOf(stack);
            if (mat == null || (material != null && !TsbMaterials.path(material).equals(TsbMaterials.path(mat)))) {
                return null;
            }
            material = mat;
            ingots += stack.getCount();
        }

        if (variant == null || material == null || ingots < 1 || ingots > MAX_INGOTS) {
            return null;
        }
        return new Plan(variant, material, ingots);
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
        return TsbAssembly.forge(plan.variant(), plan.material(), plan.ingots());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TsbRecipes.BLADE_FORGING.get();
    }
}
