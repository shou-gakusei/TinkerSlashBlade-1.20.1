#!/usr/bin/env python3
"""从 TAIGA 的匠魂材料数据生成 com.whfwtf.tsb.TsbMaterial。

读取（相对本脚本）：
  ../../taiga3/src/main/resources/data/taiga/tinkering/materials/stats/<mat>.json
  ../../taiga3/src/main/resources/data/taiga/tinkering/materials/traits/<mat>.json
  ../../taiga3/src/main/resources/data/taiga/tinkering/materials/definition/<mat>.json

输出：
  src/main/java/com/whfwtf/tsb/TsbMaterial.java

数值取匠魂的 head（刀身）属性：durability / melee_attack / miningSpeed / mining_tier；
特性取 traits 的 default 第一条；颜色与阶级取 definition。
"""
import json
import pathlib
import re

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parent
TAIGA = ROOT.parent / "taiga3" / "src" / "main" / "resources" / "data" / "taiga" / "tinkering" / "materials"
OUT = ROOT / "src" / "main" / "java" / "com" / "whfwtf" / "tsb" / "TsbMaterial.java"

# 匠魂 mining_tier 字符串 → 挖掘等级数字（moreharvestinglevel:level_N 取 N）
TIER_LEVELS = {
    "minecraft:wood": 0,
    "minecraft:gold": 0,
    "minecraft:stone": 1,
    "minecraft:iron": 2,
    "minecraft:diamond": 3,
    "minecraft:netherite": 4,
}


def tier_level(raw):
    if raw in TIER_LEVELS:
        return TIER_LEVELS[raw]
    m = re.search(r"(\d+)$", str(raw))
    return int(m.group(1)) if m else 0


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def main():
    rows = []
    for path in sorted((TAIGA / "stats").glob("*.json")):
        mat = path.stem
        stats = load(path).get("stats", {}).get("tconstruct:head", {})
        dur = int(stats.get("durability", 0))
        atk = float(stats.get("melee_attack", stats.get("melee_damage", 0.0)))
        if dur <= 0:
            continue  # 没有 head 属性的材料不能做刀

        mine = float(stats.get("miningSpeed", stats.get("mining_speed", 0.0)))
        mtier = tier_level(stats.get("mining_tier", ""))

        trait = ""
        tpath = TAIGA / "traits" / f"{mat}.json"
        if tpath.exists():
            defaults = load(tpath).get("default", [])
            if defaults:
                trait = defaults[0].get("name", "")

        color, tier = 0xFFFFFF, 0
        dpath = TAIGA / "definition" / f"{mat}.json"
        if dpath.exists():
            d = load(dpath)
            color = int(d.get("textColor", "#FFFFFF").lstrip("#"), 16)
            tier = int(d.get("tier", 0))

        rows.append((mat, dur, atk, mine, mtier, trait, color, tier))

    lines = [
        "package com.whfwtf.tsb;",
        "",
        "import java.util.Collections;",
        "import java.util.LinkedHashMap;",
        "import java.util.Map;",
        "",
        "/**",
        " * 匠魂材料的刀身数值表。",
        " *",
        " * <p>由 {@code tools/gen_tsb_materials.py} 从 TAIGA 的匠魂材料数据",
        " * （head 属性 / 特性 / 颜色 / 阶级）生成，请勿手改：改材料请改 taiga3 的 datapack，再重新跑脚本。",
        " */",
        "public final class TsbMaterial {",
        "",
        "    /** @param id 材料名（对应 {@code taiga:<id>_ingot}）",
        "     *  @param durability 刀身耐久（匠魂 head.durability）",
        "     *  @param attack 刀身攻击（匠魂 head.melee_attack）",
        "     *  @param miningSpeed 挖掘速度（匠魂 head.miningSpeed）",
        "     *  @param miningTier 挖掘等级（匠魂 head.mining_tier 折算成数字，木=0/石=1/铁=2/钻石=3/下界合金=4）",
        "     *  @param trait 该材料在匠魂里的特性 id（如 {@code tconstruct:searing}）",
        "     *  @param color 材料文字/特效颜色（0xRRGGBB）",
        "     *  @param tier 材料阶级 1~4 */",
        "    public record Entry(String id, int durability, float attack, float miningSpeed, int miningTier,",
        "                        String trait, int color, int tier) {}",
        "",
        "    public static final Map<String, Entry> ALL;",
        "",
        "    static {",
        "        Map<String, Entry> map = new LinkedHashMap<>();",
    ]

    for mat, dur, atk, mine, mtier, trait, color, tier in rows:
        lines.append(
            f'        put(map, "{mat}", {dur}, {atk:.1f}F, {mine:.1f}F, {mtier}, "{trait}", 0x{color:06X}, {tier});'
        )

    lines += [
        "        ALL = Collections.unmodifiableMap(map);",
        "    }",
        "",
        "    private static void put(Map<String, Entry> map, String id, int durability, float attack,",
        "                            float miningSpeed, int miningTier, String trait, int color, int tier) {",
        "        map.put(id, new Entry(id, durability, attack, miningSpeed, miningTier, trait, color, tier));",
        "    }",
        "",
        "    private TsbMaterial() {}",
        "}",
        "",
    ]

    OUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"生成 {OUT}（{len(rows)} 种材料）")


if __name__ == "__main__":
    main()
