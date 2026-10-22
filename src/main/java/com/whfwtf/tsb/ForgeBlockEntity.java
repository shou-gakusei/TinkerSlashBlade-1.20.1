package com.whfwtf.tsb;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 拔刀剑锻造台（工匠砧式）：左边一列配方选刀型，右边刀胚 + 材料锭出刀。
 *
 * <p>和匠魂 3 的 Tinker's Anvil 一个思路：先点左边列表挑要打的东西，
 * 再往右边摆部件。这里「部件」就是对应刀型的刀胚 + 一至三份同材料锭。
 *
 * <p>配方列表固定两条：{@link #RECIPES} = 大太刀 + 无上大快刀；
 * 打刀 / 白鞘走工作台的 {@code tsb_blade_forging} 或装配台，不进这张列表。
 * 选中项存在方块 NBT 里（{@code tsb_selected}），关界面 / 重进都不丢。
 */
public class ForgeBlockEntity extends AbstractStationEntity implements MenuProvider {

    /** 工匠砧的配方列表：大太刀 + 无上大快刀。 */
    public static final List<TsbBladeVariant> RECIPES = List.of(
            TsbBladeVariant.ODACHI, TsbBladeVariant.DAIKATANA);

    public static final int INPUT_SLOTS = 4; // 0 = 刀胚，1~3 = 材料锭

    private static final String TAG_SELECTED = "tsb_selected";

    /** 当前选中的配方下标；越界时按第一条算。 */
    private int selected;

    public ForgeBlockEntity(BlockPos pos, BlockState state) {
        super(TsbBlockEntities.BLADE_FORGE.get(), pos, state, INPUT_SLOTS);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tinkersslashblade.blade_forge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ForgeMenu(id, inventory, this);
    }

    // ------------------------------------------------------------------ 配方选择

    /** 当前选中的刀型（下标越界时回退到第一条配方）。 */
    public TsbBladeVariant selectedVariant() {
        return RECIPES.get(Mth.clamp(selected, 0, RECIPES.size() - 1));
    }

    public int selectedIndex() {
        return Mth.clamp(selected, 0, RECIPES.size() - 1);
    }

    /** 切换选中的配方（界面按钮走这里；越界下标忽略）。 */
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < RECIPES.size()) {
            this.selected = index;
        }
    }

    // ------------------------------------------------------------------ 产物

    /** 按当前输入算出产物；不合法返回空。 */
    @Override
    protected ItemStack computeResult() {
        TsbBladeVariant variant = selectedVariant();
        ItemStack blank = getItem(0);
        // 刀胚必须对上选中的配方，否则不出货
        if (!blank.is(TsbItems.blank(variant)) || blank.getCount() != 1) {
            return ItemStack.EMPTY;
        }

        String material = null;
        int ingots = 0;
        for (int i = 1; i <= 3; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            String mat = TsbAssembly.materialOfIngot(stack);
            if (mat == null || (material != null && !TsbMaterials.path(material).equals(TsbMaterials.path(mat)))) {
                return ItemStack.EMPTY;
            }
            material = mat;
            ingots += stack.getCount();
        }
        if (material == null || ingots < 1 || ingots > 3) {
            return ItemStack.EMPTY;
        }
        return TsbAssembly.forge(variant, material, ingots);
    }

    /** 取走产物时消耗输入（消耗份数与配方一致）。 */
    public void consumeInputs() {
        int ingots = 0;
        for (int i = 1; i <= 3; i++) {
            ingots += getItem(i).getCount();
        }
        removeItem(0, 1);
        int remaining = ingots;
        for (int i = 1; i <= 3 && remaining > 0; i++) {
            int take = Math.min(remaining, getItem(i).getCount());
            removeItem(i, take);
            remaining -= take;
        }
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_SELECTED, selected);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        selected = tag.getInt(TAG_SELECTED);
    }
}
