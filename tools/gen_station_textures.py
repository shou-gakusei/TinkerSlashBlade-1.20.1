#!/usr/bin/env python3
"""生成刀匠台与刀部件的贴图。

输出：
  textures/block/blade_forge.png      锻造台方块
  textures/block/blade_station.png    装配台方块
  textures/gui/blade_forge.png        锻造台界面（176x204，工匠砧式）
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


def player_inventory(d, y0):
    """画玩家背包（3x9）+ 快捷栏，y0 是背包第一行的 Y。

    快捷栏比第四行网格低 4 格（+58），和原版容器一致（label 在 y0-12）。
    """
    for row in range(3):
        for col in range(9):
            slot(d, 8 + col * 18, y0 + row * 18)
    for col in range(9):
        slot(d, 8 + col * 18, y0 + 58)


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

    player_inventory(d, 84)
    return im


def gui_forge():
    """工匠砧式锻造台界面（176x204）：左配方列 + 右刀胚/锭/产物。

    坐标与 ForgeMenu / ForgeScreen 对齐：
      配方槽 (8,30) (8,54)；分隔线 x=76；
      刀胚 (115,22)；锭 (97,46) (115,46) (133,46)；
      竖向箭头 x=124；产物 (115,82)；背包 y0=122。
    """
    im, d = panel(176, 204)

    # 左列：配方槽（选中高亮由界面代码描边，这里只画槽底）
    slot(d, 8, 30)
    slot(d, 8, 54)
    # 配方列分隔线
    d.line([(76, 16), (76, 84)], fill=PANEL_DK, width=1)

    # 右侧：刀胚 → 材料锭 →（竖向箭头）→ 产物
    slot(d, 115, 22)
    for i in range(3):
        slot(d, 97 + i * 18, 46)
    slot(d, 115, 82)
    x = 124
    d.line([(x, 66), (x, 73)], fill=PANEL_DK, width=2)
    d.polygon([(x - 4, 72), (x + 4, 72), (x, 78)], fill=PANEL_DK)

    player_inventory(d, 122)
    return im


def gui_station():
    # 2x3 网格：左列 = 主件（刀身 / 刀柄 / 刀鞘），右列 = 副件（副刃 / 副柄 / 副鞘）
    return container_gui(
        [(26, 20), (44, 20), (26, 40), (44, 40), (26, 60), (44, 60)],
        (140, 30),
        (66, 135, 38),
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
