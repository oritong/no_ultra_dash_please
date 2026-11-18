package com.oritong.no_ultra_dash_please;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

final class BoosterTravelSound extends AbstractTickableSoundInstance {
    private final Player player;

    BoosterTravelSound(Player player) {
        super(DashSounds.RED_BOOSTER_MOVE.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        looping = true;
        delay = 0;
        volume = 0.6F;
        pitch = 1.0F;
        follow();
    }

    @Override
    public void tick() {
        if (!player.isAlive() || player.isRemoved() || BoosterVisuals.mode(player) != 4) stop();
        else follow();
    }

    void finish() {
        stop();
    }

    private void follow() {
        x = player.getX();
        y = player.getY() + player.getBbHeight() * 0.5;
        z = player.getZ();
    }
}
