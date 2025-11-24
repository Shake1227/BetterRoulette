package shake1227.betterroulette.common.entity;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import shake1227.betterroulette.common.data.RouletteEntry;
import shake1227.betterroulette.compats.VaultProxy;
import shake1227.betterroulette.core.config.ModConfig;
import shake1227.betterroulette.network.ModPackets;
import shake1227.betterroulette.network.packet.SPacketOpenGui;
import shake1227.betterroulette.util.ChatUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RouletteEntity extends Entity {
    private static final EntityDataAccessor<Component> ROULETTE_NAME = SynchedEntityData.defineId(RouletteEntity.class, EntityDataSerializers.COMPONENT);
    private static final EntityDataAccessor<Float> RENDER_ROTATION = SynchedEntityData.defineId(RouletteEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<CompoundTag> ENTRIES_NBT = SynchedEntityData.defineId(RouletteEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private UUID ownerUUID;
    private int cost = 0;
    private boolean useVault = false;
    private boolean autoStop = true;
    private int stopTimeSeconds = 5;
    private float coastingLevel = 1.0f;
    private List<RouletteEntry> serverSideEntries = new ArrayList<>();

    private State currentState = State.IDLE;
    private float currentRotation = 0;
    private float rotationSpeed = 0;
    private float targetRotation = 0;
    private int spinTicks = 0;
    private int winningEntryIndex = -1;

    public enum State { IDLE, ACCELERATING, SPINNING, COASTING, STOPPED }

    public RouletteEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;

        if (player.isShiftKeyDown()) {
            if (isOwnerOrOp(player)) {
                ModPackets.sendToPlayer(serverPlayer, new SPacketOpenGui(this));
            }
        } else {
            if (currentState != State.IDLE) {
                serverPlayer.sendSystemMessage(ChatUtil.parse(Component.translatable("chat.betterroulette.play.already_spinning").getString()));
                return InteractionResult.SUCCESS;
            }
            if (serverSideEntries.isEmpty()) {
                serverPlayer.sendSystemMessage(ChatUtil.parse(Component.translatable("chat.betterroulette.play.no_entries").getString()));
                return InteractionResult.SUCCESS;
            }

            MutableComponent message = Component.literal("----------------------------------------\n");
            message.append(ChatUtil.parse(Component.translatable("chat.betterroulette.play.info", this.entityData.get(ROULETTE_NAME).getString()).getString()));
            message.append("\n");

            if (useVault && VaultProxy.isVaultLoaded) {
                message.append(ChatUtil.parse(Component.translatable("chat.betterroulette.play.cost_vault", this.cost).getString()));
            }
            message.append("\n");

            MutableComponent playComponent = ChatUtil.parse(Component.translatable("chat.betterroulette.play.click").getString());
            playComponent.setStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/roulette spin " + this.getId()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to spin!")))
            );
            message.append(playComponent);
            message.append("\n----------------------------------------");
            serverPlayer.sendSystemMessage(message);
        }
        return InteractionResult.SUCCESS;
    }

    public void playerAttemptSpin(ServerPlayer player) {
        if (currentState != State.IDLE) return;

        if (useVault && VaultProxy.isVaultLoaded) {
            if (!VaultProxy.withdraw(player, this.cost)) {
                player.sendSystemMessage(ChatUtil.parse(Component.translatable("chat.betterroulette.play.no_cost").getString()));
                return;
            }
        }

        startSpin();
    }

    private void startSpin() {
        if (serverSideEntries.isEmpty()) return;

        currentState = State.ACCELERATING;
        spinTicks = 0;

        winningEntryIndex = this.random.nextInt(serverSideEntries.size());

        float entryAngle = 360.0f / serverSideEntries.size();
        float winningAngle = 270 - (winningEntryIndex * entryAngle) - (entryAngle / 2.0f);

        int extraSpins = 3 + this.random.nextInt(3);
        this.targetRotation = winningAngle + (360 * extraSpins);

        this.rotationSpeed = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        spinTicks++;
        boolean changed = false;

        switch (currentState) {
            case ACCELERATING:
                rotationSpeed = Mth.lerp(spinTicks / 20.0f, 0, 50);
                currentRotation = (currentRotation + rotationSpeed) % 360;
                if (spinTicks >= 20) {
                    currentState = State.SPINNING;
                    spinTicks = 0;
                }
                changed = true;
                break;
            case SPINNING:
                currentRotation = (currentRotation + rotationSpeed) % 360;
                if (autoStop && spinTicks >= stopTimeSeconds * 20) {
                    currentState = State.COASTING;
                    float rotations = Mth.floor(currentRotation / 360.0f);
                    float finalTarget = targetRotation - ((Mth.floor(targetRotation/360f) - rotations) * 360);
                    if (finalTarget < currentRotation) finalTarget += 360;
                    this.targetRotation = finalTarget;
                }
                changed = true;
                break;
            case COASTING:
                float t = Mth.clamp(1.0f - (currentRotation / targetRotation), 0, 1);
                rotationSpeed = Mth.lerp(t * t, 0, 50);

                if (currentRotation >= targetRotation || rotationSpeed < 0.1) {
                    currentRotation = targetRotation % 360;
                    rotationSpeed = 0;
                    currentState = State.STOPPED;
                } else {
                    currentRotation = Mth.lerp(0.1f, currentRotation, targetRotation);
                }
                changed = true;
                break;
            case STOPPED:
                onSpinEnd();
                currentState = State.IDLE;
                break;
        }

        if (changed) {
            this.entityData.set(RENDER_ROTATION, this.currentRotation);
        }
    }

    private void onSpinEnd() {
        if (winningEntryIndex == -1 || winningEntryIndex >= serverSideEntries.size()) return;

        RouletteEntry winner = serverSideEntries.get(winningEntryIndex);
        ServerLevel serverLevel = (ServerLevel) this.level();
        Player spinningPlayer = serverLevel.getNearestPlayer(this, 16);

        Component resultMsg = ChatUtil.parse(Component.translatable("chat.betterroulette.result", winner.getName()).getString());
        serverLevel.getPlayers(p -> p.distanceToSqr(this) < 256).forEach(p -> p.sendSystemMessage(resultMsg));

        CommandSourceStack source = this.createCommandSourceStack().withPermission(2);
        if (spinningPlayer instanceof ServerPlayer) {
            source = source.withEntity(spinningPlayer);
        }

        for (String command : winner.getCommands()) {
            String parsedCmd = command.replace("@p", spinningPlayer != null ? spinningPlayer.getGameProfile().getName() : "");
            serverLevel.getServer().getCommands().performPrefixedCommand(source, parsedCmd);
        }

        if (winner.isJackpot()) {
            serverLevel.getPlayers(p -> p.distanceToSqr(this) < 256)
                    .forEach(p -> p.sendSystemMessage(ChatUtil.parse(Component.translatable("chat.betterroulette.jackpot").getString())));

            String jackpotCmd = ModConfig.SERVER.jackpotCommand.get();
            if (jackpotCmd != null && !jackpotCmd.isBlank()) {
                String parsedJackpotCmd = jackpotCmd.replace("@p", spinningPlayer != null ? spinningPlayer.getGameProfile().getName() : "");
                serverLevel.getServer().getCommands().performPrefixedCommand(source, parsedJackpotCmd);
            }
        }

        winningEntryIndex = -1;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ROULETTE_NAME, Component.literal("Roulette"));
        this.entityData.define(RENDER_ROTATION, 0.0f);
        this.entityData.define(ENTRIES_NBT, new CompoundTag());
    }

    public List<RouletteEntry> getEntries() {
        List<RouletteEntry> clientEntries = new ArrayList<>();
        CompoundTag compound = this.entityData.get(ENTRIES_NBT);
        ListTag listTag = compound.getList("Entries", 10);
        for (int i = 0; i < listTag.size(); i++) {
            clientEntries.add(RouletteEntry.fromNBT(listTag.getCompound(i)));
        }
        return clientEntries;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        if (this.ownerUUID != null) nbt.putUUID("Owner", this.ownerUUID);
        nbt.put("Config", getConfigAsNBT());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        if (nbt.hasUUID("Owner")) this.ownerUUID = nbt.getUUID("Owner");
        if (nbt.contains("Config", 10)) setConfigFromNBT(nbt.getCompound("Config"));
    }

    public CompoundTag getConfigAsNBT() {
        CompoundTag config = new CompoundTag();
        config.putString("Name", Component.Serializer.toJson(this.entityData.get(ROULETTE_NAME)));
        config.putInt("Cost", this.cost);
        config.putBoolean("UseVault", this.useVault);
        config.putBoolean("AutoStop", this.autoStop);
        config.putInt("StopTime", this.stopTimeSeconds);
        config.putFloat("Coasting", this.coastingLevel);
        ListTag list = new ListTag();
        for (RouletteEntry entry : this.serverSideEntries) {
            list.add(entry.toNBT());
        }
        config.put("Entries", list);
        return config;
    }

    public void setConfigFromNBT(CompoundTag config) {
        this.entityData.set(ROULETTE_NAME, Component.Serializer.fromJson(config.getString("Name")));
        this.cost = config.getInt("Cost");
        this.useVault = config.getBoolean("UseVault");
        this.autoStop = config.getBoolean("AutoStop");
        this.stopTimeSeconds = config.getInt("StopTime");
        this.coastingLevel = config.getFloat("Coasting");

        CompoundTag entriesTag = new CompoundTag();
        entriesTag.put("Entries", config.getList("Entries", 10));
        this.entityData.set(ENTRIES_NBT, entriesTag);

        if (!level().isClientSide) {
            this.serverSideEntries.clear();
            ListTag list = config.getList("Entries", 10);
            for (int i = 0; i < list.size(); i++) {
                this.serverSideEntries.add(RouletteEntry.fromNBT(list.getCompound(i)));
            }
        }
    }

    public void setOwner(Player player) { this.ownerUUID = player.getUUID(); }
    public UUID getOwnerId() { return this.ownerUUID; }
    public boolean isOwnerOrOp(Player player) { return player.getUUID().equals(ownerUUID) || player.hasPermissions(2); }
    public float getRenderRotation() { return this.entityData.get(RENDER_ROTATION); }
    @Override public boolean isPushable() { return false; }
    @Override public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        this.setBoundingBox(this.getType().getDimensions().makeBoundingBox(new Vec3(x, y, z)));
    }
}