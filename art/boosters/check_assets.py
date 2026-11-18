"""Audit booster source, BB/OBJ topology, pixel animation and player clearance offline."""

import base64
from collections import Counter
import io
import json
import math
from pathlib import Path

from PIL import Image

from generate_assets import ART, ASSETS, DESIGN, texture


def main():
    reports = []
    for name in ["booster", "red_booster", "booster_occupied", "red_booster_occupied"]:
        occupied = name.endswith("_occupied")
        color = "red" if name.startswith("red") else "green"
        count = DESIGN["occupied_frames" if occupied else "idle_frames"]
        source = json.loads((ART / "models" / (name + ".mesh.json")).read_text())
        bb = json.loads((ART / "models" / (name + ".bbmodel")).read_text())
        assert len(bb["elements"]) == 1 and bb["elements"][0]["type"] == "mesh"
        assert len(source["vertices"]) == len(bb["elements"][0]["vertices"]) == 42
        assert len(source["faces"]) == len(bb["elements"][0]["faces"]) == 48
        edges = Counter()
        triangles = 0
        for face in source["faces"]:
            keys = face["vertices"]
            assert len(keys) in [3, 4]
            triangles += len(keys) - 2
            for a, b in zip(keys, keys[1:] + keys[:1]):
                edges[tuple(sorted((a, b)))] += 1
            a, b, c = [source["vertices"][k] for k in keys[:3]]
            ab = [b[i] - a[i] for i in range(3)]
            ac = [c[i] - a[i] for i in range(3)]
            normal = [ab[1]*ac[2]-ab[2]*ac[1], ab[2]*ac[0]-ab[0]*ac[2], ab[0]*ac[1]-ab[1]*ac[0]]
            center = [sum(source["vertices"][k][i] for k in keys)/len(keys)-source["center"][i] for i in range(3)]
            assert sum(n*c for n,c in zip(normal,center)) > 0
            assert all(0 <= v <= 16 for uv in face["uv"].values() for v in uv)
        assert set(edges.values()) == {2} and 42 - len(edges) + 48 == 2
        parsed = {"v": [], "vt": [], "vn": [], "f": []}
        for line in (ASSETS / "models/block" / (name + ".obj")).read_text().splitlines():
            words = line.split()
            if words and words[0] in parsed:
                parsed[words[0]].append(words[1:])
        assert len(parsed["v"]) == 42 and len(parsed["f"]) == 48
        exported = [list(map(float,v)) for v in parsed["v"]]
        for vertex in source["vertices"].values():
            point = [vertex[0]/16+.5,vertex[1]/16,vertex[2]/16+.5]
            assert any(math.dist(point,v) < 1e-10 for v in exported)
        assert sum(len(face)-2 for face in parsed["f"]) == triangles == 80
        for face in parsed["f"]:
            indices = [list(map(int,p.split('/'))) for p in face]
            a,b,c = [exported[p[0]-1] for p in indices[:3]]
            ab,ac = [[b[i]-a[i] for i in range(3)],[c[i]-a[i] for i in range(3)]]
            n = [ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]]
            assert all(sum(n[i]*float(parsed['vn'][p[2]-1][i]) for i in range(3)) > 0 for p in indices)
        image = Image.open(ASSETS / "textures/block" / (name + ".png")).convert("RGBA")
        assert image.size == (16,16*count)
        for frame in range(count):
            im = image.crop((0,16*frame,16,16*frame+16))
            assert im.tobytes() == texture(color,occupied,frame).tobytes()
            if occupied:
                assert 0 < im.getextrema()[3][0] < 80 and im.getextrema()[3][1] >= 200
            else:
                assert im.getextrema()[3] == (255,255)
        stored = Image.open(io.BytesIO(base64.b64decode(bb["textures"][0]["source"].split(",")[1]))).convert("RGBA")
        assert stored.tobytes() == image.crop((0,0,16,16)).tobytes()
        meta = json.loads((ASSETS / "textures/block" / (name + ".png.mcmeta")).read_text())
        assert meta["animation"] == {"width":16,"height":16,"frametime":2,"interpolate":False}
        model = json.loads((ASSETS / "models/block" / (name + ".json")).read_text())
        assert model["loader"] == "neoforge:obj" and model["model"].endswith(name+'.obj') and model["flip_v"]
        assert ("minecraft:translucent" if occupied else "minecraft:solid") == model["render_type"]
        reports.append({"name":name,"meshes":1,"vertices":42,"faces":48,"triangles":80,
                        "closed_manifold":True,"outward_normals":True,"editor_export_match":True,
                        "frame_size":[16,16],"frames":count,"alpha":"translucent" if occupied else "opaque"})
    # All eight corners of a normal player body fit well inside the occupied ellipsoid.
    for x in [-.30,.30]:
        for z in [-.30,.30]:
            for y in [0,1.8]:
                assert (x/.75)**2+((y-.9)/1.125)**2+(z/.75)**2 < 1
                point = (x/.75,(y-.9)/1.125,z/.75)
                unit = json.loads((ART/'models/booster_occupied.mesh.json').read_text())
                for face in unit['faces']:
                    a,b,c = [unit['unit_vertices'][k] for k in face['vertices'][:3]]
                    ab,ac = [[b[i]-a[i] for i in range(3)],[c[i]-a[i] for i in range(3)]]
                    n = (ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0])
                    assert sum(n[i]*(point[i]-a[i]) for i in range(3)) < 1e-10
    root = ART.parents[1]
    renderer = (root/'src/main/java/com/oritong/no_ultra_dash_please/BoosterVisuals.java').read_text()
    assert 'RenderPlayerEvent.Post' in renderer and 'COLOR_WRITE' in renderer
    assert 'false, false' in renderer and 'count * 16' in renderer
    network = (root/'src/main/java/com/oritong/no_ultra_dash_please/DashNetwork.java').read_text()
    server = (root/'src/main/java/com/oritong/no_ultra_dash_please/ServerDash.java').read_text()
    assert 'sendToPlayersTrackingEntityAndSelf' in server and 'BubbleVisual' in network
    assert 'lastBubbleVisual' in server and 'syncBubbleVisual(player)' in server
    report = {"models":reports,"player_clearance_checked":True,"actual_polyhedron_clearance_checked":True,"runtime_shared_unit_vertices":42,
              "runtime_shared_faces":48,"runtime_texture_pixels":4608,"base_rgba_bytes":18432,
              "runtime_frames_not_geometry":True,"world_and_tracking_state_wired":True,
              "build_run":False,"minecraft_tested":False,
              "limitations":["Java API type checking and real multiplayer/renderer behavior require the feature owner's build.",
                             "Static BB previews show the first texture frame; runtime uses the full strips and a view-dependent rim."]}
    out=root/'build/art-validation/boosters';out.mkdir(parents=True,exist_ok=True)
    (out/'resource-check.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))


if __name__ == '__main__':
    main()
