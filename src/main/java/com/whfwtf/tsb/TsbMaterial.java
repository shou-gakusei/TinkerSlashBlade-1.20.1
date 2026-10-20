package com.whfwtf.tsb;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 匠魂材料的刀身数值表。
 *
 * <p>由 {@code tools/gen_tsb_materials.py} 从 TAIGA 的匠魂材料数据（head 属性 / 特性 / 颜色 / 阶级）生成，
 * 请勿手改：改材料请改 taiga3 的 datapack，再重新跑脚本。
 */
public final class TsbMaterial {

    /** @param id 材料名（对应 {@code taiga:<id>_ingot}）
     *  @param durability 刀身耐久（匠魂 head.durability）
     *  @param attack 刀身攻击（匠魂 head.melee_attack）
     *  @param trait 该材料在匠魂里的特性 id（如 {@code tconstruct:searing}）
     *  @param color 材料文字/特效颜色（0xRRGGBB）
     *  @param tier 材料阶级 1~4 */
    public record Entry(String id, int durability, float attack, String trait, int color, int tier) {}

    public static final Map<String, Entry> ALL;

    static {
        Map<String, Entry> map = new LinkedHashMap<>();
        put(map, "abyssum", 250, 5.2F, "tconstruct:enderference", 0x105B5E, 2);
        put(map, "adamant", 500, 6.0F, "taiga:berserker", 0xCC72CB, 4);
        put(map, "astrium", 900, 6.5F, "tconstruct:enhanced", 0x752E4D, 3);
        put(map, "aurorium", 550, 5.5F, "tconstruct:ductile", 0x735447, 3);
        put(map, "basalt", 100, 1.0F, "tconstruct:tanned", 0xF7F3DF, 1);
        put(map, "dilithium", 100, 1.0F, "tconstruct:jagged", 0x608A84, 2);
        put(map, "duranite", 675, 6.5F, "tconstruct:dwarven", 0x536B71, 3);
        put(map, "dyonite", 750, 7.0F, "tconstruct:overburn", 0xD49D34, 3);
        put(map, "eezo", 600, 5.5F, "tconstruct:looting", 0x405865, 3);
        put(map, "fractum", 550, 5.5F, "tconstruct:momentum", 0x8F8659, 3);
        put(map, "ignitz", 875, 7.5F, "tconstruct:searing", 0x8E162A, 3);
        put(map, "imperomite", 700, 6.5F, "tconstruct:fortune", 0x68C382, 3);
        put(map, "iox", 700, 6.0F, "tconstruct:jagged", 0x55AA55, 3);
        put(map, "jauxum", 500, 5.0F, "tconstruct:jagged", 0x608A84, 2);
        put(map, "karmesine", 500, 5.0F, "tconstruct:jagged", 0xCD3F40, 3);
        put(map, "lumix", 750, 5.0F, "tconstruct:glowing", 0x9B8772, 3);
        put(map, "meteorite", 250, 7.5F, "tconstruct:jagged", 0x2C3F31, 3);
        put(map, "nihilite", 950, 10.0F, "taiga:soul_eater", 0x31215A, 4);
        put(map, "niob", 500, 5.0F, "tconstruct:jagged", 0x3C4F60, 3);
        put(map, "nucleum", 500, 11.5F, "tconstruct:heavy", 0xCCE239, 3);
        put(map, "obsidiorite", 600, 7.5F, "tconstruct:jagged", 0x1B3A42, 3);
        put(map, "osram", 100, 1.0F, "tconstruct:jagged", 0xAE8062, 3);
        put(map, "ovium", 500, 5.0F, "tconstruct:jagged", 0x413E66, 3);
        put(map, "palladium", 500, 5.0F, "tconstruct:jagged", 0x9E5924, 3);
        put(map, "prometheum", 500, 5.0F, "tconstruct:jagged", 0x251D31, 3);
        put(map, "proxii", 500, 5.0F, "tconstruct:flamestance", 0x8DAD9A, 3);
        put(map, "seismum", 500, 5.0F, "tconstruct:jagged", 0xCCA291, 3);
        put(map, "solarium", 800, 8.5F, "tconstruct:heavy", 0xDDD857, 4);
        put(map, "terrax", 750, 5.0F, "tconstruct:looting", 0x877B74, 3);
        put(map, "tiberium", 100, 1.0F, "taiga:unstable", 0xADD000, 2);
        put(map, "triberium", 100, 1.0F, "tconstruct:jagged", 0x39861E, 2);
        put(map, "tritonite", 650, 6.5F, "tconstruct:lightweight", 0x59F5AB, 3);
        put(map, "uru", 500, 5.0F, "tconstruct:pierce", 0x9B7A9E, 3);
        put(map, "valyrium", 750, 6.5F, "tconstruct:jagged", 0x81331B, 3);
        put(map, "vibranium", 950, 8.5F, "tconstruct:crumbling", 0xA0B5BB, 4);
        put(map, "violium", 600, 6.0F, "tconstruct:tanned", 0x6D6784, 2);
        put(map, "yrdeen", 750, 6.5F, "tconstruct:cultivated", 0x632742, 3);
        ALL = Collections.unmodifiableMap(map);
    }

    private static void put(Map<String, Entry> map, String id, int durability, float attack,
                            String trait, int color, int tier) {
        map.put(id, new Entry(id, durability, attack, trait, color, tier));
    }

    private TsbMaterial() {}
}
