package shake1227.betterroulette.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import shake1227.betterroulette.common.entity.RouletteEntity;

import java.util.function.Supplier;

public class CPacketUpdateRoulette {
    private final int entityId;
    private final CompoundTag nbt;

    public CPacketUpdateRoulette(int entityId, CompoundTag nbt) {
        this.entityId = entityId;
        this.nbt = nbt;
    }

    public CPacketUpdateRoulette(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.nbt = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeNbt(nbt);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            Level level = player.level();
            if (level.getEntity(this.entityId) instanceof RouletteEntity roulette) {
                if (roulette.isOwnerOrOp(player)) {
                    roulette.setConfigFromNBT(this.nbt);
                }
            }
        });
        return true;
    }
}