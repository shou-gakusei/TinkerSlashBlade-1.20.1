package com.whfwtf.tsb;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.PartCastItem;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** 刀部件与其铸模的注册与读写。 */
public final class TsbParts {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TSB.MODID);

    private static final Map<TsbPartType, RegistryObject<Item>> PARTS;
    private static final Map<TsbPartType, RegistryObject<Item>> CASTS;

    static {
        Map<TsbPartType, RegistryObject<Item>> parts = new EnumMap<>(TsbPartType.class);
        Map<TsbPartType, RegistryObject<Item>> casts = new EnumMap<>(TsbPartType.class);
        for (TsbPartType type : TsbPartType.values()) {
            parts.put(type, ITEMS.register(type.id(),
                    () -> new TsbPartItem(type, new Item.Properties().stacksTo(16))));
            // 匠魂的部件铸模：PartCastItem 会自动显示「需要几个材料」
            casts.put(type, ITEMS.register(type.id() + "_cast",
                    () -> new PartCastItem(new Item.Properties(),
                            () -> (TsbPartItem) parts.get(type).get())));
        }
        PARTS = Collections.unmodifiableMap(parts);
        CASTS = Collections.unmodifiableMap(casts);
    }

    private TsbParts() {}

    public static Item item(TsbPartType type) {
        return PARTS.get(type).get();
    }

    public static Item cast(TsbPartType type) {
        return CASTS.get(type).get();
    }

    /** 部件上的材料 id（{@code taiga:nihilite} 这种完整 ID）；不是部件或没材料就返回空串。 */
    public static String materialOf(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IMaterialItem materialItem)) {
            return "";
        }
        MaterialVariantId id = materialItem.getMaterial(stack);
        if (id == null || id.getId().equals(IMaterial.UNKNOWN_ID)) {
            return "";
        }
        return id.getId().toString();
    }

    public static boolean isPart(ItemStack stack, TsbPartType type) {
        return stack.getItem() instanceof TsbPartItem part && part.partType() == type
                && !materialOf(stack).isEmpty();
    }

    /** 创造模式标签页：TAIGA 全部材料 × 全部部件（外加铸模）。 */
    public static void appendToTab(CreativeModeTab.Output output) {
        for (TsbPartType type : TsbPartType.values()) {
            output.accept(cast(type));
        }
        for (TsbMaterial.Entry entry : TsbMaterial.ALL.values()) {
            for (TsbPartType type : TsbPartType.values()) {
                output.accept(TsbPartItem.create(type, entry.id(), 1));
            }
        }
    }
}
