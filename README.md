# 不要再奥歘了

**No Ultra Dash, Please!**

Minecraft 1.21.1 / NeoForge 21.1.265 / Java 21 的 Celeste 风格移动模组。单人世界可直接使用；联机时客户端和服务端都需要安装同版本。

## 操作

- 按 `R` 冲刺，可在「选项 → 控制 → 按键绑定」里修改。水平方向取镜头朝向；仰角按最近的 `+90° / +45° / 0° / -45° / -90°` 吸附（抬头为正），分别对应上、斜上、前、斜下、下。
- 空中默认可冲一次，快捷栏右侧显示剩余次数；落在可回充的地面上会恢复。岩浆块、仙人掌、滴水石锥默认不能回充。身体接触水就立即补满冲刺，浅水和流水同样生效，不受地面回充开关及回充延迟限制；水中冲刺不扣次数，速度降至 75%。
- **Super / Hyper：**在地面水平冲刺后立即跳。水平冲刺时蹲着会变成低跳、快跑的 Hyper。
- **Wavedash：**先跳，再瞄准前下方冲向地板，碰地前后按跳。斜下冲刺碰地会先进入滑行，然后把速度转成低而快的跃起。即使先起跳，只要实际碰到可回充地面，冷却结束后也会补回次数。
- **Ultra：**Wavedash 后按住前进，等冷却结束再沿同一方向斜下冲地。冲刺会继承已有的同向速度，斜下触地再乘 `1.2`；可重复衔接，受服务端配置的速度上限限制。
- **墙边移动：**在空中贴近墙面按跳可蹬墙；竖直上冲时按跳会触发更强的蹬墙跳。面朝墙按住潜行可抓墙，前进上爬、后退下滑。抓墙跳后马上按离墙方向可触发 Wall Boost。冲刺后的跳跃按住跳键更高，下落时按住潜行可加快下落。

## 玩法方块

方块在创造模式「功能性方块」页签中，也可用无序配方合成。

| 方块 | 用法 | 配方 |
| --- | --- | --- |
| 梦境方块 | 六个面（包括底部）都能冲刺进入。低矮出口会自动选择能容纳玩家的站立、潜行或趴下姿态，底部出口同样生效；离开矮洞后自动恢复。成功出块补充冲刺；出口连趴下都容纳不了或途中撞到普通墙时，完整反转速度，沿原路径穿回入口，不瞬移或清零。进入、穿梭循环、离开及反弹使用蔚蓝原版音效。 | 遮光玻璃 + 紫水晶碎片 |
| 塌陷石台 | 第一章灰紫碎石薄台，半格高；踩上后 0.6 秒塌陷，留下无碰撞的白色虚线空框，2 秒后在无生物占位时恢复。四套碎石纹理按位置稳定随机；同高度、同形态的相邻石台自动连接，包含转角和内凹角。每块独立触发。 | 石台阶 + 沙砾 → 2 个 |
| 弹簧 / 超级弹簧 | 从上方落下时弹起、补充冲刺与攀爬体力；潜行可避开。 | 黏液块 + 红石粉 / 弹簧 + 活塞 |
| 侧弹簧 | 冲刺或高速侧撞时向外弹开并回充。 | 弹簧 + 拉杆 |
| 反弹 / 反射方块 | 冲刺碰撞时分别反弹起跳、沿碰撞轴反转速度；不回充。 | 黏液块 + 铁锭 / 反弹方块 + 浮冰 |
| 冲刺恢复晶体 | 碰到它时补满冲刺和攀爬体力，变为空水晶；2.5 秒后伴随聚光粒子和轻响恢复。不可挖掘、抗爆且不能被活塞推动。水平旋转并上下浮动，黑暗中自发光并照亮周围。 | 紫水晶碎片 ×2 + 荧石粉 |
| 推进器 / 红色推进器 | 进入或空手右键回充并由大泡泡裹住；泡泡将人拉向中心，按 `R` 或稍等自动带人冲出。绿色裹人形态持续到本次冲刺结束；红色持续包裹移动，直到撞击、取消或安全时限。第一人称保留清晰视野，第三人称和旁观者可见完整薄膜。进入、发射、持续红冲与释放均用蔚蓝原声。 | 末影珍珠 + 紫水晶碎片 + 黏液球 / 推进器 + 烈焰粉 + 红石粉 |

首次加载世界生成 `config/no_ultra_dash_please-server.toml`，可调整次数、冲刺速度、持续/冷却时间、起跳容错、最高保速，并可关闭蹬墙或落地回充。地图作者可用 `no_ultra_dash_please:dream_blocks` 和 `no_ultra_dash_please:no_ground_refill` 标签指定可穿行和禁止回充的方块。

客户端启动后生成 `config/no_ultra_dash_please-client.toml`。其中 `dashShaderIntensity = 100` 控制以幻棱迅光突进的视觉规格为参考重绘的棱镜效果：外围七通道径向色散、三角晶体切面、双端收尖流光和短促边缘曝光，0.40 秒内向外收缩、变细并碎裂消散。半径为屏幕高度 0.42 的中心圆内，后处理直接保留原画面；不使用全屏抖动、横向 RGB 重影、扫描线或模糊。另有短促的约 +10° 视野脉冲，恢复时不改变玩家 FOV 设置。范围 **0–200**，`0` 同时关闭后处理与视野脉冲，`100` 默认强度，`200` 双倍强度；修改文件可热重载。每次服务端确认冲刺重新开始时间轴，普通/红色推进器发射也会触发，打开界面或退出世界会停止。使用独立后处理通道，不占用原版后处理槽。此效果为原创 GLSL 实现，没有使用 Valorant 游戏资产。

## Celeste 移动机制参考

冲刺、斜下冲刺落地、Hyper/Wavedash、Ultra 速度继承、蹬墙跳、梦境冲刺和冲刺恢复的源码摘录与参数说明保存在 [`reference/celeste`](reference/celeste/)。其中 [`DASH_MECHANICS.md`](reference/celeste/DASH_MECHANICS.md) 是面向本模组移植的中文摘要，[`EXTRACTED_SOURCE.md`](reference/celeste/EXTRACTED_SOURCE.md) 是按固定提交和行号保存的原始 `Player.cs` 代码摘录，`upstream/` 保存来源文件和许可证。

参考仓库：[NoelFB/Celeste](https://github.com/NoelFB/Celeste)，固定提交 `1b0ce45c75e05649ae91b44a8bb6b196684e4352`。该仓库声明 MIT 许可证仅适用于仓库中的代码，不包括商业游戏本体或资产；`reference/celeste/` 仅保存代码研究资料。

冲刺恢复晶体已有独立的绿色八面体模型和原生 16×16 贴图，可编辑 Blockbench 工程、预览与来源说明位于 [`art/refill_crystal/`](art/refill_crystal/)。模型使用 NeoForge 自带 OBJ loader。模组触发的 23 个声音采用本机 Celeste 原始 FMOD 事件的单声道渲染，晶体重生使用原事件 `event:/game/general/diamond_return`，音高 1.0。来源、事件映射与重建脚本位于 [`art/sounds/`](art/sounds/)；音频版权归原作者，不适用代码参考的 MIT 许可。

梦境方块的黑色连续表面、起伏白色外框和彩色星点视差渲染已参考蔚蓝原版接入。40 帧像素动画、可编辑 Blockbench 方块模型、色板、来源及离线预览位于 [`art/dream_block/`](art/dream_block/)；世界渲染使用项目已有 NeoForge 与 Minecraft 原版 RenderType，不使用定制 shader。

绿、红推进器都有圆润待机模型和能裹住实际玩家的半透明形态，使用蔚蓝原版色板与 16×16 动画贴图。另补有原版共用的白色虚线空框，发射后留在原位置。六个 BB 工程、源图、对照预览及显示同步说明位于 [`art/boosters/`](art/boosters/)。

塌陷石台的正常碎石、白色虚线空框与八邻域连接纹理位于 [`art/crumble_platform/`](art/crumble_platform/)。提供原生 16×16 像素源、五份可编辑 BB 工程（四套随机碎石及塌陷空框）和实际 BB 预览；只使用原版 multipart 方块模型，不依赖连接纹理模组。

普通走跑、原版水下/梯子控制和普通起跳仍归 Minecraft。冲刺、抓墙和保速阶段才使用模组的运动状态；时间按 Minecraft 的 tick 计数，碰撞箱使用原版蹲姿。

| 项目 | 值 |
| --- | --- |
| 中文名称 | 不要再奥歘了 |
| 英文名称 | No Ultra Dash, Please! |
| Mod ID | `no_ultra_dash_please` |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.265` |
| Java | `21` |
| Gradle Wrapper | `8.13` |
| 模组版本 | `0.1.5` |

## 开发

使用 JDK 21，将 IDE 的项目 SDK 和 Gradle JVM 设为 Java 21。通过项目自带的 Gradle Wrapper 构建，无需安装系统 Gradle。首次构建需要联网下载依赖。

```powershell
# 在本项目目录下执行
.\gradlew.bat build

# 启动开发客户端
.\gradlew.bat runClient

# 启动开发服务端；首次运行需阅读并自行接受 Minecraft EULA
.\gradlew.bat runServer

# 生成数据资源
.\gradlew.bat runData
```

构建产物：`build/libs/no_ultra_dash_please-0.1.5.jar`。

运行服务端行为测试：`.\gradlew.bat runGameTestServer`。测试资源位于 `src/gameTest/`，不会打入发布包。晶体时间参数与来源记录见 [`reference/celeste/REFILL_MECHANICS.md`](reference/celeste/REFILL_MECHANICS.md)。

## 目录

- `gradle.properties`：模组名称、版本、Minecraft 与 NeoForge 版本。
- `src/main/java/com/oritong/no_ultra_dash_please/`：Java 源码与模组入口。
- `src/main/templates/META-INF/neoforge.mods.toml`：模组元数据模板，构建时自动填入版本和名称。
- `src/main/resources/`：手写资源文件。
- `src/generated/resources/`：数据生成器输出。
- `run/`：本地游戏运行目录，不纳入版本控制。

项目基于 [NeoForge 官方 Minecraft 1.21.1 ModDevGradle MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle) 创建。模板许可证保留在 `TEMPLATE_LICENSE.txt`；模组暂沿用模板默认的 `All Rights Reserved` 声明。
