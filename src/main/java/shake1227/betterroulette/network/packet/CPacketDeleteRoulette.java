package shake1227.betterroulette.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import shake1227.betterroulette.common.entity.RouletteEntity;

import java.util.function.Supplier;

public class CPacketDeleteRoulette {
    private final int entityId;

    public CPacketDeleteRoulette(int entityId) {
        this.entityId = entityId;
    }

    public CPacketDeleteRoulette(FriendlyByteBuf buf) {
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

            if (player.level().getEntity(this.entityId) instanceof RouletteEntity roulette) {
                if (roulette.isOwnerOrOp(player)) {
                    roulette.discard();
                }
            }
        });
        return true;
    }
}