package shake1227.betterroulette.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import shake1227.betterroulette.client.screen.RoulettePlayScreen;
import shake1227.betterroulette.common.entity.RouletteEntity;

import java.util.function.Supplier;

public class SPacketOpenPlayGui {
    private final int entityId;
    private final CompoundTag nbt;

    public SPacketOpenPlayGui(RouletteEntity entity) {
        this.entityId = entity.getId();
        this.nbt = entity.getConfigAsNBT();
    }

    public SPacketOpenPlayGui(FriendlyByteBuf buf) {
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
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft.getInstance().setScreen(new RoulettePlayScreen(entityId, nbt));
            });
        });
        return true;
    }
}