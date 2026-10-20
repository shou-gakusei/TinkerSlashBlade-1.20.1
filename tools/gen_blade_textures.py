#!/usr/bin/env python3
"""为每种 TAIGA 材料生成一张刃纹贴图。

做法：取拔刀剑默认刀身贴图（SlashBladeResharped jar 内的 assets/slashblade/model/blade.png），
把「低饱和度」的像素（也就是钢面、刃口、镐部）按材料色重新着色，保留原来的明暗；
刀鞘 / 刀柄那些本来就带暖色的像素不动。这样 37 种材料在手里一眼能区分。

输出：
  src/main/resources/assets/tinkersslashblade/model/blade/<material>.png
"""
import colorsys
import pathlib
import zipfile

from PIL import Image

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parent
OUT = ROOT / "src" / "main" / "resources" / "assets" / "tinkersslashblade" / "model" / "blade"
SB_JAR = ROOT.parent / "taiga3" / "libs" / "SlashBladeResharped-1.20.1-1.9.65.jar"
SB_TEXTURE = "assets/slashblade/model/blade.png"

# 饱和度阈值：低于这个值的像素算「钢面」
SATURATION_LIMIT = 0.28
# 材料色注入强度（0~1）
TINT_STRENGTH = 0.62


def read_materials():
    """从生成的 TsbMaterial.java 里读 (材料名, 颜色)。"""
    java = ROOT / "src" / "main" / "java" / "com" / "whfwtf" / "tsb" / "TsbMaterial.java"
    out = []
    for line in java.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line.startswith("put(map,"):
            continue
        # put(map, "abyssum", 250, 5.2F, "tconstruct:enderference", 0x105B5E, 2);
        parts = [p.strip() for p in line[len("put(map,"):].rstrip(");").split(",")]
        material = parts[0].strip('"')
        color = int(parts[4].strip(), 16)
        out.append((material, color))
    return out


def load_base():
    with zipfile.ZipFile(SB_JAR) as jar:
        with jar.open(SB_TEXTURE) as f:
            import io
            return Image.open(io.BytesIO(f.read())).convert("RGBA")


def tint(src: Image.Image, color: int) -> Image.Image:
    cr = (color >> 16) & 0xFF
    ch, cs, cv = colorsys.rgb_to_hsv(cr / 255, ((color >> 8) & 0xFF) / 255, (color & 0xFF) / 255)
    dst = src.copy()
    px_src = src.load()
    px_dst = dst.load()
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = px_src[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s > SATURATION_LIMIT:
                continue
            # 亮度高（近白）的地方少上色，避免把高光染脏
            strength = TINT_STRENGTH * (0.35 + 0.65 * (1.0 - v))
            new_s = min(1.0, s * (1 - strength) + cs * strength)
            nr, ng, nb = colorsys.hsv_to_rgb(ch, new_s, v)
            px_dst[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return dst


def main():
    base = load_base()
    materials = read_materials()
    OUT.mkdir(parents=True, exist_ok=True)
    for material, color in materials:
        tint(base, color).save(OUT / f"{material}.png")
    print(f"生成 {len(materials)} 张刃纹贴图 -> {OUT}")


if __name__ == "__main__":
    main()
