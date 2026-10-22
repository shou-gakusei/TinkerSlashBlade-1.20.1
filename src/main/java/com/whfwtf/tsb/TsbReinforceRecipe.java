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
import slimeknights.tconstruct.library.recipe.tinkerstation.IMutableTinkerStationContainer;
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
 *
 * <p><b>卷轴消耗语义</b>：本配方是「整叠一次性消耗」——放进一叠 N 个卷轴，点一次强化就把
 * 这一叠全吃掉，Refine 一次涨 {@code N * 每卷轴数值}。因此必须覆写
 * {@link #updateInputs}（见该方法注释），否则会退回匠魂默认的「每槽只扣 1 个」而变成刷分漏洞。
 * 工具槽的替换由匠魂工作站的标准合成流程处理，不吃任何强化槽位。
 */
public class TsbReinforceRecipe implements ITinkerStationRecipe {

    /**
     * Refine 上限（防刷）。
     *
     * <p>本配方是「整叠一次性消耗」：一叠 N 个卷轴在一次合成里全被吃掉，Refine 也一次涨
     * {@code N * 每卷轴数值}（见 {@link #scrollRefine} 与 {@link #updateInputs}）——给多少和扣多少
     * 严格成对，不存在「加 30 只扣 1 个」的刷取漏洞。保留这个上限是为了兜住另一类情况：
     * 玩家用 +1 卷轴把刀慢慢堆到 1000 以上时封顶，避免 Refine 数值溢出，也避免低价值卷轴
     * 在高 Refine 刀上被无意义地批量消耗。
     */
    private static final int MAX_REFINE = 1000;

    private final ResourceLocation id;

    public TsbReinforceRecipe(ResourceLocation id) {
        this.id = id;
    }

    private static boolean isScroll(@Nullable ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof TsbScrollItem;
    }

    /**
     * 某个输入槽里「本次要按整叠吃掉的卷轴数量」；该槽不是卷轴则返回 0。
     *
     * <p>{@link #scrollRefine}（算 Refine 加多少）和 {@link #updateInputs}（扣几个）都走这一个
     * 方法取数，保证同一叠卷轴在两边读到的数量绝对一致，不会出现多给或少给。
     */
    private static int scrollCountIn(ITinkerStationContainer container, int index) {
        ItemStack stack = container.getInput(index);
        return isScroll(stack) ? stack.getCount() : 0;
    }

    /** 输入槽里卷轴提供的总 Refine（按整叠数量累加）；没有卷轴返回 0。 */
    private static int scrollRefine(ITinkerStationContainer container) {
        int total = 0;
        for (int i = 0; i < container.getInputCount(); i++) {
            int count = scrollCountIn(container, i);
            if (count > 0) {
                total += ((TsbScrollItem) container.getInput(i).getItem()).refineAmount() * count;
            }
        }
        return total;
    }

    @Override
    public boolean matches(ITinkerStationContainer container, Level level) {
        return TsbItems.isBlade(container.getTinkerableStack()) && scrollRefine(container) > 0;
    }

    /**
     * 算出结果刀：Refine 按<b>整叠</b>卷轴的总值一次性增加（语义：吃完这一叠）。
     *
     * <p>本方法只负责「产出什么」，<b>不碰输入槽</b> —— 扣材料统一由
     * {@link #updateInputs} 在 {@code onCraft} 里完成，避免同一批卷轴被扣两次。
     * 两侧的数量都取自 {@link #scrollCountIn}，所以 Refine 加的量和卷轴扣的量严格一致。
     *
     * <p>{@code amount} 是整叠累加值（3 个 +10 卷轴 → 30），这里保留
     * {@link Math#min(int, int)} 配 {@link #MAX_REFINE} 封顶：整叠一次性消耗意味着
     * 单次加的值可能很大（甚至一次就顶到 1000），上限用来防止 Refine 溢出并兜住高 Refine 刀
     * 上批量喂低价值卷轴的浪费。
     */
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

    /**
     * 扣材料：卷轴按整槽数量一次吃完，其它物品保持匠魂默认的「每个输入槽扣 1 个」。
     *
     * <p>匠魂工匠站的合成流程是 {@code TinkerStationBlockEntity.onCraft}：
     * <ol>
     *   <li>{@code updateInputs(result, inventory, isServer)} —— 就是本方法，<b>材料在这里扣</b>；</li>
     *   <li>{@code shrinkToolSlotBy()} 决定工具槽扣几个（默认 16 = 整个工具栈，因为本配方是
     *       「改工具」而非「复制工具」，改好的刀会进结果槽）；</li>
     *   <li>结果放入结果槽。</li>
     * </ol>
     *
     * <p>必须覆写本方法：默认实现是 {@code 每个输入槽 shrinkInput(index, 1)}，只扣 1 个。
     * 由于 {@link #getValidatedResult} 是按整叠数量累加 Refine 的，不覆写本方法就会出现
     * 「3 个 +10 卷轴 → Refine +30 但只扣 1 个卷轴」的无限刷分漏洞。
     *
     * <p>扣减数量与 {@link #scrollRefine} 共用 {@link #scrollCountIn}，所以「给多少 Refine」
     * 和「扣几个卷轴」永远是同一个数，逐槽对齐，不多给也不少给。
     *
     * <p>与匠魂默认实现及官方配方一致，<b>不</b>按 {@code isServer} 过滤：客户端这边只是
     * 本地预测（容器是客户端自己的副本），服务端才是权威，之后会被同步覆盖，不会双扣。
     */
    @Override
    public void updateInputs(LazyToolStack result, IMutableTinkerStationContainer inv, boolean isServer) {
        for (int index = 0; index < inv.getInputCount(); index++) {
            int count = scrollCountIn(inv, index);
            // 卷轴槽：整槽扣完（这一叠一次全吃掉，count > 0）。
            // 其它槽（含空槽）：扣 1 个，等同匠魂默认行为；空槽时 shrinkInput 内部本身就是空操作。
            inv.shrinkInput(index, count > 0 ? count : 1);
        }
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
