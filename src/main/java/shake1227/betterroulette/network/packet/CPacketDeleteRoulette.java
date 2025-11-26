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

    // デコード（受信時）
    public CPacketDeleteRoulette(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    // エンコード（送信時）：IDだけを送るので非常に軽量
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    // 処理（サーバー側）
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // IDからエンティティを取得して、所有者なら削除(kill)する
            if (player.level().getEntity(this.entityId) instanceof RouletteEntity roulette) {
                if (roulette.isOwnerOrOp(player)) {
                    roulette.discard(); // エンティティを削除
                }
            }
        });
        return true;
    }
}