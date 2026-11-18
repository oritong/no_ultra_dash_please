"""Generate original 16px booster textures and a small rounded shell mesh."""

import json
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ART = Path(__file__).resolve().parent
ROOT = ART.parents[1]
ASSETS = ROOT / "src/main/resources/assets/no_ultra_dash_please"
DESIGN = json.loads((ART / "design.pixel.json").read_text(encoding="utf-8"))


def texture(color, occupied, frame):
    image = Image.new("RGBA", (16, 16))
    palette = DESIGN["palette"][color]
    for y, row in enumerate(DESIGN["rows"]):
        for x, value in enumerate(row):
            rgb = tuple(bytes.fromhex(palette[value][1:]))
            alpha = (34 if value == "d" else 52 if value == "m" else 120 if value == "a" else 224) if occupied else 255
            image.putpixel((x, y), rgb + (alpha,))
    # Restrict variation to the internal small reflection, never wobble the entire UV grid.
    spot = [(9, 9), (9, 8), (10, 8), (10, 9), (9, 10)][frame % 5]
    c = tuple(bytes.fromhex(palette["a"][1:])) + ((110 if occupied else 255),)
    image.putpixel(spot, c)
    if occupied:
        # The occupied form's larger highlight is offset as in the source inside frames.
        image.putpixel((10 + (frame & 1), 3), tuple(bytes.fromhex(palette["h"][1:])) + (224,))
    return image


def shell(occupied):
    segments = DESIGN["geometry"]["segments"]
    divisions = DESIGN["geometry"]["latitude_divisions"]
    radii = DESIGN["geometry"]["occupied_radii" if occupied else "idle_radii"]
    center = [0, 18 if occupied else 8, 0]
    unit = {"top": [0, 1, 0], "bottom": [0, -1, 0]}
    for ring in range(1, divisions):
        phi = math.pi * ring / divisions
        for i in range(segments):
            theta = math.tau * i / segments
            unit[f"r{ring}_{i}"] = [math.sin(phi) * math.cos(theta), math.cos(phi), math.sin(phi) * math.sin(theta)]
    vertices = {k: [center[i] + v[i] * radii[i] for i in range(3)] for k, v in unit.items()}
    face_vertices = []
    for i in range(segments):
        j = (i + 1) % segments
        face_vertices.append(["top", f"r1_{j}", f"r1_{i}"])
        for ring in range(1, divisions - 1):
            face_vertices.append([f"r{ring}_{i}", f"r{ring}_{j}", f"r{ring+1}_{j}", f"r{ring+1}_{i}"])
        face_vertices.append(["bottom", f"r{divisions-1}_{i}", f"r{divisions-1}_{j}"])
    faces = []
    for keys in face_vertices:
        a, b, c = [unit[k] for k in keys[:3]]
        ab, ac = [[b[i] - a[i] for i in range(3)], [c[i] - a[i] for i in range(3)]]
        normal = [ab[1]*ac[2]-ab[2]*ac[1], ab[2]*ac[0]-ab[0]*ac[2], ab[0]*ac[1]-ab[1]*ac[0]]
        centroid = [sum(unit[k][i] for k in keys) / len(keys) for i in range(3)]
        if sum(x*y for x, y in zip(normal, centroid)) < 0:
            keys = list(reversed(keys))
        axis = max(range(3), key=lambda i: abs(normal[i]))
        axes = (2, 1) if axis == 0 else (0, 2) if axis == 1 else (0, 1)
        uvs = {k: [8 + unit[k][axes[0]] * 7, 8 - unit[k][axes[1]] * 7] for k in keys}
        faces.append({"vertices": keys, "uv": uvs})
    return {"vertices": vertices, "faces": faces, "unit_vertices": unit, "radii": radii, "center": center}


def main():
    (ART / "models").mkdir(parents=True, exist_ok=True)
    (ART / "textures").mkdir(parents=True, exist_ok=True)
    texture_dir = ASSETS / "textures/block"
    texture_dir.mkdir(parents=True, exist_ok=True)
    for color in ["green", "red"]:
        for occupied in [False, True]:
            name = ("booster" if color == "green" else "red_booster") + ("_occupied" if occupied else "")
            count = DESIGN["occupied_frames" if occupied else "idle_frames"]
            frames = [texture(color, occupied, i) for i in range(count)]
            strip = Image.new("RGBA", (16, 16 * count))
            for i, frame in enumerate(frames):
                strip.paste(frame, (0, 16 * i))
            strip.save(texture_dir / (name + ".png"))
            (texture_dir / (name + ".png.mcmeta")).write_text(json.dumps({"animation": {
                "width": 16, "height": 16, "frametime": 2, "interpolate": False
            }}, indent=2) + "\n", encoding="utf-8")
            frames[0].save(ART / "textures" / (name + ".png"))
            (ART / "models" / (name + ".mesh.json")).write_text(json.dumps(shell(occupied), indent=2) + "\n", encoding="utf-8")
    font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 19)
    title = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 27)
    board = Image.new("RGB", (980, 400), "#17252e")
    draw = ImageDraw.Draw(board)
    draw.text((26, 18), "绿泡泡 / 红泡泡 · 原生 16×16 像素表面", font=title, fill="#e2f0e9")
    for i, (name, label) in enumerate([("booster", "绿色 · 待机"), ("booster_occupied", "绿色 · 裹人"),
                                        ("red_booster", "红色 · 待机"), ("red_booster_occupied", "红色 · 裹人")]):
        im = Image.open(ART / "textures" / (name + ".png"))
        board.paste(im.resize((192, 192), Image.Resampling.NEAREST), (26 + i * 242, 91), im.resize((192, 192), Image.Resampling.NEAREST))
        draw.text((26 + i * 242, 303), label, font=font, fill="#c9ddd2")
    draw.text((26, 355), "原版色板 / 待机 5 帧 / 裹人 4 帧 / 动画每帧 0.1 秒", font=font, fill="#9ebdac")
    board.save(ART / "texture-preview.png")
    print("Created four textures and four 42-vertex/48-face shells.")


if __name__ == "__main__":
    main()
