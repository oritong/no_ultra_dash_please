"""Draw native pixels and export diagonal-aware Java multipart slabs (no build)."""

import base64
import copy
import hashlib
import json
import uuid
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ART = Path(__file__).resolve().parent
ROOT = ART.parents[1]
NS = "no_ultra_dash_please"
ASSETS = ROOT / "src/main/resources/assets" / NS
DESIGN = json.loads((ART / "design.pixel.json").read_text(encoding="utf-8"))
TYPES = ("outer", "horizontal", "vertical", "inner", "fill")
QUARTERS = (
    ("nw", 0, 0, "north", "west", "north_west"),
    ("ne", 8, 0, "north", "east", "north_east"),
    ("se", 8, 8, "south", "east", "south_east"),
    ("sw", 0, 8, "south", "west", "south_west"),
)
DISPLAY = {
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375]*3},
    "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4]*3},
    "ground": {"translation": [0, 3, 0], "scale": [0.25]*3},
    "gui": {"rotation": [30, 225, 0], "scale": [0.625]*3},
    "fixed": {"scale": [0.5]*3},
}


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def rgba(symbol):
    data = bytes.fromhex(DESIGN["palette"][symbol][1:])
    return tuple(data) + ((255,) if len(data) == 3 else ())


def pixels(rows):
    assert len(rows) == 16 and all(len(row) == 16 for row in rows)
    image = Image.new("RGBA", (16, 16))
    image.putdata([rgba(symbol) for row in rows for symbol in row])
    return image


def dash(value):
    return value % DESIGN["dash"]["period"] < DESIGN["dash"]["length"]


def textures(collapsed, style=0):
    design = DESIGN if style == 0 else DESIGN["alternates"][style-1]
    result = {}
    for variant in TYPES:
        if collapsed and variant == "fill":
            continue
        image = Image.new("RGBA", (16, 16)) if collapsed else pixels(design["top"])
        for y in range(16):
            for x in range(16):
                horizontal = variant in ("outer", "horizontal") and y in (0, 15)
                vertical = variant in ("outer", "vertical") and x in (0, 15)
                corner = x in (0, 15) and y in (0, 15)
                if collapsed:
                    if (horizontal and dash(x)) or (vertical and dash(y)) or (corner and variant in ("outer", "inner")):
                        image.putpixel((x, y), rgba("w"))
                elif horizontal or vertical:
                    image.putpixel((x, y), rgba("l" if x != 15 and y != 15 else "s"))
                elif corner and variant == "inner":
                    image.putpixel((x, y), rgba("i"))
        result[variant] = image
    for ends in (False, True):
        if collapsed:
            image = Image.new("RGBA", (16, 16))
            for y in range(8):
                for x in range(16):
                    if (y in (0, 7) and dash(x)) or (ends and x in (0, 15) and (dash(y) or y == 7)):
                        image.putpixel((x, y), rgba("w"))
        else:
            image = pixels(design["side"])
            if ends:
                for y in range(1, 8):
                    image.putpixel((0, y), rgba("s" if y < 6 else "k"))
                    image.putpixel((15, y), rgba("i" if y < 6 else "k"))
        result["side_end" if ends else "side"] = image
    return result


def prefix_for(collapsed, style=0):
    return "crumble_platform_" + ("collapsed_" if collapsed else f"v{style}_" if style else "")


def part(quarter, variant, collapsed, style=0):
    name, x, z, first, second, _ = quarter
    prefix = prefix_for(collapsed, style)
    if collapsed and variant == "fill":
        return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "elements": [],
                "textures": {"particle": f"{NS}:block/crumble_platform_fill"}}
    top = f"{NS}:block/{prefix}{variant}"
    end = variant == "outer"
    side = f"{NS}:block/{prefix}side" + ("_end" if end else "")
    faces = {
        "up": {"uv": [x, z, x+8, z+8], "texture": "#top"},
        "down": {"uv": [x, 8-z, x+8, 16-z], "texture": "#top", "cullface": "down"},
    }
    side_uv = {
        "north": [8-x, 0, 16-x, 8], "south": [x, 0, x+8, 8],
        "west": [z, 0, z+8, 8], "east": [8-z, 0, 16-z, 8],
    }
    if variant in ("outer", "horizontal"):
        faces[first] = {"uv": side_uv[first], "texture": "#side", "cullface": first}
    if variant in ("outer", "vertical"):
        faces[second] = {"uv": side_uv[second], "texture": "#side", "cullface": second}
    return {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "ambientocclusion": not collapsed,
        "textures": {"top": top, "side": side, "particle": f"{NS}:block/{prefix_for(False, style)}fill"},
        "elements": [{"name": f"{name}_{variant}", "from": [x, 0, z], "to": [x+8, 8, z+8], "faces": faces}],
    }


def isolated_model(collapsed, style=0):
    model = part(QUARTERS[0], "outer", collapsed, style)
    model["elements"] = [{"name": "crumble_platform", "from": [0, 0, 0], "to": [16, 8, 16], "faces": {
        direction: {"uv": [0, 0, 16, 16 if direction in ("up", "down") else 8],
                    "texture": "#top" if direction in ("up", "down") else "#side"}
        for direction in ("up", "down", "north", "south", "east", "west")
    }}]
    model["display"] = DISPLAY
    return model


def conditions(first, second, diagonal, variant):
    if variant == "outer":
        return {first: "false", second: "false"}
    if variant == "horizontal":
        return {first: "false", second: "true"}
    if variant == "vertical":
        return {first: "true", second: "false"}
    return {first: "true", second: "true", diagonal: str(variant == "fill").lower()}


def bbmodel(model, name):
    texture_ids = {}
    texture_list = []
    for path in dict.fromkeys(model["textures"].values()):
        resource = ASSETS / "textures" / (path.split(":")[1] + ".png")
        number = len(texture_list)
        texture_ids[path] = number
        texture_list.append({
            "name": resource.name, "id": str(number), "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, path)),
            "namespace": NS, "folder": "block", "relative_path": "../../" + resource.relative_to(ROOT).as_posix(),
            "width": 16, "height": 16, "uv_width": 16, "uv_height": 16,
            "source": "data:image/png;base64," + base64.b64encode(resource.read_bytes()).decode(),
            "render_mode": "default", "render_sides": "front", "saved": True, "internal": True,
            "particle": path == model["textures"].get("particle"),
        })
    elements = []
    for element in model["elements"]:
        element = copy.deepcopy(element)
        element.update({"uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, name + element["name"])),
                        "type": "cube", "box_uv": False, "origin": [8, 0, 8], "autouv": 0, "export": True})
        for direction in ("up", "down", "north", "south", "east", "west"):
            if direction in element["faces"]:
                face = element["faces"][direction]
                face["texture"] = texture_ids[model["textures"][face["texture"][1:]]]
            else:
                element["faces"][direction] = {"uv": [0, 0, 0, 0], "texture": None}
        elements.append(element)
    return {
        "meta": {"format_version": "4.10", "model_format": "java_block", "box_uv": False},
        "name": name, "parent": "minecraft:block/block", "ambientocclusion": model["ambientocclusion"],
        "java_block_version": "1.9.0", "unhandled_root_fields": {"render_type": "minecraft:cutout"},
        "resolution": {"width": 16, "height": 16}, "elements": elements,
        "outliner": [element["uuid"] for element in elements], "textures": texture_list, "display": DISPLAY,
    }


def main():
    (ART / "textures").mkdir(parents=True, exist_ok=True)
    multipart = []
    for collapsed in (False, True):
        styles = range(1 if collapsed else 4)
        for style in styles:
            prefix = prefix_for(collapsed, style)
            for variant, image in textures(collapsed, style).items():
                image.save(ART / "textures" / (prefix + variant + ".png"))
                image.save(ASSETS / "textures/block" / (prefix + variant + ".png"))
            filename = "crumble_platform" + ("_collapsed" if collapsed else f"_v{style}" if style else "")
            isolated = isolated_model(collapsed, style)
            write_json(ART / (filename + ".bbmodel"), bbmodel(isolated, filename))
            if not collapsed and style == 0:
                write_json(ASSETS / "models/block/crumble_platform.json", isolated)
        for quarter in QUARTERS:
            name, _, _, first, second, diagonal = quarter
            for variant in TYPES:
                model_id = "crumble_platform/" + ("collapsed_" if collapsed else "") + name + "_" + variant
                alternatives = []
                for style in styles:
                    model = part(quarter, variant, collapsed, style)
                    style_id = model_id if style == 0 else "crumble_platform/" + f"v{style}_" + name + "_" + variant
                    if style:
                        model = {"parent": f"{NS}:block/{model_id}", "render_type": "minecraft:cutout",
                                 "textures": model["textures"]}
                    write_json(ASSETS / "models/block" / (style_id + ".json"), model)
                    alternatives.append({"model": f"{NS}:block/{style_id}", "weight": 1})
                when = {"phase": "collapsed" if collapsed else "normal|crumbling"}
                when.update(conditions(first, second, diagonal, variant))
                multipart.append({"when": when, "apply": alternatives[0] if collapsed else alternatives})
    write_json(ASSETS / "blockstates/crumble_platform.json", {"multipart": multipart})
    write_json(ASSETS / "models/item/crumble_platform.json", {"parent": f"{NS}:block/crumble_platform"})
    for directory in (ART / "textures", ASSETS / "textures/block"):
        (directory / "crumble_platform_collapsed_fill.png").unlink(missing_ok=True)
    previews()
    write_manifest()


def write_manifest():
    paths = list((ASSETS / "textures/block").glob("crumble_platform*.png"))
    paths += list((ASSETS / "models/block/crumble_platform").glob("*.json"))
    paths += [ASSETS / "models/block/crumble_platform.json", ASSETS / "models/item/crumble_platform.json",
              ASSETS / "blockstates/crumble_platform.json"]
    paths += list(ART.glob("*.bbmodel")) + [ART / "design.pixel.json", ART / "generate_assets.py"]
    write_json(ART / "manifest.json", {"sha256": {
        path.relative_to(ROOT).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest() for path in sorted(paths)
    }})


def resolved_model(path):
    data = json.loads(path.read_text(encoding="utf-8"))
    if data.get("parent", "").startswith(NS + ":"):
        parent = ASSETS / "models" / (data["parent"].split(":")[1] + ".json")
        inherited = resolved_model(parent)
        data["textures"] = inherited.get("textures", {}) | data.get("textures", {})
        for key in ("elements", "ambientocclusion"):
            if key not in data and key in inherited:
                data[key] = inherited[key]
    return data


def select_models(state, style=0):
    data = json.loads((ASSETS / "blockstates/crumble_platform.json").read_text(encoding="utf-8"))
    result = []
    for entry in data["multipart"]:
        if all(str(state[key]).lower() in value.split("|") for key, value in entry["when"].items()):
            apply = entry["apply"][style] if isinstance(entry["apply"], list) else entry["apply"]
            path = ASSETS / "models" / (apply["model"].split(":")[1] + ".json")
            result.append(resolved_model(path))
    return result


OFFSETS = {"north": (0, -1), "east": (1, 0), "south": (0, 1), "west": (-1, 0),
           "north_east": (1, -1), "south_east": (1, 1), "south_west": (-1, 1), "north_west": (-1, -1)}


def signed(value, bits):
    value &= (1 << bits)-1
    return value - (1 << bits) if value >= 1 << (bits-1) else value


def next_long(seed):
    seed = (seed ^ 0x5DEECE66D) & ((1 << 48)-1)
    values = []
    for _ in range(2):
        seed = (seed*0x5DEECE66D + 11) & ((1 << 48)-1)
        values.append(signed(seed >> 16, 32))
    return signed((values[0] << 32) + values[1], 64)


def style_at(x, z, y=0):
    # Minecraft 1.21.1 Mth.getSeed -> multipart nextLong -> weighted nextLong.
    seed = signed(x*3129871, 32) ^ (z*116129781) ^ y
    seed = signed(seed*seed*42317861 + seed*11, 64) >> 16
    return abs(signed(next_long(next_long(seed)), 32)) % 4


def scene_models(scene, styles=None):
    result = []
    for (x, z), phase in scene.items():
        state = {"phase": phase}
        for key, (dx, dz) in OFFSETS.items():
            neighbor = scene.get((x+dx, z+dz))
            state[key] = neighbor is not None and (neighbor == "collapsed") == (phase == "collapsed")
        style = styles[(x, z)] if styles and (x, z) in styles else style_at(x, z)
        result.extend((model, x*16, z*16) for model in select_models(state, style))
    return result


def top_scene(scene, styles=None):
    minx, minz = min(x for x, z in scene), min(z for x, z in scene)
    maxx, maxz = max(x for x, z in scene), max(z for x, z in scene)
    image = Image.new("RGBA", ((maxx-minx+1)*16, (maxz-minz+1)*16))
    for model, dx, dz in scene_models(scene, styles):
        if not model["elements"]:
            continue
        element = model["elements"][0]
        x, _, z = element["from"]
        face = element["faces"]["up"]
        texture = ASSETS / "textures" / (model["textures"][face["texture"][1:]].split(":")[1] + ".png")
        image.alpha_composite(Image.open(texture).crop(tuple(face["uv"])), (dx+x-minx*16, dz+z-minz*16))
    return image


def face_vertices(element, face):
    x, y, z = element["from"]
    X, Y, Z = element["to"]
    return {
        "up": [(x, Y, z), (x, Y, Z), (X, Y, Z), (X, Y, z)],
        "down": [(x, y, Z), (x, y, z), (X, y, z), (X, y, Z)],
        "north": [(X, Y, z), (X, y, z), (x, y, z), (x, Y, z)],
        "south": [(x, Y, Z), (x, y, Z), (X, y, Z), (X, Y, Z)],
        "west": [(x, Y, z), (x, y, z), (x, y, Z), (x, Y, Z)],
        "east": [(X, Y, Z), (X, y, Z), (X, y, z), (X, Y, z)],
    }[face]


def render_scene(scene, size, scale=5, yaw=0, below=False, styles=None):
    """Offline orthographic texel renderer of actual exported faces, not a game capture."""
    quads = []
    transformed = []
    for model, dx, dz in scene_models(scene, styles):
        for element in model["elements"]:
            for direction, face in element["faces"].items():
                vertices = face_vertices(element, direction)
                vertices = [(x+dx, y, z+dz) for x, y, z in vertices]
                def turn(point):
                    x, y, z = point
                    for _ in range(yaw):
                        x, z = -z, x
                    return (x, -y if below else y, z)
                vertices = [turn(v) for v in vertices]
                a, b, c = vertices[:3]
                ab, ac = [[b[i]-a[i] for i in range(3)], [c[i]-a[i] for i in range(3)]]
                normal = [ab[1]*ac[2]-ab[2]*ac[1], ab[2]*ac[0]-ab[0]*ac[2], ab[0]*ac[1]-ab[1]*ac[0]]
                # A below view reflects Y, so reverse the winding test after that reflection.
                if sum(normal) * (-1 if below else 1) <= 0:
                    continue
                path = ASSETS / "textures" / (model["textures"][face["texture"][1:]].split(":")[1] + ".png")
                texture = Image.open(path).convert("RGBA")
                u, v, U, V = face["uv"]
                for ty in range(v, V):
                    for tx in range(u, U):
                        color = texture.getpixel((tx, ty))
                        if not color[3]:
                            continue
                        positions = []
                        for su, sv in ((tx, ty), (tx, ty+1), (tx+1, ty+1), (tx+1, ty)):
                            fu, fv = (su-u)/(U-u), (sv-v)/(V-v)
                            positions.append(tuple(a[i] + fu*(vertices[3][i]-a[i]) + fv*(b[i]-a[i]) for i in range(3)))
                        depth = sum(sum(p) for p in positions)/4
                        shade = 1 if "collapsed" in model["textures"]["top"] else (1 if direction == "up" else 0.85 if direction in ("south", "north") else 0.68)
                        color = tuple(int(value*shade) for value in color[:3]) + (255,)
                        projected = [((x-z)*scale, ((x+z)*0.5-y)*scale) for x, y, z in positions]
                        quads.append((depth, projected, color))
                        transformed.extend(projected)
    result = Image.new("RGBA", size)
    if not transformed:
        return result
    minx, maxx = min(p[0] for p in transformed), max(p[0] for p in transformed)
    miny, maxy = min(p[1] for p in transformed), max(p[1] for p in transformed)
    offset = ((size[0]-maxx-minx)/2, (size[1]-maxy-miny)/2)
    draw = ImageDraw.Draw(result)
    for _, points, color in sorted(quads, key=lambda item: item[0]):
        draw.polygon([(round(x+offset[0]), round(y+offset[1])) for x, y in points], fill=color)
    return result


def previews():
    font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 19)
    small = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 15)
    title = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 27)
    board = Image.new("RGB", (1100, 930), "#202B33")
    draw = ImageDraw.Draw(board)
    draw.text((28, 18), "蔚蓝 · 第一章塌陷石台", font=title, fill="#E9EDF0")
    draw.text((28, 57), "原生 16×16 像素 / 四套位置随机纹理 / 半格薄台 / 白色虚线空框 / 连接纹理", font=font, fill="#ADBCC5")
    for index, phase in enumerate(("normal", "collapsed")):
        scene = {(0, 0): phase}
        x = 25+index*540
        draw.text((x, 100), "正常碎石" if index == 0 else "塌陷后的边框", font=font, fill="#FFFFFF")
        board.paste(render_scene(scene, (300, 170), 6), (x+50, 125), render_scene(scene, (300, 170), 6))
        top = top_scene(scene)
        board.paste(top, (x+390, 135), top)
        top = top.resize((96, 96), Image.Resampling.NEAREST)
        board.paste(top, (x+380, 170), top)
        draw.text((x+377, 275), "1× / 6×", font=small, fill="#ADBCC5")
    for index, phase in enumerate(("normal", "collapsed")):
        x = 25+index*540
        scene = {(column, 0): phase for column in range(5)}
        draw.text((x, 320), "横向拼接 · 中间没有端框", font=font, fill="#FFFFFF")
        rendered = render_scene(scene, (500, 230), 4)
        board.paste(rendered, (x, 350), rendered)
        patch = {(cx, cz): phase for cz in range(4) for cx in range(4) if (cx, cz) != (1, 1)}
        top = top_scene(patch).resize((256, 256), Image.Resampling.NEAREST)
        board.paste(top, (x+10, 595), top)
        draw.text((x+10, 860), "面状连接 / 内凹角 / 中间孔洞", font=small, fill="#ADBCC5")
        ell = {(0, 0): phase, (0, 1): phase, (0, 2): phase, (1, 2): phase, (2, 2): phase}
        top = top_scene(ell).resize((192, 192), Image.Resampling.NEAREST)
        board.paste(top, (x+300, 600), top)
        draw.text((x+300, 805), "L 形边界连续", font=small, fill="#ADBCC5")
    draw.text((28, 900), "离线导出模型示意；不是 Minecraft 或 Blockbench 截图。", font=small, fill="#9EAFBA")
    board.save(ART / "preview.png")

    board = Image.new("RGB", (1100, 600), "#202B33")
    draw = ImageDraw.Draw(board)
    draw.text((25, 15), "导出模型多视角 · 固定尺度，最近邻像素", font=title, fill="#E9EDF0")
    for row, phase in enumerate(("normal", "collapsed")):
        for col, (yaw, below) in enumerate(((0, False), (1, False), (2, False), (3, False), (0, True))):
            rendered = render_scene({(0, 0): phase}, (210, 190), 5, yaw, below)
            board.paste(rendered, (col*215+10, row*240+65), rendered)
            draw.text((col*215+28, row*240+245), "底部" if below else f"斜视 {yaw+1}", font=small, fill="#ADBCC5")
    draw.text((25, 560), "原版 Java 立方体面；内部接缝面和塌陷空心处的透明面已省略。", font=font, fill="#ADBCC5")
    board.save(ART / "views.png")

    board = Image.new("RGB", (1100, 680), "#202B33")
    draw = ImageDraw.Draw(board)
    draw.text((25, 15), "四套碎石随机纹理 · 连接形状保持一致", font=title, fill="#E9EDF0")
    for style in range(4):
        scene = {(0, 0): "normal"}
        rendered = render_scene(scene, (265, 170), 5, styles={(0, 0): style})
        board.paste(rendered, (style*270+10, 65), rendered)
        draw.text((style*270+100, 235), f"变体 {style+1}", font=font, fill="#E9EDF0")
    scene = {(x, z): "normal" for x in range(10) for z in range(6)}
    top = top_scene(scene).resize((640, 384), Image.Resampling.NEAREST)
    board.paste(top, (30, 280), top)
    draw.text((715, 305), "每块按位置稳定随机", font=font, fill="#E9EDF0")
    draw.text((715, 350), "四个分片选同一套纹理", font=font, fill="#ADBCC5")
    draw.text((715, 390), "塌陷、恢复保持原纹理", font=font, fill="#ADBCC5")
    draw.text((715, 430), "完整保留外框与内凹角", font=font, fill="#ADBCC5")
    draw.text((715, 510), "离线预览，不是游戏截图", font=small, fill="#9EAFBA")
    board.save(ART / "random-preview.png")


if __name__ == "__main__":
    main()
