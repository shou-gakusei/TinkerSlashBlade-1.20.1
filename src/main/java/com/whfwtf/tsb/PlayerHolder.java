package com.whfwtf.tsb;

import net.minecraft.world.entity.player.Player;

/** 客户端提示框用：安全地取当前玩家（服务端/无客户端环境返回 null）。 */
public final class PlayerHolder {

    private PlayerHolder() {}

    public static Player player() {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist
                != net.minecraftforge.api.distmarker.Dist.CLIENT) {
            return null;
        }
        try {
            return net.minecraft.client.Minecraft.getInstance().player;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
