package com.oritong.no_ultra_dash_please;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

final class DreamTravelSound extends AbstractTickableSoundInstance {
    private final LocalPlayer player;

    DreamTravelSound(LocalPlayer player) {
        super(DashSounds.DREAM_TRAVEL.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        looping = true;
        delay = 0;
        volume = 0.6F;
        pitch = 1.0F;
        follow();
    }

    @Override
    public void tick() {
        if (Minecraft.getInstance().player != player || !player.isAlive() || player.isRemoved()) {
            stop();
        } else {
            follow();
        }
    }

    void finish() {
        stop();
    }

    private void follow() {
        x = player.getX();
        y = player.getEyeY();
        z = player.getZ();
    }
}
