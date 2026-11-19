"""Build a review board from the real Blockbench marker captures."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ART = Path(__file__).resolve().parent
CAPTURES = ART.parents[1] / "build/art-validation/booster-outlines"


def main():
    board = Image.new("RGB", (1020, 490), "#202B33")
    draw = ImageDraw.Draw(board)
    title = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 26)
    font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 19)
    small = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 15)
    draw.text((25, 18), "红绿泡泡 · 原版白色虚线空框", font=title, fill="#E9EDF0")
    draw.text((25, 60), "Blockbench 实拍 / 16×16 共享贴图 / 两份可编辑模型", font=font, fill="#ADBCC5")
    for column, name in enumerate(("booster_outline", "red_booster_outline")):
        x = column*500+10
        draw.text((x+130, 110), "绿色泡泡边框" if column == 0 else "红色泡泡边框", font=font, fill="#E9EDF0")
        for index, view in enumerate(("front", "back", "oblique")):
            image = Image.open(CAPTURES / name / (view+".png")).convert("RGBA").resize((160, 160), Image.Resampling.NEAREST)
            board.paste(image, (x+index*162, 175), image)
            draw.text((x+index*162+40, 350), {"front": "正面", "back": "背面", "oblique": "斜视"}[view], font=small, fill="#ADBCC5")
    draw.text((25, 410), "编辑模型是双面薄片；游戏渲染时始终朝向镜头，保留完整圆形。", font=font, fill="#ADBCC5")
    draw.text((25, 455), "固定在原位置，泡泡薄膜继续随玩家移动。游戏端未测试。", font=small, fill="#9EAFBA")
    board.save(ART / "outline-preview.png")


if __name__ == "__main__":
    main()
