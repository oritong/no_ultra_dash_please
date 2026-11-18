"""Create native pixel dream-block assets and deterministic review previews."""

import json
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ART = Path(__file__).resolve().parent
ROOT = ART.parents[1]
TEXTURES = ROOT / "src/main/resources/assets/no_ultra_dash_please/textures/block"
DESIGN = json.loads((ART / "design.pixel.json").read_text(encoding="utf-8"))


def rgb(value):
    return tuple(bytes.fromhex(value.lstrip("#")))


def border(frame, size=16):
    image = Image.new("RGBA", (size, size))
    draw = ImageDraw.Draw(image)
    # One-pixel border. Corners stay fixed; its inside edge gently changes by one pixel.
    phase = frame / DESIGN["frame_count"] * math.tau
    for side in range(4):
        points = []
        for along in range(size):
            inset = int(2 <= along < size - 2 and math.sin(phase + along * math.tau / size + side * 1.3) > 0.5)
            x, y = ((along, inset), (size - 1 - inset, along),
                    (size - 1 - along, size - 1 - inset), (inset, size - 1 - along))[side]
            points.append((x, y))
        for index, point in enumerate(points):
            draw.point(point, fill=DESIGN["outline"])
            if index:
                # Orthogonal one-pixel joins; diagonal-only neighbors leave visual gaps.
                previous = points[index - 1]
                draw.line([previous, (point[0], previous[1]), point], fill=DESIGN["outline"], width=1)
    return image


def shape_name(layer, frame, phase):
    anim = frame / 20 * DESIGN["shape_rate_hz"]
    if layer == 0:
        return ["cross", "ring", "cross", "point"][int(anim + phase * 4) % 4]
    if layer == 1:
        return ["cross", "point"][int(anim + phase * 2) % 2]
    return "point"


def star(image, x, y, shape, color, clip=None):
    for sy, row in enumerate(DESIGN["shapes"][shape]):
        for sx, pixel in enumerate(row):
            px, py = x + sx - 2, y + sy - 2
            if pixel == "#" and 0 <= px < image.width and 0 <= py < image.height:
                if clip is None or (clip[0] <= px < clip[2] and clip[1] <= py < clip[3]):
                    image.putpixel((px, py), color)


def face(frame):
    image = Image.new("RGBA", (16, 16), DESIGN["background"])
    for entry in reversed(DESIGN["stars"]):
        layer = entry["layer"]
        color = rgb(DESIGN["palette"][["near", "middle", "far"][layer]][entry["color"]]) + (255,)
        star(image, entry["x"], entry["y"], shape_name(layer, frame, entry["phase"]), color, (2, 2, 14, 14))
    image.alpha_composite(border(frame))
    return image


def save_animation(name, frames):
    strip = Image.new("RGBA", (16, 16 * len(frames)))
    for i, frame in enumerate(frames):
        strip.paste(frame, (0, i * 16))
    path = TEXTURES / (name + ".png")
    strip.save(path)
    path.with_suffix(".png.mcmeta").write_text(json.dumps({"animation": {
        "frametime": 1, "width": 16, "height": 16, "interpolate": False
    }}, indent=2) + "\n", encoding="utf-8")


def main():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    frames = [face(i) for i in range(DESIGN["frame_count"])]
    save_animation("dream_block", frames)
    save_animation("dream_block_outline", [border(i) for i in range(DESIGN["frame_count"])])
    # Independent white masks allow the client renderer to use the original palette per star.
    atlas = Image.new("RGBA", (16, 16))
    for name, x, y in [("point", 3, 3), ("cross", 11, 3), ("ring", 3, 11)]:
        star(atlas, x, y, name, (255, 255, 255, 255))
    atlas.putpixel((15, 15), (255, 255, 255, 255))
    atlas.save(TEXTURES / "dream_block_particles.png")
    Image.new("RGBA", (16, 16), "#000000").save(TEXTURES / "dream_block_back.png")
    font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 19)
    title = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 27)
    board = Image.new("RGB", (1120, 710), "#17232d")
    draw = ImageDraw.Draw(board)
    draw.text((30, 22), "梦境方块 · 原生 16×16 动画贴图", font=title, fill="#e8f3ed")
    draw.text((30, 67), "纯黑内部 / 白色波动边线 / 三层彩色星点 / 40 帧循环", font=font, fill="#a7c4b8")
    for i, frame_index in enumerate([0, 7, 14, 21]):
        board.paste(frames[frame_index].resize((224, 224), Image.Resampling.NEAREST), (30 + 275 * i, 114))
        draw.text((30 + 275 * i, 354), f"帧 {frame_index} / 40", font=font, fill="#dce9e2")
        board.paste(frames[frame_index].convert("RGB"), (204 + 275 * i, 363))
    wall = Image.new("RGBA", (48, 48), "black")
    for y in range(3):
        for x in range(3):
            wall.paste(frames[(x * 7 + y * 11) % len(frames)], (x * 16, y * 16))
    board.paste(wall.resize((240, 240), Image.Resampling.NEAREST), (30, 424))
    draw.text((306, 434), "资源包动画：每帧 16×16，整条 16×640", font=font, fill="#c5d7ce")
    draw.text((306, 475), "星形与颜色参考 Celeste 原版；像素位置重新绘制", font=font, fill="#a7c4b8")
    draw.text((306, 516), "独立星点遮罩、边线与黑底可用于客户端分层视差", font=font, fill="#a7c4b8")
    draw.text((306, 557), "不使用高分辨率噪点、模糊或渐变", font=font, fill="#a7c4b8")
    draw.text((306, 620), "Minecraft 客户端尚未实测", font=font, fill="#8aa79a")
    board.save(ART / "texture-preview.png")
    gifs = [frame.convert("RGB").resize((256, 256), Image.Resampling.NEAREST) for frame in frames]
    gifs[0].save(ART / "texture-animation.gif", save_all=True, append_images=gifs[1:], duration=50,
                 loop=0, optimize=False, disposal=2)
    print(f"Created {len(frames)} native 16x16 frames, 2 layer textures and black backing.")


if __name__ == "__main__":
    main()
