package com.whfwtf.tsb.compat.jei;

import com.whfwtf.tsb.TsbPartItem;
import com.whfwtf.tsb.TsbPartType;
import com.whfwtf.tsb.TsbParts;
import mezz.jei.api.forge.ForgeTypes;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 「刀匠浇筑」分类：金模 + 熔融材料 → 部件。
 *
 * <p>匠魂自带的浇筑分类不会收录我们的部件（它的展示配方是按自己的材料表展开的），
 * 所以这里自己摆一份，保证玩家按 R 查部件时能看到配方。
 */
public class PartCastingCategory implements IRecipeCategory<PartCastingJeiRecipe> {

    private static final int WIDTH = 132;
    private static final int HEIGHT = 66;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public PartCastingCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableItemStack(new ItemStack(TsbParts.cast(TsbPartType.BLADE)));
        this.arrow = helper.createAnimatedRecipeArrow(200);
    }

    @Override
    public RecipeType<PartCastingJeiRecipe> getRecipeType() {
        return TsbJeiPlugin.PART_CASTING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.tinkersslashblade.part_casting");
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
    public void setRecipe(IRecipeLayoutBuilder builder, PartCastingJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(8, 22).addItemStack(new ItemStack(TsbParts.cast(recipe.part())));

        FluidStack fluid = recipe.fluid();
        if (!fluid.isEmpty()) {
            builder.addInputSlot(32, 22).setFluidRenderer(fluid.getAmount(), false, 16, 16)
                    .addIngredient(ForgeTypes.FLUID_STACK, fluid);
        }

        builder.addOutputSlot(92, 22).addItemStack(TsbPartItem.create(recipe.part(), recipe.materialId(), 1));
    }

    @Override
    public void draw(PartCastingJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        arrow.draw(graphics, 60, 24);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("tsb.jei.material", recipe.materialName()),
                6, 8, ChatFormatting.DARK_GRAY.getColor(), false);
        graphics.drawString(font, Component.translatable("tsb.jei.cost", recipe.cost()),
                6, 48, 0x606060, false);
    }

    /** 材料对应的熔融流体（TAIGA 的 molten_<材料>）。 */
    public static FluidStack fluidOf(String materialPath, int ingots) {
        ResourceLocation id = ResourceLocation.tryParse("taiga:molten_" + materialPath);
        var fluid = id == null ? null : ForgeRegistries.FLUIDS.getValue(id);
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, 90 * ingots);
    }
}
