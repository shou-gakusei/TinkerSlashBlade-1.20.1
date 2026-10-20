#!/usr/bin/env python3
"""生成刀锻冶匠魂的刀胚图标（16x16 像素）。

输出：
  src/main/resources/assets/tinkersslashblade/textures/item/tsb_blade_blank.png
  src/main/resources/assets/tinkersslashblade/textures/item/tsb_blade_blank_white.png
"""
import pathlib
from PIL import Image, ImageDraw

HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE.parent / "src" / "main" / "resources" / "assets" / "tinkersslashblade" / "textures" / "item"

STEEL = (185, 192, 201, 255)
STEEL_HI = (233, 238, 244, 255)
STEEL_DK = (126, 133, 143, 255)
WOOD = (107, 74, 42, 255)
WOOD_HI = (154, 110, 63, 255)
PAPER = (244, 241, 231, 255)
PAPER_DK = (203, 195, 178, 255)
PAPER_SH = (160, 152, 136, 255)


def canvas():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def blade_blank():
    """素刀胚：一截带茎的刀身。"""
    im = canvas()
    d = ImageDraw.Draw(im)
    # 刀茎（会被握柄包住的部分）
    d.line([(1, 15), (4, 12)], fill=WOOD, width=2)
    d.point((1, 14), fill=WOOD_HI)
    # 刀身主对角线
    d.line([(4, 12), (14, 2)], fill=STEEL, width=3)
    # 刃口高光 + 刀背暗部
    d.line([(4, 13), (14, 3)], fill=STEEL_HI, width=1)
    d.line([(5, 11), (14, 2)], fill=STEEL_DK, width=1)
    # 切先
    d.point((14, 1), fill=STEEL_HI)
    d.point((15, 1), fill=STEEL)
    d.point((14, 2), fill=STEEL_DK)
    return im


def white_blank():
    """白鞘胚：刀还收在白色刀鞘里，只露出柄头。"""
    im = canvas()
    d = ImageDraw.Draw(im)
    d.line([(1, 15), (3, 13)], fill=WOOD, width=2)
    d.point((1, 14), fill=WOOD_HI)
    # 鞘身
    d.line([(3, 13), (14, 2)], fill=PAPER, width=3)
    d.line([(3, 14), (14, 3)], fill=PAPER_DK, width=1)
    d.line([(4, 12), (14, 2)], fill=PAPER_SH, width=1)
    # 鞘口（鲤口）与鞘尾
    d.line([(3, 12), (5, 14)], fill=WOOD_HI, width=1)
    d.point((14, 1), fill=PAPER)
    d.point((15, 2), fill=PAPER_DK)
    return im


def daikatana_blank():
    """大快刀胚：更长更厚的一柄素刀，刃口占满整条对角线。"""
    im = canvas()
    d = ImageDraw.Draw(im)
    # 加长的刀茎（会被握柄包住的部分）
    d.line([(0, 15), (4, 11)], fill=WOOD, width=2)
    d.point((0, 14), fill=WOOD_HI)
    # 刀身主对角线：比普通刀胚粗一圈（宽 4），从茎尖拉到右上角
    d.line([(3, 12), (14, 1)], fill=STEEL, width=4)
    # 刃口高光 + 刀背暗部
    d.line([(3, 13), (14, 2)], fill=STEEL_HI, width=1)
    d.line([(3, 14), (14, 3)], fill=STEEL_DK, width=1)
    d.line([(4, 11), (14, 1)], fill=STEEL_DK, width=1)
    # 切先
    d.point((15, 0), fill=STEEL_HI)
    d.point((14, 0), fill=STEEL)
    d.point((15, 1), fill=STEEL_DK)
    return im


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    blade_blank().save(OUT / "tsb_blade_blank.png")
    white_blank().save(OUT / "tsb_blade_blank_white.png")
    daikatana_blank().save(OUT / "tsb_blade_blank_daikatana.png")
    print("生成", OUT / "tsb_blade_blank.png")
    print("生成", OUT / "tsb_blade_blank_white.png")
    print("生成", OUT / "tsb_blade_blank_daikatana.png")


if __name__ == "__main__":
    main()
