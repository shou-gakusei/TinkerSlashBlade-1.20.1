package com.whfwtf.tsb;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.tinkerstation.IMutableTinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;

/**
 * 工匠砧部件替换：无上大快刀 / 大太刀 + 一件替换部件 → 换掉对应部件并重算数值。
 *
 * <p>槽位语义（工匠砧默认布局）：下标 1（红石粉）= 主·/唯一部件，下标 3（铁锭）= 副·部件，
 * 且仅无上大快刀支持副套。放入的部件类型（刀身/刀柄/刀鞘）决定换哪一件，一次只能放一件。
 *
 * <p>替换结果由 {@link TsbAssembly#swapPart} 生成：只改材料记录与数值，
 * Refine / 附魔 / 耀魂 / 击杀数 / 已装改造全部保留。
 */
public class TsbPartSwapRecipe implements ITinkerStationRecipe {

    private static final int MAIN_SLOT = 1;
    private static final int SECONDARY_SLOT = 3;

    private final ResourceLocation id;

    public TsbPartSwapRecipe(ResourceLocation id) {
        this.id = id;
    }

    /** 本次替换使用的输入槽下标；不合法返回 -1。 */
    private static int swapSlot(ITinkerStationContainer container) {
        if (!(container.getTinkerableStack().getItem() instanceof TsbBladeItem)) {
            return -1;
        }
        TsbBladeVariant variant = TsbItems.variantOf(container.getTinkerableStack());
        if (variant != TsbBladeVariant.DAIKATANA && variant != TsbBladeVariant.ODACHI) {
            return -1;
        }
        int slot = -1;
        for (int i = 0; i < container.getInputCount(); i++) {
            if (container.getInput(i).isEmpty()) {
                continue;
            }
            if (slot >= 0) {
                return -1;
            }
            if (!(container.getInput(i).getItem() instanceof TsbPartItem)) {
                return -1;
            }
            if (i == MAIN_SLOT) {
                slot = i;
            } else if (i == SECONDARY_SLOT && variant == TsbBladeVariant.DAIKATANA) {
                slot = i;
            } else {
                return -1;
            }
        }
        return slot;
    }

    @Override
    public boolean matches(ITinkerStationContainer container, Level level) {
        int slot = swapSlot(container);
        if (slot < 0) {
            return false;
        }
        return TsbAssembly.swapPart(container.getTinkerableStack(), slot, container.getInput(slot)) != null;
    }

    @Override
    public RecipeResult<LazyToolStack> getValidatedResult(ITinkerStationContainer container,
                                                          RegistryAccess access) {
        int slot = swapSlot(container);
        if (slot < 0) {
            return RecipeResult.failure("tsb.part_swap.invalid");
        }
        ItemStack result = TsbAssembly.swapPart(container.getTinkerableStack(), slot, container.getInput(slot));
        if (result == null) {
            return RecipeResult.failure("tsb.part_swap.same_material");
        }
        return LazyToolStack.success(result);
    }

    @Override
    public ItemStack assemble(ITinkerStationContainer container, RegistryAccess access) {
        RecipeResult<LazyToolStack> result = getValidatedResult(container, access);
        return result.isSuccess() ? result.getResult().getStack() : ItemStack.EMPTY;
    }

    @Override
    public void updateInputs(LazyToolStack result, IMutableTinkerStationContainer inv, boolean isServer) {
        int slot = swapSlot(inv);
        if (slot >= 0) {
            inv.shrinkInput(slot, 1);
        }
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TsbRecipes.PART_SWAP.get();
    }

    /** 序列化器：逻辑全在代码里，JSON 只需要 {@code {"type":"tinkersslashblade:tsb_part_swap"}}。 */
    public static class Serializer implements RecipeSerializer<TsbPartSwapRecipe> {

        public static final Serializer INSTANCE = new Serializer();

        @Override
        public TsbPartSwapRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new TsbPartSwapRecipe(id);
        }

        @Override
        public TsbPartSwapRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new TsbPartSwapRecipe(id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, TsbPartSwapRecipe recipe) {
        }
    }
}
