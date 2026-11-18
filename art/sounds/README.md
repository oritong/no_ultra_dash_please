# 蔚蓝原版音效

17 个事件从本机 Steam 版 Celeste 的原始 FMOD bank 离线渲染，原事件名、bank 与输出文件的 SHA-256 记录在 `sources.json`。使用游戏附带的 FMOD 1.10.14，不启动或修改 Celeste。

运行文件在 `src/main/resources/assets/no_ultra_dash_please/sounds/celeste/`；`sounds.json` 和 `DashSounds.java` 注册事件，`ServerDash.java` 使用它们。晶体触碰、冲刺、普通/红色推进器进入与发射、超级/滑跃/墙跳/攀爬跳、弹簧、反弹和梦境方块进入/离开均已对应接入，中英字幕一并添加。

每个事件保留一次原版渲染结果，转为 Minecraft 所需的单声道 Ogg Vorbis，48 kHz、质量 6，播放音高 1.0。开头空白被裁去，保留尾音；随机事件的多个变体未全部导入。目前常规冲刺与普通墙跳取原版 right 事件。音效版权归 Celeste 原作者；参考代码仓库的 MIT 声明不适用于这些音频。

`extract_celeste_audio.py --game <Celeste目录> --ffmpeg <已有FFmpeg可执行文件>` 可重做音频。源游戏、DLL、WAV 中间文件与 FFmpeg 均不随模组打包。脚本需要 Windows Python 3，不需要 Python 第三方包。

17 个 Ogg 均已解码核查：单声道、48 kHz、非静音、无满幅削波；资源、注册和字幕路径检查通过。当前只完成离线解码与接入检查，未在 Minecraft 中试听。
