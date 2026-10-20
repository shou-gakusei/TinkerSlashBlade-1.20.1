#!/usr/bin/env python3
"""为每种 TAIGA 材料 × 每种部件生成显式浇筑配方。

用 `tconstruct:casting_table`（TConstruct 最常规的浇筑配方，JEI 一定会显示），
流体写死为 `taiga:molten_<材料>`，材料 id 直接写进结果的 NBT：

  "result": {"item": "...tsb_part_blade", "nbt": "{material:/"taiga:nihilite/"}"}

之所以不用 `table_casting_material`（按流体反推材料）：那条链路 JEI 不显示我们的部件。
"""
import json
import pathlib

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parent
OUT = ROOT / "src" / "main" / "resources" / "data" / "tinkersslashblade" / "recipes" / "tools" / "parts" / "casting"

PARTS = {"blade": 2, "handle": 1, "saya": 1}


def materials():
    java = ROOT / "src" / "main" / "java" / "com" / "whfwtf" / "tsb" / "TsbMaterial.java"
    out = []
    for line in java.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line.startswith("put(map,"):
            continue
        out.append([p.strip() for p in line[len("put(map,"):].rstrip(");").split(",")][0].strip('"'))
    return out


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("*_blade.json"):
        old.unlink()
    for old in OUT.glob("*_handle.json"):
        old.unlink()
    for old in OUT.glob("*_saya.json"):
        old.unlink()

    count = 0
    for material in materials():
        for part, cost in PARTS.items():
            data = {
                "type": "tconstruct:casting_table",
                "cast": {"item": f"tinkersslashblade:tsb_part_{part}_cast"},
                "fluid": {"fluid": f"taiga:molten_{material}", "amount": 90 * cost},
                "cooling_time": 20 + 20 * cost,
                # Mantle 的 NBTLoadable 要的是 JSON 对象，不是 SNBT 字符串
                "result": {
                    "item": f"tinkersslashblade:tsb_part_{part}",
                    "nbt": {"material": f"taiga:{material}"},
                },
            }
            (OUT / f"{material}_{part}.json").write_text(
                json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            count += 1
    print(f"生成 {count} 条浇筑配方 -> {OUT}")


if __name__ == "__main__":
    main()
