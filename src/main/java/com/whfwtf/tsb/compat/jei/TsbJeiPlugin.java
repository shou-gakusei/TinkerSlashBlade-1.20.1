package com.whfwtf.tsb.compat.jei;

import com.whfwtf.tsb.TSB;
import com.whfwtf.tsb.TsbBladeVariant;
import com.whfwtf.tsb.TsbItems;
import com.whfwtf.tsb.TsbMaterial;
import com.whfwtf.tsb.TsbPartType;
import com.whfwtf.tsb.TsbParts;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 集成：把「刀胚 + 材料锭 → 拔刀剑」这条规则显示出来。
 *
 * <p>只有装了 JEI 才会加载本类（{@code @JeiPlugin} 由 JEI 自己扫描），没装 JEI 不影响任何东西。
 */
@JeiPlugin
public class TsbJeiPlugin implements IModPlugin {

    public static final RecipeType<ForgingJeiRecipe> FORGING =
            RecipeType.create(TSB.MODID, "blade_forging", ForgingJeiRecipe.class);

    /** 金模 + 熔融材料 → 部件。 */
    public static final RecipeType<PartCastingJeiRecipe> PART_CASTING =
            RecipeType.create(TSB.MODID, "part_casting", PartCastingJeiRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.tryParse(TSB.MODID + ":jei");
    }

    // 注意：不要自己给部件注册 subtype interpreter！
    // 部件是匠魂的 ToolPartItem/MaterialItem，TConstruct 已经统一注册过了，
    // 再注册一次 JEI 会抛 IllegalArgumentException 并跳过本插件的后续步骤（分类/配方全丢）。

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        TSB.LOGGER.info("[TSB-JEI] registerCategories 被调用");
        var helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new BladeForgingCategory(helper),
                new PartCastingCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ForgingJeiRecipe> recipes = new ArrayList<>();
        for (TsbMaterial.Entry entry : TsbMaterial.ALL.values()) {
            for (TsbBladeVariant variant : TsbBladeVariant.values()) {
                recipes.add(new ForgingJeiRecipe(variant, entry));
            }
        }
        TSB.LOGGER.info("[TSB-JEI] registerRecipes 被调用，锻造条目 {}", recipes.size());
        registration.addRecipes(FORGING, recipes);

        // 部件浇筑：每种材料 × 每种部件
        List<PartCastingJeiRecipe> casting = new ArrayList<>();
        for (TsbMaterial.Entry entry : TsbMaterial.ALL.values()) {
            for (TsbPartType type : TsbPartType.values()) {
                casting.add(new PartCastingJeiRecipe(type, entry, type.ingots()));
            }
        }
        registration.addRecipes(PART_CASTING, casting);
        TSB.LOGGER.info("[TSB-JEI] 部件浇筑条目 {}", casting.size());
    }

    /** 临时诊断：进世界后把 JEI 里实际持有的配方数量打出来。 */
    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        var manager = runtime.getRecipeManager();
        long forging = manager.createRecipeLookup(FORGING).get().count();
        var casting = manager.createRecipeLookup(
                slimeknights.tconstruct.plugin.jei.TConstructJEIConstants.CASTING_TABLE).get().toList();
        var builder = manager.createRecipeLookup(
                slimeknights.tconstruct.plugin.jei.TConstructJEIConstants.PART_BUILDER).get().toList();
        long castingOurs = casting.stream().filter(r -> String.valueOf(r).contains("tsb_part")).count();
        long builderOurs = builder.stream().filter(r -> String.valueOf(r).contains("tsb_part")).count();
        TSB.LOGGER.info("[TSB-JEI] 自有锻造 {} 条 | 浇筑总 {} 条(其中我们的 {}) | 部件制造总 {} 条(其中我们的 {})",
                forging, casting.size(), castingOurs, builder.size(), builderOurs);
        // 采样：看看浇筑展示配方长什么样、类型是什么
        casting.stream().limit(3).forEach(r ->
                TSB.LOGGER.info("[TSB-JEI] 浇筑样本 {} :: {}", r.getClass().getSimpleName(), String.valueOf(r).substring(0, Math.min(160, String.valueOf(r).length()))));
        builder.stream().limit(3).forEach(r ->
                TSB.LOGGER.info("[TSB-JEI] 制造样本 {} :: {}", r.getClass().getSimpleName(), String.valueOf(r).substring(0, Math.min(160, String.valueOf(r).length()))));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        TSB.LOGGER.info("[TSB-JEI] registerRecipeCatalysts 被调用");
        registration.addRecipeCatalysts(FORGING,
                TsbItems.blank(TsbBladeVariant.UCHIGATANA),
                TsbItems.blank(TsbBladeVariant.SHIRASAYA),
                TsbItems.blank(TsbBladeVariant.DAIKATANA));
        registration.addRecipeCatalysts(PART_CASTING,
                TsbParts.cast(TsbPartType.BLADE),
                TsbParts.cast(TsbPartType.HANDLE),
                TsbParts.cast(TsbPartType.SAYA));
    }
}
