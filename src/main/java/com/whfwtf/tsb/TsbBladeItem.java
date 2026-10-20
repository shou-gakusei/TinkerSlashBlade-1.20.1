package com.whfwtf.tsb;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 刀锻冶匠魂：由匠魂材料锻造出的拔刀剑。
 *
 * <p>继承 {@link ItemSlashBlade}，因此完整保留拔刀剑的一整套机制（连击、SA、Refine、耀魂、附魔兼容等）；
 * 数值（耐久 / 基础攻击）在锻造时由匠魂材料决定并写入 BladeState，材料特性则在 {@link TsbTrait} 里生效。
 */
public class TsbBladeItem extends ItemSlashBlade {

    private static final UUID TRAIT_SPEED_UUID = UUID.fromString("6f0a2c1e-9b7d-4f2a-8f3e-1c5d7a9b0e21");

    private final TsbBladeVariant variant;

    public TsbBladeItem(TsbBladeVariant variant, Properties properties) {
        // Tier 只影响原版附魔台可附魔性等，真实数值走 BladeState
        super(Tiers.IRON, 3, -2.4F, properties);
        this.variant = variant;
    }

    public TsbBladeVariant variant() {
        return variant;
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
            float bonus = TsbTrait.attackSpeedBonus(stack);
            if (bonus != 0.0F) {
                Multimap<Attribute, AttributeModifier> copy = ArrayListMultimap.create(result);
                copy.put(Attributes.ATTACK_SPEED, new AttributeModifier(TRAIT_SPEED_UUID, "tsb_trait_speed",
                        bonus, AttributeModifier.Operation.ADDITION));
                return copy;
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ 被动

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
                              int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) {
            return;
        }
        int repair = TsbTrait.idleTick(stack, TsbItems.variantOf(stack), living);
        if (repair > 0) {
            stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state ->
                    state.setDamage(Math.max(0, state.getDamage() - repair)));
        }
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

        tooltip.add(Component.translatable("tsb.tooltip.material")
                .append(TsbMaterials.coloredName(material)));

        stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            tooltip.add(Component.translatable("tsb.tooltip.durability", state.getMaxDamage())
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tsb.tooltip.attack",
                            String.format("%.1f", state.getBaseAttackModifier()))
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
        if (TsbItems.variantOf(stack) == TsbBladeVariant.SHIRASAYA) {
            tooltip.add(Component.translatable("tsb.tooltip.plain").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
