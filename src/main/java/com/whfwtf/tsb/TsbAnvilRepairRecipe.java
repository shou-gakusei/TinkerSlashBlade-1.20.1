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
import java.util.HashSet;
import java.util.Set;

/**
 * 工匠砧材料修复：刀（工具槽）+ 任意槽的「同材料锭」→ 修复耐久（每锭 25%，最多 3 锭），
 * 并清除刀的 broken 状态。
 *
 * <p><b>为什么需要本配方</b>：TSB 的刀是拔刀剑（{@link ItemSlashBlade} 子类），不是匠魂工具，
 * 所以匠魂自带的 {@code ModifierMaterialRepairRecipe} 根本不会匹配它，只能由本 mod 自己实现
 * 一个 {@link ITinkerStationRecipe}（配方类型即 {@code tconstruct:tinker_station}，
 * 工匠砧 / 工匠站 / 工具台 / 武器台 / 防具台都按这个类型查配方）。
 *
 * <p><b>修复材料认「六槽里任意一件」的材料</b>：刀身 / 副刀身 / 主柄 / 副柄 / 刀鞘 / 副刀鞘，
 * 即玩家用做这把刀的任何一种材料锭都能修。
 *
 * <p><b>跨命名空间的材料比对</b>：刀身上记录的是完整材料 id（如 {@code taiga:nihilite}、
 * {@code tconstruct:iron}），而玩家手里的锭可能来自任意命名空间（
 * {@code taiga:nihilite_ingot} / {@code tconstruct:iron_ingot} / {@code minecraft:iron_ingot}）。
 * 因此比对时统一走 {@link TsbMaterials#path(String)} <b>去掉命名空间只比路径</b>，
 * 让 {@code tconstruct:iron} 与 {@code minecraft:iron} 落到同一个材料上；
 * 锭 → 材料的解析复用 {@link TsbAssembly#materialOfIngot}（它内部已按
 * 「锭命名空间 → {@code taiga:} → {@code tconstruct:} → 裸路径」顺序试探，
 * 所以原版铁锭能解析成 {@code tconstruct:iron}）。
 *
 * <p><b>消耗与回复同源</b>：{@link #usedIngotsPerSlot} 是唯一的取数入口，
 * {@link #getValidatedResult}（算回多少耐久）与 {@link #updateInputs}（扣几个锭）都读它，
 * 因此不会出现「回 75% 只扣 1 锭」的漏洞。必须覆写 {@code updateInputs}：
 * 匠魂默认实现是「每个输入槽各扣 1 个」，只扣 1 个。
 *
 * <p>与 {@link TsbReinforceRecipe}（强化卷轴涨 Refine）互斥：本配方在输入里出现卷轴时不匹配，
 * 让卷轴配方单独生效。
 */
public class TsbAnvilRepairRecipe implements ITinkerStationRecipe {

    /** 每锭回复的最大耐久比例。 */
    private static final float RESTORE_PER_INGOT = 0.25F;

    /** 单次修复最多吃多少锭（3 × 25% = 75%，留 25% 给玩家「省料多次修」的选择）。 */
    private static final int MAX_INGOTS = 3;

    private static final String[] BLADE_MATERIAL_KEYS = {
            TsbItems.TAG_MATERIAL, TsbItems.TAG_BLADE2, TsbItems.TAG_HANDLE,
            TsbItems.TAG_HANDLE2, TsbItems.TAG_SAYA, TsbItems.TAG_SAYA2
    };

    private final ResourceLocation id;

    public TsbAnvilRepairRecipe(ResourceLocation id) {
        this.id = id;
    }

    /** 这把刀的六槽材料路径集合（用于判断锭是否匹配）。 */
    private static Set<String> bladeMaterials(ItemStack blade) {
        Set<String> set = new HashSet<>();
        if (!blade.hasTag()) {
            return set;
        }
        for (String key : BLADE_MATERIAL_KEYS) {
            String mat = blade.getTag().getString(key);
            if (!mat.isEmpty()) {
                // 刀身记的是完整 id（taiga:nihilite），存进集合时统一只留路径，
                // 这样和锭解析出来的材料能按路径对上（见类注释的「跨命名空间比对」）
                set.add(TsbMaterials.path(mat));
            }
        }
        return set;
    }

    /**
     * 输入槽里的这一叠是不是「这把刀能用的修复材料」。
     *
     * <p>判定分两步，两步都<b>只比材料路径</b>（去命名空间）：
     * <ol>
     *   <li>{@link TsbAssembly#materialOfIngot} 把锭解析成材料 id——它按
     *       「锭自己的命名空间 → {@code taiga:} → {@code tconstruct:} → 裸路径」依次试探，
     *       所以 {@code minecraft:iron_ingot} 会被解析成 {@code tconstruct:iron}；</li>
     *   <li>用 {@link TsbMaterials#path} 取路径后与刀六槽的材料路径集合比对，
     *       于是 {@code tconstruct:iron_ingot} / {@code minecraft:iron_ingot} 都能修
     *       刀身上记着 {@code tconstruct:iron} 的那把刀。</li>
     * </ol>
     *
     * <p>卷轴被排除：它带强化语义，归 {@link TsbReinforceRecipe} 管，
     * 两个配方靠这条互斥，避免同一叠卷轴既被算成修复材料又被算成强化材料。
     */
    private static boolean isRepairIngot(ItemStack stack, Set<String> materials) {
        if (stack.isEmpty() || stack.getItem() instanceof TsbScrollItem) {
            return false;
        }
        String mat = TsbAssembly.materialOfIngot(stack);
        return mat != null && materials.contains(TsbMaterials.path(mat));
    }

    /**
     * 逐槽分配「本次实际吃掉几个锭」，总量封顶 {@link #MAX_INGOTS}。
     *
     * <p><b>这是回复量与消耗量的唯一取数入口</b>：{@link #getValidatedResult} 用它算回多少耐久、
     * {@link #updateInputs} 用它决定每个槽 shrink 几个，两边读到的是同一批数字，
     * 所以「回多少」和「扣多少」严格成对。分配按槽号从小到大贪心，
     * 保证同一份输入每次得到同一结果（客户端预测与服务端权威也能对上）。
     *
     * @return 长度与输入槽数相同；{@code used[i]} 为第 i 槽本次被吃掉的锭数（非修复材料槽为 0）
     */
    private static int[] usedIngotsPerSlot(ITinkerStationContainer container, Set<String> materials) {
        int[] used = new int[container.getInputCount()];
        int total = 0;
        for (int i = 0; i < used.length && total < MAX_INGOTS; i++) {
            if (!isRepairIngot(container.getInput(i), materials)) {
                continue;
            }
            // 本槽能吃多少：受「该槽实际堆了几个」和「总额度还剩多少」双重限制
            int take = Math.min(container.getInput(i).getCount(), MAX_INGOTS - total);
            used[i] = take;
            total += take;
        }
        return used;
    }

    /** 本次修复实际使用的锭总数（= 回复量的来源）。 */
    private static int totalUsedIngots(int[] usedPerSlot) {
        int total = 0;
        for (int count : usedPerSlot) {
            total += count;
        }
        return total;
    }

    @Override
    public boolean matches(ITinkerStationContainer c, Level level) {
        ItemStack tool = c.getTinkerableStack();
        if (!TsbItems.isBlade(tool)) {
            return false;
        }
        Set<String> materials = bladeMaterials(tool);
        if (materials.isEmpty()) {
            return false;
        }
        // 注意：这里<b>不</b>判断「刀是否满耐久」。满耐久时也要匹配上，
        // 否则工匠砧会因为 matches == false 而根本不去调 getValidatedResult，
        // 玩家只能看到结果槽空着、没有任何提示，反而分不清是「材料不对」还是「刀不用修」。
        // 满耐久的拒绝理由改由 getValidatedResult 用 RecipeResult.failure 明确报出来。
        return totalUsedIngots(usedIngotsPerSlot(c, materials)) > 0;
    }

    @Nullable
    @Override
    public RecipeResult<LazyToolStack> getValidatedResult(ITinkerStationContainer c, RegistryAccess access) {
        ItemStack tool = c.getTinkerableStack();
        Set<String> materials = bladeMaterials(tool);
        if (materials.isEmpty()) {
            return RecipeResult.failure("tsb.anvil_repair.no_material");
        }
        int ingots = totalUsedIngots(usedIngotsPerSlot(c, materials));
        if (ingots <= 0) {
            return RecipeResult.failure("tsb.anvil_repair.no_ingot");
        }
        // 满耐久且没坏 → 明确报错（而不是静默不给结果）。
        // 拔刀剑的真实耐久是 NBT Damage 标签，BladeState.damage 不参与战斗掉耐久。
        boolean broken = tool.getCapability(ItemSlashBlade.BLADESTATE)
                .map(mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState::isBroken)
                .orElse(false);
        if (tool.getDamageValue() <= 0 && !broken) {
            return RecipeResult.failure("tsb.anvil_repair.already_full");
        }
        ItemStack out = tool.copy();
        out.setCount(1);
        out.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            int max = out.getMaxDamage();
            if (max > 0) {
                // 回复量按「实际吃掉的锭数」算，不是固定 3 锭，避免「放 1 锭却按 3 锭回血」
                int restore = Math.max(1, Math.round(max * RESTORE_PER_INGOT * ingots));
                // 耐久读写走物品 NBT 的 Damage 标签（与耐久条同口径）
                out.setDamageValue(Math.max(0, out.getDamageValue() - restore));
            }
            // 只要修了就解除破损；修满与否不影响可用性
            if (!state.isSealed()) {
                state.setBroken(false);
            }
        });
        return LazyToolStack.success(out);
    }

    @Override
    public ItemStack assemble(ITinkerStationContainer c, RegistryAccess access) {
        RecipeResult<LazyToolStack> r = getValidatedResult(c, access);
        return r.isSuccess() ? r.getResult().getStack() : ItemStack.EMPTY;
    }

    /**
     * 扣材料：只按 {@link #usedIngotsPerSlot} 分配的量扣那些<b>真的被用作修复材料的锭</b>。
     *
     * <p>匠魂工匠站的合成流程是 {@code TinkerStationBlockEntity.onCraft}：
     * <ol>
     *   <li>{@code updateInputs(result, inventory, isServer)} —— 就是本方法，<b>材料在这里扣</b>，
     *       且它跑在 {@code shrinkToolSlotBy()} 之前，所以这时工具槽里的刀还在，
     *       {@code getTinkerableStack()} 仍能读到六槽材料；</li>
     *   <li>{@code shrinkToolSlotBy()} 决定工具槽扣几个（默认 16 = 整个工具栈，
     *       因为本配方是「改工具」而非「复制工具」，改好的刀会进结果槽）；</li>
     *   <li>结果放入结果槽。</li>
     * </ol>
     *
     * <p>必须覆写本方法：默认实现是「每个输入槽各 {@code shrinkInput(index, 1)}」，
     * 而 {@link #getValidatedResult} 是按锭数（最多 {@link #MAX_INGOTS} 个）算回复的，
     * 不覆写就会出现「回 75% 只扣 1 锭」的无限修复漏洞。
     *
     * <p>非修复材料的槽（空槽、卷轴槽、放错的锭）一律<b>不扣</b>：本配方只吃自己认得的锭，
     * 玩家顺手塞进去的其它东西不该凭空消失。
     *
     * <p>与匠魂默认实现及 {@link TsbReinforceRecipe} 一致，<b>不</b>按 {@code isServer} 过滤：
     * 客户端这边只是本地预测（容器是客户端自己的副本），服务端才是权威，之后会被同步覆盖，
     * 不会双扣。
     */
    @Override
    public void updateInputs(LazyToolStack result, IMutableTinkerStationContainer inv, boolean isServer) {
        Set<String> materials = bladeMaterials(inv.getTinkerableStack());
        if (materials.isEmpty()) {
            return;
        }
        int[] used = usedIngotsPerSlot(inv, materials);
        for (int index = 0; index < used.length; index++) {
            if (used[index] > 0) {
                inv.shrinkInput(index, used[index]);
            }
        }
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TsbRecipes.ANVIL_REPAIR.get();
    }

    /** 序列化器：逻辑全在代码里，JSON 只需要 {@code {"type":"tinkersslashblade:tsb_anvil_repair"}}。 */
    public static class Serializer implements RecipeSerializer<TsbAnvilRepairRecipe> {

        public static final Serializer INSTANCE = new Serializer();

        @Override
        public TsbAnvilRepairRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new TsbAnvilRepairRecipe(id);
        }

        @Override
        public TsbAnvilRepairRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new TsbAnvilRepairRecipe(id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, TsbAnvilRepairRecipe recipe) {
        }
    }
}