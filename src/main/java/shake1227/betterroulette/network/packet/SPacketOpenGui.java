package shake1227.betterroulette.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import shake1227.betterroulette.client.screen.RouletteConfigScreen;
import shake1227.betterroulette.common.entity.RouletteEntity;

import java.util.function.Supplier;

public class SPacketOpenGui {
    private final int entityId;
    private final CompoundTag nbt;

    public SPacketOpenGui(RouletteEntity entity) {
        this.entityId = entity.getId();
        this.nbt = entity.getConfigAsNBT();
    }

    public SPacketOpenGui(FriendlyByteBuf buf) {
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
                Minecraft.getInstance().setScreen(new RouletteConfigScreen(entityId, nbt));
            });
        });
        return true;
    }
}