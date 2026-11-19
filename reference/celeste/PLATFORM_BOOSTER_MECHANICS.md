# 石台与泡泡的时间、声音对应

行为参考为本机 Steam 版 Celeste 的 `Celeste.dll`，SHA-256：`b2d17db24cd1affc886bcc62a8f809907d3107379bb704b4dc398fb61e356a47`。使用游戏已有 Mono.Cecil 离线读取方法常量与调用顺序，没有启动或修改游戏。商业程序集及 IL 输出只保存在本地忽略目录，不随仓库分发；本文仅记录参数和移植关系，不改变原代码参考的许可说明。

| 原版方法 | 原版行为 | Minecraft 20 tick/s 对应 |
| --- | --- | --- |
| `CrumblePlatform.Sequence` | 玩家踩上播放解体声，等待 0.2 s；玩家仍在台上时，最多再等 0.4 s，提前离开则结束等待 | 共用预警最少 4 tick、最多 12 tick |
| `CrumblePlatform.Sequence` | 关闭碰撞，显示虚线轮廓，等待 2 s；有 Actor 或 Solid 占位则继续等待 | 无碰撞空框保持至少 40 tick，整片无生物占位后恢复 |
| `CrumblePlatform.TileIn` | 恢复时播放平台返回声 | 每片恢复播放一次，避免大量方块叠声 |
| `Player.BoostCoroutine` | 等待约 0.25 s 或玩家主动冲刺 | 现有 5 tick 吸入等待与主动发射保持 |
| `Booster.PlayerReleased` | 停止移动声，设置 `respawnTimer = 1` | 释放后 20 tick 空框冷却 |
| `Booster.Respawn` | 播放对应颜色重现声，隐藏轮廓、恢复实体、出现粒子 | 红绿泡泡各自重现声与聚光粒子 |

原版的逐小片掉落和返回有 0.05 s 的分组错开。用户指定整片一起塌陷，因此这里在同一个服务端 tick 移除整片碰撞并显示空框，保留原版预警与恢复时间。平台按同一 Y 高度的四邻接遍历整片已加载连接区域，无固定搜索半径；斜角接触和上下层不触发联动。八邻域仍仅用于贴图拼接。活动群组与绝对世界时间存档，成员区块卸载时暂停处理，不强制加载或生成区块。

新增声音均来自原始 FMOD 事件，以音高 1.0 播放，路径与 SHA-256 见 [`../../art/sounds/sources.json`](../../art/sounds/sources.json)：

- `event:/game/general/platform_disintegrate`
- `event:/game/general/platform_return`
- `event:/game/04_cliffside/greenbooster_reappear`
- `event:/game/05_mirror_temple/redbooster_reappear`
