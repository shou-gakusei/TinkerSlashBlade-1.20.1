#!/usr/bin/env python3
"""生成刀匠台与刀部件的贴图。

输出：
  textures/block/blade_forge.png      锻造台方块
  textures/block/blade_station.png    装配台方块
  textures/gui/blade_forge.png        锻造台界面（176x166）
  textures/gui/blade_station.png      装配台界面（176x166）
  textures/item/tsb_part_blade.png    刀身
  textures/item/tsb_part_handle.png   刀柄
  textures/item/tsb_part_saya.png     刀鞘
"""
import pathlib

from PIL import Image, ImageDraw

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "tinkersslashblade" / "textures"

PLANK = (150, 108, 66, 255)
PLANK_DK = (120, 84, 50, 255)
PLANK_LT = (176, 132, 84, 255)
IRON = (150, 156, 166, 255)
IRON_DK = (104, 110, 120, 255)
IRON_LT = (196, 202, 212, 255)
WOOD_H = (107, 74, 42, 255)
STEEL = (198, 206, 218, 255)
PAPER = (232, 226, 210, 255)

PANEL = (198, 198, 198, 255)
PANEL_DK = (85, 85, 85, 255)
PANEL_LT = (255, 255, 255, 255)
SLOT = (139, 139, 139, 255)
SLOT_DK = (55, 55, 55, 255)


def planks(draw, x0, y0, w, h):
    """画一块木板底。"""
    draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=PLANK)
    for y in range(y0, y0 + h, 4):
        draw.line([(x0, y), (x0 + w - 1, y)], fill=PLANK_DK)
    for y in range(y0 + 2, y0 + h, 8):
        draw.line([(x0, y), (x0 + w - 1, y)], fill=PLANK_LT)


def block_forge():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    planks(d, 0, 0, 16, 16)
    # 中央的砧面
    d.rectangle([2, 3, 13, 12], fill=IRON_DK)
    d.rectangle([3, 4, 12, 11], fill=IRON)
    d.line([(3, 4), (12, 4)], fill=IRON_LT)
    d.line([(3, 5), (3, 11)], fill=IRON_LT)
    # 刀痕
    d.line([(5, 10), (10, 6)], fill=(90, 96, 106, 255))
    d.point((11, 5), fill=STEEL)
    return im


def block_station():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    planks(d, 0, 0, 16, 16)
    # 两个刀架卡口
    for y in (4, 9):
        d.rectangle([2, y, 13, y + 2], fill=WOOD_H)
        d.line([(2, y), (13, y)], fill=PLANK_LT)
    d.line([(7, 3), (7, 13)], fill=IRON_DK)
    d.point((8, 6), fill=IRON)
    return im


def panel(width, height):
    im = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, width - 1, height - 1], fill=PANEL)
    d.line([(0, 0), (width - 1, 0)], fill=PANEL_LT)
    d.line([(0, 0), (0, height - 1)], fill=PANEL_LT)
    d.line([(width - 1, 0), (width - 1, height - 1)], fill=PANEL_DK)
    d.line([(0, height - 1), (width - 1, height - 1)], fill=PANEL_DK)
    return im, d


def slot(d, x, y):
    d.rectangle([x, y, x + 17, y + 17], fill=SLOT)
    d.line([(x, y), (x + 17, y)], fill=SLOT_DK)
    d.line([(x, y), (x, y + 17)], fill=SLOT_DK)
    d.line([(x + 17, y), (x + 17, y + 17)], fill=PANEL_LT)
    d.line([(x, y + 17), (x + 17, y + 17)], fill=PANEL_LT)


def container_gui(machine_slots, out_slot, arrow):
    """画一个完整的容器界面：面板 + 机器槽 + 玩家背包（3x9 + 快捷栏）。"""
    im, d = panel(176, 166)

    for (x, y) in machine_slots:
        slot(d, x, y)
    slot(d, *out_slot)

    # 箭头
    x0, x1, y = arrow
    d.line([(x0, y), (x1 - 4, y)], fill=PANEL_DK, width=2)
    d.polygon([(x1 - 5, y - 4), (x1, y), (x1 - 5, y + 4)], fill=PANEL_DK)

    # 玩家背包与快捷栏
    for row in range(3):
        for col in range(9):
            slot(d, 8 + col * 18, 84 + row * 18)
    for col in range(9):
        slot(d, 8 + col * 18, 142)
    return im


def gui_forge():
    return container_gui(
        [(26, 26), (62, 26), (80, 26), (98, 26)],
        (140, 26),
        (122, 137, 34),
    )


def gui_station():
    return container_gui(
        [(26, 20), (26, 40), (26, 60)],
        (140, 30),
        (60, 135, 38),
    )


# 部件都是「白膜」：只用灰阶画明暗，颜色交给游戏里的 ItemColor 按材料色相乘。
MASK_HI = (255, 255, 255, 255)
MASK_MID = (226, 226, 226, 255)
MASK_LOW = (186, 186, 186, 255)
MASK_DK = (146, 146, 146, 255)


def part_blade():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.line([(3, 13), (13, 3)], fill=MASK_MID, width=3)
    d.line([(3, 14), (13, 4)], fill=MASK_HI, width=1)   # 刃口高光
    d.line([(4, 12), (13, 3)], fill=MASK_LOW, width=1)  # 刀背
    d.point((14, 2), fill=MASK_HI)
    d.line([(2, 14), (4, 12)], fill=MASK_DK, width=2)   # 茎
    return im


def part_handle():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.line([(5, 11), (11, 5)], fill=MASK_MID, width=4)  # 短而粗，和刀身区分开
    d.line([(5, 12), (11, 6)], fill=MASK_DK, width=1)   # 下缘阴影
    d.line([(6, 10), (11, 5)], fill=MASK_HI, width=1)   # 上缘高光
    for i in range(3):                                   # 缠绳
        d.point((7 + i * 1, 9 - i * 1), fill=MASK_HI)
    return im


def part_saya():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.line([(4, 13), (12, 5)], fill=MASK_LOW, width=5)   # 最粗：鞘身
    d.line([(4, 13), (12, 5)], fill=MASK_MID, width=3)
    d.line([(5, 13), (12, 6)], fill=MASK_HI, width=1)
    d.line([(11, 4), (13, 6)], fill=MASK_DK, width=1)    # 鞘口
    return im


GOLD = (245, 215, 110)
GOLD_DK = (226, 188, 88)
CAVITY = (176, 168, 184, 255)
CAVITY_DK = (136, 128, 148, 255)
CAST_EDGE = (196, 148, 30)


def cast_of(mask):
    """铸模：整块金色底板 + 按部件形状挖出的灰色内腔（照匠魂原生铸模的画法）。

    匠魂的铸模贴图 = 16x16 铺满金板（深金描边 + 碎金噪点），
    形状部分是**灰色内腔**（不是深褐色凹槽），所以这里对齐配色。
    """
    w, h = mask.size
    src = mask.load()
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    dst = im.load()

    # 1) 金板：铺满，四周一圈深金描边
    for y in range(h):
        for x in range(w):
            edge = x in (0, w - 1) or y in (0, h - 1)
            dst[x, y] = CAST_EDGE if edge else GOLD

    # 2) 碎金噪点（固定图案，保证每次生成一致）
    for y in range(1, h - 1):
        for x in range(1, w - 1):
            if (x * 7 + y * 13) % 11 in (0, 3, 7):
                dst[x, y] = GOLD_DK

    # 3) 按部件剪影挖出灰色内腔，内腔边缘再压暗一格
    hollow = []
    for y in range(h):
        for x in range(w):
            if src[x, y][3] > 0 and 0 < x < w - 1 and 0 < y < h - 1:
                hollow.append((x, y))
    for (x, y) in hollow:
        dst[x, y] = CAVITY
    for (x, y) in hollow:
        for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in hollow:
                dst[nx, ny] = CAVITY_DK
    return im


def main():
    (ASSETS / "block").mkdir(parents=True, exist_ok=True)
    (ASSETS / "gui").mkdir(parents=True, exist_ok=True)
    (ASSETS / "item").mkdir(parents=True, exist_ok=True)

    block_forge().save(ASSETS / "block" / "blade_forge.png")
    block_station().save(ASSETS / "block" / "blade_station.png")
    gui_forge().save(ASSETS / "gui" / "blade_forge.png")
    gui_station().save(ASSETS / "gui" / "blade_station.png")
    part_blade().save(ASSETS / "item" / "tsb_part_blade.png")
    part_handle().save(ASSETS / "item" / "tsb_part_handle.png")
    part_saya().save(ASSETS / "item" / "tsb_part_saya.png")
    for name, maker in (("tsb_part_blade", part_blade), ("tsb_part_handle", part_handle),
                        ("tsb_part_saya", part_saya)):
        cast_of(maker()).save(ASSETS / "item" / f"{name}_cast.png")
    print("生成刀匠台 / 部件 / 铸模贴图")


if __name__ == "__main__":
    main()
