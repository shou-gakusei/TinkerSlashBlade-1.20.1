package com.whfwtf.tsb;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 刀匠的「装配逻辑」：工作台配方、锻造台、装配台共用同一套算法。
 *
 * <p>材料一律用完整 ID（{@code taiga:nihilite} / {@code tconstruct:wood}）：
 * TAIGA 的材料查本 mod 的表（带刀性、材料色），其它匠魂材料按匠魂的刀身属性兜底。
 */
public final class TsbAssembly {

    /** 拔刀剑的基础攻击速度（ItemSlashBlade 构造参数的 -2.4 修正，即 4.0 - 2.4）。 */
    public static final float BASE_ATTACK_SPEED = 1.6F;

    private TsbAssembly() {}

    /** 打刀 / 白鞘的耐久与攻击折算，外加材料锭数量的耐久加成。 */
    public static int durability(TsbMaterial.Entry entry, TsbBladeVariant variant, int ingots) {
        float ingotFactor = 1.0F + 0.1F * (Math.max(1, ingots) - 1);
        return Math.max(2, Math.round(entry.durability() * variant.durabilityFactor() * ingotFactor));
    }

    public static float attack(TsbMaterial.Entry entry, TsbBladeVariant variant) {
        return Math.max(1.0F, entry.attack() + variant.attackOffset());
    }

    // ------------------------------------------------------------------ 锻造

    /** 刀胚 + 1~3 锭同材料 → 刀。 */
    public static ItemStack forge(TsbBladeVariant variant, String material, int ingots) {
        TsbMaterial.Entry entry = TsbMaterials.resolve(material);
        if (entry == null) {
            return ItemStack.EMPTY;
        }
        int durability = durability(entry, variant, ingots);
        float attack = attack(entry, variant);

        ItemStack out = new ItemStack(TsbItems.blade(variant));
        var tag = out.getOrCreateTag();
        tag.putString(TsbItems.TAG_MATERIAL, material);
        tag.putString(TsbItems.TAG_VARIANT, variant.name());
        tag.putInt("tsb_ingots", ingots);
        putStats(tag, entry.miningSpeed(), entry.miningTier(), 1.0F);

        out.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setNonEmpty();
            state.setMaxDamage(durability);
            state.setBaseAttackModifier(attack);
            state.setColorCode(entry.color());
            state.setSlashArtsKey(variant == TsbBladeVariant.SHIRASAYA
                    ? TsbArts.NONE
                    : TsbArts.forMaterial(entry));
            var texture = variant == TsbBladeVariant.SHIRASAYA
                    ? variant.texture()
                    : bladeTexture(material);
            if (texture != null) {
                state.setTexture(texture);
            }
        });
        out.setHoverName(named(variant, material));
        return out;
    }

    /** 白鞘 + 3 锭同材料 → 打刀（保留附魔 / 耀魂 / 击杀数，耐久按比例换算）。 */
    public static ItemStack polish(ItemStack source, String material, int ingots) {
        TsbMaterial.Entry entry = TsbMaterials.resolve(material);
        if (entry == null || source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        TsbBladeVariant target = TsbBladeVariant.UCHIGATANA;
        int oldMax = Math.max(1, source.getMaxDamage());
        int oldDamage = Math.max(0, source.getDamageValue());
        int newMax = durability(entry, target, ingots);
        int newDamage = Math.min(newMax - 1, Math.round((float) oldDamage / oldMax * newMax));

        ItemStack out = new ItemStack(TsbItems.blade(target));
        if (source.hasTag()) {
            out.setTag(source.getTag().copy());
        }
        var tag = out.getOrCreateTag();
        tag.putString(TsbItems.TAG_MATERIAL, material);
        tag.putString(TsbItems.TAG_VARIANT, target.name());
        tag.putInt("tsb_ingots", ingots);
        putStats(tag, entry.miningSpeed(), entry.miningTier(), 1.0F);
        out.setHoverName(named(target, material));

        out.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setNonEmpty();
            state.setMaxDamage(newMax);
            state.setBaseAttackModifier(attack(entry, target));
            state.setColorCode(entry.color());
            state.setSlashArtsKey(TsbArts.forMaterial(entry));
            var texture = bladeTexture(material);
            if (texture != null) {
                state.setTexture(texture);
            }
            if (newDamage <= 0 && !state.isSealed()) {
                state.setBroken(false);
            }
        });
        // 旧刀的 Damage 按比例换算后写回 NBT（真实耐久口径，见 remainingDurability）
        out.setDamageValue(newDamage);
        return out;
    }

    /** 刀 + 同材料锭 → 修复（每锭 25%，最少回 1 点）。 */
    public static ItemStack repair(ItemStack source, int ingots) {
        if (source.isEmpty() || ingots <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack out = source.copy();
        out.setCount(1);
        // 耐久读写一律走物品 NBT 的 Damage 标签（与耐久条同口径）
        int max = out.getMaxDamage();
        int restore = Math.max(1, Math.round(max * 0.25F * ingots));
        out.setDamageValue(Math.max(0, out.getDamageValue() - restore));
        out.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            // 只要修了就清 broken（不要求修满），修过的刀即可正常使用
            if (!state.isSealed()) {
                state.setBroken(false);
            }
        });
        return out;
    }

    // ------------------------------------------------------------------ 装配

    /**
     * 打刀：刀身 + 刀柄 + 刀鞘 → 刀（老三槽配方）。
     *
     * <p>刀身决定基础耐久、攻击、挖掘速度与刀性；刀柄按匠魂的百分比修饰符修正
     * 耐久 / 攻击 / 攻速 / 挖掘速度（0.85~1.15 量级，见 {@link TsbMaterials}）；
     * 刀鞘按它自己刀身攻击的两成折算攻击加成（配重 / 研磨）。三件可以是不同材料。
     */
    public static ItemStack assemble(ItemStack bladePart, ItemStack handlePart, ItemStack sayaPart) {
        String bladeMat = TsbParts.materialOf(bladePart);
        TsbMaterial.Entry blade = TsbMaterials.resolve(bladeMat);
        if (blade == null) {
            return ItemStack.EMPTY;
        }
        String handleMat = TsbParts.materialOf(handlePart);
        String sayaMat = TsbParts.materialOf(sayaPart);
        TsbMaterial.Entry handle = TsbMaterials.resolve(handleMat);
        TsbMaterial.Entry saya = TsbMaterials.resolve(sayaMat);
        if (handle == null || saya == null) {
            return ItemStack.EMPTY;
        }

        TsbBladeVariant variant = TsbBladeVariant.UCHIGATANA;

        // 刀柄：匠魂的百分比修饰符（耐久 / 攻击 / 攻速 / 挖掘速度）
        float durabilityFactor = 1.0F + TsbMaterials.handleDurability(handleMat);
        float attackFactor = 1.0F + TsbMaterials.handleAttack(handleMat);
        float speedFactor = 1.0F + TsbMaterials.handleMeleeSpeed(handleMat);
        float miningFactor = 1.0F + TsbMaterials.handleMiningSpeed(handleMat);
        // 刀鞘：按它自己刀身攻击的两成折算攻击，算是「配重 / 研磨」的加成
        float sayaBonus = saya.attack() * 0.2F;

        int durability = Math.max(2,
                Math.round(blade.durability() * variant.durabilityFactor() * durabilityFactor));
        float attack = Math.max(1.0F, blade.attack() * attackFactor + sayaBonus + variant.attackOffset());
        float miningSpeed = Math.max(0.0F, blade.miningSpeed() * miningFactor);

        ItemStack out = new ItemStack(TsbItems.blade(variant));
        var tag = out.getOrCreateTag();
        tag.putString(TsbItems.TAG_MATERIAL, bladeMat);
        tag.putString(TsbItems.TAG_VARIANT, variant.name());
        tag.putString(TsbItems.TAG_HANDLE, handleMat);
        tag.putString(TsbItems.TAG_SAYA, sayaMat);
        putStats(tag, miningSpeed, blade.miningTier(), speedFactor);

        out.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setNonEmpty();
            state.setMaxDamage(durability);
            state.setBaseAttackModifier(attack);
            state.setColorCode(blade.color());
            state.setSlashArtsKey(TsbArts.forMaterial(blade));
            var texture = bladeTexture(bladeMat);
            if (texture != null) {
                state.setTexture(texture);
            }
        });
        out.setHoverName(named(variant, bladeMat));
        return out;
    }

    /**
     * 无上大快刀：刀柄×2 + 刀刃×2 + 刀鞘×2 → 刀，六槽数值全部硬堆（配合游戏进度的完全体）。
     *
     * <p>三组部件各自单独加算：
     * <ul>
     *   <li><b>双刀柄</b>：两根刀柄的匠魂百分比修饰符（耐久 / 攻击 / 攻速 / 挖掘速度）直接相加，
     *       倍率 = 1 + 主柄 + 副柄；</li>
     *   <li><b>双刀身</b>：两把刀身的近战伤害、挖掘速度、耐久各自相加；</li>
     *   <li><b>双刀鞘</b>：两把刀鞘的近战伤害、挖掘速度、耐久各自相加。</li>
     * </ul>
     * 耐久 =（两刃 + 两鞘）耐久 × 大快刀系数（1.2）× 刀柄耐久倍率；
     * 攻击 =（两刃 + 两鞘）攻击 × 刀柄攻击倍率 + 大快刀修正（+1.0）；
     * 挖掘速度 =（两刃 + 两鞘）挖掘速度 × 刀柄挖掘倍率；挖掘等级取六件里最高的一件。
     *
     * <p>六件可以是六种不同材料；刃色 / 刀技 / 贴图 / 修复材料看主刃，
     * 副刃与两把刀鞘的刀性按折扣在 {@link TsbTrait#onHit} 里额外生效。
     */
    public static ItemStack assembleDaikatana(ItemStack bladePart, ItemStack blade2Part,
                                              ItemStack handlePart, ItemStack handle2Part,
                                              ItemStack sayaPart, ItemStack saya2Part) {
        String bladeMat = TsbParts.materialOf(bladePart);
        String blade2Mat = TsbParts.materialOf(blade2Part);
        String handleMat = TsbParts.materialOf(handlePart);
        String handle2Mat = TsbParts.materialOf(handle2Part);
        String sayaMat = TsbParts.materialOf(sayaPart);
        String saya2Mat = TsbParts.materialOf(saya2Part);
        TsbMaterial.Entry blade = TsbMaterials.resolve(bladeMat);
        TsbMaterial.Entry blade2 = TsbMaterials.resolve(blade2Mat);
        TsbMaterial.Entry handle = TsbMaterials.resolve(handleMat);
        TsbMaterial.Entry handle2 = TsbMaterials.resolve(handle2Mat);
        TsbMaterial.Entry saya = TsbMaterials.resolve(sayaMat);
        TsbMaterial.Entry saya2 = TsbMaterials.resolve(saya2Mat);
        if (blade == null || blade2 == null || handle == null || handle2 == null
                || saya == null || saya2 == null) {
            return ItemStack.EMPTY;
        }

        TsbBladeVariant variant = TsbBladeVariant.DAIKATANA;

        // 双刀柄：百分比修饰符单独加算（倍率 = 1 + 主柄 + 副柄）
        float durabilityFactor = 1.0F + TsbMaterials.handleDurability(handleMat)
                + TsbMaterials.handleDurability(handle2Mat);
        float attackFactor = 1.0F + TsbMaterials.handleAttack(handleMat)
                + TsbMaterials.handleAttack(handle2Mat);
        float speedFactor = 1.0F + TsbMaterials.handleMeleeSpeed(handleMat)
                + TsbMaterials.handleMeleeSpeed(handle2Mat);
        float miningFactor = 1.0F + TsbMaterials.handleMiningSpeed(handleMat)
                + TsbMaterials.handleMiningSpeed(handle2Mat);

        // 双刀身 + 双刀鞘：近战伤害 / 挖掘速度 / 耐久各自单独加算
        float durabilitySum = blade.durability() + blade2.durability()
                + saya.durability() + saya2.durability();
        float attackSum = blade.attack() + blade2.attack() + saya.attack() + saya2.attack();
        float miningSum = blade.miningSpeed() + blade2.miningSpeed()
                + saya.miningSpeed() + saya2.miningSpeed();
        int miningTier = Math.max(Math.max(blade.miningTier(), blade2.miningTier()),
                Math.max(saya.miningTier(), saya2.miningTier()));

        int durability = Math.max(2,
                Math.round(durabilitySum * variant.durabilityFactor() * durabilityFactor));
        float attack = Math.max(1.0F, attackSum * attackFactor + variant.attackOffset());
        float miningSpeed = Math.max(0.0F, miningSum * miningFactor);

        ItemStack out = new ItemStack(TsbItems.blade(variant));
        var tag = out.getOrCreateTag();
        tag.putString(TsbItems.TAG_MATERIAL, bladeMat);
        tag.putString(TsbItems.TAG_VARIANT, variant.name());
        tag.putString(TsbItems.TAG_BLADE2, blade2Mat);
        tag.putString(TsbItems.TAG_HANDLE, handleMat);
        tag.putString(TsbItems.TAG_HANDLE2, handle2Mat);
        tag.putString(TsbItems.TAG_SAYA, sayaMat);
        tag.putString(TsbItems.TAG_SAYA2, saya2Mat);
        putStats(tag, miningSpeed, miningTier, speedFactor);

        out.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setNonEmpty();
            state.setMaxDamage(durability);
            state.setBaseAttackModifier(attack);
            state.setColorCode(blade.color());
            state.setSlashArtsKey(TsbArts.forMaterial(blade));
            var texture = bladeTexture(bladeMat);
            if (texture != null) {
                state.setTexture(texture);
            }
        });
        out.setHoverName(named(variant, bladeMat));
        return out;
    }

    // ------------------------------------------------------------------ 工具

    /** 把 TIC3 式属性数值（挖掘速度 / 挖掘等级 / 攻速倍率）写进 NBT，供挖掘与提示框读取。 */
    private static void putStats(net.minecraft.nbt.CompoundTag tag, float miningSpeed,
                                 int miningTier, float speedFactor) {
        tag.putFloat(TsbItems.TAG_MINING_SPEED, miningSpeed);
        tag.putInt(TsbItems.TAG_MINING_TIER, miningTier);
        tag.putFloat(TsbItems.TAG_ATTACK_SPEED, speedFactor);
    }

    @Nullable
    private static ResourceLocation bladeTexture(String material) {
        return TsbMaterials.isTaiga(material) ? TsbTextures.forMaterial(TsbMaterials.path(material)) : null;
    }

    public static Component named(TsbBladeVariant variant, String material) {
        return Component.translatable("item.tinkersslashblade." + variant.itemPath() + ".named",
                TsbMaterials.displayName(material));
    }

    /**
     * 材料锭对应哪个材料完整 ID；不是可识别的材料锭就返回 null。
     *
     * <p>TAIGA（{@code taiga:xxx_ingot}）、原版（{@code minecraft:iron_ingot}）与
     * 匠魂（{@code tconstruct:manyullyn_ingot}）的锭都认：按「锭命名空间 + 同名 →
     * taiga: → tconstruct:」顺序试探，{@link TsbMaterials#resolve} 认得出（有刀身属性）即返回。
     */
    @Nullable
    public static String materialOfIngot(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !id.getPath().endsWith("_ingot")) {
            return null;
        }
        String path = id.getPath().substring(0, id.getPath().length() - "_ingot".length());
        String[] candidates = {
                id.getNamespace() + ":" + path,
                "taiga:" + path,
                "tconstruct:" + path,
                path
        };
        for (String candidate : candidates) {
            if (TsbMaterials.resolve(candidate) != null) {
                return candidate;
            }
        }
        return null;
    }
}
