# 绿泡泡与红泡泡

两种推进器均有待机小泡泡和裹住玩家的大泡泡形态。模型为一个低面数圆润薄壳，薄荷绿/青色亮边与橙红色亮边分别参考蔚蓝原版。裹人形态保留透明内部，让 Minecraft 正常绘制玩家的皮肤、装备和姿态；预览里的灰色人台只用于检查尺寸，没有打包进模型。

## 可编辑文件和运行资源

| 形态 | BB 工程 | 运行模型 ID |
| --- | --- | --- |
| 绿色待机 | `models/booster.bbmodel` | `no_ultra_dash_please:block/booster` |
| 红色待机 | `models/red_booster.bbmodel` | `no_ultra_dash_please:block/red_booster` |
| 绿色裹人 | `models/booster_occupied.bbmodel` | `no_ultra_dash_please:block/booster_occupied` |
| 红色裹人 | `models/red_booster_occupied.bbmodel` | `no_ultra_dash_please:block/red_booster_occupied` |

`design.pixel.json` 是原生 16×16 像素源，`generate_assets.py` 输出像素与几何源；`build_in_blockbench.js` 在四个对应命名的空白通用模型项目中创建网格、贴图，使用 Blockbench 原生 OBJ 编译器导出并保存。编辑器的静态首帧图位于 `textures/`，运行资源使用 `src/main/resources/assets/no_ultra_dash_please/textures/block/` 中的完整动画。BB 通用网格应通过 NeoForge OBJ loader 接入，不能直接当 Java cuboid JSON 导出。

待机贴图每帧 16×16，纵排 5 帧为 16×80；裹人贴图纵排 4 帧为 16×64。均为 0.1 秒/帧，与原版 `Sprites.xml` 的 loop/inside 节奏对应。表面高光在小范围内变化，内部像素保持稳定，没有柔边或噪点。它们是根据原版特征重新安排的三维表面，未复制原游戏的 sprite 像素图。

## 实际显示切换

- `BoosterBlock` 保留接触和空手右键使用，注册一个共用的方块实体类型，原位置使用轻微浮动、呼吸的待机泡泡。
- `ServerDash` 在等待进入和红/绿推进时发送独立的 `BubbleVisual` 视觉消息给玩家及附近观察者，不改变原来的运动 `Frame` 结构。模式 1/2 为绿色/红色等待，3/4 为绿色/红色推进，0 为结束。
- 等待时每 tick 最多向泡泡中心靠近 0.35 格，保留正常方块碰撞；贴地放置时不会把玩家脚部拉进地板。绿色薄膜持续本次冲刺，红色持续到撞击、取消或安全时限；结束时清理薄膜和移动声。
- `BoosterVisuals` 在正常玩家渲染后追加薄膜，随实际玩家的位置移动。普通玩家的 0.6×1.8 包围盒可完整置于薄膜内，较大玩家形体按尺寸扩大。原位置的待机模型在裹人时隐藏，避免两个泡泡重叠。
- 一次共用的 42 顶点网格用于四种显示，原生满亮度 shader、最近邻纹理、只有正面薄壳、不写透明深度。轮廓角度较大时采样原版亮边颜色；没有多层内外壳或烘焙人物贴图。
- 视觉状态每半秒保活，结束时清除；离开世界、人物移除/死亡或状态超时均清理。旧地图加载时补建原推进器的方块实体。
- 进入、发射和释放播放蔚蓝原事件；红色持续移动声跟随玩家位置，只维护一个循环实例。事件路径、提取脚本和音频哈希见 [`../sounds/`](../sounds/)。
- 新视觉消息使网络握手版本从 3 更新为 4；客户端与服务端需要同一新版。第一人称相机在薄膜内时不会被封闭面的颜色遮住视线，完整裹人外观主要从第三人称和其他玩家视角观看。

## 原版参考与成本

查看了本机原版 `objects/booster/booster00–04`、`05–08`、`18–25` 和对应 `boosterRed` 帧，以及原 `Sprites.xml` 的 loop/inside/spin 定义。薄荷色 `8CF7CF`、青色 `4ACFC6`、绿 `1C7856/0E4A36` 与红色对应色板均直接采样原版。原图、条目及源文件哈希详见 `sources.json`；商业游戏 sprite 仅放在本地 `build/art-reference/boosters/` 供核对，未作为运行贴图打包。

每种形态：1 个编辑网格、42 顶点、48 个面（32 四边面、16 三角面，总 80 个三角形），没有内部重复壳、额外装饰体或逐像素方块。NeoForge OBJ 会烘焙为 48 个 quad；客户端共用 48 面几何，剔除背向相机的面。四张运行纹理共 4,608 texels，RGBA 基础数据约 18,432 字节，不包含 mipmap、图集、GPU 对齐及副本，不是显存实测。

## 验证

`check_assets.py` 检查封闭流形、面朝向、所有动画帧与像素源、BB/OBJ 坐标和法线、UV、引用、原生尺寸、透明度，以及普通玩家包围盒八角在裹人椭球内。`capture_previews.js` 在 BB 离屏视图检查四种形态的六个方向和斜视，并临时添加人台检查裹人形态，完成后移除人台。`preview.png` 是实际 BB 图板，运行时的亮边会额外随观察角度变化。

0.1.4 已使用 Java 21 执行 `gradlew build runGameTestServer`，全部 13 项行为测试通过，其中 5 项覆盖红绿泡泡的吸入居中、实际移动、自动发射、撞墙释放、中断清理、地板碰撞和旧方块实体迁移。

通过地牢餐厅整合包自带 AutoJS，在独立 QA 世界检查当前 Iris 光影环境：两种待机小泡泡、两种等待及推进中的裹人薄膜、原位置隐藏、实际玩家随泡泡移动和红泡泡撞墙停止均正常；第一人称没有薄膜遮挡。声音事件记录确认两种进入/发射/释放以及红色移动循环触发，撞墙后循环停止。截图、客户端状态轨迹和声音事件记录保存在本地忽略的 `build/validation/0.1.4/`。多人远端观察、Fabulous 模式和客户端帧率未实测。
