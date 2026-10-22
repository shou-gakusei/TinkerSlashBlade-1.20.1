package com.whfwtf.tsb;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 匠魂材料本色表（0xAARRGGBB）。
 *
 * <p>由 {@code tools/gen_tsb_materials.py} 生成：扫描匠魂材质外观 json
 * （jar 内 {@code assets/tconstruct/tinkering/materials/<id>.json}）与 TAIGA 的
 * （{@code assets/taiga/tinkering/materials/<id>.json}），取顶层 {@code color} 字段；
 * 带 {@code parent} 的（如 {@code rock} → {@code tconstruct:rock/stone}）沿 parent 继承颜色。
 * 请勿手改：改颜色请改上游材质 json，再重新跑脚本。
 *
 * <p>key 是材料路径（不含命名空间），所以 {@code tconstruct:iron} 与 {@code iron} 都查得到。
 */
public final class TsbMaterialColors {

    /** 材料路径（不含命名空间）→ 材料本色 0xAARRGGBB。 */
    public static final Map<String, Integer> ALL;

    static {
        Map<String, Integer> map = new LinkedHashMap<>();
        put(map, "abyssum", 0xFF105B5E);
        put(map, "adamant", 0xFFCC72CB);
        put(map, "aluminum", 0xFFB2BABC);
        put(map, "amethyst_bronze", 0xFFD9A2D0);
        put(map, "ancient", 0xFF7E6059);
        put(map, "ancient_hide", 0xFF7E6059);
        put(map, "astrium", 0xFF752E4D);
        put(map, "aurorium", 0xFF735447);
        put(map, "bamboo", 0xFF8AAE60);
        put(map, "basalt", 0xFFF7F3DF);
        put(map, "blazewood", 0xFF5C2B29);
        put(map, "blazing_bone", 0xFFF2D500);
        put(map, "blood", 0xFFB80000);
        put(map, "bone", 0xFFE8E5D2);
        put(map, "bronze", 0xFFD49765);
        put(map, "cactus", 0xFF649832);
        put(map, "chorus", 0xFF8F648F);
        put(map, "cinderslime", 0xFFB80000);
        put(map, "clay", 0xFFA1A7B1);
        put(map, "cobalt", 0xFF2376DD);
        put(map, "constantan", 0xFFFF8B70);
        put(map, "copper", 0xFFE77C56);
        put(map, "copper/oxidized", 0xFF4FAB90);
        put(map, "darkthread", 0xFF3B2754);
        put(map, "dilithium", 0xFF608A84);
        put(map, "duranite", 0xFF536B71);
        put(map, "dyonite", 0xFFD49D34);
        put(map, "earthslime", 0xFF8CD782);
        put(map, "eezo", 0xFF405865);
        put(map, "electrum", 0xFFFFEA65);
        put(map, "ender_pearl", 0xFF349988);
        put(map, "enderslime", 0xFFD37CFF);
        put(map, "enderslime_vine", 0xFFA92DFF);
        put(map, "flint", 0xFF3D3C3C);
        put(map, "flint/basalt", 0xFF747474);
        put(map, "fractum", 0xFF8F8659);
        put(map, "glass", 0xFF000000);
        put(map, "gold", 0xFFFDF55F);
        put(map, "hepatizon", 0xFF60496B);
        put(map, "honey", 0xFFFFCE5D);
        put(map, "ice", 0xFF74ABFE);
        put(map, "ichor", 0xFFFFB97C);
        put(map, "ichorskin", 0xFFFFB97C);
        put(map, "ignitz", 0xFF8E162A);
        put(map, "imperomite", 0xFF68C382);
        put(map, "invar", 0xFFCADBD0);
        put(map, "iox", 0xFF55AA55);
        put(map, "iron", 0xFFD8D8D8);
        put(map, "iron/oxidized", 0xFFE9C8B1);
        put(map, "jauxum", 0xFF608A84);
        put(map, "karmesine", 0xFFCD3F40);
        put(map, "lead", 0xFF696579);
        put(map, "leather", 0xFFC65C35);
        put(map, "lumix", 0xFF9B8772);
        put(map, "magma", 0xFF652828);
        put(map, "manyullyn", 0xFF9261CC);
        put(map, "meteorite", 0xFF2C3F31);
        put(map, "nahuatl", 0xFF3B2754);
        put(map, "necronium", 0xFF9CBD89);
        put(map, "necrotic_bone", 0xFF2A2A2A);
        put(map, "nihilite", 0xFF31215A);
        put(map, "niob", 0xFF3C4F60);
        put(map, "nucleum", 0xFFCCE239);
        put(map, "obsidian", 0xFF3B2754);
        put(map, "obsidiorite", 0xFF1B3A42);
        put(map, "osmium", 0xFFC1E6F4);
        put(map, "osram", 0xFFAE8062);
        put(map, "ovium", 0xFF413E66);
        put(map, "palladium", 0xFF9E5924);
        put(map, "pewter", 0xFF999483);
        put(map, "phantom", 0xFFC3B9A1);
        put(map, "pig_iron", 0xFFF0A8A4);
        put(map, "plated_slimewood", 0xFFFFE170);
        put(map, "platinum", 0xFFA3E7FE);
        put(map, "prometheum", 0xFF251D31);
        put(map, "proxii", 0xFF8DAD9A);
        put(map, "queens_slime", 0xFF809912);
        put(map, "rock", 0xFFB1AFAD);
        put(map, "rock/andesite", 0xFF9C9C9C);
        put(map, "rock/blackstone", 0xFF3C3947);
        put(map, "rock/deepslate", 0xFF646464);
        put(map, "rock/diorite", 0xFFCECECF);
        put(map, "rock/granite", 0xFFA97764);
        put(map, "rock/stone", 0xFFB1AFAD);
        put(map, "rose_gold", 0xFFF7CDBB);
        put(map, "rotten_flesh", 0xFFB44420);
        put(map, "scorched_stone", 0xFF5B4C43);
        put(map, "seared_stone", 0xFF4F4A47);
        put(map, "seismum", 0xFFCCA291);
        put(map, "silver", 0xFFDAF3ED);
        put(map, "skyslime", 0xFF82D7D5);
        put(map, "skyslime_vine", 0xFF00F4DA);
        put(map, "slimeskin", 0xFF56CF4C);
        put(map, "slimesteel", 0xFF46ECE7);
        put(map, "slimewood", 0xFF56CF4C);
        put(map, "slimewood/bloodshroom", 0xFFC10000);
        put(map, "slimewood/enderbark", 0xFF5B2F66);
        put(map, "slimewood/greenheart", 0xFF56CF4C);
        put(map, "slimewood/skyroot", 0xFF00B4B8);
        put(map, "solarium", 0xFFDDD857);
        put(map, "steel", 0xFF959595);
        put(map, "terrax", 0xFF877B74);
        put(map, "tiberium", 0xFFADD000);
        put(map, "treated_wood", 0xFF5C3523);
        put(map, "triberium", 0xFF39861E);
        put(map, "tritonite", 0xFF59F5AB);
        put(map, "tungsten", 0xFF6F6F62);
        put(map, "twisting_vine", 0xFF14B485);
        put(map, "unknown", 0xFF000000);
        put(map, "uru", 0xFF9B7A9E);
        put(map, "valyrium", 0xFF81331B);
        put(map, "venombone", 0xFFA2935E);
        put(map, "vibranium", 0xFFA0B5BB);
        put(map, "vine", 0xFF48B518);
        put(map, "violium", 0xFF6D6784);
        put(map, "weeping_vine", 0xFFC53439);
        put(map, "whitestone", 0xFFCDC68B);
        put(map, "whitestone/aluminum", 0xFFE0E9EC);
        put(map, "whitestone/composite", 0xFFE0E9EC);
        put(map, "whitestone/end", 0xFFCDC68B);
        put(map, "whitestone/tin", 0xFFE0E9EC);
        put(map, "whitestone/zinc", 0xFFE0E9EC);
        put(map, "wood", 0xFF876627);
        put(map, "wood/crimson", 0xFF4B2737);
        put(map, "wood/warped", 0xFF1F5752);
        put(map, "yrdeen", 0xFF632742);
        ALL = Collections.unmodifiableMap(map);
    }

    private static void put(Map<String, Integer> map, String id, int color) {
        map.put(id, color);
    }

    /**
     * 查材料本色；查不到返回 -1。
     *
     * @param materialId 材料 id，可带命名空间（{@code tconstruct:iron}）也可不带（{@code iron}）
     */
    public static int colorOf(String materialId) {
        if (materialId == null || materialId.isEmpty()) {
            return -1;
        }
        Integer color = ALL.get(TsbMaterials.path(materialId));
        return color == null ? -1 : color;
    }

    private TsbMaterialColors() {}
}
