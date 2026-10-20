#!/usr/bin/env python3
"""生成《刀匠标准手册》的 Mantle 书数据（assets/tinkersslashblade/book/bladesmith/）。

Mantle 书的结构：
  index.json            章节目录（name 对应 language.lang 里的标题键）
  appearance.json       封面 / 内页边框贴图、配色
  sections/*.json       章节内的页面列表（type: mantle:text / mantle:crafting）
  <locale>/*.json       每种语言的页面内容（路径与 sections 里的 data 对应）
  <locale>/language.lang 章节标题

改文案直接改这个脚本再重跑，免得手写一堆 JSON。
"""
import json
import pathlib

HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE.parent / "src" / "main" / "resources" / "assets" / "tinkersslashblade" / "book" / "bladesmith"

INDEX = [
    {"name": "basics", "data": "sections/basics.json", "icon": {"item": "tinkersslashblade:tsb_blade_blank"}},
    {"name": "materials", "data": "sections/materials.json", "icon": {"item": "tinkersslashblade:tsb_blade"}},
    {"name": "traits", "data": "sections/traits.json", "icon": {"item": "taiga:ignitz_ingot"}},
    {"name": "finishing", "data": "sections/finishing.json", "icon": {"item": "tinkersslashblade:tsb_blade_white"}},
]

APPEARANCE = {
    "coverTexture": "tinkersslashblade:book/images/cover.png",
    "bookTexture": "tinkersslashblade:book/images/pages.png",
    "coverColor": -1,
    "arrowColor": -1,
    "slotColor": 0xE5C682,
    "drawSectionListText": True,
    "largePageTitles": False,
    "centerPageTitles": True,
    # 必须开：Mantle 只在 uniformFont 为真时才给 BookData.fontRenderer 赋值，
    # 而居中标题会直接用这个字体算宽度，否则 NPE（PageContent.addTitle）。
    "uniformFont": True,
}

SECTIONS = {
    "basics": [
        {"name": "forging", "type": "mantle:text", "data": "basics/forging.json"},
        {"name": "blank", "type": "mantle:crafting", "data": "basics/blank.json"},
        {"name": "shirasaya_blank", "type": "mantle:crafting", "data": "basics/shirasaya_blank.json"},
        {"name": "stations", "type": "mantle:text", "data": "basics/stations.json"},
        {"name": "forge_craft", "type": "mantle:crafting", "data": "basics/forge_craft.json"},
        {"name": "station_craft", "type": "mantle:crafting", "data": "basics/station_craft.json"},
        {"name": "daikatana", "type": "mantle:text", "data": "basics/daikatana.json"},
    ],
    "materials": [
        {"name": "stats", "type": "mantle:text", "data": "materials/stats.json"},
    ],
    "traits": [
        {"name": "traits", "type": "mantle:text", "data": "traits/traits.json"},
    ],
    "finishing": [
        {"name": "polish", "type": "mantle:text", "data": "finishing/polish.json"},
        {"name": "arts", "type": "mantle:text", "data": "finishing/arts.json"},
        {"name": "repair", "type": "mantle:text", "data": "finishing/repair.json"},
    ],
}

LANG = {
    "zh_cn": {
        "section": {
            "basics": "锻造",
            "materials": "材料的数值",
            "traits": "刀性",
            "finishing": "研磨与妖刀",
        },
        "pages": {
            "basics/forging.json": {
                "title": "锻造",
                "text": [
                    {"text": "把一块刀胚和一到三份同种匠魂材料锭一起放进工作台，就能锻出一把拔刀剑。"},
                    {"text": "刀身的一切数值都取自材料在匠魂里的刀身属性：耐久等于材料耐久乘刀型系数，攻击等于材料攻击加刀型修正。",
                     "paragraph": True},
                    {"text": "多放材料锭只加耐久，每多一份加一成，不会改变刀性。", "paragraph": True},
                    {"text": "不同的刀胚出不同的刀：刀胚得到打刀，白鞘胚得到白鞘。", "paragraph": True},
                ],
            },
            "basics/blank.json": {"title": "刀胚", "recipe": "tinkersslashblade:blank", "grid_size": "small"},
            "basics/shirasaya_blank.json": {"title": "白鞘胚", "recipe": "tinkersslashblade:blank_white",
                                            "grid_size": "small"},
            "basics/stations.json": {
                "title": "锻造台与装配台",
                "text": [
                    {"text": "两条路通到同一把刀：锻造台图快，装配台图精。"},
                    {"text": "锻造台：左边放刀胚，右边三个格子放同材料的锭（一至三份）。多放锭只加耐久，每多一份加一成。",
                     "paragraph": True},
                    {"text": "装配台：刀身、刀柄、刀鞘各一件，可以是三种不同材料。刀身定耐久、攻击和刀性；刀柄按阶级加耐久；刀鞘按阶级修正攻击。",
                     "paragraph": True},
                    {"text": "部件走匠魂自己的产线：金属材料把熔融流体浇进金模（浇筑台）；木头、石头这类不能浇的，拿材料配金模在部件制造台里捶。",
                     "paragraph": True},
                    {"text": "金模用金锭加一张匠魂图案在工作台里做，做一次能一直用。刀身要两份材料，刀柄、刀鞘各一份。",
                     "paragraph": True},
                    {"text": "两种台子都能直接放下、搬走，内容物会掉出来。", "paragraph": True},
                    {"text": "右列再补上副刃与副鞘、五槽全满，拼出来的就是无上大快刀——五槽数值全部硬堆，本模组的完全体。",
                     "paragraph": True},
                ],
            },
            "basics/daikatana.json": {
                "title": "无上大快刀",
                "text": [
                    {"text": "无上大快刀是装配台的完全体：刀柄一根、刀刃两柄、刀鞘两具，五个部件全部上场，数值全堆。"},
                    {"text": "耐久 =（主刃 + 副刃）耐久 × 刀柄耐久倍率 × 1.2；攻击 =（主刃 + 副刃）攻击 × 刀柄攻击倍率 + 两把刀鞘攻击的一半 × 2 + 1。五件可以是五种不同材料。",
                     "paragraph": True},
                    {"text": "刃色、刀技、修复材料看主刃；副刃的刀性按七成五生效，刀柄与两把刀鞘各按五成生效。",
                     "paragraph": True},
                    {"text": "大快刀胚也能在锻造台直接锻成刀，但那只是预支：拼满五槽才是完全体。",
                     "paragraph": True},
                ],
            },
            "basics/forge_craft.json": {"title": "锻造台", "recipe": "tinkersslashblade:blade_forge",
                                        "grid_size": "large"},
            "basics/station_craft.json": {"title": "装配台", "recipe": "tinkersslashblade:blade_station",
                                          "grid_size": "large"},
            "materials/stats.json": {
                "title": "材料的数值",
                "text": [
                    {"text": "三十七种 TAIGA 材料各有各的刀身数值，从一百耐久的玄武岩，到九百五十耐久、十点攻击的虚无石。"},
                    {"text": "在 JEI 里看任意刀胚，就能翻到「刀匠锻造」的全部条目，每条都标着耐久、攻击和材料阶级。",
                     "paragraph": True},
                    {"text": "手里的刀也会把材料、耐久、攻击写在提示里。", "paragraph": True},
                    {"text": "想调整数值？改 TAIGA 的材料数据，再重跑 tools/gen_tsb_materials.py。", "paragraph": True},
                ],
            },
            "traits/traits.json": {
                "title": "刀性",
                "text": [
                    {"text": "匠魂的特性只对匠魂工具生效，拔刀剑不是匠魂工具，所以本模组按特性重做了一套近战效果：同一材料做的刀，和它做的斧头凿子是一个脾气。"},
                    {"text": "灼热点燃目标，锯齿每次命中追加伤害，沉重击退并迟滞，轻盈挥刀更快，穿刺无视部分护甲。",
                     "paragraph": True},
                    {"text": "噬魂在击杀时额外吸收耀魂，狂战在残血时伤害更高，不稳则每次命中随机附带一种效果。",
                     "paragraph": True},
                    {"text": "还有些特性与挥刀无关（鞣制、培育、时运、矮人、崩解……），刀上只作显示。",
                     "paragraph": True},
                ],
            },
            "finishing/polish.json": {
                "title": "白鞘与研磨",
                "text": [
                    {"text": "白鞘是素刀：耐久只有六成，刀性只发挥一半，也没有刀技，但白鞘胚便宜，适合先拿它试材料。"},
                    {"text": "把白鞘和它的三份材料锭一起合成，就是研磨：耐久回满、刀性全额、刀技显现。已有的附魔、耀魂、击杀数都会保留，耐久按比例换算。",
                     "paragraph": True},
                ],
            },
            "finishing/arts.json": {
                "title": "妖刀与刀技",
                "text": [
                    {"text": "每把打刀都预设了一个刀技，由材料决定：火系横扫，穿刺突刺，沉重圆斩，噬魂虚空斩，狂战与不稳则是樱花落。"},
                    {"text": "拔刀剑本体的规矩是：只有附了魔的刀才放得出刀技。锻出来的刀本来就带着刀名，所以给它附个魔，它就成了「妖刀」。",
                     "paragraph": True},
                    {"text": "刀刃的染色也跟着材料走，三十七种材料是三十七种刃色。", "paragraph": True},
                ],
            },
            "finishing/repair.json": {
                "title": "保养",
                "text": [
                    {"text": "拔刀剑的耐久存在刀自己的状态里，不在物品的损耗值上，所以铁砧敲不好使。"},
                    {"text": "把刀和同材料的匠魂锭一起合成，每锭回复四分之一耐久。带「韧性」刀性的刀还会自己慢慢修。",
                     "paragraph": True},
                ],
            },
        },
    },
    "en_us": {
        "section": {
            "basics": "Forging",
            "materials": "Material Stats",
            "traits": "Blade Traits",
            "finishing": "Polishing & Bewitching",
        },
        "pages": {
            "basics/forging.json": {
                "title": "Forging",
                "text": [
                    {"text": "Put one blade blank and one to three ingots of the same Tinkers' material into a crafting table to forge a SlashBlade."},
                    {"text": "Every stat comes from the material's head stats: durability is the material's durability times the blade type factor, attack is the material's attack plus a blade type modifier.",
                     "paragraph": True},
                    {"text": "Extra ingots only add durability, ten percent each. They never change the trait.", "paragraph": True},
                    {"text": "Different blanks make different blades: a blade blank makes an uchigatana, a shirasaya blank makes a shirasaya.",
                     "paragraph": True},
                ],
            },
            "basics/blank.json": {"title": "Blade Blank", "recipe": "tinkersslashblade:blank", "grid_size": "small"},
            "basics/shirasaya_blank.json": {"title": "Shirasaya Blank", "recipe": "tinkersslashblade:blank_white",
                                            "grid_size": "small"},
            "basics/stations.json": {
                "title": "Forge & Station",
                "text": [
                    {"text": "Two roads lead to the same blade: the forge is quick, the station is precise."},
                    {"text": "Forge: put a blade blank on the left and one to three ingots of the same material on the right. Extra ingots only add durability, ten percent each.",
                     "paragraph": True},
                    {"text": "Station: one blade part, one handle part and one saya part, each of a different material if you like. The blade sets durability, attack and trait; the handle scales durability by tier; the saya shifts attack by tier.",
                     "paragraph": True},
                    {"text": "Parts come from Tinkers' own stations: pour molten metal into a gold cast on the casting table, or hammer a non-castable material with the cast in the part builder.",
                     "paragraph": True},
                    {"text": "Make a cast from gold ingots and a Tinkers' pattern in a crafting table; one cast lasts forever. The blade takes two materials, the handle and saya one each.",
                     "paragraph": True},
                    {"text": "Both stations can be picked up; their contents spill out.", "paragraph": True},
                    {"text": "Fill the right column as well - a second blade and a second saya - and all five slots forge the supreme great blade, where every number stacks: the full form of this mod.",
                     "paragraph": True},
                ],
            },
            "basics/daikatana.json": {
                "title": "The Supreme Great Blade",
                "text": [
                    {"text": "The supreme great blade is the station's full form: one handle, two blades and two saya, all five parts on the bench and every number stacked."},
                    {"text": "Durability = (main blade + second blade) durability × handle durability × 1.2; attack = (main + second) attack × handle attack + half of each saya's attack × 2 + 1. All five parts may be different materials.",
                     "paragraph": True},
                    {"text": "Colour, slash art and repair material follow the main blade; the second blade's trait counts at 75%, the handle and both saya at 50% each.",
                     "paragraph": True},
                    {"text": "A great blade blank can be forged directly, but that is only a down payment: filling all five slots is the real thing.",
                     "paragraph": True},
                ],
            },
            "basics/forge_craft.json": {"title": "Blade Forge", "recipe": "tinkersslashblade:blade_forge",
                                        "grid_size": "large"},
            "basics/station_craft.json": {"title": "Blade Station", "recipe": "tinkersslashblade:blade_station",
                                          "grid_size": "large"},
            "materials/stats.json": {
                "title": "Material Stats",
                "text": [
                    {"text": "The thirty-seven TAIGA materials range from 100 durability basalt to nihilite with 950 durability and 10 attack."},
                    {"text": "Look up any blade blank in JEI to browse every Blade Forging entry, each labelled with durability, attack and material tier.",
                     "paragraph": True},
                    {"text": "A finished blade lists its material, durability and attack in its tooltip.", "paragraph": True},
                    {"text": "To retune the numbers, edit the TAIGA material data and rerun tools/gen_tsb_materials.py.",
                     "paragraph": True},
                ],
            },
            "traits/traits.json": {
                "title": "Blade Traits",
                "text": [
                    {"text": "Tinkers' traits only work on Tinkers' tools, and a SlashBlade is not one. So this mod reimplements each trait as a melee effect: a blade behaves like the axe made from the same material."},
                    {"text": "Searing sets targets on fire, Jagged adds damage on every hit, Heavy knocks targets back and slows them, Lightweight swings faster, Pierce ignores part of the armor.",
                     "paragraph": True},
                    {"text": "Soul Eater absorbs extra proud souls on kill, Berserker hits harder below half health, and Unstable rolls a random effect on each hit.",
                     "paragraph": True},
                    {"text": "Some traits have nothing to do with swinging (Tanned, Cultivated, Fortune, Dwarven, Crumbling) and are only shown for reference.",
                     "paragraph": True},
                ],
            },
            "finishing/polish.json": {
                "title": "Shirasaya & Polishing",
                "text": [
                    {"text": "A shirasaya is a plain blade: sixty percent durability, half-strength trait and no slash art, but its blank is cheap. It is the perfect way to test a material."},
                    {"text": "Combine a shirasaya with three ingots of its own material to polish it: full durability, full trait, and its slash art appears. Enchantments, proud souls and kill count carry over, with durability rescaled.",
                     "paragraph": True},
                ],
            },
            "finishing/arts.json": {
                "title": "Bewitching & Slash Arts",
                "text": [
                    {"text": "Every uchigatana carries a preset slash art chosen by its material: fire materials sweep, pierce thrusts, heavy materials spin, soul eaters cut the void, berserkers and unstable blades drop cherry blossoms."},
                    {"text": "SlashBlades only release a slash art when enchanted. A forged blade already carries a name, so enchanting it makes it a bewitched blade.",
                     "paragraph": True},
                    {"text": "The steel itself is tinted by the material as well: thirty-seven materials, thirty-seven blade colours.",
                     "paragraph": True},
                ],
            },
            "finishing/repair.json": {
                "title": "Maintenance",
                "text": [
                    {"text": "A SlashBlade keeps its durability in its own blade state, not in the item damage tag, so an anvil will not help."},
                    {"text": "Craft the blade together with ingots of its own material: each ingot restores a quarter of the maximum durability. Blades with the Ductile trait also repair themselves over time.",
                     "paragraph": True},
                ],
            },
        },
    },
}


def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main():
    dump(OUT / "index.json", INDEX)
    dump(OUT / "appearance.json", APPEARANCE)
    for name, pages in SECTIONS.items():
        dump(OUT / "sections" / f"{name}.json", pages)

    for locale, data in LANG.items():
        for rel, page in data["pages"].items():
            dump(OUT / locale / rel, page)
        lines = [f"{key}={value}" for key, value in data["section"].items()]
        (OUT / locale).mkdir(parents=True, exist_ok=True)
        (OUT / locale / "language.lang").write_text("\n".join(lines) + "\n", encoding="utf-8")

    # 非本地化的兜底页（其它语言的客户端也能正常翻，不至于报缺页）
    for rel, page in LANG["en_us"]["pages"].items():
        dump(OUT / rel, page)

    print(f"生成手册数据 -> {OUT}")


if __name__ == "__main__":
    main()
