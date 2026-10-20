package com.whfwtf.tsb.compat.jei;

import com.whfwtf.tsb.TsbMaterial;
import com.whfwtf.tsb.TsbMaterials;
import com.whfwtf.tsb.TsbPartType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;

/** JEI 里的「金模 + 熔融材料 → 部件」条目。 */
public record PartCastingJeiRecipe(TsbPartType part, TsbMaterial.Entry material, int cost) {

    public String materialId() {
        return material.id();
    }

    public Component materialName() {
        return TsbMaterials.displayName(material.id());
    }

    public FluidStack fluid() {
        return PartCastingCategory.fluidOf(material.id(), cost);
    }
}
