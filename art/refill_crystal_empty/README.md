# 耗尽的冲刺恢复晶体

`refill_crystal_empty.bbmodel` 是可编辑主文件，`texture.pixel.json` 是原生 16×16 像素源。`preview.png` 对照充能/耗尽两种外观并展示六个方向。

## 原版参考

实际查看了本机原版 Celeste `Content/Graphics/Atlases/Gameplay` 中的 `objects/refill/idle00` 和 `objects/refill/outline`。后者是白色断续菱形轮廓、透明中心，逻辑画布 16×16，只有 12 个不透明白色像素。本版本把这些特征重新安排到现有正八面体的三维边缘上，图案是适配 UV 的原创排列。绿色实心体和表面高光在耗尽版中由透明区域取代。

图集条目、坐标、SHA-256 与格式参考链接在 `sources.json`。原版参考裁图仅在 `build/art-reference/` 中用于核对，没有放进运行资源。沿用 [Blockbench Minecraft Style Guide](https://blockbench.net/wiki/guides/minecraft-style-guide/) 的简单主体、有限色板和统一像素密度原则。

## 资源接口

- 方块模型：`no_ultra_dash_please:block/refill_crystal_empty`
- 物品模型：`no_ultra_dash_please:item/refill_crystal_empty`
- 纹理：`no_ultra_dash_please:block/refill_crystal_empty`
- 实际运行文件：`src/main/resources/assets/no_ultra_dash_please/models/{block,item}/refill_crystal_empty.*` 与 `textures/block/refill_crystal_empty.png`

与充能版共享相同的轮廓、中心、比例、UV 密度及物品显示变换，方便耗尽/恢复时切换。使用 NeoForge 原有 OBJ loader 和 `minecraft:cutout`，关闭面明暗以保持白色标记。透明度只有 0/255，不需要半透明排序或额外插件。

这次新增两个状态之间可引用的耗尽模型资源；状态属性、冷却和自动恢复逻辑由功能实现方接入，现有方块状态定义继续引用充能模型。

## 成本与检查

1 个编辑网格、6 顶点、8 个三角面，OBJ 烘焙为 8 个 BakedQuad；1 个 cutout 材质、1 张 16×16 静态纹理，12 个白色像素、244 个透明像素，无部分透明像素。没有内外重复壳、逐像素立方体或动画副本。编辑器纹理设置为正面显示，与运行时剔除方向一致。

已核对充能/耗尽几何和导出一致性、UV、贴图引用及正背左右上下视角；工程保存后重新读取验证。通用审计中的 `non_cube_geometry` 提示来自网格格式，本项目实际使用 OBJ，不使用 Java cuboid 导出。未执行构建，未启动 Minecraft，游戏内状态切换和显示尚未实测。

`create_and_export.js` 可以在名为 `refill_crystal_empty` 的空白 Blockbench 通用项目中重建资源。它从充能版磁盘工程读取几何，只向耗尽版路径写入。`capture_previews.js` 使用单独离屏视图，不改模型数据。
