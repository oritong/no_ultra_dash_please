# 蔚蓝原版音效

27 个事件从本机 Steam 版 Celeste 的原始 FMOD bank 离线渲染，原事件名、bank 与输出文件的 SHA-256 记录在 `sources.json`。使用游戏附带的 FMOD 1.10.14，不启动或修改 Celeste。

运行文件在 `src/main/resources/assets/no_ultra_dash_please/sounds/celeste/`；`sounds.json` 和 `DashSounds.java` 注册事件，`ServerDash.java` 使用它们。晶体触碰、冲刺、普通/红色推进器进入与发射、超级/滑跃/墙跳/攀爬跳、弹簧、反弹和梦境方块进入/离开均已对应接入，中英字幕一并添加。

每个事件保留一次原版渲染结果，转为 Minecraft 所需的单声道 Ogg Vorbis，48 kHz、质量 6，播放音高 1.0。开头空白被裁去，保留尾音；随机事件的多个变体未全部导入。目前常规冲刺与普通墙跳取原版 right 事件。音效版权归 Celeste 原作者；参考代码仓库的 MIT 声明不适用于这些音频。

`extract_celeste_audio.py --game <Celeste目录> --ffmpeg <已有FFmpeg可执行文件>` 可重做音频。源游戏、DLL、WAV 中间文件与 FFmpeg 均不随模组打包。脚本需要 Windows Python 3，不需要 Python 第三方包。

27 个 Ogg 均已解码核查：单声道、48 kHz、非静音、无满幅削波；资源、注册和字幕路径检查通过。

`refill_return` 为 `event:/game/general/diamond_return`，在晶体 50 tick 冷却结束、空水晶恢复时向附近玩家播放，使用 BLOCKS 音量分类、音高 1.0。此次新增音频为 48 kHz 单声道，约 2.62 秒，峰值 -11.43 dBFS；没有调速、移调或合成替代声音。

梦境穿梭新增 `event:/char/madeline/dreamblock_travel` 循环与 `event:/game/general/assist_dreamblockbounce` 反弹音效。循环从原事件预热后的 2.160–4.320 秒截取一个完整 2160 ms 周期；进入/退出音效沿用既有蔚蓝原声。客户端由服务端穿梭状态启动循环，离开、反弹后出块、死亡、传送或退出世界时停止，不向玩家重复叠放单次声音。

推进器新增 `event:/game/04_cliffside/greenbooster_end`、`event:/game/05_mirror_temple/redbooster_move` 和 `event:/game/05_mirror_temple/redbooster_end`。原有进入/发射事件继续使用蔚蓝原声；红泡泡持续移动声绑定到实际玩家位置，在释放、死亡、传送、断线或离开世界时停止。旁观者也按收到的红泡泡状态播放，事件音高维持 1.0。

平台与泡泡新增四个原始事件：platform_disintegrate、platform_return、greenbooster_reappear、redbooster_reappear。平台整片预警/恢复各播放一次，泡泡在释放玩家后 20 tick 重现时播放对应颜色音效；均为单声道 48 kHz，音高 1.0。
