package com.whfwtf.tsb;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.LootingModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.LootingContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 拔刀剑 ←→ 匠魂 3 改造/材料特性的桥。
 *
 * <p>TSB 的刀是拔刀剑（{@link mods.flammpfeil.slashblade.item.ItemSlashBlade}），不是匠魂工具，
 * 因此匠魂不会自动调用改造钩子。本类负责：
 * <ul>
 *   <li>给无上大快刀/大太刀写入 2 升级槽 + 2 能力槽（{@code tic_persistent}）；</li>
 *   <li>合并 {@code tic_upgrades} 与 {@code tic_modifiers}，并追加非 TAIGA 材料特性；</li>
 *   <li>用 {@code TOOL_STATS} 钩子计算攻击/攻速/耐久/挖掘增量；</li>
 *   <li>转发战斗、耐久、背包 tick、提示等钩子。</li>
 * </ul>
 */
public final class TsbModifierBridge {

    /** 槽位只补一次的标记。 */
    public static final String TAG_SLOTS_INIT = "tsb_tic_slots";
    private static final String TAG_PERSISTENT = "tic_persistent";

    private TsbModifierBridge() {}

    // ------------------------------------------------------------------ 槽位

    /** 只有无上大快刀 / 大太刀吃这套改造槽位。 */
    public static boolean supports(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof TsbBladeItem)) {
            return false;
        }
        TsbBladeVariant variant = TsbItems.variantOf(stack);
        return variant == TsbBladeVariant.DAIKATANA || variant == TsbBladeVariant.ODACHI;
    }

    /** 首次读取时补 2 升级 + 2 能力；重复调用不叠加。 */
    public static void ensureSlots(ItemStack stack) {
        if (!supports(stack)) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.getBoolean(TAG_SLOTS_INIT)) {
            return;
        }
        CompoundTag persistent = tag.contains(TAG_PERSISTENT, Tag.TAG_COMPOUND)
                ? tag.getCompound(TAG_PERSISTENT) : new CompoundTag();
        ToolDataNBT data = ToolDataNBT.readFromNBT(persistent);
        data.setSlots(SlotType.UPGRADE, 2);
        data.setSlots(SlotType.ABILITY, 2);
        tag.put(TAG_PERSISTENT, data.getCopy());
        tag.putBoolean(TAG_SLOTS_INIT, true);
    }

    public static int freeSlots(ItemStack stack, SlotType type) {
        return ToolStack.from(stack).getFreeSlots(type);
    }

    // ------------------------------------------------------------------ 改造列表

    /** 已装改造 + 非 TAIGA 材料特性（不占槽位）。 */
    public static List<ModifierEntry> activeModifiers(ItemStack stack) {
        ToolStack tool = ToolStack.from(stack);
        ModifierNBT.Builder builder = ModifierNBT.builder();
        builder.add(tool.getUpgrades());
        builder.add(tool.getModifiers());
        builder.add(materialTraits(stack));
        return builder.build().getModifiers();
    }

    /** 非 TAIGA 部分材料的匠魂特性。 */
    public static List<ModifierEntry> materialTraits(ItemStack stack) {
        List<ModifierEntry> traits = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return traits;
        }
        addTraits(traits, tag.getString(TsbItems.TAG_MATERIAL), HeadMaterialStats.ID);
        addTraits(traits, tag.getString(TsbItems.TAG_BLADE2), HeadMaterialStats.ID);
        addTraits(traits, tag.getString(TsbItems.TAG_HANDLE), HandleMaterialStats.ID);
        addTraits(traits, tag.getString(TsbItems.TAG_HANDLE2), HandleMaterialStats.ID);
        addTraits(traits, tag.getString(TsbItems.TAG_SAYA), HeadMaterialStats.ID);
        addTraits(traits, tag.getString(TsbItems.TAG_SAYA2), HeadMaterialStats.ID);
        return traits;
    }

    private static void addTraits(List<ModifierEntry> out, @Nullable String material, MaterialStatsId statsId) {
        if (material == null || material.isEmpty() || TsbMaterials.isTaiga(material)) {
            return;
        }
        var id = TsbMaterials.materialId(material);
        if (id == null) {
            return;
        }
        if (MaterialRegistry.getInstance().getTraits(id, statsId).isEmpty()) {
            out.addAll(MaterialRegistry.getInstance().getDefaultTraits(id));
        } else {
            out.addAll(MaterialRegistry.getInstance().getTraits(id, statsId));
        }
    }

    /** 是否装了某个改造（用于不毁等物品级判断）。 */
    public static boolean hasModifier(ItemStack stack, String modifierId) {
        var id = slimeknights.tconstruct.library.modifiers.ModifierId.tryParse(modifierId);
        if (id == null) {
            return false;
        }
        ToolStack tool = ToolStack.from(stack);
        return tool.getModifierLevel(id) > 0 || tool.getUpgrades().getLevel(id) > 0;
    }

    // ------------------------------------------------------------------ 数值

    /** 改造提供的数值增量（构建值 − 该 stat 默认值）。 */
    public static float statDelta(ItemStack stack, FloatToolStat stat) {
        ToolStack tool = ToolStack.from(stack);
        ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
        for (ModifierEntry entry : activeModifiers(stack)) {
            entry.getHook(ModifierHooks.TOOL_STATS).addToolStats(tool, entry, builder);
        }
        Float value = builder.getStat(stat);
        return (value == null ? 0.0F : value) - stat.getDefaultValue();
    }

    public static float attackBonus(ItemStack stack) {
        return statDelta(stack, ToolStats.ATTACK_DAMAGE);
    }

    public static float attackSpeedBonus(ItemStack stack) {
        return statDelta(stack, ToolStats.ATTACK_SPEED);
    }

    public static float durabilityBonus(ItemStack stack) {
        return statDelta(stack, ToolStats.DURABILITY);
    }

    public static float miningSpeedBonus(ItemStack stack) {
        return statDelta(stack, ToolStats.MINING_SPEED);
    }

    /** 改造给出的挖掘等级（0~4，没有则 -1）。 */
    public static int harvestTier(ItemStack stack) {
        ToolStack tool = ToolStack.from(stack);
        ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
        for (ModifierEntry entry : activeModifiers(stack)) {
            entry.getHook(ModifierHooks.TOOL_STATS).addToolStats(tool, entry, builder);
        }
        Tier tier = builder.getStat(ToolStats.HARVEST_TIER);
        return tier == null ? -1 : tier.getLevel();
    }

    // ------------------------------------------------------------------ 战斗

    /** 按目标计算条件攻击加成（亡灵杀手等）。 */
    public static float conditionalAttack(ItemStack stack, LivingEntity target, float base) {
        return ConditionalStatModifierHook.getModifiedStat(
                ToolStack.from(stack), target, ToolStats.ATTACK_DAMAGE, base);
    }

    /** 近战额外伤害钩子链。 */
    public static float modifyMeleeDamage(ItemStack stack, ToolAttackContext context, float damage) {
        ToolStack tool = ToolStack.from(stack);
        float result = damage;
        for (ModifierEntry entry : activeModifiers(stack)) {
            result = entry.getHook(ModifierHooks.MELEE_DAMAGE)
                    .getMeleeDamage(tool, entry, context, result, damage);
        }
        return result;
    }

    /** 命中后效果（点燃、吸血等）。 */
    public static void afterMeleeHit(ItemStack stack, ToolAttackContext context, float damage) {
        ToolStack tool = ToolStack.from(stack);
        for (ModifierEntry entry : activeModifiers(stack)) {
            entry.getHook(ModifierHooks.MELEE_HIT).afterMeleeHit(tool, entry, context, damage);
        }
    }

    /** 造成伤害后的效果。 */
    public static void onDamageDealt(ItemStack stack, LivingEntity attacker, LivingEntity target,
                                     net.minecraft.world.damagesource.DamageSource source, float damage,
                                     boolean critical) {
        ToolStack tool = ToolStack.from(stack);
        EquipmentContext context = new EquipmentContext(attacker);
        for (ModifierEntry entry : activeModifiers(stack)) {
            entry.getHook(ModifierHooks.DAMAGE_DEALT).onDamageDealt(
                    tool, entry, context, EquipmentSlot.MAINHAND, target, source, damage, critical);
        }
    }

    /** 击杀掉落加成。 */
    public static int looting(ItemStack stack, LivingEntity attacker, LivingEntity target,
                              net.minecraft.world.damagesource.DamageSource source) {
        LootingContext context = new LootingContext(attacker, target, source, EquipmentSlot.MAINHAND);
        return LootingModifierHook.getLooting(ToolStack.from(stack), context, 0);
    }

    /** 耐久消耗减免。 */
    public static int modifyDurabilityLoss(ItemStack stack, int amount, @Nullable LivingEntity entity) {
        ToolStack tool = ToolStack.from(stack);
        int result = amount;
        for (ModifierEntry entry : activeModifiers(stack)) {
            result = entry.getHook(ModifierHooks.TOOL_DAMAGE).onDamageTool(tool, entry, result, entity);
        }
        return result;
    }

    /** 背包 tick（黏液覆层等被动）。 */
    public static void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
                                     int slot, boolean selected) {
        InventoryTickModifierHook.heldInventoryTick(stack, level, entity, slot, selected);
    }

    // ------------------------------------------------------------------ 提示

    public static void addTooltip(ItemStack stack, @Nullable Player player, List<Component> tooltip,
                                  TooltipFlag flag) {
        if (player == null) {
            return;
        }
        ToolStack tool = ToolStack.from(stack);
        TooltipKey key = net.minecraft.client.gui.screens.Screen.hasControlDown() ? TooltipKey.CONTROL
                : (net.minecraft.client.gui.screens.Screen.hasShiftDown() ? TooltipKey.SHIFT : TooltipKey.NORMAL);
        for (ModifierEntry entry : activeModifiers(stack)) {
            entry.getHook(ModifierHooks.TOOLTIP).addTooltip(tool, entry, player, tooltip, key, flag);
        }
    }

    /** 改造显示名（提示框用）。 */
    public static Component modifierName(ModifierEntry entry) {
        return entry.getDisplayName().copy().withStyle(ChatFormatting.GRAY);
    }
}
