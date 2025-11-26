package shake1227.betterroulette.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import shake1227.betterroulette.common.data.RouletteEntry;
import shake1227.betterroulette.network.ModPackets;
import shake1227.betterroulette.network.packet.CPacketPlayRoulette;
import shake1227.betterroulette.util.ChatUtil;

import java.util.ArrayList;
import java.util.List;

public class RoulettePlayScreen extends Screen {
    private final int entityId;
    private final CompoundTag nbt;
    private final List<RouletteEntry> entries = new ArrayList<>();

    private Component rouletteName;
    private int cost;
    private boolean useVault;
    private boolean useItemCost;
    private ItemStack costItem = ItemStack.EMPTY;

    private PlayEntryList playEntryList;
    private Component hoveredDesc = null;

    public RoulettePlayScreen(int entityId, CompoundTag nbt) {
        super(Component.translatable("gui.betterroulette.play.title"));
        this.entityId = entityId;
        this.nbt = nbt;
        loadDataFromNbt();
    }

    private void loadDataFromNbt() {
        String jsonName = this.nbt.getString("Name");
        try {
            Component rawComp = Component.Serializer.fromJson(jsonName);
            String rawText = rawComp.getString();
            this.rouletteName = ChatUtil.parse(rawText);
        } catch (Exception e) {
            this.rouletteName = Component.literal("Roulette");
        }

        this.cost = this.nbt.getInt("Cost");
        this.useVault = this.nbt.getBoolean("UseVault");
        this.useItemCost = this.nbt.getBoolean("UseItemCost");
        if (this.nbt.contains("CostItem")) {
            this.costItem = ItemStack.of(this.nbt.getCompound("CostItem"));
        }

        this.entries.clear();
        ListTag listTag = this.nbt.getList("Entries", 10);
        for (int i = 0; i < listTag.size(); i++) {
            this.entries.add(RouletteEntry.fromNBT(listTag.getCompound(i)));
        }
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int topY = 40;

        int btnWidth = 120;
        Button playButton = Button.builder(Component.translatable("gui.betterroulette.play.start"), b -> {
            ModPackets.sendToServer(new CPacketPlayRoulette(this.entityId));
            this.onClose();
        }).bounds(centerX - btnWidth / 2, topY + 45, btnWidth, 20).build();
        this.addRenderableWidget(playButton);

        int listWidth = 200;
        int listTop = topY + 80;
        int listBottom = this.height - 20;
        int listHeight = listBottom - listTop;

        this.playEntryList = new PlayEntryList(this, this.minecraft, listWidth, listHeight, listTop, 24);
        this.playEntryList.setLeftPos(centerX - listWidth / 2);

        this.entries.forEach(entry -> this.playEntryList.addEntry(new PlayEntryList.Entry(entry)));
        this.addRenderableWidget(this.playEntryList);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.hoveredDesc = null;

        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        int centerX = this.width / 2;
        int topY = 40;

        guiGraphics.pose().pushPose();
        float scale = 1.5f;
        guiGraphics.pose().scale(scale, scale, scale);
        float scaledX = centerX / scale;
        float scaledY = topY / scale;
        guiGraphics.drawCenteredString(this.font, this.rouletteName, (int)scaledX, (int)scaledY, 0xFFFFFF);
        guiGraphics.pose().popPose();

        String costText = "";
        if (useVault && cost > 0) {
            costText += Component.translatable("gui.betterroulette.config.cost").getString() + ": " + cost;
        }
        if (useItemCost && !costItem.isEmpty()) {
            if (!costText.isEmpty()) costText += " + ";
            costText += costItem.getHoverName().getString() + " x" + costItem.getCount();
        }
        if (costText.isEmpty()) {
            costText = "Free";
        }
        guiGraphics.drawCenteredString(this.font, costText, centerX, topY + 25, 0xAAAAAA);

        if (this.hoveredDesc != null) {
            guiGraphics.renderTooltip(this.font, this.hoveredDesc, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class PlayEntryList extends ObjectSelectionList<PlayEntryList.Entry> {
        private final RoulettePlayScreen parent;

        public PlayEntryList(RoulettePlayScreen parent, Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, mc.screen.height, y, y + height, itemHeight);
            this.parent = parent;
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
        }

        @Override
        public int addEntry(Entry entry) {
            entry.setList(this);
            return super.addEntry(entry);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowLeft() + this.getRowWidth() + 6;
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        public static class Entry extends ObjectSelectionList.Entry<Entry> {
            private final RouletteEntry entry;
            private PlayEntryList list;

            public Entry(RouletteEntry entry) {
                this.entry = entry;
            }

            public void setList(PlayEntryList list) {
                this.list = list;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovering, float partialTicks) {
                if (isHovering) {
                    guiGraphics.fill(left, top, left + width, top + height, 0x22FFFFFF);
                    if (list != null && list.parent != null && entry.getDesc() != null && !entry.getDesc().isEmpty()) {
                        list.parent.hoveredDesc = Component.literal(entry.getDesc());
                    }
                }

                int colorBoxSize = 16;
                int boxLeft = left + 5;
                int boxTop = top + (height - colorBoxSize) / 2;
                guiGraphics.fill(boxLeft, boxTop, boxLeft + colorBoxSize, boxTop + colorBoxSize, 0xFF000000 | entry.getColor());
                guiGraphics.renderOutline(boxLeft, boxTop, colorBoxSize, colorBoxSize, 0xFFFFFFFF);

                guiGraphics.drawString(Minecraft.getInstance().font, entry.getName(), boxLeft + colorBoxSize + 8, top + (height - 8) / 2, 0xFFFFFF);

                String weight = "(" + entry.getWeight() + ")";
                int wWidth = Minecraft.getInstance().font.width(weight);
                guiGraphics.drawString(Minecraft.getInstance().font, weight, left + width - wWidth - 5, top + (height - 8) / 2, 0xAAAAAA);
            }

            @Override
            public Component getNarration() {
                return Component.literal(entry.getName());
            }
        }
    }
}