package com.whfwtf.tsb;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * 刀型。
 *
 * <p>同一套匠魂数值，按刀型折算成不同的成品：
 * <ul>
 *   <li>{@link #UCHIGATANA} 打刀：标准刀型，耐久与特性全额生效。</li>
 *   <li>{@link #SHIRASAYA} 白鞘：素刀，耐久打折、攻击略低、特性只有一半，但刀胚便宜，
 *       适合先用它试材料，再走拔刀剑本体的「妖刀化」路线。</li>
 * </ul>
 */
public enum TsbBladeVariant {

    UCHIGATANA("tsb_blade", "tsb_blade_blank", 1.0F, 0.0F, null),
    SHIRASAYA("tsb_blade_white", "tsb_blade_blank_white", 0.6F, -1.0F,
            ResourceLocation.tryParse("slashblade:model/white.png"));

    private final String itemPath;
    private final String blankPath;
    private final float durabilityFactor;
    private final float attackOffset;
    @Nullable
    private final ResourceLocation texture;

    TsbBladeVariant(String itemPath, String blankPath, float durabilityFactor, float attackOffset,
                    @Nullable ResourceLocation texture) {
        this.itemPath = itemPath;
        this.blankPath = blankPath;
        this.durabilityFactor = durabilityFactor;
        this.attackOffset = attackOffset;
        this.texture = texture;
    }

    public String itemPath() {
        return itemPath;
    }

    public String blankPath() {
        return blankPath;
    }

    public float durabilityFactor() {
        return durabilityFactor;
    }

    public float attackOffset() {
        return attackOffset;
    }

    /** 刀身贴图；{@code null} 表示用拔刀剑默认贴图。 */
    @Nullable
    public ResourceLocation texture() {
        return texture;
    }

    /** 从 NBT 里记录的名字还原刀型。 */
    public static TsbBladeVariant byName(@Nullable String name) {
        if (name != null) {
            for (TsbBladeVariant variant : values()) {
                if (variant.name().equals(name)) {
                    return variant;
                }
            }
        }
        return UCHIGATANA;
    }

    public String translationKey() {
        return "item.tinkersslashblade." + itemPath;
    }
}
