package com.whfwtf.tsb;

import com.whfwtf.tsb.TSB;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 刀锻冶匠魂的物品注册。
 *
 * <p>整个类只有在检测到 {@code slashblade} 模组时才会被加载（见 {@link TSB}），
 * 因此没有装拔刀剑时不会触发 {@code NoClassDefFoundError}。
 */
public final class TsbItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TSB.MODID);

    /** 刀胚：还需要与匠魂材料一起锻造才能成刀。 */
    public static final Map<TsbBladeVariant, RegistryObject<Item>> BLANKS;
    /** 成品：刀锻冶匠魂的拔刀剑（材料不同 → 数值不同）。 */
    public static final Map<TsbBladeVariant, RegistryObject<TsbBladeItem>> BLADES;

    /** 材料名在物品 NBT 上的键。 */
    public static final String TAG_MATERIAL = "tsb_material";
    /** 刀型在物品 NBT 上的键。 */
    public static final String TAG_VARIANT = "tsb_variant";
    /** 装配出来的刀：刀柄材料。 */
    public static final String TAG_HANDLE = "tsb_handle";
    /** 装配出来的刀：刀鞘材料。 */
    public static final String TAG_SAYA = "tsb_saya";
    /** 无上大快刀：副刀刃材料（第二把刀身）。 */
    public static final String TAG_BLADE2 = "tsb_blade2";
    /** 无上大快刀：副刀鞘材料（第二段配重）。 */
    public static final String TAG_SAYA2 = "tsb_saya2";

    /** 《刀匠标准手册》。 */
    public static final RegistryObject<Item> GUIDE =
            ITEMS.register("tsb_guide_bladesmith", () -> new TsbGuideItem(new Item.Properties().stacksTo(1)));

    static {
        Map<TsbBladeVariant, RegistryObject<Item>> blanks = new EnumMap<>(TsbBladeVariant.class);
        Map<TsbBladeVariant, RegistryObject<TsbBladeItem>> blades = new EnumMap<>(TsbBladeVariant.class);
        for (TsbBladeVariant variant : TsbBladeVariant.values()) {
            blanks.put(variant, ITEMS.register(variant.blankPath(),
                    () -> new Item(new Item.Properties())));
            blades.put(variant, ITEMS.register(variant.itemPath(),
                    () -> new TsbBladeItem(variant, new Item.Properties().stacksTo(1))));
        }
        BLANKS = Collections.unmodifiableMap(blanks);
        BLADES = Collections.unmodifiableMap(blades);
    }

    private TsbItems() {}

    public static Item blank(TsbBladeVariant variant) {
        return BLANKS.get(variant).get();
    }

    public static TsbBladeItem blade(TsbBladeVariant variant) {
        return BLADES.get(variant).get();
    }

    /** 刀型：先看 NBT 记录，再退回物品本体。 */
    public static TsbBladeVariant variantOf(ItemStack stack) {
        if (!(stack.getItem() instanceof TsbBladeItem blade) || !stack.hasTag()) {
            return TsbBladeVariant.UCHIGATANA;
        }
        String name = stack.getTag().getString(TAG_VARIANT);
        return name.isEmpty() ? blade.variant() : TsbBladeVariant.byName(name);
    }

    /** 该刀胚 + 材料名是否可以锻造成这把刀。 */
    public static boolean isBlankFor(ItemStack stack, TsbBladeVariant variant) {
        return stack.is(blank(variant));
    }

    /** 是不是任意一种刀胚。 */
    public static boolean isBlank(ItemStack stack) {
        for (TsbBladeVariant variant : TsbBladeVariant.values()) {
            if (stack.is(blank(variant))) {
                return true;
            }
        }
        return false;
    }

    /** 是不是本 mod 的刀。 */
    public static boolean isBlade(ItemStack stack) {
        return stack.getItem() instanceof TsbBladeItem;
    }

    /** 读物品上记录的材料名；不是本 mod 的刀则返回空串。 */
    public static String materialOf(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return "";
        }
        return stack.getOrCreateTag().getString(TAG_MATERIAL);
    }

    /** 读物品上某个 NBT 键记录的材料名（无上大快刀的副刃 / 副鞘等）。 */
    public static String taggedMaterialOf(ItemStack stack, String key) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return "";
        }
        return stack.getOrCreateTag().getString(key);
    }

    /** 由创造模式标签页调用（仅当拔刀剑存在时）。 */
    public static void appendToTab(CreativeModeTab.Output output) {
        for (TsbBladeVariant variant : TsbBladeVariant.values()) {
            output.accept(blank(variant));
            // 放「真的刀」而不是裸物品：裸物品没有 BladeState，图标是空白、提示还会显示「封」
            output.accept(TsbAssembly.forge(variant,
                    variant == TsbBladeVariant.SHIRASAYA ? "basalt" : "ignitz", 1));
        }
        output.accept(GUIDE.get());
        TsbParts.appendToTab(output);
        output.accept(TsbBlocks.BLADE_FORGE.get());
        output.accept(TsbBlocks.BLADE_STATION.get());
    }
}
