package shake1227.betterroulette.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import shake1227.betterroulette.BetterRoulette;
import shake1227.betterroulette.network.packet.CPacketUpdateRoulette;
import shake1227.betterroulette.network.packet.SPacketOpenGui;

public class ModPackets {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath(BetterRoulette.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(SPacketOpenGui.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SPacketOpenGui::new)
                .encoder(SPacketOpenGui::toBytes)
                .consumerMainThread(SPacketOpenGui::handle)
                .add();

        net.messageBuilder(CPacketUpdateRoulette.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CPacketUpdateRoulette::new)
                .encoder(CPacketUpdateRoulette::toBytes)
                .consumerMainThread(CPacketUpdateRoulette::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(ServerPlayer player, MSG message) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}