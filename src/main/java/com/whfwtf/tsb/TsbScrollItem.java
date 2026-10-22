package com.whfwtf.tsb;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 强化卷轴：放入工匠砧（含工具/武器/防具台）与刀锻冶匠魂的刀一起，
 * 免费提升 Refine（不吃任何强化槽位）。+1 / +5 / +10 三档。
 *
 * <p>贴图用原版附魔书（见 models/item 下的三个 json）。
 */
public class TsbScrollItem extends Item {

    /** 该卷轴一次提供的 Refine。 */
    private final int refine;

    public TsbScrollItem(int refine, Properties properties) {
        super(properties);
        this.refine = refine;
    }

    public int refineAmount() {
        return refine;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tsb.scroll.desc", refine)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tsb.scroll.desc2")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
