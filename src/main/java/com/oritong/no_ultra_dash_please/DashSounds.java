package com.oritong.no_ultra_dash_please;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

final class DashSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, NoUltraDashPlease.MOD_ID);

    static final DeferredHolder<SoundEvent, SoundEvent> REFILL_TOUCH = sound("refill_touch");
    static final DeferredHolder<SoundEvent, SoundEvent> REFILL_RETURN = sound("refill_return");
    static final DeferredHolder<SoundEvent, SoundEvent> DASH = sound("dash");
    static final DeferredHolder<SoundEvent, SoundEvent> BOOSTER_ENTER = sound("booster_enter");
    static final DeferredHolder<SoundEvent, SoundEvent> RED_BOOSTER_ENTER = sound("red_booster_enter");
    static final DeferredHolder<SoundEvent, SoundEvent> BOOSTER_DASH = sound("booster_dash");
    static final DeferredHolder<SoundEvent, SoundEvent> RED_BOOSTER_DASH = sound("red_booster_dash");
    static final DeferredHolder<SoundEvent, SoundEvent> BOOSTER_END = sound("booster_end");
    static final DeferredHolder<SoundEvent, SoundEvent> RED_BOOSTER_MOVE = sound("red_booster_move");
    static final DeferredHolder<SoundEvent, SoundEvent> RED_BOOSTER_END = sound("red_booster_end");
    static final DeferredHolder<SoundEvent, SoundEvent> BOOSTER_REAPPEAR = sound("booster_reappear");
    static final DeferredHolder<SoundEvent, SoundEvent> RED_BOOSTER_REAPPEAR = sound("red_booster_reappear");
    static final DeferredHolder<SoundEvent, SoundEvent> PLATFORM_DISINTEGRATE = sound("platform_disintegrate");
    static final DeferredHolder<SoundEvent, SoundEvent> PLATFORM_RETURN = sound("platform_return");
    static final DeferredHolder<SoundEvent, SoundEvent> JUMP_SUPER = sound("jump_super");
    static final DeferredHolder<SoundEvent, SoundEvent> JUMP_SUPERSLIDE = sound("jump_superslide");
    static final DeferredHolder<SoundEvent, SoundEvent> JUMP_WALL = sound("jump_wall");
    static final DeferredHolder<SoundEvent, SoundEvent> JUMP_SUPERWALL = sound("jump_superwall");
    static final DeferredHolder<SoundEvent, SoundEvent> JUMP_CLIMB = sound("jump_climb");
    static final DeferredHolder<SoundEvent, SoundEvent> JUMP = sound("jump");
    static final DeferredHolder<SoundEvent, SoundEvent> JUMP_DREAMBLOCK = sound("jump_dreamblock");
    static final DeferredHolder<SoundEvent, SoundEvent> SPRING = sound("spring");
    static final DeferredHolder<SoundEvent, SoundEvent> REBOUND = sound("rebound");
    static final DeferredHolder<SoundEvent, SoundEvent> DREAM_ENTER = sound("dream_enter");
    static final DeferredHolder<SoundEvent, SoundEvent> DREAM_EXIT = sound("dream_exit");
    static final DeferredHolder<SoundEvent, SoundEvent> DREAM_TRAVEL = sound("dream_travel");
    static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BOUNCE = sound("dream_bounce");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(NoUltraDashPlease.id(name)));
    }

    static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    private DashSounds() {}
}
