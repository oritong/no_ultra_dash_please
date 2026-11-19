"""Audit the new hollow marker assets without rebuilding or modifying the old forms."""

import base64
import io
import json

from PIL import Image

from generate_outlines import ART, ASSETS, DESIGN, texture


def main():
    image = Image.open(ASSETS / "textures/block/booster_outline.png").convert("RGBA")
    assert image.size == (16, 16) and image.tobytes() == texture().tobytes()
    assert image.tobytes() == Image.open(ART / "textures/booster_outline.png").convert("RGBA").tobytes()
    raw = image.tobytes()
    colors = {tuple(raw[index:index+4]) for index in range(0, len(raw), 4)}
    assert colors == {(0, 0, 0, 0), (255, 255, 255, 191)}
    assert image.transpose(Image.Transpose.FLIP_LEFT_RIGHT).tobytes() == image.tobytes()
    assert image.transpose(Image.Transpose.FLIP_TOP_BOTTOM).tobytes() == image.tobytes()
    assert not image.crop((4, 4, 12, 12)).getbbox(), "Outline center must remain completely hollow"
    assert image.getbbox() == (1, 1, 15, 15)
    assert not (ASSETS / "textures/block/booster_outline.png.mcmeta").exists()
    for name in DESIGN["forms"]:
        model = json.loads((ASSETS / "models/block" / (name+".json")).read_text())
        project = json.loads((ART / "models" / (name+".bbmodel")).read_text())
        assert model["render_type"] == "minecraft:translucent" and not model["ambientocclusion"]
        assert len(model["elements"]) == len(project["elements"]) == 1
        element = model["elements"][0]
        assert element["from"] == [0, 0, 8] and element["to"] == [16, 16, 8]
        assert set(element["faces"]) == {"north", "south"} and not element["shade"]
        for face in element["faces"].values():
            assert face["uv"] == [0, 0, 16, 16]
            assert model["textures"][face["texture"][1:]] == "no_ultra_dash_please:block/booster_outline"
        assert project["java_block_version"] == "1.9.0"
        assert project["resolution"] == {"width": 16, "height": 16}
        assert len(project["textures"]) == 1
        entry = project["textures"][0]
        embedded = Image.open(io.BytesIO(base64.b64decode(entry["source"].split(",")[1]))).convert("RGBA")
        linked = Image.open((ART / "models" / entry["relative_path"]).resolve()).convert("RGBA")
        assert embedded.tobytes() == linked.tobytes() == image.tobytes()
    root = ART.parents[1]
    output = root / "build/art-validation/booster-outlines/resource-check.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    report = {"forms": DESIGN["forms"], "elements_per_model": 1, "faces_per_model": 2,
              "shared_texture_size": [16, 16], "shared_textures": 1, "frame_count": 1,
              "visible_pixels": sum(alpha != 0 for alpha in image.getchannel("A").tobytes()),
              "opacity": "191/255, approximately 0.75", "symmetric": True, "hollow_center": True,
              "runtime_geometry": "one camera-facing quad at fixed source center; shared by red and green",
              "existing_shell_assets_changed": False, "build_run": False, "minecraft_tested": False}
    output.write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
