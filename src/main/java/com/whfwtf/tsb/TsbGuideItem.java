package com.whfwtf.tsb;

import com.whfwtf.tsb.client.TsbBooks;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.client.book.BookScreenOpener;
import slimeknights.mantle.item.AbstractBookItem;

/**
 * 《刀匠标准手册》：一本 Mantle 书，记录锻造规则、材料数值、刀性与研磨流程。
 *
 * <p>右键打开，和匠魂的《材料与你》是同一套书页界面。
 */
public class TsbGuideItem extends AbstractBookItem {

    public TsbGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public BookScreenOpener getBook(ItemStack stack) {
        // 只会在客户端被调用（AbstractBookItem 在 use() 里判断 isClientSide）
        return TsbBooks.bladesmith();
    }
}
