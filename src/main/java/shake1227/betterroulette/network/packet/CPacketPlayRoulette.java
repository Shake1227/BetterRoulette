package shake1227.betterroulette.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import shake1227.betterroulette.common.entity.RouletteEntity;

import java.util.function.Supplier;

public class CPacketPlayRoulette {
    private final int entityId;

    public CPacketPlayRoulette(int entityId) {
        this.entityId = entityId;
    }

    public CPacketPlayRoulette(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            Level level = player.level();
            if (level.getEntity(this.entityId) instanceof RouletteEntity roulette) {
                roulette.playerAttemptSpin(player);
            }
        });
        return true;
    }
}