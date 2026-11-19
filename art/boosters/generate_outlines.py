"""Generate only the two new booster outline forms; preserve existing bubble art."""

import base64
import json
import uuid
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ART = Path(__file__).resolve().parent
ROOT = ART.parents[1]
NS = "no_ultra_dash_please"
ASSETS = ROOT / "src/main/resources/assets" / NS
DESIGN = json.loads((ART / "outline.pixel.json").read_text(encoding="utf-8"))


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")


def texture():
    assert len(DESIGN["rows"]) == 16 and all(len(row) == 16 for row in DESIGN["rows"])
    image = Image.new("RGBA", (16, 16))
    image.putdata([tuple(bytes.fromhex(DESIGN["palette"][symbol][1:])) for row in DESIGN["rows"] for symbol in row])
    return image


def model():
    return {
        "parent": "minecraft:block/block", "render_type": "minecraft:translucent", "ambientocclusion": False,
        "textures": {"ring": f"{NS}:block/booster_outline", "particle": f"{NS}:block/booster_outline"},
        "elements": [{"name": "vacated_booster_ring", **DESIGN["geometry"], "shade": False,
                      "faces": {side: {"uv": [0, 0, 16, 16], "texture": "#ring"} for side in ("north", "south")}}],
        "display": {"gui": {"rotation": [0, 0, 0], "scale": [0.9]*3},
                    "fixed": {"rotation": [0, 0, 0]}, "ground": {"translation": [0, 3, 0], "scale": [0.3]*3}}
    }


def project(name, image_path):
    data = model()
    element_id = str(uuid.uuid5(uuid.NAMESPACE_URL, name+"_ring"))
    element = data["elements"][0] | {"uuid": element_id, "type": "cube", "box_uv": False, "origin": [8, 8, 8], "autouv": 0}
    element["faces"] = {side: {"uv": [0, 0, 16, 16], "texture": 0 if side in ("north", "south") else None}
                        for side in ("north", "south", "east", "west", "up", "down")}
    return {
        "meta": {"format_version": "4.10", "model_format": "java_block", "box_uv": False},
        "name": name, "java_block_version": "1.9.0", "ambientocclusion": False,
        "parent": "minecraft:block/block", "unhandled_root_fields": {"render_type": "minecraft:translucent"},
        "resolution": {"width": 16, "height": 16}, "elements": [element], "outliner": [element_id],
        "textures": [{"name": image_path.name, "id": "0", "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, name+"_texture")),
                      "namespace": NS, "folder": "block", "relative_path": "../textures/booster_outline.png",
                      "width": 16, "height": 16, "uv_width": 16, "uv_height": 16,
                      "source": "data:image/png;base64,"+base64.b64encode(image_path.read_bytes()).decode(),
                      "render_mode": "default", "render_sides": "front", "particle": True, "internal": True, "saved": True}],
        "display": data["display"]
    }


def main():
    image = texture()
    image_path = ART / "textures/booster_outline.png"
    image.save(image_path)
    image.save(ASSETS / "textures/block/booster_outline.png")
    for name in DESIGN["forms"]:
        write_json(ART / "models" / (name+".bbmodel"), project(name, image_path))
        write_json(ASSETS / "models/block" / (name+".json"), model())
    font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 20)
    small = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 16)
    board = Image.new("RGBA", (960, 370), "#202B33")
    draw = ImageDraw.Draw(board)
    draw.text((25, 20), "红绿泡泡 · 用掉后的白色虚线边框", font=font, fill="#E9EDF0")
    for index, (base, label) in enumerate((("booster", "绿色泡泡"), ("red_booster", "红色泡泡"))):
        x = 30+index*470
        idle = Image.open(ART / "textures" / (base+".png")).convert("RGBA")
        board.alpha_composite(idle.resize((160, 160), Image.Resampling.NEAREST), (x, 95))
        board.alpha_composite(image.resize((160, 160), Image.Resampling.NEAREST), (x+240, 95))
        draw.text((x+180, 165), "→", font=font, fill="#ADBCC5")
        draw.text((x+20, 275), label, font=font, fill="#ADBCC5")
        draw.text((x+250, 275), "原位置空框", font=font, fill="#ADBCC5")
    draw.text((25, 330), "原生 16×16；红绿共用白色边框，75% 不透明度；左侧为原有表面贴图。", font=small, fill="#9EAFBA")
    board.convert("RGB").save(ART / "outline-texture-preview.png")


if __name__ == "__main__":
    main()
