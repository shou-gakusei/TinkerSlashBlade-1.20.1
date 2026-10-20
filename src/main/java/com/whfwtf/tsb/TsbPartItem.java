package com.whfwtf.tsb;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 刀部件：匠魂的「材料物品」（{@link MaterialItem}）。
 *
 * <p>因为它是材料物品，所以能直接吃匠魂的两条产线：
 * <ul>
 *   <li>浇筑台：把 {@code taiga:molten_*} 等金属流体浇进金模，材料由流体决定；</li>
 *   <li>部件制造台：拿材料（木头、石头等可手工制作的材料）配图案敲出来。</li>
 * </ul>
 * 材料写在匠魂标准的 NBT 里，数值在装配时才折算（见 {@link TsbAssembly#assemble}）。
 */
public class TsbPartItem extends ToolPartItem {

    private final TsbPartType partType;

    public TsbPartItem(TsbPartType partType, Properties properties) {
        super(properties, partType.statsId());
        this.partType = partType;
    }

    public TsbPartType partType() {
        return partType;
    }

    /** 造一个带材料的部件。 */
    public static ItemStack create(TsbPartType type, String materialId, int count) {
        ItemStack stack = new ItemStack(TsbParts.item(type), count);
        MaterialVariantId variant = MaterialVariantId.tryParse(
                materialId.contains(":") ? materialId : "taiga:" + materialId);
        if (variant != null) {
            return IMaterialItem.withMaterial(stack, variant);
        }
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        MaterialVariantId material = getMaterial(stack);
        if (material == null || material.getId().equals(IMaterial.UNKNOWN_ID)) {
            return Component.translatable(partType.translationKey());
        }
        return Component.translatable("item.tinkersslashblade.part.named",
                Component.translatable(partType.translationKey()),
                TsbMaterials.displayName(material.getId().toString()));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        MaterialVariantId material = getMaterial(stack);
        if (material == null || material.getId().equals(IMaterial.UNKNOWN_ID)) {
            return;
        }
        String id = material.getId().toString();
        tooltip.add(Component.translatable("tsb.tooltip.material")
                .append(TsbMaterials.coloredName(id)));
        tooltip.add(Component.translatable("tsb.part." + partType.name().toLowerCase() + ".desc")
                .withStyle(ChatFormatting.DARK_GRAY));
        if (partType == TsbPartType.BLADE) {
            TsbTrait trait = TsbMaterials.trait(id);
            if (trait != TsbTrait.NONE) {
                tooltip.add(Component.translatable("tsb.tooltip.trait",
                                Component.translatable("tsb.trait." + trait.name().toLowerCase()))
                        .withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
