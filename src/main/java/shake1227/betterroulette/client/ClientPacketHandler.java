package shake1227.betterroulette.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import shake1227.betterroulette.client.screen.RouletteConfigScreen;
import shake1227.betterroulette.client.screen.RoulettePlayScreen;

public class ClientPacketHandler {
    public static void handleOpenGui(int entityId, CompoundTag nbt) {
        Minecraft.getInstance().setScreen(new RouletteConfigScreen(entityId, nbt));
    }

    public static void handleOpenPlayGui(int entityId, CompoundTag nbt) {
        Minecraft.getInstance().setScreen(new RoulettePlayScreen(entityId, nbt));
    }
}