package com.whfwtf.tsb;

import com.whfwtf.tsb.TSB;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 刀锻冶匠魂的配方序列化器注册。 */
public final class TsbRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TSB.MODID);

    /** 刀胚 + 材料锭 → 拔刀剑（JSON: {@code {"type":"tinkersslashblade:tsb_blade_forging"}}）。 */
    public static final RegistryObject<RecipeSerializer<BladeForgingRecipe>> BLADE_FORGING =
            SERIALIZERS.register("tsb_blade_forging",
                    () -> new SimpleCraftingRecipeSerializer<>(BladeForgingRecipe::new));

    /** 刀 + 同材料锭 → 修复（JSON: {@code {"type":"tinkersslashblade:tsb_blade_repair"}}）。 */
    public static final RegistryObject<RecipeSerializer<BladeRepairRecipe>> BLADE_REPAIR =
            SERIALIZERS.register("tsb_blade_repair",
                    () -> new SimpleCraftingRecipeSerializer<>(BladeRepairRecipe::new));

    /** 白鞘 + 3 锭同材料 → 打刀（JSON: {@code {"type":"tinkersslashblade:tsb_blade_upgrade"}}）。 */
    public static final RegistryObject<RecipeSerializer<BladeUpgradeRecipe>> BLADE_UPGRADE =
            SERIALIZERS.register("tsb_blade_upgrade",
                    () -> new SimpleCraftingRecipeSerializer<>(BladeUpgradeRecipe::new));

    /** 工匠砧强化：刀 + 强化卷轴 → Refine 提升（JSON: {@code {"type":"tinkersslashblade:tsb_reinforce"}}）。 */
    public static final RegistryObject<RecipeSerializer<TsbReinforceRecipe>> REINFORCE =
            SERIALIZERS.register("tsb_reinforce", () -> TsbReinforceRecipe.Serializer.INSTANCE);

    /** 工匠砧材料修复：刀 + 六槽任一材料锭 → 修复耐久（JSON: {@code {"type":"tinkersslashblade:tsb_anvil_repair"}}）。 */
    public static final RegistryObject<RecipeSerializer<TsbAnvilRepairRecipe>> ANVIL_REPAIR =
            SERIALIZERS.register("tsb_anvil_repair", () -> TsbAnvilRepairRecipe.Serializer.INSTANCE);

    private TsbRecipes() {}
}
