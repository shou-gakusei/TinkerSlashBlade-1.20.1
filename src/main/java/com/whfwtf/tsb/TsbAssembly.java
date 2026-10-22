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

        out.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setNonEmpty();
            state.setMaxDamage(durability);
            state.setDamage(0);
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
        out.setHoverName(named(target, material));

        out.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setNonEmpty();
            state.setMaxDamage(newMax);
            state.setDamage(newDamage);
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
        return out;
    }

    /** 刀 + 同材料锭 → 修复（每锭 25%）。 */
    public static ItemStack repair(ItemStack source, int ingots) {
        if (source.isEmpty() || ingots <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack out = source.copy();
        out.setCount(1);
        int max = out.getMaxDamage();
        int restore = Math.max(1, Math.round(max * 0.25F * ingots));
        out.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            int damage = Math.max(0, state.getDamage() - restore);
            state.setDamage(damage);
            if (damage <= 0 && !state.isSealed()) {
                state.setBroken(false);
            }
        });
        return out;
    }

    // ------------------------------------------------------------------ 装配

    /**
     * 刀身 + 刀柄 + 刀鞘 → 刀。
     *
     * <p>刀身决定基础耐久与攻击、以及刀性；刀柄按阶级给耐久加成（0.85~1.15）；
     * 刀鞘按阶级给攻击修正（−0.5~+1.5）。三件可以是不同材料。
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

        // 刀柄：匠魂真实的耐久 / 攻击倍率（HandleMaterialStats）
        float durabilityFactor = TsbMaterials.handleDurability(handleMat);
        float attackFactor = TsbMaterials.handleAttack(handleMat);
        // 刀鞘：按它自己刀身属性的两成折算攻击，算是「配重 / 研磨」的加成
        float sayaBonus = saya.attack() * 0.2F;

        int durability = Math.max(2,
                Math.round(blade.durability() * variant.durabilityFactor() * durabilityFactor));
        float attack = Math.max(1.0F, blade.attack() * attackFactor + sayaBonus + variant.attackOffset());

        ItemStack out = new ItemStack(TsbItems.blade(variant));
        var tag = out.getOrCreateTag();
        tag.putString(TsbItems.TAG_MATERIAL, bladeMat);
        tag.putString(TsbItems.TAG_VARIANT, variant.name());
        tag.putString(TsbItems.TAG_HANDLE, handleMat);
        tag.putString(TsbItems.TAG_SAYA, sayaMat);

        out.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setNonEmpty();
            state.setMaxDamage(durability);
            state.setDamage(0);
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
     * 无上大快刀：刀柄×1 + 刀刃×2 + 刀鞘×2 → 刀，五槽数值全部硬堆。
     *
     * <p>耐久 =（主刃 + 副刃）耐久 × 刀柄耐久倍率 × 大快刀系数（1.2）；
     * 攻击 =（主刃 + 副刃）攻击 × 刀柄攻击倍率 +（刀鞘 + 副鞘）攻击 × 0.5 + 大快刀修正（+1.0）。
     * 五件可以是五种不同材料；刃色 / 刀技 / 贴图 / 修复材料看主刃，
     * 副刃与两把刀鞘的刀性按折扣在 {@link TsbTrait#onHit} 里额外生效。
     */
    public static ItemStack assembleDaikatana(ItemStack bladePart, ItemStack blade2Part,
                                              ItemStack handlePart, ItemStack sayaPart,
                                              ItemStack saya2Part) {
        String bladeMat = TsbParts.materialOf(bladePart);
        String blade2Mat = TsbParts.materialOf(blade2Part);
        String handleMat = TsbParts.materialOf(handlePart);
        String sayaMat = TsbParts.materialOf(sayaPart);
        String saya2Mat = TsbParts.materialOf(saya2Part);
        TsbMaterial.Entry blade = TsbMaterials.resolve(bladeMat);
        TsbMaterial.Entry blade2 = TsbMaterials.resolve(blade2Mat);
        TsbMaterial.Entry handle = TsbMaterials.resolve(handleMat);
        TsbMaterial.Entry saya = TsbMaterials.resolve(sayaMat);
        TsbMaterial.Entry saya2 = TsbMaterials.resolve(saya2Mat);
        if (blade == null || blade2 == null || handle == null || saya == null || saya2 == null) {
            return ItemStack.EMPTY;
        }

        TsbBladeVariant variant = TsbBladeVariant.DAIKATANA;

        // 刀柄：匠魂真实的耐久 / 攻击倍率（HandleMaterialStats）
        float durabilityFactor = TsbMaterials.handleDurability(handleMat);
        float attackFactor = TsbMaterials.handleAttack(handleMat);
        // 两把刀鞘都是配重 / 研磨，各按自己刀身攻击的一半折算
        float sayaBonus = (saya.attack() + saya2.attack()) * 0.5F;

        int durability = Math.max(2,
                Math.round((blade.durability() + blade2.durability())
                        * variant.durabilityFactor() * durabilityFactor));
        float attack = Math.max(1.0F,
                (blade.attack() + blade2.attack()) * attackFactor + sayaBonus + variant.attackOffset());

        ItemStack out = new ItemStack(TsbItems.blade(variant));
        var tag = out.getOrCreateTag();
        tag.putString(TsbItems.TAG_MATERIAL, bladeMat);
        tag.putString(TsbItems.TAG_VARIANT, variant.name());
        tag.putString(TsbItems.TAG_BLADE2, blade2Mat);
        tag.putString(TsbItems.TAG_HANDLE, handleMat);
        tag.putString(TsbItems.TAG_SAYA, sayaMat);
        tag.putString(TsbItems.TAG_SAYA2, saya2Mat);

        out.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setNonEmpty();
            state.setMaxDamage(durability);
            state.setDamage(0);
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

    @Nullable
    private static ResourceLocation bladeTexture(String material) {
        return TsbMaterials.isTaiga(material) ? TsbTextures.forMaterial(TsbMaterials.path(material)) : null;
    }

    public static Component named(TsbBladeVariant variant, String material) {
        return Component.translatable("item.tinkersslashblade." + variant.itemPath() + ".named",
                TsbMaterials.displayName(material));
    }

    /** 材料锭对应哪个材料 ID；不是可识别的材料锭就返回 null。 */
    @Nullable
    public static String materialOfIngot(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !"taiga".equals(id.getNamespace()) || !id.getPath().endsWith("_ingot")) {
            return null;
        }
        String mat = id.getPath().substring(0, id.getPath().length() - "_ingot".length());
        return TsbMaterial.ALL.containsKey(mat) ? "taiga:" + mat : null;
    }
}
