package shake1227.betterroulette.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import shake1227.betterroulette.common.data.RouletteEntry;
import shake1227.betterroulette.core.config.ModConfig;
import shake1227.betterroulette.network.ModPackets;
import shake1227.betterroulette.network.packet.CPacketPlayRoulette;
import shake1227.betterroulette.client.renderer.util.ChatUtil;

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

    private float scaleFactor = 1.0f;

    public RoulettePlayScreen(int entityId, CompoundTag nbt) {
        super(Component.translatable("gui.betterroulette.play.title"));
        this.entityId = entityId;
        this.nbt = nbt;
        loadDataFromNbt();
    }

    private void loadDataFromNbt() {
        String jsonName = this.nbt.getString("Name");
        try {
            MutableComponent c = Component.Serializer.fromJson(jsonName);
            if (c != null) {
                this.rouletteName = ChatUtil.parse(c.getString());
            } else {
                this.rouletteName = ChatUtil.parse(jsonName);
            }
        } catch (Exception e) {
            this.rouletteName = ChatUtil.parse(jsonName);
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

        float currentGuiScale = (float) this.minecraft.getWindow().getGuiScale();
        this.scaleFactor = 3.0f / currentGuiScale;

        int virtualWidth = (int) (this.width / this.scaleFactor);
        int virtualHeight = (int) (this.height / this.scaleFactor);

        int centerX = virtualWidth / 2;
        int topY = 40;

        int btnWidth = 120;
        Button playButton = Button.builder(Component.translatable("gui.betterroulette.play.start"), b -> {
            ModPackets.sendToServer(new CPacketPlayRoulette(this.entityId));
            this.onClose();
        }).bounds(centerX - btnWidth / 2, topY + 45, btnWidth, 20).build();
        this.addRenderableWidget(playButton);

        int listWidth = 200;
        int listTop = topY + 80;
        int listBottom = virtualHeight - 20;
        int listHeight = listBottom - listTop;

        this.playEntryList = new PlayEntryList(this, this.minecraft, listWidth, listHeight, listTop, 24, virtualHeight);
        this.playEntryList.setLeftPos(centerX - listWidth / 2);

        this.entries.forEach(entry -> this.playEntryList.addEntry(new PlayEntryList.Entry(entry)));
        this.addRenderableWidget(this.playEntryList);
    }

    public float getScaleFactor() {
        return this.scaleFactor;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int virtualMouseX = (int) (mouseX / this.scaleFactor);
        int virtualMouseY = (int) (mouseY / this.scaleFactor);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(this.scaleFactor, this.scaleFactor, 1.0f);

        int virtualWidth = (int) (this.width / this.scaleFactor);
        int virtualHeight = (int) (this.height / this.scaleFactor);
        guiGraphics.fillGradient(0, 0, virtualWidth, virtualHeight, 0xC0101010, 0xD0101010);

        super.render(guiGraphics, virtualMouseX, virtualMouseY, partialTicks);

        int centerX = virtualWidth / 2;
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
            costText += Component.translatable("gui.betterroulette.config.cost").getString() + ": " + cost + ModConfig.SERVER.currencyUnit.get();
        }
        if (useItemCost && !costItem.isEmpty()) {
            if (!costText.isEmpty()) costText += " + ";
            costText += costItem.getHoverName().getString() + " x" + costItem.getCount();
        }
        if (costText.isEmpty()) {
            costText = "Free";
        }
        guiGraphics.drawCenteredString(this.font, costText, centerX, topY + 25, 0xAAAAAA);

        PlayEntryList.Entry hoveredEntry = this.playEntryList.getEntryAtMouse(virtualMouseX, virtualMouseY);
        if (hoveredEntry != null) {
            RouletteEntry entry = hoveredEntry.entry;
            if (entry.getDesc() != null && !entry.getDesc().isEmpty()) {
                guiGraphics.renderTooltip(this.font, ChatUtil.parse(entry.getDesc()), virtualMouseX, virtualMouseY);
            }
        }

        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX / this.scaleFactor, mouseY / this.scaleFactor, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX / this.scaleFactor, mouseY / this.scaleFactor, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX / this.scaleFactor, mouseY / this.scaleFactor, button, dragX / this.scaleFactor, dragY / this.scaleFactor);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX / this.scaleFactor, mouseY / this.scaleFactor, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class PlayEntryList extends ObjectSelectionList<PlayEntryList.Entry> {
        private final RoulettePlayScreen parent;

        public PlayEntryList(RoulettePlayScreen parent, Minecraft mc, int width, int height, int y, int itemHeight, int screenHeight) {
            super(mc, width, screenHeight, y, y + height, itemHeight);
            this.parent = parent;
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
        }

        public Entry getEntryAtMouse(double mouseX, double mouseY) {
            return super.getEntryAtPosition(mouseX, mouseY);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            double scale = guiScale * parent.getScaleFactor();

            int sX = (int) (this.x0 * scale);
            int sY = (int) (Minecraft.getInstance().getWindow().getHeight() - (this.y1 * scale));
            int sW = (int) ((this.x1 - this.x0) * scale);
            int sH = (int) ((this.y1 - this.y0) * scale);

            RenderSystem.enableScissor(sX, sY, sW, sH);
            this.renderList(guiGraphics, mouseX, mouseY, partialTick);
            RenderSystem.disableScissor();
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
            public final RouletteEntry entry;
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
                    // ハイライト（白半透明）
                    guiGraphics.fill(left, top, left + width, top + height, 0x80FFFFFF);
                }

                int colorBoxSize = 16;
                int boxLeft = left + 5;
                int boxTop = top + (height - colorBoxSize) / 2;

                guiGraphics.fill(boxLeft, boxTop, boxLeft + colorBoxSize, boxTop + colorBoxSize, 0xFF000000 | entry.getColor());
                guiGraphics.renderOutline(boxLeft, boxTop, colorBoxSize, colorBoxSize, 0xFFFFFFFF);

                // 名前をChatUtil.parseで描画
                guiGraphics.drawString(Minecraft.getInstance().font, ChatUtil.parse(entry.getName()), boxLeft + colorBoxSize + 8, top + (height - 8) / 2, 0xFFFFFF);

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