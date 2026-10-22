#!/usr/bin/env python3
"""从 TAIGA / 匠魂的材料数据生成 com.whfwtf.tsb.TsbMaterial 与 TsbMaterialColors。

读取（相对本脚本）：
  ../../taiga3/src/main/resources/data/taiga/tinkering/materials/stats/<mat>.json
  ../../taiga3/src/main/resources/data/taiga/tinkering/materials/traits/<mat>.json
  ../../taiga3/src/main/resources/data/taiga/tinkering/materials/definition/<mat>.json
  ../../taiga3/src/main/resources/assets/taiga/tinkering/materials/<mat>.json
  ../../taiga3/libs/TConstruct-*.jar ! assets/tconstruct/tinkering/materials/<mat>.json

输出：
  src/main/java/com/whfwtf/tsb/TsbMaterial.java         刀身数值表
  src/main/java/com/whfwtf/tsb/TsbMaterialColors.java   材料本色表（0xAARRGGBB）

数值取匠魂的 head（刀身）属性：durability / melee_attack / miningSpeed / mining_tier；
特性取 traits 的 default 第一条；颜色与阶级取 definition。
颜色表取匠魂材质外观 json 的顶层 color 字段（材料本色），带 parent 的沿 parent 继承。
"""
import json
import pathlib
import re
import zipfile

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parent
TAIGA3 = ROOT.parent / "taiga3"
TAIGA = TAIGA3 / "src" / "main" / "resources" / "data" / "taiga" / "tinkering" / "materials"
TAIGA_ASSETS = TAIGA3 / "src" / "main" / "resources" / "assets" / "taiga" / "tinkering" / "materials"
TCONSTRUCT_LIBS = TAIGA3 / "libs"
OUT = ROOT / "src" / "main" / "java" / "com" / "whfwtf" / "tsb" / "TsbMaterial.java"
OUT_COLORS = ROOT / "src" / "main" / "java" / "com" / "whfwtf" / "tsb" / "TsbMaterialColors.java"

# 匠魂材质外观 json 在 jar 里的位置：assets/<命名空间>/tinkering/materials/<id>.json
JAR_ASSET_PREFIX = "assets/{ns}/tinkering/materials/"

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


# ------------------------------------------------------------------ 材质数值表

def gen_tsb_material():
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
    return rows


# ------------------------------------------------------------------ 材料颜色表

def find_tconstruct_jar():
    """匠魂 jar（材质外观 json 在里面）。取版本号最大的那个。"""
    jars = sorted(TCONSTRUCT_LIBS.glob("TConstruct-*.jar"))
    if not jars:
        raise SystemExit(f"在 {TCONSTRUCT_LIBS} 里找不到 TConstruct-*.jar，无法生成材料颜色表")
    return jars[-1]


def load_appearances():
    """收集所有材质外观 json。

    @return (名称空间 → {相对路径 → json})
    """
    tree = {"tconstruct": {}, "taiga": {}}

    jar = find_tconstruct_jar()
    with zipfile.ZipFile(jar) as zf:
        for name in zf.namelist():
            for ns in tree:
                prefix = JAR_ASSET_PREFIX.format(ns=ns)
                if name.startswith(prefix) and name.endswith(".json"):
                    rel = name[len(prefix):-len(".json")]
                    tree[ns][rel] = json.loads(zf.read(name).decode("utf-8"))

    for path in sorted(TAIGA_ASSETS.rglob("*.json")):
        rel = path.relative_to(TAIGA_ASSETS).as_posix()[:-len(".json")]
        tree["taiga"][rel] = load(path)

    return tree


def parse_color(raw):
    """匠魂的颜色字段是 "AARRGGBB" 字符串。"""
    s = str(raw).strip().lstrip("#")
    if len(s) == 6:  # 只有 RGB，补个不透明 alpha
        s = "FF" + s
    return int(s, 16)


def resolve_color(tree, ns, rel, seen=None):
    """取材质本色：自身 color → 沿 parent 继承 → 没有就返回 None。"""
    seen = set() if seen is None else seen
    if (ns, rel) in seen:  # 环，认输
        return None
    seen.add((ns, rel))

    data = tree.get(ns, {}).get(rel)
    if data is None:
        return None
    if "color" in data:
        return parse_color(data["color"])
    parent = data.get("parent")
    if parent:
        pns, _, prel = str(parent).partition(":")
        if not prel:
            pns, prel = ns, prel
        return resolve_color(tree, pns, prel, seen)
    return None


def collect_colors():
    """所有材料（TAIGA + 匠魂自带）→ 材料本色 0xAARRGGBB。

    key 是材质路径（不含命名空间），TAIGA 优先（同名时以 TAIGA 为准）。
    """
    tree = load_appearances()
    colors = {}

    for ns in ("tconstruct", "taiga"):
        for rel in sorted(tree[ns]):
            # ui_render/* 是匠魂材质渲染 UI 的贴图信息，不是材料
            if ns == "tconstruct" and rel.startswith("ui_render/"):
                continue
            color = resolve_color(tree, ns, rel)
            if color is None:
                continue  # 没有 color 也没有 parent 的（如 string）取不到颜色
            if rel in colors and colors[rel] != color:
                print(f"警告：材料 {rel} 在 tconstruct 与 taiga 下颜色不同，以 taiga 为准")
            colors[rel] = color

    return dict(sorted(colors.items()))


def gen_tsb_material_colors():
    colors = collect_colors()

    lines = [
        "package com.whfwtf.tsb;",
        "",
        "import java.util.Collections;",
        "import java.util.LinkedHashMap;",
        "import java.util.Map;",
        "",
        "/**",
        " * 匠魂材料本色表（0xAARRGGBB）。",
        " *",
        " * <p>由 {@code tools/gen_tsb_materials.py} 生成：扫描匠魂材质外观 json",
        " * （jar 内 {@code assets/tconstruct/tinkering/materials/<id>.json}）与 TAIGA 的",
        " * （{@code assets/taiga/tinkering/materials/<id>.json}），取顶层 {@code color} 字段；",
        " * 带 {@code parent} 的（如 {@code rock} → {@code tconstruct:rock/stone}）沿 parent 继承颜色。",
        " * 请勿手改：改颜色请改上游材质 json，再重新跑脚本。",
        " *",
        " * <p>key 是材料路径（不含命名空间），所以 {@code tconstruct:iron} 与 {@code iron} 都查得到。",
        " */",
        "public final class TsbMaterialColors {",
        "",
        "    /** 材料路径（不含命名空间）→ 材料本色 0xAARRGGBB。 */",
        "    public static final Map<String, Integer> ALL;",
        "",
        "    static {",
        "        Map<String, Integer> map = new LinkedHashMap<>();",
    ]

    for rel, color in colors.items():
        lines.append(f'        put(map, "{rel}", 0x{color:08X});')

    lines += [
        "        ALL = Collections.unmodifiableMap(map);",
        "    }",
        "",
        "    private static void put(Map<String, Integer> map, String id, int color) {",
        "        map.put(id, color);",
        "    }",
        "",
        "    /**",
        "     * 查材料本色；查不到返回 -1。",
        "     *",
        "     * @param materialId 材料 id，可带命名空间（{@code tconstruct:iron}）也可不带（{@code iron}）",
        "     */",
        "    public static int colorOf(String materialId) {",
        "        if (materialId == null || materialId.isEmpty()) {",
        "            return -1;",
        "        }",
        "        Integer color = ALL.get(TsbMaterials.path(materialId));",
        "        return color == null ? -1 : color;",
        "    }",
        "",
        "    private TsbMaterialColors() {}",
        "}",
        "",
    ]

    OUT_COLORS.write_text("\n".join(lines), encoding="utf-8")
    print(f"生成 {OUT_COLORS}（{len(colors)} 种材料）")
    return colors


def main():
    gen_tsb_material()
    gen_tsb_material_colors()


if __name__ == "__main__":
    main()
