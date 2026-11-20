package com.oritong.no_ultra_dash_please;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Shared by input prediction and the authoritative server inventory check. */
public final class ClimbHands {
    public static final TagKey<Item> EMPTY_HAND = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("no_ultra_dash", "empty_hand"));

    private ClimbHands() { }

    public static boolean empty(Player player) {
        return empty(player.getMainHandItem()) && empty(player.getOffhandItem());
    }

    private static boolean empty(ItemStack stack) { return stack.isEmpty() || stack.is(EMPTY_HAND); }
}
