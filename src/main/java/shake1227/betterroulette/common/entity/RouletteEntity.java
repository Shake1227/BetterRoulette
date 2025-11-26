package shake1227.betterroulette.common.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import shake1227.betterroulette.common.data.RouletteEntry;
import shake1227.betterroulette.compats.VaultProxy;
import shake1227.betterroulette.core.config.ModConfig;
import shake1227.betterroulette.network.ModPackets;
import shake1227.betterroulette.network.packet.SPacketOpenGui;
import shake1227.betterroulette.network.packet.SPacketOpenPlayGui;
import shake1227.betterroulette.client.renderer.util.ChatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class RouletteEntity extends Entity {
    private static final EntityDataAccessor<Component> ROULETTE_NAME = SynchedEntityData.defineId(RouletteEntity.class, EntityDataSerializers.COMPONENT);
    private static final EntityDataAccessor<Float> RENDER_ROTATION = SynchedEntityData.defineId(RouletteEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<CompoundTag> ENTRIES_NBT = SynchedEntityData.defineId(RouletteEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private UUID ownerUUID;
    private int cost = 0;
    private boolean useVault = false;

    private boolean useItemCost = false;
    private ItemStack costItem = ItemStack.EMPTY;

    private float coastingValue = 1.0f;
    private boolean isAutoStop = true;
    private int autoStopTimeMin = 5;
    private int autoStopTimeMax = 10;

    private boolean isMixMode = false;
    private int mixModeSlotCount = 60;
    private long shuffleSeed = 0L;

    private List<RouletteEntry> serverSideEntries = new ArrayList<>();
    private List<RouletteEntry> clientSideEntries = new ArrayList<>();

    private List<Integer> cachedMixIndices = null;

    private State currentState = State.IDLE;
    private float currentRotation = 0;
    private float rotationSpeed = 0;
    private float targetRotation = 0;
    private int spinTicks = 0;
    private int targetSpinTicks = 0;
    private int winningEntryIndex = -1;
    private UUID currentPlayerUUID;

    public enum State { IDLE, ACCELERATING, SPINNING, COASTING, STOPPED }

    public RouletteEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ROULETTE_NAME, Component.literal("Roulette"));
        this.entityData.define(RENDER_ROTATION, 0.0f);
        this.entityData.define(ENTRIES_NBT, new CompoundTag());
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

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ENTRIES_NBT.equals(key) && this.level().isClientSide) {
            CompoundTag tag = this.entityData.get(ENTRIES_NBT);

            this.clientSideEntries.clear();
            ListTag list = tag.getList("Entries", 10);
            for (int i = 0; i < list.size(); i++) {
                this.clientSideEntries.add(RouletteEntry.fromNBT(list.getCompound(i)));
            }

            if (tag.contains("IsMixMode")) {
                this.isMixMode = tag.getBoolean("IsMixMode");
                this.shuffleSeed = tag.getLong("ShuffleSeed");
            }
            if (tag.contains("MixSlotCount")) {
                this.mixModeSlotCount = tag.getInt("MixSlotCount");
            }

            this.cachedMixIndices = null;
        }
    }

    public List<RouletteEntry> getEntries() {
        if (this.level().isClientSide) {
            if (this.clientSideEntries.isEmpty()) {
                CompoundTag tag = this.entityData.get(ENTRIES_NBT);
                ListTag list = tag.getList("Entries", 10);
                for (int i = 0; i < list.size(); i++) {
                    this.clientSideEntries.add(RouletteEntry.fromNBT(list.getCompound(i)));
                }
                if (tag.contains("IsMixMode")) {
                    this.isMixMode = tag.getBoolean("IsMixMode");
                    this.shuffleSeed = tag.getLong("ShuffleSeed");
                }
                if (tag.contains("MixSlotCount")) {
                    this.mixModeSlotCount = tag.getInt("MixSlotCount");
                }
            }
            return this.clientSideEntries;
        } else {
            return this.serverSideEntries;
        }
    }

    public List<Integer> getMixModeIndices() {
        if (cachedMixIndices != null && !cachedMixIndices.isEmpty()) return cachedMixIndices;

        List<RouletteEntry> currentEntries = getEntries();
        List<Integer> indices = new ArrayList<>();
        if (currentEntries.isEmpty()) return indices;

        int totalWeight = currentEntries.stream().mapToInt(RouletteEntry::getWeight).sum();
        if (totalWeight <= 0) totalWeight = 1;

        int totalSlots = Math.max(10, this.mixModeSlotCount);

        for (int i = 0; i < currentEntries.size(); i++) {
            int weight = currentEntries.get(i).getWeight();
            int slots = Math.round(((float)weight / totalWeight) * totalSlots);
            for (int k = 0; k < slots; k++) {
                indices.add(i);
            }
        }

        while (indices.size() < totalSlots) {
            indices.add(this.random.nextInt(currentEntries.size()));
        }
        while (indices.size() > totalSlots) {
            indices.remove(indices.size() - 1);
        }

        Collections.shuffle(indices, new Random(this.shuffleSeed));

        this.cachedMixIndices = indices;
        return indices;
    }

    public void setConfigFromNBT(CompoundTag config) {
        this.entityData.set(ROULETTE_NAME, Component.Serializer.fromJson(config.getString("Name")));
        this.cost = config.getInt("Cost");
        this.useVault = config.getBoolean("UseVault");
        this.useItemCost = config.getBoolean("UseItemCost");
        if (config.contains("CostItem")) {
            this.costItem = ItemStack.of(config.getCompound("CostItem"));
        }

        if (config.contains("Coasting")) this.coastingValue = config.getFloat("Coasting");
        if (config.contains("IsAutoStop")) this.isAutoStop = config.getBoolean("IsAutoStop");
        if (config.contains("AutoMin")) this.autoStopTimeMin = config.getInt("AutoMin");
        if (config.contains("AutoMax")) this.autoStopTimeMax = config.getInt("AutoMax");

        if (config.contains("IsMixMode")) {
            this.isMixMode = config.getBoolean("IsMixMode");
            if (config.contains("ShuffleSeed")) {
                this.shuffleSeed = config.getLong("ShuffleSeed");
            }
        } else {
            this.isMixMode = false;
        }

        if (config.contains("MixSlotCount")) {
            this.mixModeSlotCount = Math.max(10, config.getInt("MixSlotCount"));
        }

        this.cachedMixIndices = null;

        CompoundTag syncTag = new CompoundTag();
        syncTag.put("Entries", config.getList("Entries", 10));
        syncTag.putBoolean("IsMixMode", this.isMixMode);
        syncTag.putLong("ShuffleSeed", this.shuffleSeed);
        syncTag.putInt("MixSlotCount", this.mixModeSlotCount);

        this.entityData.set(ENTRIES_NBT, syncTag);

        if (!level().isClientSide) {
            this.serverSideEntries.clear();
            ListTag list = config.getList("Entries", 10);
            for (int i = 0; i < list.size(); i++) {
                this.serverSideEntries.add(RouletteEntry.fromNBT(list.getCompound(i)));
            }
        }
    }

    public CompoundTag getConfigAsNBT() {
        CompoundTag config = new CompoundTag();
        config.putString("Name", Component.Serializer.toJson(this.entityData.get(ROULETTE_NAME)));
        config.putInt("Cost", this.cost);
        config.putBoolean("UseVault", this.useVault);
        config.putBoolean("UseItemCost", this.useItemCost);
        config.put("CostItem", this.costItem.save(new CompoundTag()));

        config.putFloat("Coasting", this.coastingValue);
        config.putBoolean("IsAutoStop", this.isAutoStop);
        config.putInt("AutoMin", this.autoStopTimeMin);
        config.putInt("AutoMax", this.autoStopTimeMax);
        config.putBoolean("IsMixMode", this.isMixMode);
        config.putLong("ShuffleSeed", this.shuffleSeed);
        config.putInt("MixSlotCount", this.mixModeSlotCount);

        ListTag list = new ListTag();
        for (RouletteEntry entry : this.serverSideEntries) {
            list.add(entry.toNBT());
        }
        config.put("Entries", list);
        return config;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;

        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;

        if (player.isShiftKeyDown()) {
            if (isOwnerOrOp(player)) {
                ModPackets.sendToPlayer(serverPlayer, new SPacketOpenGui(this));
            }
        } else {
            if (currentState == State.SPINNING && !isAutoStop) {
                if (currentPlayerUUID != null && currentPlayerUUID.equals(player.getUUID())) {
                    triggerManualStop();
                    return InteractionResult.SUCCESS;
                }
            }

            if (currentState != State.IDLE) {
                serverPlayer.sendSystemMessage(ChatUtil.parse(Component.translatable("chat.betterroulette.play.already_spinning").getString()));
                return InteractionResult.SUCCESS;
            }

            if (isPlayerPlayingOtherRoulette(serverPlayer)) {
                serverPlayer.sendSystemMessage(Component.literal("§cYou are already playing another roulette!"));
                return InteractionResult.SUCCESS;
            }

            if (serverSideEntries.isEmpty()) {
                serverPlayer.sendSystemMessage(ChatUtil.parse(Component.translatable("chat.betterroulette.play.no_entries").getString()));
                return InteractionResult.SUCCESS;
            }
            ModPackets.sendToPlayer(serverPlayer, new SPacketOpenPlayGui(this));
        }
        return InteractionResult.SUCCESS;
    }

    private boolean isPlayerPlayingOtherRoulette(ServerPlayer player) {
        for (String tag : player.getTags()) {
            if (tag.startsWith("betterroulette:playing_")) {
                try {
                    String idStr = tag.substring("betterroulette:playing_".length());
                    int entityId = Integer.parseInt(idStr);
                    if (entityId == this.getId()) {
                        player.removeTag(tag);
                        continue;
                    }
                    Entity target = player.level().getEntity(entityId);
                    if (target instanceof RouletteEntity roulette && roulette.currentState != State.IDLE) {
                        return true;
                    } else {
                        player.removeTag(tag);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return false;
    }

    public void playerAttemptSpin(ServerPlayer player) {
        if (currentState != State.IDLE) return;
        if (isPlayerPlayingOtherRoulette(player)) return;

        if (useVault && VaultProxy.isVaultLoaded && cost > 0) {
            if (!VaultProxy.withdraw(player, this.cost)) {
                player.sendSystemMessage(ChatUtil.parse(Component.translatable("chat.betterroulette.play.no_cost").getString()));
                return;
            }
        }
        if (useItemCost && !costItem.isEmpty()) {
            if (!hasEnoughItems(player, costItem)) {
                player.sendSystemMessage(Component.literal("You don't have enough items to play!"));
                return;
            }
            consumeItems(player, costItem);
        }

        startSpin(player);
    }

    private boolean hasEnoughItems(Player player, ItemStack cost) {
        int needed = cost.getCount();
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameTags(stack, cost)) {
                count += stack.getCount();
            }
        }
        return count >= needed;
    }

    private void consumeItems(Player player, ItemStack cost) {
        int needed = cost.getCount();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack stack = inv.items.get(i);
            if (ItemStack.isSameItemSameTags(stack, cost)) {
                int take = Math.min(stack.getCount(), needed);
                stack.shrink(take);
                needed -= take;
                if (stack.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
                if (needed <= 0) break;
            }
        }
    }

    private void startSpin(ServerPlayer player) {
        if (serverSideEntries.isEmpty()) return;

        this.currentPlayerUUID = player.getUUID();
        player.addTag("betterroulette:playing_" + this.getId());

        currentState = State.ACCELERATING;
        spinTicks = 0;

        if (isAutoStop) {
            int minTicks = Math.max(20, autoStopTimeMin * 20);
            int maxTicks = Math.max(minTicks, autoStopTimeMax * 20);
            this.targetSpinTicks = this.random.nextInt(maxTicks - minTicks + 1) + minTicks;
        }

        int totalWeight = serverSideEntries.stream().mapToInt(RouletteEntry::getWeight).sum();
        if (totalWeight <= 0) totalWeight = 1;

        int randomVal = this.random.nextInt(totalWeight);
        int currentWeight = 0;
        winningEntryIndex = 0;

        for (int i = 0; i < serverSideEntries.size(); i++) {
            currentWeight += serverSideEntries.get(i).getWeight();
            if (randomVal < currentWeight) {
                winningEntryIndex = i;
                break;
            }
        }

        float targetAngle = calculateTargetAngle(winningEntryIndex, totalWeight);
        int extraSpins = 3 + this.random.nextInt(3);
        this.targetRotation = targetAngle + (360 * extraSpins);
        this.rotationSpeed = 0;
    }

    private void triggerManualStop() {
        if (currentState == State.SPINNING) {
            currentState = State.COASTING;
            recalculateTargetForCoasting();
        }
    }

    private float calculateTargetAngle(int index, int totalWeight) {
        if (isMixMode) {
            List<Integer> mixIndices = getMixModeIndices();
            List<Integer> targetSlots = new ArrayList<>();

            for (int i = 0; i < mixIndices.size(); i++) {
                if (mixIndices.get(i) == index) {
                    targetSlots.add(i);
                }
            }

            if (targetSlots.isEmpty()) return 0;
            int targetSlotIndex = targetSlots.get(this.random.nextInt(targetSlots.size()));

            float slotAngleSize = 360.0f / mixIndices.size();
            float centerAngle = (targetSlotIndex * slotAngleSize) + (slotAngleSize / 2.0f);

            return 270.0f - centerAngle;

        } else {
            float accumulatedWeight = 0;
            for(int i = 0; i < index; i++) {
                accumulatedWeight += serverSideEntries.get(i).getWeight();
            }
            float startAngle = (accumulatedWeight / totalWeight) * 360.0f;
            float endAngle = ((accumulatedWeight + serverSideEntries.get(index).getWeight()) / totalWeight) * 360.0f;
            float entryCenter = (startAngle + endAngle) / 2.0f;
            return 270.0f - entryCenter;
        }
    }

    private void recalculateTargetForCoasting() {
        float rotations = Mth.floor(currentRotation / 360.0f);
        float baseTarget = targetRotation % 360;
        if (baseTarget < 0) baseTarget += 360;

        float finalTarget = (rotations * 360) + baseTarget;
        if (finalTarget < currentRotation) finalTarget += 360;

        if (coastingValue > 0) {
            finalTarget += 360 * (1 + (int)coastingValue);
        }

        this.targetRotation = finalTarget;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (currentState == State.SPINNING && !isAutoStop && currentPlayerUUID != null) {
            Player p = ((ServerLevel)level()).getServer().getPlayerList().getPlayer(currentPlayerUUID);
            if (p != null) {
                p.displayClientMessage(Component.translatable("chat.betterroulette.action.click_to_stop"), true);
            }
        }

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
                currentRotation = (currentRotation + rotationSpeed) % 36000;
                if (isAutoStop && spinTicks >= targetSpinTicks) {
                    currentState = State.COASTING;
                    recalculateTargetForCoasting();
                }
                changed = true;
                break;

            case COASTING:
                if (coastingValue <= 0) {
                    currentRotation = targetRotation;
                    rotationSpeed = 0;
                    currentState = State.STOPPED;
                } else {
                    float dist = targetRotation - currentRotation;
                    if (dist <= 0.1f) {
                        currentRotation = targetRotation;
                        rotationSpeed = 0;
                        currentState = State.STOPPED;
                    } else {
                        currentRotation = Mth.lerp(0.1f / coastingValue, currentRotation, targetRotation);
                    }
                }
                changed = true;
                break;

            case STOPPED:
                onSpinEnd();
                currentState = State.IDLE;
                currentPlayerUUID = null;
                break;
        }

        if (changed) {
            this.entityData.set(RENDER_ROTATION, this.currentRotation % 360);
        }
    }

    private void onSpinEnd() {
        if (currentPlayerUUID != null) {
            Player p = ((ServerLevel)level()).getServer().getPlayerList().getPlayer(currentPlayerUUID);
            if (p != null) {
                p.removeTag("betterroulette:playing_" + this.getId());
            }
        }

        if (winningEntryIndex == -1 || winningEntryIndex >= serverSideEntries.size()) return;

        RouletteEntry winner = serverSideEntries.get(winningEntryIndex);
        ServerLevel serverLevel = (ServerLevel) this.level();
        Component resultMsg = ChatUtil.parse(Component.translatable("chat.betterroulette.result", winner.getName()).getString());
        serverLevel.getPlayers(p -> p.distanceToSqr(this) < 256).forEach(p -> p.sendSystemMessage(resultMsg));

        if (winner.getDesc() != null && !winner.getDesc().isEmpty()) {
            MutableComponent descMsg = Component.translatable("chat.betterroulette.prize_get")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(winner.getDesc()).withStyle(ChatFormatting.WHITE));

            if (currentPlayerUUID != null) {
                Player p = serverLevel.getPlayerByUUID(currentPlayerUUID);
                if (p != null) p.sendSystemMessage(descMsg);
            }
        }

        CommandSourceStack source = serverLevel.getServer().createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();

        String playerName = "Unknown";
        if (currentPlayerUUID != null) {
            Player p = serverLevel.getPlayerByUUID(currentPlayerUUID);
            if (p != null) playerName = p.getGameProfile().getName();
        }

        for (String command : winner.getCommands()) {
            String parsedCmd = command
                    .replace("@p", playerName)
                    .replace("@dp", playerName);
            serverLevel.getServer().getCommands().performPrefixedCommand(source, parsedCmd);
        }

        if (winner.isJackpot()) {
            if (currentPlayerUUID != null) {
                ServerPlayer p = serverLevel.getServer().getPlayerList().getPlayer(currentPlayerUUID);
                if (p != null) {
                    p.connection.send(new ClientboundSetTitleTextPacket(
                            Component.literal("★JACKPOT!!★").withStyle(style -> style.withColor(0xFFA500).withBold(true))
                    ));
                }
            }

            String jackpotCmd = ModConfig.SERVER.jackpotCommand.get();
            if (jackpotCmd != null && !jackpotCmd.isBlank()) {
                String parsedJackpotCmd = jackpotCmd
                        .replace("@p", playerName)
                        .replace("@dp", playerName);
                serverLevel.getServer().getCommands().performPrefixedCommand(source, parsedJackpotCmd);
            }
        }

        winningEntryIndex = -1;
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (currentPlayerUUID != null && !level().isClientSide) {
            Player p = ((ServerLevel)level()).getServer().getPlayerList().getPlayer(currentPlayerUUID);
            if (p != null) {
                p.removeTag("betterroulette:playing_" + this.getId());
            }
        }
    }

    public boolean isMixMode() { return isMixMode; }
    public void setOwner(Player player) { this.ownerUUID = player.getUUID(); }
    public UUID getOwnerId() { return this.ownerUUID; }
    public boolean isOwnerOrOp(Player player) { return player.getUUID().equals(ownerUUID) || player.hasPermissions(2); }
    public float getRenderRotation() { return this.entityData.get(RENDER_ROTATION); }
}