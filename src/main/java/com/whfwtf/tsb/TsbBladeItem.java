package com.whfwtf.tsb;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 刀锻冶匠魂：由匠魂材料锻造出的拔刀剑。
 *
 * <p>继承 {@link ItemSlashBlade}，因此完整保留拔刀剑的一整套机制（连击、SA、Refine、耀魂、附魔兼容等）；
 * 数值（耐久 / 基础攻击 / 挖掘 / 攻速）在锻造时由匠魂材料决定并写入 BladeState 与 NBT，
 * 材料特性则在 {@link TsbTrait} 里生效。
 *
 * <p>提示框照匠魂 3 原版做三层：默认（耐久 + 刀技 + 刀性）、按住 Shift（属性数值）、
 * 按住 Ctrl（部件明细）；刀同时也是全能工具——挖掘速度与挖掘等级取自材料，
 * 能挖动等级不高于自身挖掘等级的方块。
 */
public class TsbBladeItem extends ItemSlashBlade {

    private static final UUID TSB_SPEED_UUID = UUID.fromString("6f0a2c1e-9b7d-4f2a-8f3e-1c5d7a9b0e21");

    /** RankAttackDamage 三档门槛（照 1.12.2 老版 TSB 的阶级观）：SS / SSS / Limit。 */
    private static final float RANK_SS = 5.0F;
    private static final float RANK_SSS = 8.0F;
    private static final float RANK_LIMIT = 11.0F;
    private static final ChatFormatting[] RANK_COLORS = {
            ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.LIGHT_PURPLE, ChatFormatting.RED
    };

    private final TsbBladeVariant variant;

    public TsbBladeItem(TsbBladeVariant variant, Properties properties) {
        // Tier 只影响原版附魔台可附魔性等，真实数值走 BladeState
        super(Tiers.IRON, 3, -2.4F, properties);
        this.variant = variant;
    }

    public TsbBladeVariant variant() {
        return variant;
    }

    // ------------------------------------------------------------------ 数值读取

    /** 刀的挖掘速度；旧刀没有该 NBT 时按主刃材料兜底。 */
    public static float miningSpeedOf(ItemStack stack) {
        if (hasStat(stack, TsbItems.TAG_MINING_SPEED)) {
            return stack.getTag().getFloat(TsbItems.TAG_MINING_SPEED);
        }
        TsbMaterial.Entry entry = TsbMaterials.resolve(TsbItems.materialOf(stack));
        return entry == null ? 0.0F : entry.miningSpeed();
    }

    /** 刀的挖掘等级（数字）；旧刀没有该 NBT 时按主刃材料兜底，认不出返回 -1（不能挖）。 */
    public static int miningTierOf(ItemStack stack) {
        if (hasStat(stack, TsbItems.TAG_MINING_TIER)) {
            return stack.getTag().getInt(TsbItems.TAG_MINING_TIER);
        }
        TsbMaterial.Entry entry = TsbMaterials.resolve(TsbItems.materialOf(stack));
        return entry == null ? -1 : entry.miningTier();
    }

    /** 刀的攻击速度倍率（刀柄攻速系数之和，1.0 = 不修正）。 */
    public static float speedFactorOf(ItemStack stack) {
        if (hasStat(stack, TsbItems.TAG_ATTACK_SPEED)) {
            float factor = stack.getTag().getFloat(TsbItems.TAG_ATTACK_SPEED);
            return factor > 0.0F ? factor : 1.0F;
        }
        return 1.0F;
    }

    /** 这把刀最终的攻击速度（基础 1.6 × 刀柄系数 + 特性攻速加成）。 */
    public static float attackSpeedOf(ItemStack stack) {
        return TsbAssembly.BASE_ATTACK_SPEED * speedFactorOf(stack) + TsbTrait.attackSpeedBonus(stack);
    }

    private static boolean hasStat(ItemStack stack, String key) {
        return stack.hasTag() && stack.getTag().contains(key);
    }

    // ------------------------------------------------------------------ 战斗

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        TsbTrait.onHit(stack, TsbItems.variantOf(stack), target, attacker, attackDamageOf(attacker));
        return result;
    }

    private static float attackDamageOf(LivingEntity attacker) {
        return (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> result = super.getAttributeModifiers(slot, stack);
        if (slot == EquipmentSlot.MAINHAND) {
            // 目标攻速 = 1.6 × 刀柄系数 + 特性加成；与现有修正的差值用自己的 UUID 补上
            float target = attackSpeedOf(stack);
            float current = 4.0F;
            for (AttributeModifier modifier : result.get(Attributes.ATTACK_SPEED)) {
                current += modifier.getAmount();
            }
            float delta = target - current;
            if (delta != 0.0F) {
                Multimap<Attribute, AttributeModifier> copy = ArrayListMultimap.create(result);
                copy.put(Attributes.ATTACK_SPEED, new AttributeModifier(TSB_SPEED_UUID, "tsb_trait_speed",
                        delta, AttributeModifier.Operation.ADDITION));
                return copy;
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ 挖掘

    /**
     * 挖掘速度：只有「正确工具」才按刀的挖掘速度结算，否则和普通物品一样（1.0）。
     *
     * <p>刀的挖掘等级够了就当全能工具用（TIC 思路）：等级取自材料，见 {@link #miningTierOf}。
     */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!isCorrectToolForDrops(stack, state)) {
            return 1.0F;
        }
        return Math.max(1.0F, miningSpeedOf(stack));
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        int tier = miningTierOf(stack);
        return tier >= 0 && tier >= requiredTier(state);
    }

    /** 原版方块的需求工具等级（木 0 / 石 1 / 铁 2 / 钻石 3；下界合金级没有原版方块需求）。 */
    private static int requiredTier(BlockState state) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return 3;
        }
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
            return 2;
        }
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
            return 1;
        }
        return 0;
    }

    // ------------------------------------------------------------------ 被动

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
                              int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        // 注意：这里不再应用韧性（DUCTILE）的被动自我修复。
        // 原逻辑会每 ~240 tick 把耐久 +1，与战斗损耗叠加导致「砍怪耐久先降后涨」的波动，
        // 与玩家预期（耐久只减、靠工匠砧/工作台修复）冲突，视为 BUG-4 修复。
        // 刀的耐久现在完全由 damageItem 单调下降，修复只能走 TsbAnvilRepairRecipe / BladeRepairRecipe。
    }

    // ------------------------------------------------------------------ 修复

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        String material = TsbItems.materialOf(toRepair);
        if (!material.isEmpty()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(repair.getItem());
            if (id != null && "taiga".equals(id.getNamespace())
                    && id.getPath().equals(material + "_ingot")) {
                return true;
            }
        }
        return super.isValidRepairItem(toRepair, repair);
    }

    // ------------------------------------------------------------------ 耐久

    /**
     * 重写耐久损耗：刀锻冶匠魂的刀「永不碎裂」，只把耐久压到上限以下（永远留 1 点，
     * 不进入拔刀剑原生的 broken 视觉——其 broken 子贴图与我们的平纹贴图不兼容，会显示错贴图），
     * 并且不再被 Refine（研磨值）自动回血，让耐久单调下降、可被工匠砧/工作台修复。
     */
    @Override
    public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(ItemStack stack, int amount,
                                                                             T entity, Consumer<T> onBroken) {
        if (amount <= 0) {
            return 0;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return amount;
        }
        int next = Math.min(max - 1, stack.getDamageValue() + amount);
        stack.setDamageValue(next);
        return 0;
    }

    // ------------------------------------------------------------------ 耐久显示

    /**
     * 剩余耐久（= 上限 − 已损），即提示框要显示的「当前耐久」。
     *
     * <p><b>为什么不能直接用 {@code getDamage()}</b>：拔刀剑的 BladeState 沿用原版「已损值」语义
     * （{@code damage == 0} 表示满耐久，{@code maxDamage − damage} 才是剩余），
     * 而匠魂的提示框惯例是「剩余 / 上限」（{@code TooltipBuilder#formatDurability(current, max)} 的
     * {@code current} 传的就是剩余耐久）。两者方向相反，直接把 {@code getDamage()} 填进
     * 「耐久度：%s / %s」会让一把满耐久的刀显示成「0 / 3840」，玩家读成「耐久 0」而误以为刀已报废。
     *
     * <p>取值口径与 {@link #getMaxDamage(ItemStack)} 保持一致（都走 BladeState），
     * 保证「剩余 ≤ 上限」恒成立；{@link #damageItem} 把已损值封在 {@code max − 1}，
     * 所以剩余耐久最少是 1，不会出现「剩余 0 / 上限 N」这种自相矛盾的显示。
     */
    public static int remainingDurability(ItemStack stack) {
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> Math.max(0, state.getMaxDamage() - state.getDamage()))
                .orElseGet(() -> Math.max(0, stack.getMaxDamage() - stack.getDamageValue()));
    }

    @Override
    public void appendRefineCount(List<Component> tooltip, ItemStack stack) {
        int refine = stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState::getRefine)
                .orElse(0);
        tooltip.add(Component.translatable("tsb.tooltip.refine", refine)
                .withStyle(refine > 0 ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
    }

    // ------------------------------------------------------------------ 提示

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        String material = TsbItems.materialOf(stack);
        TsbMaterial.Entry entry = TsbMaterials.resolve(material);
        if (entry == null) {
            return;
        }
        TsbBladeVariant variant = TsbItems.variantOf(stack);
        boolean ctrl = Screen.hasControlDown();

        // 材料行（三种视图都有）
        tooltip.add(Component.translatable("tsb.tooltip.material")
                .append(TsbMaterials.coloredName(material)));

        if (ctrl && hasParts(stack)) {
            // ---- Ctrl：部件明细（照匠魂 3 的详细信息视图）
            appendPartDetails(stack, variant, tooltip);
            return;
        }

        if (Screen.hasShiftDown()) {
            // ---- Shift：属性数值（照匠魂 3 的属性数值视图）
            appendStats(stack, tooltip);
            return;
        }

        // ---- 默认视图：耐久 + 刀技 + 刀性 + 操作提示
        stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            // 显示「剩余 / 上限」而不是 BladeState 的已损值，见 remainingDurability 的注释
            tooltip.add(Component.translatable("tsb.tooltip.durability_value",
                            remainingDurability(stack), state.getMaxDamage())
                    .withStyle(ChatFormatting.GRAY));
            if (state.getSlashArtsKey() != null && !TsbArts.NONE.equals(state.getSlashArtsKey())) {
                tooltip.add(Component.translatable("tsb.tooltip.arts",
                                state.getSlashArts().getDescription())
                        .withStyle(ChatFormatting.GOLD));
            }
        });

        TsbTrait trait = TsbMaterials.trait(material);
        if (trait != TsbTrait.NONE) {
            tooltip.add(Component.translatable("tsb.tooltip.trait",
                            Component.translatable("tsb.trait." + trait.name().toLowerCase()))
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tsb.trait." + trait.name().toLowerCase() + ".desc")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        // RankAttackDamage（照 1.12.2 老版 TSB 的三档伤害显示）
        float attackValue = stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState::getBaseAttackModifier)
                .orElse(0.0F);
        int rank = attackValue >= RANK_LIMIT ? 3
                : attackValue >= RANK_SSS ? 2
                : attackValue >= RANK_SS ? 1 : 0;
        tooltip.add(Component.translatable("tsb.tooltip.rank").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("B-SS").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("SSS").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Limit").withStyle(ChatFormatting.RED)));
        tooltip.add(Component.literal(String.format("+%.1f", attackValue))
                .withStyle(RANK_COLORS[rank])
                .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("tsb.tooltip.rank." + rank)
                        .withStyle(RANK_COLORS[rank])));

        if (variant == TsbBladeVariant.SHIRASAYA) {
            tooltip.add(Component.translatable("tsb.tooltip.plain").withStyle(ChatFormatting.DARK_GRAY));
        }

        tooltip.add(Component.translatable("tsb.tooltip.shift_hint").withStyle(ChatFormatting.DARK_GRAY));
        if (hasParts(stack)) {
            tooltip.add(Component.translatable("tsb.tooltip.ctrl_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** Shift 视图：耐久 / 近战伤害 / 近战速度 / 挖掘等级 / 挖掘速度。 */
    private static void appendStats(ItemStack stack, List<Component> tooltip) {
        stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            // 与默认视图同一口径：剩余 / 上限
            tooltip.add(Component.translatable("tsb.tooltip.durability_value",
                            remainingDurability(stack), state.getMaxDamage())
                    .withStyle(ChatFormatting.GRAY));
        });
        stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> tooltip.add(
                Component.translatable("tsb.tooltip.attack_damage",
                                String.format("%.1f", state.getBaseAttackModifier()))
                        .withStyle(ChatFormatting.GRAY)));
        tooltip.add(Component.translatable("tsb.tooltip.attack_speed",
                        String.format("%.1f", attackSpeedOf(stack)))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tsb.tooltip.mining_tier",
                        TsbMaterials.tierName(miningTierOf(stack)))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tsb.tooltip.mining_speed",
                        String.format("%.1f", miningSpeedOf(stack)))
                .withStyle(ChatFormatting.GRAY));
    }

    /** Ctrl 视图：逐部件列数值（刀身 / 刀鞘按头部属性，刀柄按百分比修饰符）。 */
    private static void appendPartDetails(ItemStack stack, TsbBladeVariant variant,
                                          List<Component> tooltip) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        boolean daikatana = variant == TsbBladeVariant.DAIKATANA;
        String[][] parts = daikatana
                ? new String[][]{
                {"tsb.tooltip.part.blade1", TsbItems.TAG_MATERIAL},
                {"tsb.tooltip.part.blade2", TsbItems.TAG_BLADE2},
                {"tsb.tooltip.part.handle1", TsbItems.TAG_HANDLE},
                {"tsb.tooltip.part.handle2", TsbItems.TAG_HANDLE2},
                {"tsb.tooltip.part.saya1", TsbItems.TAG_SAYA},
                {"tsb.tooltip.part.saya2", TsbItems.TAG_SAYA2}}
                : new String[][]{
                {"tsb.tooltip.part.blade", TsbItems.TAG_MATERIAL},
                {"tsb.tooltip.part.handle", TsbItems.TAG_HANDLE},
                {"tsb.tooltip.part.saya", TsbItems.TAG_SAYA}};

        for (String[] part : parts) {
            String mat = tag.getString(part[1]);
            if (mat.isEmpty()) {
                continue;
            }
            boolean isHandle = part[0].contains("handle");
            appendPartHeader(tooltip, part[0], mat);
            if (isHandle) {
                // 刀柄：四个百分比修饰符（照匠魂 3 的手柄行）
                tooltip.add(Component.translatable("tsb.tooltip.handle_durability",
                                percent(TsbMaterials.handleDurability(mat)))
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tsb.tooltip.handle_attack",
                                percent(TsbMaterials.handleAttack(mat)))
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tsb.tooltip.handle_speed",
                                percent(TsbMaterials.handleMeleeSpeed(mat)))
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tsb.tooltip.handle_mining",
                                percent(TsbMaterials.handleMiningSpeed(mat)))
                        .withStyle(ChatFormatting.GRAY));
            } else {
                // 刀身 / 刀鞘：四项头部属性（照匠魂 3 的镐头行）
                TsbMaterial.Entry entry = TsbMaterials.resolve(mat);
                if (entry == null) {
                    continue;
                }
                tooltip.add(Component.translatable("tsb.tooltip.head_durability", entry.durability())
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tsb.tooltip.head_tier",
                                TsbMaterials.tierName(entry.miningTier()))
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tsb.tooltip.head_mining",
                                String.format("%.1f", entry.miningSpeed()))
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tsb.tooltip.head_attack",
                                String.format("%.1f", entry.attack()))
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /** 部件标题行：黄字 + 下划线（照匠魂 3），有刀性的材料把刀性名括在后面。 */
    private static void appendPartHeader(List<Component> tooltip, String labelKey, String material) {
        MutableComponent header = Component.translatable(labelKey, TsbMaterials.coloredName(material));
        TsbTrait trait = TsbMaterials.trait(material);
        if (trait != TsbTrait.NONE) {
            header = header.append(Component.literal("（")
                    .append(Component.translatable("tsb.trait." + trait.name().toLowerCase()))
                    .append("）"));
        }
        tooltip.add(header.withStyle(style -> style.withColor(ChatFormatting.YELLOW).withUnderlined(true)));
    }

    /** 这把刀有没有部件记录（装配出来的刀才有；锻造台直出的没有）。 */
    private static boolean hasParts(ItemStack stack) {
        return stack.hasTag() && !stack.getTag().getString(TsbItems.TAG_HANDLE).isEmpty();
    }

    /** 百分比修饰符格式化：-0.25 → "-25%"，0.35 → "+35%"。 */
    private static String percent(float modifier) {
        return String.format("%+d%%", Math.round(modifier * 100.0F));
    }
}
