package com.oritong.no_ultra_dash_please;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

final class DashNetwork {
    static final int DASH = 1;
    static final int JUMP = 2;
    static final int SNEAK = 4;
    static final int FORWARD = 8;
    static final int BACK = 16;
    static final int LEFT = 32;
    static final int RIGHT = 64;
    static final int IDLE = 0;
    static final int MOVING = 1;
    static final int RELEASE = 2;
    static final int RESET = 3;
    static final int REJECT = 4;

    static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Input.TYPE, Input.CODEC, (data, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerDash.get(player).input(player, data);
            }
        });
        registrar.playToClient(Frame.TYPE, Frame.CODEC, (data, context) -> DashClient.receive(data));
    }

    record Input(int buttons, float yaw, float pitch, int release) implements CustomPacketPayload {
        static final Type<Input> TYPE = new Type<>(NoUltraDashPlease.id("input"));
        static final StreamCodec<RegistryFriendlyByteBuf, Input> CODEC = StreamCodec.of(
                (buffer, data) -> {
                    buffer.writeByte(data.buttons);
                    buffer.writeFloat(data.yaw);
                    buffer.writeFloat(data.pitch);
                    buffer.writeVarInt(data.release);
                }, buffer -> new Input(buffer.readUnsignedByte(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt()));

        @Override
        public Type<Input> type() {
            return TYPE;
        }
    }

    record Frame(int mode, int release, int charges, int maximum, int cooldown, boolean grounded, boolean duck,
                 Vec3 position, Vec3 speed, double dashSpeed, int dashTicks) implements CustomPacketPayload {
        static final Type<Frame> TYPE = new Type<>(NoUltraDashPlease.id("frame"));
        static final StreamCodec<RegistryFriendlyByteBuf, Frame> CODEC = StreamCodec.of(
                (buffer, data) -> {
                    buffer.writeByte(data.mode);
                    buffer.writeVarInt(data.release);
                    buffer.writeByte(data.charges);
                    buffer.writeByte(data.maximum);
                    buffer.writeByte(data.cooldown);
                    buffer.writeBoolean(data.grounded);
                    buffer.writeBoolean(data.duck);
                    buffer.writeVec3(data.position);
                    buffer.writeVec3(data.speed);
                    buffer.writeDouble(data.dashSpeed);
                    buffer.writeByte(data.dashTicks);
                }, buffer -> new Frame(buffer.readUnsignedByte(), buffer.readVarInt(), buffer.readUnsignedByte(),
                        buffer.readUnsignedByte(), buffer.readUnsignedByte(), buffer.readBoolean(), buffer.readBoolean(),
                        buffer.readVec3(), buffer.readVec3(), buffer.readDouble(), buffer.readUnsignedByte()));

        @Override
        public Type<Frame> type() {
            return TYPE;
        }
    }

    private DashNetwork() {}
}
