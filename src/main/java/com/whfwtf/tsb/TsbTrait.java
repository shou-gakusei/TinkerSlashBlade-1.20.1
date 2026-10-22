package com.whfwtf.tsb;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 匠魂材料特性的「刀化」实现。
 *
 * <p>匠魂 3 的特性（trait）只对匠魂工具生效，拔刀剑不是匠魂工具，所以这里按特性 id
 * 重新实现一份轻量的近战效果：锯齿额外伤害、灼热点燃、沉重击退、轻盈加攻速……
 *
 * <p>特性 id 与 TAIGA 材料一一对应（见 {@link TsbMaterial} 的 {@code trait} 字段），
 * 因此同一材料做出的刀，和它做的匠魂工具「性格」一致。
 */
public enum TsbTrait {

    /** 灼热：命中点燃目标。 */
    SEARING(Kind.IGNITE, 4.0F, 0, "tconstruct:searing"),
    /** 锯齿：每次命中额外伤害。 */
    JAGGED(Kind.FLAT_DAMAGE, 1.0F, 0, "tconstruct:jagged"),
    /** 沉重：击退并迟缓目标。 */
    HEAVY(Kind.KNOCKBACK, 0.55F, 40, "tconstruct:heavy"),
    /** 轻盈：攻击速度更快（属性修改，不在此处触发）。 */
    LIGHTWEIGHT(Kind.ATTACK_SPEED, 0.15F, 0, "tconstruct:lightweight"),
    /** 穿刺：无视部分护甲。 */
    PIERCE(Kind.ARMOR_PIERCE, 0.5F, 0, "tconstruct:pierce"),
    /** 过燃：对燃烧中的目标额外伤害。 */
    OVERBURN(Kind.BURNING_BONUS, 3.0F, 0, "tconstruct:overburn"),
    /** 炎势：自身着火时额外伤害。 */
    FLAMESTANCE(Kind.SELF_BURN_BONUS, 3.0F, 0, "tconstruct:flamestance"),
    /** 发光：命中使目标发光。 */
    GLOWING(Kind.GLOW, 10.0F, 0, "tconstruct:glowing"),
    /** 抑界：命中压制目标的行动（缓慢）。 */
    ENDERFERENCE(Kind.DAMPEN, 4.0F, 1, "tconstruct:enderference"),
    /** 强化：命中额外伤害（匠魂里是加升级槽，刀上折算为破防伤害）。 */
    ENHANCED(Kind.FLAT_DAMAGE, 0.75F, 0, "tconstruct:enhanced"),
    /** 韧性：闲置时缓慢自我修复（见 {@link #idleTick}）。 */
    DUCTILE(Kind.SELF_REPAIR, 240.0F, 0, "tconstruct:ductile"),
    /** 噬魂：击杀额外吸收耀魂。 */
    SOUL_EATER(Kind.SOUL_ON_KILL, 60.0F, 0, "taiga:soul_eater"),
    /** 狂战：半血以下伤害提升（按最终伤害的百分比）。 */
    BERSERKER(Kind.LOW_HP_BONUS, 0.35F, 0, "taiga:berserker"),
    /** 不稳：命中随机附带一种效果。 */
    UNSTABLE(Kind.RANDOM, 1.0F, 0, "taiga:unstable"),
    /** 掠夺：击杀额外吸收少量耀魂。 */
    LOOTING(Kind.SOUL_ON_KILL, 20.0F, 0, "tconstruct:looting"),
    /** 无近战效果的特性（鞣制 / 培育 / 动量 / 时运 / 矮人 / 崩解等），只作说明展示。 */
    NONE(Kind.NONE, 0.0F, 0, "tconstruct:tanned", "tconstruct:cultivated", "tconstruct:momentum",
            "tconstruct:fortune", "tconstruct:dwarven", "tconstruct:crumbling");

    /** 特性的作用方式。 */
    public enum Kind {
        NONE, IGNITE, FLAT_DAMAGE, ARMOR_PIERCE, BURNING_BONUS, SELF_BURN_BONUS,
        KNOCKBACK, GLOW, DAMPEN, ATTACK_SPEED, SELF_REPAIR, SOUL_ON_KILL, LOW_HP_BONUS, RANDOM
    }

    private static final Map<String, TsbTrait> BY_ID = new HashMap<>();

    static {
        for (TsbTrait trait : values()) {
            for (String id : trait.ids) {
                BY_ID.put(id, trait);
            }
        }
    }

    private final Kind kind;
    private final float value;
    private final int extra;
    private final String[] ids;

    TsbTrait(Kind kind, float value, int extra, String... ids) {
        this.kind = kind;
        this.value = value;
        this.extra = extra;
        this.ids = ids;
    }

    public Kind kind() {
        return kind;
    }

    /** 按匠魂特性 id 查表；未知特性返回 {@link #NONE}。 */
    public static TsbTrait byId(@Nullable String traitId) {
        if (traitId == null || traitId.isEmpty()) {
            return NONE;
        }
        return BY_ID.getOrDefault(traitId, NONE);
    }

    /** 该材料这把刀实际生效的特性（白鞘只发挥一半）。 */
    public float scale(TsbBladeVariant variant) {
        return variant == TsbBladeVariant.SHIRASAYA ? 0.5F : 1.0F;
    }

    // ------------------------------------------------------------------ 命中

    /** 刀身特性全额，副刃七成半，刀柄 / 刀鞘 / 副鞘各半：五件材料都参与，才不算白搭进去。 */
    private static final float BLADE2_SCALE = 0.75F;
    private static final float HANDLE_SCALE = 0.5F;
    private static final float SAYA_SCALE = 0.5F;
    private static final float SAYA2_SCALE = 0.5F;

    /**
     * 命中后施加特性效果：主刃 100%、副刃 75%、刀柄 50%、刀鞘 50%、副鞘 50%。
     *
     * <p>打刀只有主刃 / 刀柄 / 刀鞘三个标签；无上大快刀五槽全标签，全部参与。
     *
     * @param damage 本次攻击已结算的伤害（用于百分比类特性）
     */
    public static void onHit(ItemStack stack, TsbBladeVariant variant, LivingEntity target,
                             LivingEntity attacker, float damage) {
        if (!stack.hasTag()) {
            return;
        }
        apply(stack, variant, stack.getTag().getString(TsbItems.TAG_MATERIAL), 1.0F, target, attacker, damage);
        apply(stack, variant, stack.getTag().getString(TsbItems.TAG_BLADE2), BLADE2_SCALE, target, attacker, damage);
        apply(stack, variant, stack.getTag().getString(TsbItems.TAG_HANDLE), HANDLE_SCALE, target, attacker, damage);
        apply(stack, variant, stack.getTag().getString(TsbItems.TAG_SAYA), SAYA_SCALE, target, attacker, damage);
        apply(stack, variant, stack.getTag().getString(TsbItems.TAG_SAYA2), SAYA2_SCALE, target, attacker, damage);
    }

    private static void apply(ItemStack stack, TsbBladeVariant variant, String materialId, float partScale,
                              LivingEntity target, LivingEntity attacker, float damage) {
        TsbTrait trait = TsbMaterials.trait(materialId);
        if (trait == TsbTrait.NONE) {
            return;
        }
        float scale = trait.scale(variant) * partScale;
        switch (trait.kind) {
            case IGNITE -> target.setSecondsOnFire(Math.max(1, Math.round(trait.value * scale)));
            case FLAT_DAMAGE -> dealExtra(target, attacker, trait.value * scale);
            case ARMOR_PIERCE -> dealExtra(target, attacker, target.getArmorValue() * trait.value * scale);
            case BURNING_BONUS -> {
                if (target.isOnFire()) {
                    dealExtra(target, attacker, trait.value * scale);
                }
            }
            case SELF_BURN_BONUS -> {
                if (attacker.isOnFire()) {
                    dealExtra(target, attacker, trait.value * scale);
                }
            }
            case KNOCKBACK -> {
                double dx = target.getX() - attacker.getX();
                double dz = target.getZ() - attacker.getZ();
                double len = Math.max(1.0E-4, Math.sqrt(dx * dx + dz * dz));
                target.push(dx / len * trait.value * scale, 0.3, dz / len * trait.value * scale);
                addEffect(target, MobEffects.MOVEMENT_SLOWDOWN, trait.extra, 0);
            }
            case GLOW -> addEffect(target, MobEffects.GLOWING, (int) (trait.value * 20 * scale), 0);
            case DAMPEN -> addEffect(target, MobEffects.MOVEMENT_SLOWDOWN,
                    (int) (trait.value * 20 * scale), trait.extra);
            case LOW_HP_BONUS -> {
                if (attacker.getHealth() / attacker.getMaxHealth() <= 0.5F) {
                    dealExtra(target, attacker, damage * trait.value * scale);
                }
            }
            case SOUL_ON_KILL -> {
                if (!target.isAlive()) {
                    stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state ->
                            state.setProudSoulCount(state.getProudSoulCount() + Math.round(trait.value * scale)));
                }
            }
            case RANDOM -> randomEffect(target, attacker, scale);
            case ATTACK_SPEED, SELF_REPAIR, NONE -> {
                // 攻速走属性修改，自修走 idleTick
            }
        }
    }

    private static void randomEffect(LivingEntity target, LivingEntity attacker, float scale) {
        switch (attacker.getRandom().nextInt(4)) {
            case 0 -> target.setSecondsOnFire(3);
            case 1 -> addEffect(target, MobEffects.MOVEMENT_SLOWDOWN, 60, 0);
            case 2 -> addEffect(target, MobEffects.GLOWING, 100, 0);
            default -> dealExtra(target, attacker, 2.0F * scale);
        }
        if (attacker.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY(0.8), target.getZ(),
                    12, 0.4, 0.4, 0.4, 0.1);
        }
    }

    private static void addEffect(LivingEntity target, net.minecraft.world.effect.MobEffect effect,
                                  int ticks, int amplifier) {
        if (ticks > 0) {
            target.addEffect(new MobEffectInstance(effect, ticks, amplifier));
        }
    }

    private static void dealExtra(LivingEntity target, LivingEntity attacker, float amount) {
        if (amount <= 0.0F || !target.isAlive()) {
            return;
        }
        DamageSource source = attacker instanceof Player player
                ? attacker.damageSources().playerAttack(player)
                : attacker.damageSources().mobAttack(attacker);
        target.invulnerableTime = 0;
        target.hurt(source, amount);
    }

    // ------------------------------------------------------------ 被动 / 属性

    /** 攻速加成（轻盈）。 */
    public static float attackSpeedBonus(ItemStack stack) {
        TsbTrait trait = materialTrait(stack);
        return trait.kind == Kind.ATTACK_SPEED ? trait.value : 0.0F;
    }

    /** 闲置时的自我修复（韧性），返回要恢复的耐久点数。 */
    public static int idleTick(ItemStack stack, TsbBladeVariant variant, LivingEntity holder) {
        TsbTrait trait = materialTrait(stack);
        if (trait.kind != Kind.SELF_REPAIR) {
            return 0;
        }
        if (holder.tickCount % (int) Math.max(20.0F, trait.value) != 0) {
            return 0;
        }
        return stack.getDamageValue() > 0 ? 1 : 0;
    }

    /** 读物品上记录的材料特性。 */
    public static TsbTrait materialTrait(ItemStack stack) {
        TsbMaterial.Entry entry = TsbMaterial.ALL.get(TsbItems.materialOf(stack));
        return entry == null ? NONE : byId(entry.trait());
    }
}
