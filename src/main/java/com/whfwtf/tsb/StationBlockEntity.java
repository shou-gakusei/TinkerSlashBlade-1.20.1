package com.whfwtf.tsb;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * 装配台：刀身 + 刀柄 + 刀鞘 → 打刀；五槽填满 → 无上大快刀。
 *
 * <p>五个输入槽：0/1 = 刀刃（刀身部件），2 = 刀柄，3/4 = 刀鞘。
 * <ul>
 *   <li>只填 0、2、3（1、4 留空）→ 打刀（老三槽配方，保持兼容）；</li>
 *   <li>五槽全满 → 无上大快刀（数值硬堆，见 {@link TsbAssembly#assembleDaikatana}）；</li>
 *   <li>其它组合（缺件 / 混搭）→ 无产物。</li>
 * </ul>
 */
public class StationBlockEntity extends AbstractStationEntity implements MenuProvider {

    public static final int INPUT_SLOTS = 5; // 0/1 = 刀刃，2 = 刀柄，3/4 = 刀鞘

    /** 输入槽位对应的部件类型（顺序即槽位下标）。 */
    public static final TsbPartType[] SLOT_TYPES = {
            TsbPartType.BLADE, TsbPartType.BLADE, TsbPartType.HANDLE, TsbPartType.SAYA, TsbPartType.SAYA
    };

    /** 当前输入能拼什么。 */
    public enum Plan {
        NONE, UCHIGATANA, DAIKATANA
    }

    public StationBlockEntity(BlockPos pos, BlockState state) {
        super(TsbBlockEntities.BLADE_STATION.get(), pos, state, INPUT_SLOTS);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tinkersslashblade.blade_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new StationMenu(id, inventory, this);
    }

    private boolean filled(int slot) {
        return TsbParts.isPart(getItem(slot), SLOT_TYPES[slot]);
    }

    private boolean empty(int slot) {
        return getItem(slot).isEmpty();
    }

    /** 按当前输入判定拼法；NONE 表示缺件或组合不合法。 */
    public Plan plan() {
        boolean b0 = filled(0), b1 = filled(1), h = filled(2), s0 = filled(3), s1 = filled(4);
        if (b0 && h && s0 && empty(1) && empty(4)) {
            return Plan.UCHIGATANA;
        }
        if (b0 && b1 && h && s0 && s1) {
            return Plan.DAIKATANA;
        }
        return Plan.NONE;
    }

    @Override
    protected ItemStack computeResult() {
        return switch (plan()) {
            case UCHIGATANA -> TsbAssembly.assemble(getItem(0), getItem(2), getItem(3));
            case DAIKATANA -> TsbAssembly.assembleDaikatana(getItem(0), getItem(1), getItem(2),
                    getItem(3), getItem(4));
            case NONE -> ItemStack.EMPTY;
        };
    }

    /** 取走产物时消耗输入：打刀消耗 0/2/3，大快刀五槽全消耗。 */
    public void consumeInputs() {
        switch (plan()) {
            case UCHIGATANA -> {
                removeItem(0, 1);
                removeItem(2, 1);
                removeItem(3, 1);
            }
            case DAIKATANA -> {
                for (int i = 0; i < INPUT_SLOTS; i++) {
                    removeItem(i, 1);
                }
            }
            case NONE -> {
            }
        }
    }
}
