"""Offline resource and parallax invariants; does not compile or run Minecraft."""

import base64
from collections import deque
import io
import json
import math
from pathlib import Path

from PIL import Image

from generate_textures import ART, ROOT, TEXTURES, DESIGN, face
from preview_parallax import entries, render


def main():
    strip = Image.open(TEXTURES / "dream_block.png").convert("RGBA")
    assert strip.size == (16, 640)
    frames = [strip.crop((0, i * 16, 16, i * 16 + 16)) for i in range(40)]
    assert all(a.tobytes() == face(i).tobytes() for i, a in enumerate(frames))
    assert len({frame.tobytes() for frame in frames}) > 12
    assert all(frame.getextrema()[3] == (255, 255) for frame in frames)
    meta = json.loads((TEXTURES / "dream_block.png.mcmeta").read_text())
    assert meta["animation"] == {"frametime": 1, "width": 16, "height": 16, "interpolate": False}
    for frame in frames:
        white = {(x, y) for y in range(16) for x in range(16) if frame.getpixel((x, y)) == (255, 255, 255, 255)}
        found, pending = set(), deque([next(iter(white))])
        while pending:
            point = pending.popleft()
            if point in found:
                continue
            found.add(point)
            x, y = point
            pending.extend(p for p in [(x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)] if p in white and p not in found)
        assert found == white, "broken white item contour"
    particle = Image.open(TEXTURES / "dream_block_particles.png").convert("RGBA")
    assert particle.size == (16, 16) and particle.getpixel((15, 15)) == (255, 255, 255, 255)
    assert {pixel[3] for pixel in particle.get_flattened_data()} == {0, 255}
    for name, ox, oy in [("point", 1, 1), ("cross", 9, 1), ("ring", 1, 9)]:
        for y, row in enumerate(DESIGN["shapes"][name]):
            for x, value in enumerate(row):
                assert particle.getpixel((ox + x, oy + y))[3] == (255 if value == "#" else 0)
    renderer = (ROOT / "src/main/java/com/oritong/no_ultra_dash_please/DreamBlockRenderer.java").read_text()
    assert all(color[1:].upper() in renderer for values in DESIGN["palette"].values() for color in values)
    assert "0.30, 0.55, 0.80" in renderer and "seconds * 6" in renderer
    assert "DreamBlock.isDream(level, other)" in renderer and "SurfaceType.createSurface" in renderer
    assert "EdgeType.createEdge" in renderer and "VertexConsumer edges" in renderer
    # Translation stays linear and deterministic even at negative and distant block coordinates.
    shifts = []
    for origin in [(0, 0), (-100, -17), (20000000, -10000000)]:
        left = {e["id"]: e for e in entries(origin, (0, 0))}
        right = {e["id"]: e for e in entries(origin, (.01, .01))}
        for key in left.keys() & right.keys():
            a, b = left[key], right[key]
            expected = .01 * DESIGN["parallax"][a["layer"]]
            assert math.isclose(b["x"] - a["x"], expected, abs_tol=1e-8)
            assert math.isclose(b["y"] - a["y"], expected, abs_tol=1e-8)
            shifts.append(a["layer"])
    assert set(shifts) == {0, 1, 2}
    # Two adjacent surface tiles compute the same star coordinates and phase at their shared edge.
    a = {e["id"]: e for e in entries((0, 0), (.4, -.2))}
    b = {e["id"]: e for e in entries((1, 0), (.4, -.2))}
    shared = a.keys() & b.keys()
    assert shared
    for key in shared:
        assert math.isclose(a[key]["x"], b[key]["x"] + 1, abs_tol=1e-12)
        assert a[key]["color"] == b[key]["color"] and a[key]["phase"] == b[key]["phase"]
    # The exact runtime face bases yield outward quad winding in all six directions.
    bases = [((0, 0, -1), (1, 0, 0), (0, 1, 0)), ((0, 0, 1), (-1, 0, 0), (0, 1, 0)),
             ((-1, 0, 0), (0, 0, -1), (0, 1, 0)), ((1, 0, 0), (0, 0, 1), (0, 1, 0)),
             ((0, 1, 0), (1, 0, 0), (0, 0, 1)), ((0, -1, 0), (1, 0, 0), (0, 0, -1))]
    for normal, u, v in bases:
        cross = (v[1] * u[2] - v[2] * u[1], v[2] * u[0] - v[0] * u[2], v[0] * u[1] - v[1] * u[0])
        assert cross == normal
    counts = []
    for frame in range(40):
        _, count = render((frame / 40, .2), frame / 20)
        counts.append(count)
    model_path = ROOT / "src/main/resources/assets/no_ultra_dash_please/models/block/dream_block.json"
    model = json.loads(model_path.read_text())
    assert len(model["elements"]) == 1 and len(model["elements"][0]["faces"]) == 6
    assert model["elements"][0]["shade"] is False
    for direction, value in model["elements"][0]["faces"].items():
        assert value["cullface"] == direction and value["uv"] == [0, 0, 16, 16]
    bb = json.loads((ART / "dream_block.bbmodel").read_text())
    assert bb["java_block_version"] == "1.9.0" and len(bb["elements"]) == 1
    assert strip.tobytes() == Image.open(io.BytesIO(base64.b64decode(bb["textures"][0]["source"].split(",")[1]))).convert("RGBA").tobytes()
    report = {"native_frame_size": [16, 16], "frames": 40, "strip_size": [16, 640],
              "item_contour_connected_all_frames": True, "mask_binary_alpha": True,
              "parallax_layers": DESIGN["parallax"], "parallax_translation_checks": len(shifts),
              "cross_tile_star_continuity": True, "outward_face_winding": True,
              "item_model_cubes": 1, "item_model_faces": 6,
              "runtime_3x4_surface_star_quads_sample_min_max": [min(counts), max(counts)],
              "runtime_render_texture": [16, 16], "runtime_materials": 1,
              "build_run": False, "minecraft_client_tested": False,
              "limits": ["Reference simulation exercises geometry math, not the actual GPU renderer.",
                         "Client render, old-world migration and API compilation remain untested per no-build instruction."]}
    out = ROOT / "build/art-validation/dream"
    out.mkdir(parents=True, exist_ok=True)
    (out / "resource-check.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
