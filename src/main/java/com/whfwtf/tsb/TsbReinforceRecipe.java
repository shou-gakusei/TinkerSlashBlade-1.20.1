package com.whfwtf.tsb;

import com.google.gson.JsonObject;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;

import javax.annotation.Nullable;

/**
 * 工匠砧强化：刀锻冶匠魂的刀 + 强化卷轴 → Refine 提升。
 *
 * <p>实现匠魂 3 的 {@link ITinkerStationRecipe}（配方类型即 {@code tconstruct:tinker_station}，
 * 工匠砧 / 工匠站 / 工具台 / 武器台 / 防具台都按这个类型查配方），因此刀放进
 * 工匠砧的工具槽（{@code tconstruct:modifiable} 标签）+ 任意输入槽放卷轴即可合成。
 *
 * <p>结果直接用 {@link LazyToolStack#success(ItemStack)} 包改好的 ItemStack 返回，
 * 绕过匠魂的工具数据模型（刀不是匠魂工具，Refine 走拔刀剑自己的 BladeState）。
 * 卷轴消耗、工具槽替换由匠魂工作站的标准合成流程处理，不吃任何强化槽位。
 */
public class TsbReinforceRecipe implements ITinkerStationRecipe {

    /** Refine 上限（防溢出 / 防刷爆；+10 卷轴 100 次也够用了）。 */
    private static final int MAX_REFINE = 1000;

    private final ResourceLocation id;

    public TsbReinforceRecipe(ResourceLocation id) {
        this.id = id;
    }

    private static boolean isScroll(@Nullable ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof TsbScrollItem;
    }

    /** 输入槽里卷轴提供的总 Refine；没有卷轴返回 0。 */
    private static int scrollRefine(ITinkerStationContainer container) {
        int total = 0;
        for (int i = 0; i < container.getInputCount(); i++) {
            ItemStack stack = container.getInput(i);
            if (isScroll(stack)) {
                total += ((TsbScrollItem) stack.getItem()).refineAmount() * stack.getCount();
            }
        }
        return total;
    }

    @Override
    public boolean matches(ITinkerStationContainer container, Level level) {
        return TsbItems.isBlade(container.getTinkerableStack()) && scrollRefine(container) > 0;
    }

    @Override
    public RecipeResult<LazyToolStack> getValidatedResult(ITinkerStationContainer container,
                                                          RegistryAccess access) {
        ItemStack tool = container.getTinkerableStack();
        int amount = scrollRefine(container);
        if (amount <= 0) {
            return RecipeResult.failure("tsb.reinforce.no_scroll");
        }
        ItemStack out = tool.copy();
        out.setCount(1);
        out.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state ->
                state.setRefine(Math.min(MAX_REFINE, state.getRefine() + amount)));
        return LazyToolStack.success(out);
    }

    @Override
    public ItemStack assemble(ITinkerStationContainer container, RegistryAccess access) {
        RecipeResult<LazyToolStack> result = getValidatedResult(container, access);
        return result.isSuccess() ? result.getResult().getStack() : ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TsbRecipes.REINFORCE.get();
    }

    /** 序列化器：逻辑全在代码里，JSON 只需要 {@code {"type":"tinkersslashblade:tsb_reinforce"}}。 */
    public static class Serializer implements RecipeSerializer<TsbReinforceRecipe> {

        public static final Serializer INSTANCE = new Serializer();

        @Override
        public TsbReinforceRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new TsbReinforceRecipe(id);
        }

        @Override
        public TsbReinforceRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new TsbReinforceRecipe(id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, TsbReinforceRecipe recipe) {
        }
    }
}
