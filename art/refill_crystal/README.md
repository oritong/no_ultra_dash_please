# 冲刺恢复晶体

- 主文件：`refill_crystal.bbmodel`，Blockbench 5.2.2 通用模型，可直接编辑网格与像素贴图。
- 配套的白色断续轮廓耗尽版位于 [`../refill_crystal_empty/`](../refill_crystal_empty/)，与本模型形状和大小一致。
- 像素源：`texture.pixel.json`；运行贴图位于 `../../src/main/resources/assets/no_ultra_dash_please/textures/block/refill_crystal.png`。
- 轮廓：正八面体，6 顶点、12 条等长边、8 个等边三角面；每条边长 8 个 BB 单位。悬浮于方块中心，六个极点距离方块中心均为 `4√2` 单位。
- 贴图：原生 16×16、7 色、单帧、全不透明；一个纹理像素对应一个 BB 单位。上四面与下四面各复用一个等边 UV 岛，腰线连续。
- 成本：1 个编辑网格、1 个材质、8 个导出三角面；NeoForge OBJ 将其烘焙为 8 个 BakedQuad。没有透明叠层、内部面、描边壳或动画几何副本。未测量 Minecraft FPS。

## 参考

1. [Blockbench Minecraft Style Guide](https://blockbench.net/wiki/guides/minecraft-style-guide/)：用几何表现主体，纹理表现细节；有限色阶、硬边像素、统一像素密度，不用阶梯堆叠斜面。
2. [Celeste Wiki / Dash Refills](https://celeste.ink/wiki/Dash_Refills)，实际查看的原版游戏画面为 [Celeste dash crystal.gif](https://static.wikitide.net/celestewiki/d/d2/Celeste_dash_crystal.gif)：绿色腰部、偏黄的浅绿上半部、较冷的深绿下半部和白色高光。观察了循环中的三个画面。

访问日期：2026-11-18。模型及 PNG 是按上述特征原创绘制；原版 GIF 仅供参考，没有放入运行资源。

## 运行资源

Minecraft 1.21.1 原生 cuboid JSON 不能表达这八个三角面，因此使用项目既有 NeoForge 的 `neoforge:obj` loader，不增加依赖。`models/block/refill_crystal.json` 指向同目录 OBJ/MTL；物品沿用该父模型。粒子贴图指向新的绿色 PNG。

Blockbench 中的 X/Z 原点位于模型中央，导出 OBJ 后 X/Z 平移 +0.5 个方块；Y 保持以方块底部为原点。OBJ 坐标单位为 1 个方块。`flip_v: true` 与 Blockbench 原生 OBJ 的 UV 翻转配对。MTL 使用 Minecraft 资源定位符。当前注册的无碰撞、回充和照明逻辑保持原有行为。

`create_model.js` → `paint_texture.js` → `export_runtime.js` 可在空白的 `refill_crystal` 通用项目中重建。保存时保留完整数值精度；不要把通用网格直接导出为原版 Java cuboid JSON。

## 离线检查

已保存并重开 BB 工程，核对 PNG 依赖；检查正、背、左、右、顶、底和斜视，临时启用正面剔除以发现反面。`preview.png` 是检查图板。检查器确认封闭流形、面朝外、12 条等长边、UV 密度和 BB/OBJ 几何一致。GUI、地面、展示框参数写在运行模型 JSON 中，手持继承 1.21.1 的 `minecraft:block/block`。

技能通用检查器的 `non_cube_geometry` 提示来自网格格式；本模型走 NeoForge OBJ 路径。游戏客户端未启动，游戏内显示和手持效果尚未实测。详细临时证据位于 `build/art-validation/`。按用户后续要求，不再执行构建。
