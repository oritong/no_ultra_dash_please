"""Offline reference simulation of the client's north-facing connected surface.

This makes review images and checks world-space clipping. It does not launch or
certify Minecraft's renderer. Runtime implementation lives in DreamBlockRenderer.
"""

import math

from PIL import Image, ImageDraw, ImageFont

from generate_textures import DESIGN, ART, star, rgb


MASK = (1 << 64) - 1
PIXEL = 1 / 16
RADIUS = 2.5 * PIXEL
CELL = 0.5
PALETTES = [DESIGN["palette"][name] for name in ["near", "middle", "far"]]


def mix(value):
    value &= MASK
    value = ((value ^ (value >> 30)) * 0xBF58476D1CE4E5B9) & MASK
    value = ((value ^ (value >> 27)) * 0x94D049BB133111EB) & MASK
    return value ^ (value >> 31)


def entries(origin, camera, bounds=(0, 0, 1, 1)):
    """North-face cells, same bounded coordinate hash and layer weights as Java."""
    output = []
    for layer in [2, 1, 0]:
        su, sv = camera[0] * DESIGN["parallax"][layer], camera[1] * DESIGN["parallax"][layer]
        for cy in range(math.floor((origin[1] - sv - RADIUS) / CELL),
                        math.floor((origin[1] - sv + 1 + RADIUS) / CELL) + 1):
            for cx in range(math.floor((origin[0] - su - RADIUS) / CELL),
                            math.floor((origin[0] - su + 1 + RADIUS) / CELL) + 1):
                seed = mix(cx * 0x9E3779B97F4A7C15 ^ cy * 0xC2B2AE3D27D4EB4F ^ 2 * 0x85EBCA77)
                if (seed >> 11) * 2**-53 >= 0.7:
                    continue
                details = mix(seed)
                choice = details % 6
                if (0 if choice == 0 else 1 if choice < 3 else 2) != layer:
                    continue
                x = cx * CELL + ((details >> 8) & 7) * PIXEL + PIXEL / 2 + su - origin[0]
                y = cy * CELL + ((details >> 12) & 7) * PIXEL + PIXEL / 2 + sv - origin[1]
                clip = (max(x - RADIUS, bounds[0]), max(y - RADIUS, bounds[1]),
                        min(x + RADIUS, bounds[2]), min(y + RADIUS, bounds[3]))
                if clip[0] >= clip[2] or clip[1] >= clip[3]:
                    continue
                output.append({"id": (layer, cx, cy), "layer": layer, "x": x, "y": y,
                               "phase": ((details >> 20) & 1023) / 1024,
                               "color": PALETTES[layer][(details >> 30) % 3], "clip": clip})
    return output


def render(camera=(0, 0), seconds=0, width=4, height=3, scale=96):
    image = Image.new("RGB", (width * scale, height * scale), "black")
    draw = ImageDraw.Draw(image)
    quads = 0
    for by in range(height):
        for bx in range(width):
            bounds = (2 * PIXEL if bx == 0 else 0, 2 * PIXEL if by == 0 else 0,
                      1 - 2 * PIXEL if bx == width - 1 else 1,
                      1 - 2 * PIXEL if by == height - 1 else 1)
            for entry in entries((bx, by), camera, bounds):
                layer, phase = entry["layer"], entry["phase"]
                shape = "point" if layer == 2 else (
                    ["cross", "point"][int(seconds * 6 + phase * 2) & 1] if layer == 1
                    else ["cross", "ring", "cross", "point"][int(seconds * 6 + phase * 4) & 3])
                quads += 1
                for sy, row in enumerate(DESIGN["shapes"][shape]):
                    for sx, value in enumerate(row):
                        if value != "#":
                            continue
                        left = max(entry["x"] + (sx - 2.5) * PIXEL, entry["clip"][0])
                        right = min(entry["x"] + (sx - 1.5) * PIXEL, entry["clip"][2])
                        bottom = max(entry["y"] + (1.5 - sy) * PIXEL, entry["clip"][1])
                        top = min(entry["y"] + (2.5 - sy) * PIXEL, entry["clip"][3])
                        if left < right and bottom < top:
                            box = (round((bx + left) * scale), round((height - by - top) * scale),
                                   round((bx + right) * scale) - 1, round((height - by - bottom) * scale) - 1)
                            if box[0] <= box[2] and box[1] <= box[3]:
                                draw.rectangle(box, fill=entry["color"])
            # Only silhouette edges get outlines. No borders between neighboring faces.
            for side in range(4):
                if not [bx == 0, bx == width - 1, by == 0, by == height - 1][side]:
                    continue
                vertical = side < 2
                along = by if vertical else bx
                edge = bx + (side == 1) if vertical else by + (side == 3)
                start = (by == 0 if vertical else bx == 0)
                end = (by == height - 1 if vertical else bx == width - 1)
                previous = None
                for segment in range(4):
                    a, b = segment / 4, (segment + 1) / 4
                    wave = math.sin(seconds * 2 + (along + a) * 2.7 + math.sin(seconds * 1.1 + edge * .7) * 2)
                    inset = PIXEL if wave > .35 else 0
                    if (segment == 0 and start) or (segment == 3 and end):
                        inset = 0
                    low = 1 - PIXEL - inset if side in [1, 3] else inset
                    boxes = [(low, a, low + PIXEL, b)] if vertical else [(a, low, b, low + PIXEL)]
                    if previous is not None and previous != inset:
                        old = 1 - PIXEL - previous if side in [1, 3] else previous
                        boxes.append((old, a, old + PIXEL, a + PIXEL) if vertical else (a, old, a + PIXEL, old + PIXEL))
                    for u0, v0, u1, v1 in boxes:
                        draw.rectangle((round((bx + u0) * scale), round((height - by - v1) * scale),
                                        round((bx + u1) * scale) - 1, round((height - by - v0) * scale) - 1), fill="white")
                    previous = inset
    return image, quads


def main():
    font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 20)
    title = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 27)
    board = Image.new("RGB", (1120, 740), "#17232d")
    d = ImageDraw.Draw(board)
    d.text((30, 22), "梦境方块 · 连续表面与分层视差", font=title, fill="#e5f0e9")
    d.text((30, 68), "离线参考模拟，非 Minecraft 游戏截图", font=font, fill="#9db9ad")
    for i, camera in enumerate([(0, 0), (.6, .25)]):
        im, count = render(camera, .3, scale=120)
        board.paste(im, (30 + i * 550, 122))
        d.text((30 + i * 550, 502), f"镜头偏移 {camera} · {count} 个星点裁片", font=font, fill="#cddfd4")
    d.text((30, 563), "相邻块共用连续星域，只在外轮廓显示白线", font=font, fill="#cddfd4")
    d.text((30, 607), "远 / 中 / 近三层响应镜头：0.80 / 0.55 / 0.30", font=font, fill="#a6c0b2")
    d.text((30, 651), "星形按 6 Hz 切换；黑底、彩色星点与白线在暗处可见", font=font, fill="#a6c0b2")
    board.save(ART / "parallax-preview.png")
    frames = [render((.6 * math.sin(i / 60 * math.tau), .2 * math.sin(i / 60 * math.tau)), i / 20)[0] for i in range(60)]
    frames[0].save(ART / "parallax-animation.gif", save_all=True, append_images=frames[1:], duration=50,
                   loop=0, optimize=False, disposal=2)
    print("Created connected-surface preview and camera parallax animation.")


if __name__ == "__main__":
    main()
