# Celeste 冲刺相关机制摘要

本文件用于移植到 NeoForge 模组的设计参考。提取对象是 NoelFB/Celeste 的 `Source/Player/Player.cs`，固定提交 `1b0ce45c75e05649ae91b44a8bb6b196684e4352`。完整原始代码和许可证见同目录 `upstream/`；逐行源码摘录见 `EXTRACTED_SOURCE.md`。

## 用户举例对应的真实调用链

“向斜下方冲刺撞地板后跳起并按住前进”在原代码中拆成以下步骤：

1. `StartDash()` 消耗一次冲刺，进入 `StDash`。
2. `DashCoroutine()` 把速度设为瞄准向量 × 240；斜下方向即 `(+/-240, +240)`，然后开始 0.15 秒冲刺计时。
3. 垂直碰撞 `OnCollideV()` 在向下运动时检测到 `DashDir.X != 0 && DashDir.Y > 0`，调用 Dash Slide：`DashDir.Y=0`、`Speed.Y=0`、`Speed.X*=1.2`、`Ducking=true`。
4. 每帧落地会刷新 `jumpGraceTimer=0.1`；冲刺状态的 `DashUpdate()` 在 `jumpGraceTimer > 0` 且收到跳跃时调用 `SuperJump()`。
5. `SuperJump()` 设置 `Speed.X=260×Facing`、`Speed.Y=-105`；如果仍处于蹲姿，则 `Speed.X*=1.25`、`Speed.Y*=0.5`，所以水平初速度是 `325`，垂直初速度是 `-52.5`。
6. 冲刺恢复是另一个并行机制：`dashRefillCooldown` 先等待 0.1 秒；之后普通落地且脚下是 Solid/NegaBlock/JumpThru、没有尖刺阻挡时 `RefillDash()`。因此“恢复冲刺”和“SuperJump”在原实现中并非同一个函数。

这里的第 5 步是 Hyper/蹲滑跳的核心；社区常把“空中斜下冲刺 → 落地滑行 → 立即跳跃”称作 Wavedash。Ultra 通常是在 Wavedash 或 Hyper 的高速水平状态上再次向前冲刺，关键原因是 `DashCoroutine()` 的同向速度继承：若冲刺前水平速度绝对值大于 240 且方向相同，则不降低该水平速度。

## 机制和参数

| ID | 原代码位置 | 可直接确认的行为 |
| --- | --- | --- |
| `dash-start` | `Player.cs:3358-3384` | 普通状态检测到 Dash.Pressed、冷却归零且 Dashes>0 后进入 StDash；StartDash 将 Dashes 减 1，最低为 0。 |
| `dash-speed` | `Player.cs:3548-3564` | 冲刺速度为瞄准单位向量 × 240；如果冲刺方向与原水平速度同向且原速度更大，则保留原水平速度。 |
| `dash-duration` | `Player.cs:75-84,3619-3633` | 冲刺状态持续 0.15 秒；结束时水平/上冲速度按方向 × 160，向上的 Y 再乘 0.75，并设置 AutoJump。 |
| `diagonal-down-slide` | `Player.cs:2544-2552,3576-3585` | 斜下冲刺向下碰撞或落地时，DashDir 转为水平，Speed.Y=0，Speed.X 乘 1.2，进入蹲姿。 |
| `hyper` | `Player.cs:3502-3507,1695-1728` | 水平冲刺落地后的 0.1 秒 Jump Grace 内按跳跃触发 SuperJump；Speed.X=260×Facing，Speed.Y=-105；若处于蹲姿，水平再乘 1.25、垂直乘 0.5。 |
| `wavedash` | `Player.cs:2544-2552,3502-3507` | Wavedash 的可复刻核心序列是：空中斜下冲刺 → 以向下速度碰撞地面转成水平蹲滑 → 在 Jump Grace 内按跳跃。原代码没有名为 Wavedash 的函数。 |
| `ground-refill` | `Player.cs:722-740` | 每帧先更新 dashRefillCooldown；冷却结束后，在没有 NoRefills 的情况下，普通状态落在 Solid/NegaBlock/JumpThru 上且不被尖刺阻挡时恢复到 MaxDashes。 |
| `dash-refill-cooldown` | `Player.cs:79-80,3442-3452` | DashRefillCooldown=0.1 秒；DashBegin 同时设置 DashCooldown=0.2 秒和恢复冷却=0.1 秒，避免冲刺期间立即恢复。 |
| `bounce-refill` | `Player.cs:1844-1909,1919-1988` | Bounce、SuperBounce、SideBounce 会恢复冲刺和体力；Bounce 速度方向来自玩家中心与弹跳点，水平至少 120 且乘 1.2；SuperBounce 垂直速度=-185。 |
| `wall-jumps` | `Player.cs:1731-1810,2980-2996` | 普通蹬墙跳检测距离 3；水平速度为 130，垂直速度=-105；冲刺期间按跳跃撞墙会触发 WallJump；竖直向上冲刺撞墙则触发 SuperWallJump，水平为 170、垂直=-160。 |
| `wall-boost` | `Player.cs:688-699,1813-1830` | ClimbJump 没有水平输入时启动 0.2 秒 Wall Boost；期间按反向输入可把水平速度改为普通 WallJumpHSpeed=130。 |
| `wall-retention` | `Player.cs:799-810,2431-2436` | 撞墙时保存水平速度并保留 0.06 秒；若很快离开墙面且没有反向，则恢复保存的水平速度。 |
| `dash-collision` | `Player.cs:2377-2450,2475-2502` | 冲刺撞方块可由目标返回 Ignore、Rebound 或 Bounce；普通墙碰撞会清除水平速度和 Dash Attack，红冲则进入 HitSquash。 |
| `corner-correction` | `Player.cs:81-83,2398-2421,2504-2534` | 冲刺水平/垂直撞墙或地面时最多寻找 4 像素的邻近空位纠角，防止被小台阶卡住。 |
| `dream-dash` | `Player.cs:2642-2705,4166-4277` | 拥有 DreamDash 且正在攻击时，沿冲刺方向接触 DreamBlock 就进入梦境冲刺；速度 240，至少持续 0.1 秒，离开时恢复冲刺并水平出块时给予 Jump Grace。 |
| `water-dash` | `Player.cs:3561-3562,3640-3647` | 水中冲刺速度整体乘 0.75；水中状态的 CanDash 入口直接消费 Dash 缓冲并进入冲刺状态。 |
| `duck-collider` | `Player.cs:2113-2147,2879-2881` | 蹲姿使用 8×6 碰撞箱，站立为 8×11；蹲在地面时水平速度以 500/s 摩擦趋近 0。 |
| `jump-grace` | `Player.cs:48-52,713-720,2261-2264` | Jump Grace 为 0.1 秒；着地时刷新，离地后递减；StartJumpGraceTime 可由外部碰撞调用。 |
| `variable-jump` | `Player.cs:49-52,1660-1675,2960-2967` | 跳跃变高时间 0.2 秒；按住跳跃期间保持 varJumpSpeed，松开立即结束变高阶段。 |
| `run-physics` | `Player.cs:2883-2895,2927-2967` | 地面水平加速度 1000/s、空中乘 0.65；正常跑速 90；基础重力 900/s²，最大下落 160；按住跳跃且低速上升时重力乘 0.5。 |
| `boost-refill` | `Player.cs:3788-3792` | 进入 Booster 状态立即恢复全部冲刺和体力；普通 Booster 到时进入普通冲刺，红色 Booster 进入红冲。 |

## 面向 Minecraft 模组的移植建议

- 把速度单位转换单独做成配置：Celeste 的速度单位是每秒像素，Minecraft 需要转成每 tick 的水平/垂直速度；不要直接把 `240`、`260`、`325` 当作 Minecraft 速度。
- 用一个服务端权威的玩家移动状态保存 `dashCount`、`dashCooldown`、`dashRefillCooldown`、`dashAttackTimer`、`jumpGraceTimer`、`varJumpTimer`、`dashDirection`、`wasDucking`。客户端只负责输入和表现，服务端判定方块碰撞、落地和恢复次数。
- 用碰撞箱状态区分站立和蹲姿；先检查站立空间，再决定能否从蹲姿起身。否则 Hyper 的落地状态会和一格高空间冲突。
- 对角输入先归一化，否则斜向冲刺的长度会比水平/垂直冲刺大约 1.414 倍；原 Celeste 的 `lastAim` 是单位向量。
- 帧内处理顺序应固定为：更新落地与 Grace → 处理冲刺冷却/恢复 → 消费 Dash 输入 → 设置冲刺速度 → 移动并处理碰撞 → 在向下碰撞中执行 Dash Slide → 处理跳跃输入。
- `DodgeSlideSpeedMult=1.2` 是斜下冲刺落地后的水平加速倍率；`DuckSuperJumpXMult=1.25` 是蹲姿 SuperJump 的额外倍率。两者叠加时要明确是否只在同一次落地事件中生效。
- 原代码使用 0.1 秒 Jump Grace，实际移植到 Minecraft 时建议保留配置项，便于在 tick 离散、网络延迟和不同碰撞箱尺寸下调校。

## 版权和使用边界

仓库声明 MIT 许可证只适用于该仓库中的代码，不包括商业游戏本体或资产。本工作区保存了原始 `LICENSE`、来源提交、下载时间和 SHA-256 清单，后续实现应只借鉴机制并保留许可证/来源说明，不复制游戏资产。
