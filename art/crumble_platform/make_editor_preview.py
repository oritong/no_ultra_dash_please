"""Assemble real Blockbench captures already saved by the skill's bb_capture.py."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ART = Path(__file__).resolve().parent
CAPTURES = ART.parents[1] / "build/art-validation/crumble/captures"
FONT = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 19)
SMALL = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 15)
TITLE = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 27)
NAMES = ["normal0", "normal1", "normal2", "normal3", "collapsed"]


def paste(board, path, position, size):
    image = Image.open(path).convert("RGBA").resize(size, Image.Resampling.NEAREST)
    board.paste(image, position, image)


def main():
    board = Image.new("RGB", (1100, 575), "#202B33")
    draw = ImageDraw.Draw(board)
    draw.text((25, 18), "Blockbench 实际预览 · 四套碎石 + 塌陷空框", font=TITLE, fill="#E9EDF0")
    draw.text((25, 60), "Java 方块模型 / 每份工程仅 1 个立方体、6 个面 / 原生 16×16 像素", font=FONT, fill="#ADBCC5")
    for column, name in enumerate(NAMES):
        label = f"随机变体 {column+1}" if column < 4 else "塌陷边框"
        draw.text((column*215+38, 108), label, font=FONT, fill="#E9EDF0")
        paste(board, CAPTURES / name / "oblique.png", (column*215+10, 145), (210, 210))
        for row, angle in enumerate(("front", "top", "bottom")):
            paste(board, CAPTURES / name / (angle+".png"), (column*215+10+row*70, 380), (68, 68))
    draw.text((25, 495), "下排依次为正面 / 顶部 / 底部；完整七视角检查见 blockbench-views.png。", font=FONT, fill="#ADBCC5")
    draw.text((25, 537), "Blockbench 5.2.2 / MCP 1.10.1 实拍。Minecraft 游戏端未运行。", font=SMALL, fill="#9EAFBA")
    board.save(ART / "blockbench-preview.png")

    board = Image.new("RGB", (1100, 1330), "#202B33")
    draw = ImageDraw.Draw(board)
    draw.text((25, 18), "Blockbench 七视角检查", font=TITLE, fill="#E9EDF0")
    for column, name in enumerate(NAMES):
        draw.text((column*215+45, 65), f"变体 {column+1}" if column < 4 else "塌陷空框", font=FONT, fill="#ADBCC5")
        for row, angle in enumerate(("front", "back", "left", "right", "top", "bottom", "oblique")):
            paste(board, CAPTURES / name / (angle+".png"), (column*215+28, row*170+100), (160, 160))
            draw.text((column*215+32, row*170+245), angle, font=SMALL, fill="#ADBCC5")
    board.save(ART / "blockbench-views.png")

    board = Image.new("RGB", (1100, 380), "#202B33")
    draw = ImageDraw.Draw(board)
    draw.text((25, 15), "Blockbench 原生 Display 检查", font=TITLE, fill="#E9EDF0")
    for column, slot in enumerate(("gui", "thirdperson_righthand", "firstperson_righthand", "ground")):
        path = CAPTURES.parent / ("display_"+slot+".png")
        image = Image.open(path).convert("RGBA")
        bounds = image.getbbox()
        image = image.crop(bounds)
        image.thumbnail((235, 245), Image.Resampling.NEAREST)
        board.paste(image, (column*270+25+(235-image.width)//2, 85+(245-image.height)//2), image)
        draw.text((column*270+25, 340), slot, font=SMALL, fill="#ADBCC5")
    board.save(ART / "blockbench-display.png")


if __name__ == "__main__":
    main()
