"""Read exported assets; exhaust all 768 states and four stable art choices."""

import base64
import io
import itertools
import json
from pathlib import Path

from PIL import Image

from generate_assets import ART, ASSETS, DESIGN, NS, OFFSETS, resolved_model, select_models, style_at


def texture(model, face):
    key = face["texture"][1:]
    path = ASSETS / "textures" / (model["textures"][key].split(":")[1] + ".png")
    return Image.open(path).convert("RGBA")


def main():
    images = sorted((ASSETS / "textures/block").glob("crumble_platform*.png"))
    assert len(images) == DESIGN["budget"]["texture_count_max"]
    for path in images:
        image = Image.open(path).convert("RGBA")
        assert image.size == (16, 16), path
        assert set(image.getchannel("A").tobytes()) <= {0, 255}, path
        assert image.getbbox(), path
        assert path.read_bytes() == (ART / "textures" / path.name).read_bytes()

    blockstate = json.loads((ASSETS / "blockstates/crumble_platform.json").read_text())
    assert len(blockstate["multipart"]) == 40
    for entry in blockstate["multipart"]:
        choices = entry["apply"]
        if entry["when"]["phase"] == "collapsed":
            assert isinstance(choices, dict)
        else:
            assert len(choices) == 4 and [choice["weight"] for choice in choices] == [1]*4
            for index, choice in enumerate(choices):
                suffix = choice["model"].split("/")[-1]
                assert suffix.startswith(f"v{index}_") if index else not suffix.startswith("v")

    counts = []
    tested = 0
    unique_topologies = set()
    for phase in ("normal", "crumbling", "collapsed"):
        for bits in itertools.product((False, True), repeat=8):
            state = dict(zip(OFFSETS, bits)) | {"phase": phase}
            for style in range(4):
                models = select_models(state, style)
                assert len(models) == 4
                elements = [element for model in models for element in model["elements"]]
                assert len(elements) <= 4
                face_count = sum(len(e["faces"]) for e in elements)
                assert face_count <= 16
                counts.append((len(elements), face_count))
                top = Image.new("RGBA", (16, 16))
                topology = []
                for model in models:
                    for element in model["elements"]:
                        x, y, z = element["from"]
                        X, Y, Z = element["to"]
                        assert [X-x, Y-y, Z-z] == [8, 8, 8]
                        assert y == 0 and x in (0, 8) and z in (0, 8)
                        assert not element.get("rotation")
                        expected_sides = {side for side, on_border in (
                            ("north", z == 0), ("south", Z == 16),
                            ("west", x == 0), ("east", X == 16)) if on_border and not state[side]}
                        assert set(element["faces"]) == expected_sides | {"up", "down"}
                        for side, face in element["faces"].items():
                            u, v, U, V = face["uv"]
                            assert U-u == V-v == 8, (side, face)
                            assert all(0 <= value <= 16 for value in face["uv"])
                        face = element["faces"]["up"]
                        top.alpha_composite(texture(model, face).crop(tuple(face["uv"])), (x, z))
                    topology.append((model.get("textures", {}).get("top"), tuple(tuple(e["from"]) + tuple(e["faces"]) for e in model["elements"])))
                if phase == "collapsed":
                    expected = set()
                    # Independent raster oracle: exposed perimeter dashes, closed end corners,
                    # and one inner vertex where only the diagonal neighbor is absent.
                    for x in range(16):
                        for z in range(16):
                            horizontal = (z == 0 and not state["north"]) or (z == 15 and not state["south"])
                            vertical = (x == 0 and not state["west"]) or (x == 15 and not state["east"])
                            if horizontal and x % 4 < 2 or vertical and z % 4 < 2:
                                expected.add((x, z))
                    for x, z, a, b, diagonal in (
                        (0, 0, "north", "west", "north_west"), (15, 0, "north", "east", "north_east"),
                        (15, 15, "south", "east", "south_east"), (0, 15, "south", "west", "south_west")):
                        if not state[a] and not state[b] or state[a] and state[b] and not state[diagonal]:
                            expected.add((x, z))
                    actual = {(x, z) for z in range(16) for x in range(16) if top.getpixel((x, z))[3]}
                    assert actual == expected, (state, actual ^ expected)
                    assert all(top.getpixel(point) == (255, 255, 255, 255) for point in actual)
                else:
                    assert set(top.getchannel("A").tobytes()) == {255}
                    if style == 0 and phase == "normal":
                        unique_topologies.add(tuple(topology))
                tested += 1
    assert len(unique_topologies) == 47

    geometry_models = list((ASSETS / "models/block/crumble_platform").glob("*.json"))
    assert len(geometry_models) == 100
    for path in geometry_models + [ASSETS / "models/block/crumble_platform.json"]:
        model = resolved_model(path)
        assert model.get("render_type") == "minecraft:cutout", path
        for value in model["textures"].values():
            assert value.startswith(NS+":")
            assert (ASSETS / "textures" / (value.split(":")[1]+".png")).is_file()
        for element in model["elements"]:
            assert all(0 <= value <= 16 for value in element["from"]+element["to"])

    for path in ART.glob("*.bbmodel"):
        data = json.loads(path.read_text())
        assert data["java_block_version"] == "1.9.0"
        assert len(data["elements"]) == 1
        assert len(data["elements"][0]["faces"]) == 6
        assert data["resolution"] == {"width": 16, "height": 16}
        for entry in data["textures"]:
            embedded = Image.open(io.BytesIO(base64.b64decode(entry["source"].split(",")[1]))).convert("RGBA")
            linked = Image.open((path.parent / entry["relative_path"]).resolve()).convert("RGBA")
            assert embedded.tobytes() == linked.tobytes()
    choices = [style_at(x, z) for x in range(-8, 8) for z in range(-8, 8)]
    assert set(choices) == {0, 1, 2, 3}
    result = {
        "states": 768, "state_and_style_cases": tested, "unique_normal_topologies": len(unique_topologies),
        "random_variants": 4, "weighted_quarter_order_consistent": True,
        "textures": len(images), "texture_size": [16, 16], "total_texture_pixels": len(images)*256,
        "parts": len(geometry_models), "selected_elements_range": [min(v[0] for v in counts), max(v[0] for v in counts)],
        "selected_faces_range": [min(v[1] for v in counts), max(v[1] for v in counts)],
        "editable_projects": 5, "editable_elements_per_project": 1, "editable_faces_per_project": 6,
        "limitations": ["Offline resource and topology checks only", "No Java typecheck, mod build or Minecraft test"],
    }
    out = ART.parents[1] / "build/art-validation/crumble/assets-check.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(result, indent=2)+"\n", encoding="utf-8")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
