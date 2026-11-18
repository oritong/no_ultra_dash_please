# 冲刺恢复晶体的时间基准

本次行为以 Celeste 普通可重生晶体为基准：成功回充时开始 **2.5 秒**计时，耗尽时展示空轮廓，计时结束时恢复并发出 16 个重生粒子。原版浮动使用 **0.6 Hz** 正弦波、**2 像素**振幅。

核对来源：[TheCyndaquilDecompilers/Celeste_Decompiled 的 Refill.cs](https://github.com/TheCyndaquilDecompilers/Celeste_Decompiled/blob/e9f9661dc1bff4398c0bcd9519ea5cffe3c6eaca/Celeste/Refill.cs)，固定提交 `e9f9661dc1bff4398c0bcd9519ea5cffe3c6eaca`；原始文件 SHA-256 为 `c29144e6f08addc45ee3348d77536b91a9e188a61dabca6443aa71ff302c1d21`。这是第三方反编译参考，权利归原作者；此处只记录观察到的数值和行为，没有复制该文件，也不把既有 `upstream/LICENSE` 的 MIT 许可扩展到它。

Minecraft 按 20 tick/s 换算为 **50 tick**。第 48 tick 空轮廓周围出现短促粒子环，第 50 tick 恢复实体晶体，并释放 16 个绿色粒子与轻响。正常情况下不消耗冲刺和体力均为满值的玩家所碰到的晶体；耗尽状态由服务端方块状态统一管理，因此多人共享同一轮冷却。计时使用原版持久化方块计划刻，区块卸载期间暂停，重新加载后继续。

三维表现沿用现有原创模型：绕竖直轴每秒旋转 45°，上下振幅为 2/16 格、频率 0.6 Hz。两种外观均使用自发光渲染，充能时提供 12 级方块光，耗尽时保留 2 级微光。旋转、发光与重生前粒子环是本模组的三维适配，不声称来自 Celeste 原版。
