# 攀爬操作与原版参数

参考同目录固定提交的 MIT 代码 [`upstream/Player.cs`](upstream/Player.cs)，保留其原始许可。以下常量来自 `Player` 的 Climb State：

| 参数 | 蔚蓝原值 | Minecraft 20 tick/s 对应 |
| --- | --- | --- |
| `ClimbMaxStamina` | 110 | 110 |
| `ClimbUpCost` | `100 / 2.2` 每秒 | 每 tick 约 2.2727 |
| `ClimbStillCost` | `100 / 10` 每秒 | 每 tick 0.5 |
| `ClimbJumpCost` | `110 / 4` | 27.5 |
| `ClimbUpSpeed` | 45 px/s | 基础冲刺速度 × `45 / 240`，方向向上 |
| `ClimbDownSpeed` | 80 px/s | 基础冲刺速度 × `80 / 240`，方向向下 |

原版下滑不消耗体力，悬挂与上爬消耗体力；本次按原值换算，保留项目既有碰撞、攀爬跳和 Wall Boost 实现。

右键是按住抓墙，松开脱离。双方各检查主手和副手：空物品堆或 `#no_ultra_dash:empty_hand` 内物品均算空手；任意一只手持有其他物品即停止右键抓墙。默认物品标签为空，地图可用数据包扩展；测试专用的木棍/羽毛标签只在 GameTest 资源中，不进入发布包。

右键攀爬由服务端 `wallClimbing` 配置控制，独立于原有 `wallJumps` 设置。原潜行键抓墙保留原设置约束。贴墙按住右键时，物品使用动作让位于抓墙，避免标签内物品的使用动画打断攀爬；其他普通物品不受这一处理影响。输入/状态网络版本为 6，客户端与服务端需要同版本模组。
