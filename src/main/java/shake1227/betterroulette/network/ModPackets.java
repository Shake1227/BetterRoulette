package shake1227.betterroulette.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import shake1227.betterroulette.BetterRoulette;
import shake1227.betterroulette.network.packet.*;

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

        // 既存
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

        net.messageBuilder(CPacketDeleteRoulette.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CPacketDeleteRoulette::new)
                .encoder(CPacketDeleteRoulette::toBytes)
                .consumerMainThread(CPacketDeleteRoulette::handle)
                .add();

        // 【新規】プレイ画面を開くパケット
        net.messageBuilder(SPacketOpenPlayGui.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SPacketOpenPlayGui::new)
                .encoder(SPacketOpenPlayGui::toBytes)
                .consumerMainThread(SPacketOpenPlayGui::handle)
                .add();

        // 【新規】プレイ開始パケット
        net.messageBuilder(CPacketPlayRoulette.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CPacketPlayRoulette::new)
                .encoder(CPacketPlayRoulette::toBytes)
                .consumerMainThread(CPacketPlayRoulette::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(ServerPlayer player, MSG message) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}