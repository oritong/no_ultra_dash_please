# 梦境方块

梦境方块保留全方块碰撞与现有穿行机制。客户端呈现蔚蓝梦境方块的黑色内部、白色起伏外框和彩色星点；星点按远、中、近三层随镜头以不同幅度滚动。资源、物品动画、可编辑像素源、Blockbench 工程及预览已接入。

## 资源

- Blockbench：`dream_block.bbmodel`，Minecraft Java Block 1.9.0/1.21.1 目标格式；单个 16×16×16 方块、六个 16×16 贴图面。
- 像素源：`design.pixel.json`；`generate_textures.py` 生成的物品 PNG 每帧 16×16，纵向为 16×640、40 帧、每帧 1 tick，完整循环 2 秒。
- 运行物品贴图：`src/main/resources/assets/no_ultra_dash_please/textures/block/dream_block.png`。方块物品沿用父模型上的 Minecraft 原版动画贴图播放；环境中的立方体由客户端方块实体渲染器绘制。
- 像素遮罩：`dream_block_particles.png`，16×16，在一个小 atlas 中复用 5×5 像素点、十字与菱形环。游戏中颜色由原版色板计算。
- 运行代码：`DreamBlock` 增加方块实体标记，`DreamBlockRenderer` 绘制外露面；加载旧区块时由 `RefillCrystalMigration` 懒创建旧版方块实体。

世界表面使用 Minecraft 已有 Text shader、最近邻纹理采样和二值 alpha，不使用自定义 GLSL；外框复用遮罩的白色像素，与黑底和星点使用同一个 POSITION_COLOR_TEX_LIGHTMAP 缓冲通道，避免在 Iris 下切换备用缓冲时丢失轮廓或面。无需纹理 mip 或 GUI bloom。在满亮度下保持黑色背景、彩色星点和白色外框清楚。物理方块本身正常不透明和阻挡，不参与实体半透明排序。

## 原版参考

实际查看了 [Celeste Wiki 梦境方块页面](https://celeste.ink/wiki/Dream_Blocks)上的 200×200、38 帧循环画面，以及本机 Steam 版 Celeste `Gameplay0.data` 中的 `objects/dreamblock/active`、`disabled` 与 `particles`。提炼了纯黑内部、白色单像素外框、三种星形、三组颜色、深度权重 1:2:3、视差系数 0.30/0.55/0.80 与 6 Hz 星形切换。资产条目、哈希与实现依据保存在 `sources.json`；原版 GIF 和纹理没有放进运行资源。

游戏侧的星点位置按整数 world lattice 确定。邻面复用相同颜色、相位、形状和位置，并将透明星点裁到各个方块外表面，所以同一颗星越过共享边缘时仍连续移动。梦境方块碰到其他梦境方块的面被跳过，外框只画在暴露的外缘。粒子位置、原版颜色和深度比例来自真实游戏实现；轮廓形变和世界坐标铺星则为面向单位方块网格的程序化重绘。

## 成本与验证

- 编辑器：1 个立方体、6 个有效面、1 个不透明材质、40 张纵向排列的 16×16 物品动画帧。
- 运行资源：物品动画 40,960 texels，粒子遮罩 256 texels；世界渲染只创建可见方块面、裁片与暴露边框，不保存分帧实体或网格副本。未测量实际客户端帧率。
- 离线检查：帧尺寸和内容、边框连通性、二值 alpha、星形遮罩与色板、视差因子、负坐标和大坐标精度、相邻方块星点连续性、六个面外向、Java 1.21.1 模型 JSON、BB 内嵌贴图相符，以及代表性的 3×4 表面动画。
- `texture-preview.png` 和 `texture-animation.gif` 展示物品贴图；`blockbench-preview.png` 是单方块 Blockbench 实际画面；`parallax-preview.png` 和 `parallax-animation.gif` 用 Python/Pillow 重画相邻表面与镜头运动。后两张离线图证明资产与同一组坐标数学正常，**不是游戏截图**。

0.1.3 已由功能方合入、构建并在地牢餐厅整合包（Sodium/Iris）中实测。加入六向穿入、底部上冲、封闭出口、斜向继承速度和普通墙不可穿行的回归测试；穿梭期间保持玩家姿态，避免原版自动压成游泳姿态后误判出块。普通墙会完整反转穿梭速度并沿原路返回；进入、循环穿梭、反弹与离开均使用 Celeste 原版声音。极远视距和实际性能未做专项测量。
