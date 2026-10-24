#!/usr/bin/env python3
"""为每种 TAIGA 材料 × 每种部件生成显式浇筑配方。

用 `tconstruct:casting_table`（TConstruct 最常规的浇筑配方，JEI 一定会显示），
流体写死为 `taiga:molten_<材料>`，材料 id 直接写进结果的 NBT：

  "result": {"item": "...tsb_part_blade", "nbt": "{material:/"taiga:nihilite/"}"}

之所以不用 `table_casting_material`（按流体反推材料）：那条链路 JEI 不显示我们的部件。
"""
import json
import pathlib
import zipfile

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parent
OUT = ROOT / "src" / "main" / "resources" / "data" / "tinkersslashblade" / "recipes" / "tools" / "parts" / "casting"

PARTS = {"blade": 2, "handle": 1, "saya": 1}

TCONSTRUCT_LIBS = ROOT.parent / "taiga3" / "libs"


def tconstruct_jar():
    jars = sorted(TCONSTRUCT_LIBS.glob("TConstruct-*.jar"))
    if not jars:
        raise SystemExit(f"{TCONSTRUCT_LIBS} 里找不到 TConstruct jar")
    return jars[-1]


def materials():
    java = ROOT / "src" / "main" / "java" / "com" / "whfwtf" / "tsb" / "TsbMaterial.java"
    out = []
    for line in java.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line.startswith("put(map,"):
            continue
        out.append([p.strip() for p in line[len("put(map,"):].rstrip(");").split(",")][0].strip('"'))
    return out


def ticonstruct_casting_materials():
    """TiC3 自带材料里有 head 属性且有 material_fluid 配方的 → {id: fluid 对象}。"""
    jar = tconstruct_jar()
    with zipfile.ZipFile(jar) as zf:
        head = set()
        for name in zf.namelist():
            if name.startswith("data/tconstruct/tinkering/materials/stats/") and name.endswith(".json"):
                stats = json.loads(zf.read(name).decode("utf-8")).get("stats", {})
                if "tconstruct:head" in stats:
                    head.add(pathlib.Path(name).stem)

        out = {}
        prefix = "data/tconstruct/recipes/tools/materials/casting/"
        for name in zf.namelist():
            if not (name.startswith(prefix) and name.endswith(".json")):
                continue
            mat = pathlib.Path(name).stem
            if mat not in head:
                continue
            data = json.loads(zf.read(name).decode("utf-8"))
            fluid = data.get("fluid", {})
            amount = int(fluid.get("amount", 90))
            if "tag" in fluid:
                out[mat] = {"tag": fluid["tag"], "amount": amount}
            elif "fluid" in fluid:
                out[mat] = {"fluid": fluid["fluid"], "amount": amount}
        return out


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for pattern in ("*_blade.json", "*_handle.json", "*_saya.json"):
        for old in OUT.glob(pattern):
            old.unlink()

    count = 0

    # TAIGA：流体写死 taiga:molten_<mat>
    for material in materials():
        for part, cost in PARTS.items():
            data = {
                "type": "tconstruct:casting_table",
                "cast": {"item": f"tinkersslashblade:tsb_part_{part}_cast"},
                "fluid": {"fluid": f"taiga:molten_{material}", "amount": 90 * cost},
                "cooling_time": 20 + 20 * cost,
                "result": {
                    "item": f"tinkersslashblade:tsb_part_{part}",
                    "nbt": {"material": f"taiga:{material}"},
                },
            }
            (OUT / f"{material}_{part}.json").write_text(
                json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            count += 1

    # TiC3 本体/联动材料：只取有 head 属性且有 material_fluid 配方者
    for material, fluid in sorted(ticonstruct_casting_materials().items()):
        for part, cost in PARTS.items():
            fluid_spec = dict(fluid)
            fluid_spec["amount"] = fluid["amount"] * cost
            data = {
                "type": "tconstruct:casting_table",
                "cast": {"item": f"tinkersslashblade:tsb_part_{part}_cast"},
                "fluid": fluid_spec,
                "cooling_time": 20 + 20 * cost,
                "result": {
                    "item": f"tinkersslashblade:tsb_part_{part}",
                    "nbt": {"material": f"tconstruct:{material}"},
                },
            }
            (OUT / f"{material}_{part}.json").write_text(
                json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            count += 1

    print(f"生成 {count} 条浇筑配方 -> {OUT}")


if __name__ == "__main__":
    main()
