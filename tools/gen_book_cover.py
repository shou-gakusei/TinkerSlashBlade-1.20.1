#!/usr/bin/env python3
"""生成《刀匠标准手册》的封面贴图。

Mantle 的书封面贴图约定：512x512，只有左上 206x200 的区域可见（其余透明）。
内含内页框架 pages.png 复用了 TConstruct 的书页边框（MIT，见 CREDITS.txt），此处只画封面。

输出：
  src/main/resources/assets/tinkersslashblade/book/images/cover.png
"""
import pathlib

from PIL import Image, ImageDraw, ImageFont

HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE.parent / "src" / "main" / "resources" / "assets" / "tinkersslashblade" / "book" / "images"

W, H = 206, 200
LEATHER_TOP = (46, 52, 92)
LEATHER_BOTTOM = (26, 30, 58)
STITCH = (150, 160, 205)
STEEL = (206, 214, 226)
STEEL_DK = (128, 138, 155)
GOLD = (198, 166, 84)

CJK_FONTS = [
    r"C:/Windows/Fonts/msyh.ttc",
    r"C:/Windows/Fonts/simhei.ttf",
    r"C:/Windows/Fonts/simsun.ttc",
]


def font(size):
    for path in CJK_FONTS:
        if pathlib.Path(path).exists():
            try:
                return ImageFont.truetype(path, size)
            except OSError:
                continue
    return ImageFont.load_default()


def cover():
    img = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    # 竖向渐变皮革
    grad = Image.new("RGBA", (W, H))
    gd = ImageDraw.Draw(grad)
    for y in range(H):
        t = y / (H - 1)
        gd.line([(0, y), (W, y)], fill=(
            round(LEATHER_TOP[0] * (1 - t) + LEATHER_BOTTOM[0] * t),
            round(LEATHER_TOP[1] * (1 - t) + LEATHER_BOTTOM[1] * t),
            round(LEATHER_TOP[2] * (1 - t) + LEATHER_BOTTOM[2] * t),
            255,
        ))
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, W - 1, H - 1], radius=10, fill=255)
    img.paste(grad, (0, 0), mask)

    d = ImageDraw.Draw(img)
    # 双层缝线边框
    d.rounded_rectangle([6, 6, W - 7, H - 7], radius=8, outline=STITCH, width=1)
    d.rounded_rectangle([11, 11, W - 12, H - 12], radius=6, outline=(STITCH[0] // 2, STITCH[1] // 2, STITCH[2] // 2), width=1)

    # 中间刀形徽记
    cx, cy = W // 2, 74
    d.line([(cx - 44, cy + 40), (cx + 34, cy - 38)], fill=STEEL, width=5)
    d.line([(cx - 44, cy + 42), (cx + 34, cy - 36)], fill=STEEL_DK, width=2)
    # 切先
    d.polygon([(cx + 34, cy - 38), (cx + 46, cy - 50), (cx + 40, cy - 32)], fill=STEEL)
    # 柄
    d.line([(cx - 44, cy + 40), (cx - 58, cy + 54)], fill=(92, 62, 40), width=6)
    # 镡
    d.line([(cx - 40, cy + 30), (cx - 30, cy + 40)], fill=GOLD, width=3)

    # 标题
    title = font(26)
    sub = font(13)
    t1 = "刀匠手册"
    t2 = "TINKER'S SLASHBLADE"
    w1 = d.textlength(t1, font=title)
    w2 = d.textlength(t2, font=sub)
    d.text(((W - w1) / 2, 126), t1, font=title, fill=(226, 231, 244))
    d.text(((W - w2) / 2, 162), t2, font=sub, fill=GOLD)
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    cover().save(OUT / "cover.png")
    print("生成", OUT / "cover.png")


if __name__ == "__main__":
    main()
