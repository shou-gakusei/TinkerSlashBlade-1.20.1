package com.whfwtf.tsb;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 刀匠台界面的注册。 */
public final class TsbMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, TSB.MODID);

    public static final RegistryObject<MenuType<ForgeMenu>> BLADE_FORGE =
            MENUS.register("blade_forge", () -> IForgeMenuType.create(ForgeMenu::new));

    public static final RegistryObject<MenuType<StationMenu>> BLADE_STATION =
            MENUS.register("blade_station", () -> IForgeMenuType.create(StationMenu::new));

    private TsbMenus() {}
}
