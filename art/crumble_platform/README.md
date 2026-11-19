# 蔚蓝第一章：塌陷石台

方块 ID：`no_ultra_dash_please:crumble_platform`。原版素材来自本机正版 Celeste 的 `objects/crumbleBlock/default` 和 `outline`，实际查看后提炼灰紫色碎石、黑色底部阴影与白色虚线。这里的 PNG 均为原生 16×16 重绘；原版精灵裁图只保存在忽略提交的 `build/art-reference/crumble`，不随模组分发。

正常形态是 16×8×16 的半格石台，包含四套不同的顶面和侧面碎石图案。塌陷形态用透明贴图留下立体白色虚线边框，顶面、侧面和底部都可见。两种形态均支持同一高度的八邻域连接：长条、面状区域、L 形、孤立斜邻接与内凹角。正常和预塌陷状态互相连接，塌陷框只连接相邻塌陷框。

`blockbench-preview.png`、`blockbench-views.png`、`blockbench-display.png` 是 Blockbench 实际截图图板；`preview.png` 和 `random-preview.png` 是读取导出 JSON 的离线模型/拼接示意，不是游戏截图。

## 编辑与重新生成

- `design.pixel.json`：完整 16×16 字符像素源、四套图案、色板、虚线节奏及成本预算。
- `generate_assets.py`：沿用项目 Pillow 绘图方式，输出 PNG、Java 模型、multipart 方块状态、五份内嵌贴图的 BB 工程和离线预览。
- `crumble_platform.bbmodel`、`crumble_platform_v1.bbmodel`、`crumble_platform_v2.bbmodel`、`crumble_platform_v3.bbmodel`：四套正常外观。
- `crumble_platform_collapsed.bbmodel`：塌陷空框。每份工程是一个六面立方体，设为 BB 的 `1.9–1.21.5` Java 导出格式，适用于本项目 1.21.1 的传统 cuboid 数据。
- `textures/`：工程引用的重绘 PNG，与运行资源逐字节一致。
- `manifest.json`：生成时同步更新的模型、纹理与像素源 SHA-256 清单；相关文本资源固定使用 LF，避免 Windows 检出改变哈希。
- `open_in_blockbench.js`：在同名、空白的新 Java 方块工程中加载资源；检查目标后创建 Undo 事务，不覆盖其他工程。
- `check_assets.py`：穷举连接状态与随机变体，检查导出、UV、透明边框和嵌入贴图。
- `make_editor_preview.py`：拼合 `build/art-validation/crumble/captures` 中的实际 BB 截图。

在项目根目录、已安装 Pillow 的 Python 环境中执行：

```text
python art/crumble_platform/generate_assets.py
python art/crumble_platform/check_assets.py
```

重新生成会覆盖本目录中生成的 BB/PNG 与本方块的模型/方块状态；手动修改请先保存到独立文件，再同步回像素源或生成器。

## 连接与随机纹理

使用 Minecraft/NeoForge 自带 multipart 和等权重模型，无连接纹理依赖、无方块实体、无定制 shader。四个四分片分别依据两个边邻接和一个对角邻接选择五种拓扑，组合覆盖 47 种有意义的连接图案。

四套正常纹理在每条 multipart 规则中按相同顺序、相同权重排列。已核对目标版本的 `MultiPartBakedModel`：它把同一随机种子交给每个选中的子模型；`WeightedBakedModel` 因而对四分片选择一致的图案。随机结果由位置决定，邻接变化、预塌陷及恢复不会让正常图案跳变。预览按目标版本的 `Mth.getSeed`、`LegacyRandomSource` 与两层 `nextLong` 选择规则取样。

相邻块的侧面不生成；四分片内部的侧面也不生成。完全位于塌陷区域内部的空白四分片使用空模型，省去全透明几何。每个方块最多选择 4 个立方体、16 个面；四套图案只替换纹理，不把同时显示的几何乘四。手持和物品栏模型另用一个立方体、6 个面。

资源共 34 张 16×16 静态纹理，总计 8,704 像素。100 份连接模型中，60 份是复用基础几何的材质覆盖，4 份是省略透明几何的空模型。没有逐像素堆叠组件、描边壳或动画几何副本；未测量 draw call、显存和 FPS。

## 必要的方块接入

创造模式「功能性方块」中可取出；石台阶 + 沙砾无序合成 2 个。玩家踩上后 12 tick（0.6 秒）进入无碰撞的塌陷状态，40 tick（2 秒）后恢复；若玩家或生物正处在石台体积内，则每 2 tick 等待，直到可安全恢复。每块独立触发，相邻块只连接外观，不连锁倒塌。为保留定时恢复位置，不支持活塞移动。

这是 Minecraft 方块粒度的适配。原版平台有按整个平台宽度触发、抖动、离开顶面后的额外计时及逐块掉落/返回动画；本次未移植这些额外行为，也没有新增音频。`phase=normal/crumbling/collapsed` 可用于地图命令调试。

## 实际验证范围

- 穷举 768 个方块状态 × 4 套图案 = 3,072 个组合，核对各四分片、外侧面、UV、47 种连接拓扑及白色虚线的逐像素边界。
- 检查 34 张 PNG 的尺寸、0/255 透明度和源/运行资源一致性；BB 内嵌图与外链 PNG 一致。
- 五份 BB 工程已在 Blockbench 5.2.2 / MCP 1.10.1 打开，并通过 Java 原生导出器以 `1.9.0` 格式编译；捕获每份七个视角，以及正常模型的 GUI、第一/第三人称和地面 Display。
- 对 5 份 BB、100 份连接模型及 1 份物品模型执行技能结构审计，无结构错误。审计使用 `generic` 作公共 cuboid 检查；其 `parent_unresolved` 警告只涉及未随本项目复制的原版 `minecraft:block/block`。本项目的纹理及自定义父模型另由资源检查器完整解析。
- Java 21 仅做语法解析，无语法错误；没有类型检查。按用户要求未运行 Gradle、构建、Minecraft、部署或游戏性能测试。
- 离屏相机删除遇到该版本插件已知的 `onContextMenu is not defined`，已核对工具注册表与 `Preview.all`，本次创建的视图均已移除；不据此保证所有 controls 监听器已释放。

模型、贴图和导出检查不代替游戏验证。详细离线报告与原生导出位于忽略提交的 `build/art-validation/crumble/`。
