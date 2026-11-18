"""Render original Celeste FMOD events offline, then encode Minecraft mono Vorbis.

Uses the local game's FMOD 1.10 libraries; never launches or changes the game.
Requires Python 3 on Windows and an existing FFmpeg executable.
"""

import argparse
import ctypes as c
import hashlib
import json
import math
import os
from pathlib import Path
import subprocess
import wave


EVENTS = {
    "refill_touch": "game/general/diamond_touch",
    "refill_return": "game/general/diamond_return",
    "dash": "char/madeline/dash_red_right",
    "booster_enter": "game/04_cliffside/greenbooster_enter",
    "red_booster_enter": "game/05_mirror_temple/redbooster_enter",
    "booster_dash": "game/04_cliffside/greenbooster_dash",
    "red_booster_dash": "game/05_mirror_temple/redbooster_dash",
    "booster_end": "game/04_cliffside/greenbooster_end",
    "red_booster_move": "game/05_mirror_temple/redbooster_move",
    "red_booster_end": "game/05_mirror_temple/redbooster_end",
    "jump_super": "char/madeline/jump_super",
    "jump_superslide": "char/madeline/jump_superslide",
    "jump_wall": "char/madeline/jump_wall_right",
    "jump_superwall": "char/madeline/jump_superwall",
    "jump_climb": "char/madeline/jump_climb_right",
    "jump": "char/madeline/jump",
    "jump_dreamblock": "char/madeline/jump_dreamblock",
    "spring": "game/general/spring",
    "rebound": "game/06_reflection/pinballbumper_hit",
    "dream_enter": "char/madeline/dreamblock_enter",
    "dream_exit": "char/madeline/dreamblock_exit",
    "dream_travel": "char/madeline/dreamblock_travel",
    "dream_bounce": "game/general/assist_dreamblockbounce",
}


def digest(path):
    return hashlib.file_digest(path.open("rb"), "sha256").hexdigest()


class Fmod:
    def __init__(self, game):
        self.game = game
        self.dll_dir = os.add_dll_directory(str(game / "lib64-win-x64"))
        self.core = c.WinDLL(str(game / "lib64-win-x64/fmod64.dll"))
        self.studio = c.WinDLL(str(game / "lib64-win-x64/fmodstudio.dll"))
        p, i, u, s = c.c_void_p, c.c_int, c.c_uint, c.c_char_p
        self.bind(self.core, "FMOD_System_Create", [c.POINTER(p)])
        self.bind(self.core, "FMOD_System_GetVersion", [p, c.POINTER(u)])
        self.bind(self.core, "FMOD_System_Release", [p])
        self.bind(self.core, "FMOD_System_SetOutput", [p, i])
        self.bind(self.core, "FMOD_System_SetSoftwareFormat", [p, i, i, i])
        self.bind(self.core, "FMOD_System_SetDSPBufferSize", [p, u, i])
        for name, args in {
            "Create": [c.POINTER(p), u],
            "GetLowLevelSystem": [p, c.POINTER(p)],
            "Initialize": [p, i, u, u, p],
            "LoadBankFile": [p, s, u, c.POINTER(p)],
            "GetEvent": [p, s, c.POINTER(p)],
            "FlushCommands": [p],
            "FlushSampleLoading": [p],
            "Update": [p],
            "Release": [p],
        }.items():
            self.bind(self.studio, "FMOD_Studio_System_" + name, args)
        for name, args in {
            "LoadSampleData": [p],
            "GetLength": [p, c.POINTER(i)],
            "CreateInstance": [p, c.POINTER(p)],
        }.items():
            self.bind(self.studio, "FMOD_Studio_EventDescription_" + name, args)
        for name in ["Start", "Release"]:
            self.bind(self.studio, "FMOD_Studio_EventInstance_" + name, [p])
        system, version = p(), u()
        self.check(self.core.FMOD_System_Create(c.byref(system)))
        self.check(self.core.FMOD_System_GetVersion(system, c.byref(version)))
        self.check(self.core.FMOD_System_Release(system))
        self.version = version.value

    @staticmethod
    def bind(lib, name, args):
        fn = getattr(lib, name)
        fn.argtypes, fn.restype = args, c.c_int

    @staticmethod
    def check(result):
        if result:
            raise RuntimeError(f"FMOD error {result}")

    def render(self, event, output):
        system, low = c.c_void_p(), c.c_void_p()
        self.check(self.studio.FMOD_Studio_System_Create(c.byref(system), self.version))
        try:
            self.check(self.studio.FMOD_Studio_System_GetLowLevelSystem(system, c.byref(low)))
            # WAVWRITER_NRT + synchronous Studio + MIX_FROM_UPDATE: entirely offline.
            self.check(self.core.FMOD_System_SetOutput(low, 5))
            self.check(self.core.FMOD_System_SetSoftwareFormat(low, 48000, 3, 0))
            self.check(self.core.FMOD_System_SetDSPBufferSize(low, 1024, 4))
            filename = c.create_string_buffer(os.fsencode(output))
            self.check(self.studio.FMOD_Studio_System_Initialize(system, 512, 4, 0x80000, filename))
            for bank in ["Master Bank.strings.bank", "Master Bank.bank", "sfx.bank"]:
                handle = c.c_void_p()
                self.check(self.studio.FMOD_Studio_System_LoadBankFile(
                    system, os.fsencode(self.game / "Content/FMOD/Desktop" / bank), 0, c.byref(handle)))
            description, instance, length = c.c_void_p(), c.c_void_p(), c.c_int()
            self.check(self.studio.FMOD_Studio_System_GetEvent(
                system, ("event:/" + event).encode(), c.byref(description)))
            self.check(self.studio.FMOD_Studio_EventDescription_LoadSampleData(description))
            self.check(self.studio.FMOD_Studio_System_FlushSampleLoading(system))
            self.check(self.studio.FMOD_Studio_EventDescription_GetLength(description, c.byref(length)))
            if length.value > 8000:
                raise ValueError(f"Unexpected long/looped event: {event}: {length.value} ms")
            self.check(self.studio.FMOD_Studio_EventDescription_CreateInstance(description, c.byref(instance)))
            self.check(self.studio.FMOD_Studio_EventInstance_Start(instance))
            self.check(self.studio.FMOD_Studio_System_FlushCommands(system))
            repeats = 2 if event.endswith("/dreamblock_travel") else 1
            for _ in range(math.ceil((length.value / 1000 * repeats + 0.5) * 48000 / 1024)):
                self.check(self.studio.FMOD_Studio_System_Update(system))
            self.check(self.studio.FMOD_Studio_EventInstance_Release(instance))
            return length.value
        finally:
            self.check(self.studio.FMOD_Studio_System_Release(system))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--game", type=Path, required=True)
    parser.add_argument("--ffmpeg", type=Path, required=True)
    parser.add_argument("--only", choices=EVENTS)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    output = root / "src/main/resources/assets/no_ultra_dash_please/sounds/celeste"
    scratch = root / "build/celeste-audio"
    output.mkdir(parents=True, exist_ok=True)
    scratch.mkdir(parents=True, exist_ok=True)
    fmod = Fmod(args.game)
    manifest = {"source": "Local Celeste installation / Content/FMOD/Desktop",
                "fmod_version": hex(fmod.version), "banks": {}, "events": {}}
    for name in ["Master Bank.strings.bank", "Master Bank.bank", "sfx.bank"]:
        manifest["banks"][name] = digest(args.game / "Content/FMOD/Desktop" / name)
    for name, event in EVENTS.items():
        if args.only and name != args.only:
            continue
        wav_path, ogg_path = scratch / (name + ".wav"), output / (name + ".ogg")
        length = fmod.render(event, wav_path)
        filters = "silenceremove=start_periods=1:start_threshold=-80dB"
        if name == "dream_travel":
            # Keep one full 2160 ms period after the FMOD loop has already warmed up.
            filters = f"atrim=start={length / 1000}:duration={length / 1000},asetpts=PTS-STARTPTS"
        subprocess.run([str(args.ffmpeg), "-hide_banner", "-loglevel", "error", "-y",
                        "-i", str(wav_path), "-ac", "1", "-ar", "48000", "-af",
                        filters,
                        "-c:a", "libvorbis", "-q:a", "6", str(ogg_path)], check=True)
        with wave.open(str(wav_path)) as audio:
            duration = audio.getnframes() / audio.getframerate()
        manifest["events"][name] = {"event": "event:/" + event,
                                    "event_length_ms": length, "rendered_seconds": duration,
                                    "file": ogg_path.relative_to(root).as_posix(),
                                    "sha256": digest(ogg_path)}
        if name == "dream_travel":
            manifest["events"][name]["loop_window_seconds"] = [length / 1000, length / 500]
        print(name, length, ogg_path.stat().st_size, flush=True)
    manifest_path = scratch / "sources.json" if args.only else root / "art/sounds/sources.json"
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
