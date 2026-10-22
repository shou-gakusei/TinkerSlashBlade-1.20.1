package com.whfwtf.tsb.client;

import com.whfwtf.tsb.ForgeBlockEntity;
import com.whfwtf.tsb.ForgeMenu;
import com.whfwtf.tsb.TsbBladeVariant;
import com.whfwtf.tsb.TsbItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 拔刀剑锻造台界面：匠魂工匠砧（Tinker's Anvil）式多配方选择。
 *
 * <ul>
 *   <li>左列是配方列表（大太刀 / 无上大快刀），图标就是该刀型要用的刀胚，
 *       点一下切换选中项（走 {@code clickMenuButton}，选中项持久化在方块里）；</li>
 *   <li>右侧刀胚槽只收当前选中刀型的刀胚，下面三格放同材料锭，最下面是产物；</li>
 *   <li>选中项由服务端经 DataSlot 同步，高亮描边随之刷新。</li>
 * </ul>
 */
public class ForgeScreen extends AbstractContainerScreen<ForgeMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryParse("tinkersslashblade:textures/gui/blade_forge.png");

    /** 配方列：图标槽坐标与行距（界面内坐标，和贴图 / 菜单一致）。 */
    public static final int RECIPE_X = 8;
    public static final int RECIPE_Y = 30;
    public static final int RECIPE_STEP = 24;
    /** 配方名称相对图标槽的偏移。 */
    private static final int NAME_DX = 22;
    private static final int NAME_DY = 5;

    private static final int SELECT_COLOR = 0xFFFFC14A; // 选中描边（金）
    private static final int LABEL_COLOR = 0xFF404040;  // 说明文字（深灰）
    private static final int HINT_X = 124;
    private static final int HINT_Y = 104;

    private final List<TsbBladeVariant> recipes;

    public ForgeScreen(ForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
        this.recipes = ForgeBlockEntity.RECIPES;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        graphics.drawString(font, Component.translatable("tsb.forge.recipes"),
                leftPos + RECIPE_X, topPos + 20, LABEL_COLOR);

        int selected = menu.selectedIndex();
        for (int i = 0; i < recipes.size(); i++) {
            TsbBladeVariant variant = recipes.get(i);
            int x = leftPos + RECIPE_X;
            int y = topPos + RECIPE_Y + i * RECIPE_STEP;
            // 配方图标 = 该刀型的刀胚（要提供的部件），名称跟在右边
            graphics.renderItem(new ItemStack(TsbItems.blank(variant)), x, y);
            graphics.drawString(font, Component.translatable("item.tinkersslashblade." + variant.itemPath()),
                    x + NAME_DX, y + NAME_DY, LABEL_COLOR);
            if (i == selected) {
                graphics.renderOutline(x - 1, y - 1, 20, 20, SELECT_COLOR);
            }
        }

        graphics.drawCenteredString(font, Component.translatable("tsb.forge.hint"),
                leftPos + HINT_X, topPos + HINT_Y, LABEL_COLOR);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // 配方图标的悬停提示（刀胚的名字与说明）
        for (int i = 0; i < recipes.size(); i++) {
            if (isHovering(RECIPE_X - 1, RECIPE_Y + i * RECIPE_STEP - 1, 20, 20, mouseX, mouseY)) {
                graphics.renderTooltip(font, new ItemStack(TsbItems.blank(recipes.get(i))), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < recipes.size(); i++) {
            if (isHovering(RECIPE_X - 1, RECIPE_Y + i * RECIPE_STEP - 1, 20, 20, mouseX, mouseY)) {
                // 按钮 id = 配方下标，服务端 ForgeMenu.clickMenuButton 接手
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
