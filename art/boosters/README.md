# 绿泡泡与红泡泡

两种推进器均有待机小泡泡、裹住玩家的大泡泡和原位置的白色虚线空框。体积模型为一个低面数圆润薄壳，薄荷绿/青色亮边与橙红色亮边分别参考蔚蓝原版。裹人形态保留透明内部，让 Minecraft 正常绘制玩家的皮肤、装备和姿态；预览里的灰色人台只用于检查尺寸，没有打包进模型。

## 可编辑文件和运行资源

| 形态 | BB 工程 | 运行模型 ID |
| --- | --- | --- |
| 绿色待机 | `models/booster.bbmodel` | `no_ultra_dash_please:block/booster` |
| 红色待机 | `models/red_booster.bbmodel` | `no_ultra_dash_please:block/red_booster` |
| 绿色裹人 | `models/booster_occupied.bbmodel` | `no_ultra_dash_please:block/booster_occupied` |
| 红色裹人 | `models/red_booster_occupied.bbmodel` | `no_ultra_dash_please:block/red_booster_occupied` |
| 绿色空框 | `models/booster_outline.bbmodel` | `no_ultra_dash_please:block/booster_outline` |
| 红色空框 | `models/red_booster_outline.bbmodel` | `no_ultra_dash_please:block/red_booster_outline` |

`design.pixel.json` 是原生 16×16 像素源，`generate_assets.py` 输出像素与几何源；`build_in_blockbench.js` 在四个对应命名的空白通用模型项目中创建网格、贴图，使用 Blockbench 原生 OBJ 编译器导出并保存。编辑器的静态首帧图位于 `textures/`，运行资源使用 `src/main/resources/assets/no_ultra_dash_please/textures/block/` 中的完整动画。前四种体积薄膜的 BB 通用网格应通过 NeoForge OBJ loader 接入，不能直接当 Java cuboid JSON 导出；新增空框的 Java 平面格式见下文。

待机贴图每帧 16×16，纵排 5 帧为 16×80；裹人贴图纵排 4 帧为 16×64。均为 0.1 秒/帧，与原版 `Sprites.xml` 的 loop/inside 节奏对应。表面高光在小范围内变化，内部像素保持稳定，没有柔边或噪点。它们是根据原版特征重新安排的三维表面，未复制原游戏的 sprite 像素图。

## 实际显示切换

- `BoosterBlock` 保留接触和空手右键使用，注册一个共用的方块实体类型，原位置使用轻微浮动、呼吸的待机泡泡。
- `ServerDash` 在等待进入和红/绿推进时发送独立的 `BubbleVisual` 视觉消息给玩家及附近观察者，不改变原来的运动 `Frame` 结构。模式 1/2 为绿色/红色等待，3/4 为绿色/红色推进，0 为结束。
- 等待时每 tick 最多向泡泡中心靠近 0.35 格，保留正常方块碰撞；贴地放置时不会把玩家脚部拉进地板。绿色薄膜持续本次冲刺，红色持续到撞击、取消或安全时限；结束时清理薄膜和移动声。
- `BoosterVisuals` 在正常玩家渲染后追加薄膜，随实际玩家的位置移动。普通玩家的 0.6×1.8 包围盒可完整置于薄膜内，较大玩家形体按尺寸扩大。等待时隐藏原位置的待机模型；发射后原位置显示白色虚线空框，薄膜继续随玩家移动，推进结束后保留 1 秒空框，再恢复待机模型。
- 一次共用的 42 顶点网格用于四种显示，原生满亮度 shader、最近邻纹理、只有正面薄壳、不写透明深度。轮廓角度较大时采样原版亮边颜色；没有多层内外壳或烘焙人物贴图。
- 视觉状态每半秒保活，结束时清除；离开世界、人物移除/死亡或状态超时均清理。旧地图加载时补建原推进器的方块实体。
- 进入、发射和释放播放蔚蓝原事件；红色持续移动声跟随玩家位置，只维护一个循环实例。事件路径、提取脚本和音频哈希见 [`../sounds/`](../sounds/)。
- 当前网络握手版本为 5，包含泡泡状态和梦境出口姿态同步；客户端与服务端需要同一新版。第一人称相机在薄膜内时不会被封闭面的颜色遮住视线，完整裹人外观主要从第三人称和其他玩家视角观看。

## 原版参考与成本

查看了本机原版 `objects/booster/booster00–04`、`05–08`、`18–25` 和对应 `boosterRed` 帧，以及原 `Sprites.xml` 的 loop/inside/spin 定义。薄荷色 `8CF7CF`、青色 `4ACFC6`、绿 `1C7856/0E4A36` 与红色对应色板均直接采样原版。原图、条目及源文件哈希详见 `sources.json`；商业游戏 sprite 仅放在本地 `build/art-reference/boosters/` 供核对，未作为运行贴图打包。

四种体积薄膜各有：1 个编辑网格、42 顶点、48 个面（32 四边面、16 三角面，总 80 个三角形），没有内部重复壳、额外装饰体或逐像素方块。NeoForge OBJ 会烘焙为 48 个 quad；客户端共用 48 面几何，剔除背向相机的面。四张运行纹理共 4,608 texels，RGBA 基础数据约 18,432 字节，不包含 mipmap、图集、GPU 对齐及副本，不是显存实测。

## 新增的空框形态

原版绿、红泡泡都使用 `objects/booster/outline`：白色断续圆环，`Booster.Added` 设定 alpha 为 0.75；`PlayerBoosted` 显示原位置标记，`Respawn` 隐藏它。已实际查看原图。这里按原生 16×16 网格重绘，两种空框共用一张 `booster_outline.png`（alpha 191/255），没有复制原版精灵像素。

`outline.pixel.json` 是可编辑像素源；`generate_outlines.py` 只生成空框 PNG、两份 BB 与运行 JSON，不重写已有四种泡泡。`open_outline_in_blockbench.js` 可在同名的空白 Java 方块工程中载入。两份空框 BB 为 Java 方块格式 `1.9–1.21.5`，使用一个双面薄片、2 个面，不需要 OBJ；原有四种体积薄膜仍使用 Generic 网格/OBJ。

原位置的空框在模式 3/4（绿色/红色推进）显示，模式 1/2（等待）只显示裹人薄膜；释放后进入 20 tick 共用冷却，再恢复待机。世界渲染只绘制一个满亮度、75% 不透明度的面，并用相机旋转保持圆形，固定在方块中心，不跟随待机呼吸和上下浮动。实体/占用/空框/冷却状态由服务端方块状态同步；新增原版 1 秒冷却和两种重现音效，沿用 `BubbleVisual` 发送裹人形态。

`outline-preview.png` 是 BB 正面、背面和斜视实拍，斜视中的薄片会变窄；游戏的朝向镜头变换使其始终保持圆形。`outline-texture-preview.png` 是像素图对照。两份 BB 的原生 Java 导出与生成模型的几何、UV、共享纹理已核对；技能公共结构审计无错误，原版父模型缺省与半透明检查警告已人工核查。`check_outlines.py` 检查 16×16、白色 alpha、对称性、透明中心、BB 内外链贴图和模型引用；原有四形态的 `check_assets.py` 仍通过。0.1.6 已完成 Java 21 构建和 29 项游戏测试，并在整合包中验证红绿空框、重现与原声；空框使用双面材质，避免活动渲染器剔除薄片。

新增两份编辑工程各 1 个组件、2 个面；运行时空框每处 1 个 quad、1 张共享静态 16×16 贴图。红绿共用材质，整个泡泡资源的纹理总量从 4,608 增至 4,864 像素，无动画副本或额外壳层；未测量帧率。BB 5.2.2 / MCP 1.10.1 离屏视图清理遇到已知 `onContextMenu is not defined`，已核对本次视图均从工具和 Preview 注册表移除，controls 监听器的释放未验证。

## 验证

`check_assets.py` 检查封闭流形、面朝向、所有动画帧与像素源、BB/OBJ 坐标和法线、UV、引用、原生尺寸、透明度，以及普通玩家包围盒八角在裹人椭球内。`capture_previews.js` 在 BB 离屏视图检查四种形态的六个方向和斜视，并临时添加人台检查裹人形态，完成后移除人台。`preview.png` 是实际 BB 图板，运行时的亮边会额外随观察角度变化。

0.1.4 已使用 Java 21 执行 `gradlew build runGameTestServer`，全部 13 项行为测试通过，其中 5 项覆盖红绿泡泡的吸入居中、实际移动、自动发射、撞墙释放、中断清理、地板碰撞和旧方块实体迁移。

通过地牢餐厅整合包自带 AutoJS，在独立 QA 世界检查当前 Iris 光影环境：两种待机小泡泡、两种等待及推进中的裹人薄膜、原位置隐藏、实际玩家随泡泡移动和红泡泡撞墙停止均正常；第一人称没有薄膜遮挡。声音事件记录确认两种进入/发射/释放以及红色移动循环触发，撞墙后循环停止。截图、客户端状态轨迹和声音事件记录保存在本地忽略的 `build/validation/0.1.4/`。多人远端观察、Fabulous 模式和客户端帧率未实测。

0.1.6 的新增空框、平台联动、恢复计时和四个原声事件记录保存在本地 build/validation/0.1.6/。释放后保持原版 1 秒冷却，重现声按颜色区分并伴随聚光粒子；冷却期间接触或右键不会再次触发。
