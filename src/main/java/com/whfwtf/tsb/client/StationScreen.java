package com.whfwtf.tsb.client;

import com.whfwtf.tsb.StationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 装配台界面。 */
public class StationScreen extends AbstractContainerScreen<StationMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryParse("tinkersslashblade:textures/gui/blade_station.png");
    private static final int LABEL_COLOR = 0xFF404040;

    public StationScreen(StationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // 槽位分工提示：左列 = 主件，右列 = 副件；六槽填满 = 无上大快刀
        int y = topPos + 52;
        for (String key : new String[]{"tsb.station.hint_main", "tsb.station.hint_sub",
                "tsb.station.hint_daikatana"}) {
            graphics.drawString(font, Component.translatable(key), leftPos + 70, y, LABEL_COLOR, false);
            y += 11;
        }
    }
}
