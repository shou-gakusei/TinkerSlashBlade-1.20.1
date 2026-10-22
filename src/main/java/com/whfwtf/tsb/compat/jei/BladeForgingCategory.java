package com.whfwtf.tsb.compat.jei;

import com.whfwtf.tsb.TsbAssembly;
import com.whfwtf.tsb.TsbBladeVariant;
import com.whfwtf.tsb.TsbItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI 的「刀匠锻造」分类：刀胚 + 材料锭 → 拔刀剑。 */
public class BladeForgingCategory implements IRecipeCategory<ForgingJeiRecipe> {

    private static final int WIDTH = 132;
    private static final int HEIGHT = 66;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public BladeForgingCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableItemStack(new ItemStack(TsbItems.blank(TsbBladeVariant.UCHIGATANA)));
        this.arrow = helper.createAnimatedRecipeArrow(200);
    }

    @Override
    public RecipeType<ForgingJeiRecipe> getRecipeType() {
        return TsbJeiPlugin.FORGING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.tinkersslashblade.blade_forging");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ForgingJeiRecipe recipe, IFocusGroup focuses) {
        // 刀胚
        builder.addInputSlot(8, 22).addItemStack(new ItemStack(TsbItems.blank(recipe.variant())));

        // 材料锭 1~3 份（多放只加耐久）
        List<ItemStack> ingots = new ArrayList<>();
        for (int count = 1; count <= 3; count++) {
            ItemStack stack = recipe.ingot(count);
            if (!stack.isEmpty()) {
                ingots.add(stack);
            }
        }
        builder.addInputSlot(32, 22).addItemStacks(ingots);

        // 成品：用真正锻出来的刀（带材料与数值），图标才不会是空白
        builder.addOutputSlot(92, 22)
                .addItemStack(TsbAssembly.forge(recipe.variant(), recipe.material().id(), 1));
    }

    @Override
    public void draw(ForgingJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        arrow.draw(graphics, 60, 24);

        var font = Minecraft.getInstance().font;
        int tier = recipe.material().tier();
        Component stats = Component.translatable("tsb.jei.stats",
                Math.round(recipe.material().durability() * recipe.variant().durabilityFactor()),
                String.format("%.1f", Math.max(1.0F, recipe.material().attack() + recipe.variant().attackOffset())));
        graphics.drawString(font, stats, 6, 8, 0x404040, false);
        graphics.drawString(font, Component.translatable("tsb.jei.tier", tier),
                6, 48, tier >= 4 ? ChatFormatting.DARK_PURPLE.getColor() : 0x606060, false);
    }
}
