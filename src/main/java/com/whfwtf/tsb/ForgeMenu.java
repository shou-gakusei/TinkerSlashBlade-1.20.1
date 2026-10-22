package com.whfwtf.tsb;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 锻造台界面（工匠砧式）：刀胚槽 + 三个材料槽 + 产物槽 + 配方选中项同步。
 *
 * <p>配方列表画在界面上（见 {@link com.whfwtf.tsb.client.ForgeScreen}），
 * 点某一条就走 {@link #clickMenuButton} 切到对应刀型；选中项用
 * {@link DataSlot} 服务端 → 客户端同步，刀胚槽只收当前选中刀型的刀胚。
 */
public class ForgeMenu extends AbstractContainerMenu {

    /** 界面槽位坐标（与 {@link com.whfwtf.tsb.client.ForgeScreen} 的贴图对齐）。 */
    public static final int BLANK_X = 115;
    public static final int BLANK_Y = 22;
    public static final int INGOT_X = 97;
    public static final int INGOT_Y = 46;
    public static final int RESULT_X = 115;
    public static final int RESULT_Y = 82;

    private final ForgeBlockEntity entity;

    public ForgeMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, (ForgeBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ForgeMenu(int id, Inventory inventory, ForgeBlockEntity entity) {
        super(TsbMenus.BLADE_FORGE.get(), id);
        this.entity = entity;

        // 刀胚：只收当前选中配方那种刀胚
        addSlot(new Slot(entity, 0, BLANK_X, BLANK_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(TsbItems.blank(entity.selectedVariant()));
            }
        });
        for (int i = 0; i < 3; i++) {
            addSlot(new Slot(entity, 1 + i, INGOT_X + i * 18, INGOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return TsbAssembly.materialOfIngot(stack) != null;
                }
            });
        }
        addSlot(new Slot(entity, entity.resultSlot(), RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                entity.consumeInputs();
                ForgeMenu.this.slotsChanged(entity);
                super.onTake(player, stack);
            }
        });

        // 配方选中项：服务端改动 → 客户端高亮（读的就是方块里的持久化值）
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return entity.selectedIndex();
            }

            @Override
            public void set(int value) {
                entity.setSelectedIndex(value);
            }
        });

        // 玩家背包 + 快捷栏
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 180));
        }

        // 打开界面时先把产物算一次（方块里本来就有东西的情况）
        slotsChanged(entity);
    }

    /** 界面配方按钮：切换选中的刀型（按钮 id = 配方下标）。 */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= ForgeBlockEntity.RECIPES.size() || id == entity.selectedIndex()) {
            return false;
        }
        entity.setSelectedIndex(id);
        entity.setChanged(); // 选中项随方块存档持久化
        // 换了刀型，刀胚可能就不再匹配，重算一次产物并立刻同步
        slotsChanged(entity);
        broadcastChanges();
        return true;
    }

    /** 当前选中的刀型（界面画高亮用）。 */
    public TsbBladeVariant selected() {
        return entity.selectedVariant();
    }

    public int selectedIndex() {
        return entity.selectedIndex();
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == entity) {
            entity.setItem(entity.resultSlot(), entity.computeResult());
        }
        super.slotsChanged(container);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int inputs = entity.inputSlots();
        if (index == entity.resultSlot()) {
            if (!moveItemStackTo(stack, inputs + 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < inputs) {
            if (!moveItemStackTo(stack, inputs + 1, slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 从背包往输入槽塞：先刀胚，再材料锭
            if (!moveItemStackTo(stack, 0, 1, false)
                    && !moveItemStackTo(stack, 1, inputs, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return entity.stillValid(player);
    }
}
