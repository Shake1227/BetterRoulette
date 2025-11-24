package shake1227.betterroulette.client.screen;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import shake1227.betterroulette.client.screen.widget.CommandEntryList;
import shake1227.betterroulette.client.screen.widget.RouletteEntryList;
import shake1227.betterroulette.common.data.RouletteEntry;
import shake1227.betterroulette.network.ModPackets;
import shake1227.betterroulette.network.packet.CPacketUpdateRoulette;

import java.util.ArrayList;
import java.util.List;

public class RouletteConfigScreen extends Screen {
    private final int entityId;
    private final CompoundTag nbt;
    private final List<RouletteEntry> entries = new ArrayList<>();

    private EditBox nameBox;
    private EditBox costBox;
    private CycleButton<Boolean> useVaultButton;
    private RouletteEntryList entryList;
    private Button addEntryButton;
    private Button removeEntryButton;

    private EditBox entryNameBox;
    private EditBox entryColorBox;
    private EditBox commandBox;
    private CommandEntryList commandList;
    private Button addCommandButton;
    private Button removeCommandButton;
    private CycleButton<Boolean> isJackpotButton;
    private final List<AbstractWidget> detailPanelWidgets = new ArrayList<>();

    private RouletteEntryList.Entry selectedEntry;
    private String selectedCommand;

    public RouletteConfigScreen(int entityId, CompoundTag nbt) {
        super(Component.translatable("gui.betterroulette.config.title"));
        this.entityId = entityId;
        this.nbt = nbt;
        loadEntriesFromNbt();
    }

    private void loadEntriesFromNbt() {
        this.entries.clear();
        ListTag listTag = this.nbt.getList("Entries", 10);
        for (int i = 0; i < listTag.size(); i++) {
            this.entries.add(RouletteEntry.fromNBT(listTag.getCompound(i)));
        }
    }

    private void saveEntriesToNbt() {
        ListTag listTag = new ListTag();
        this.entries.forEach(entry -> listTag.add(entry.toNBT()));
        this.nbt.put("Entries", listTag);
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.detailPanelWidgets.clear();

        int mainPanelWidth = 150;
        int detailPanelWidth = 150;
        int panelGap = 20;
        int totalWidth = mainPanelWidth + panelGap + detailPanelWidth;
        int startX = (this.width - totalWidth) / 2;
        int topY = 40;

        this.nameBox = new EditBox(this.font, startX, topY, mainPanelWidth, 20, Component.translatable("gui.betterroulette.config.name"));
        this.nameBox.setValue(Component.Serializer.fromJson(this.nbt.getString("Name")).getString());
        this.addRenderableWidget(this.nameBox);

        this.costBox = new EditBox(this.font, startX, topY + 25, mainPanelWidth / 2 - 5, 20, Component.translatable("gui.betterroulette.config.cost"));
        this.costBox.setValue(String.valueOf(this.nbt.getInt("Cost")));
        this.costBox.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.addRenderableWidget(this.costBox);

        this.useVaultButton = CycleButton.onOffBuilder(this.nbt.getBoolean("UseVault")).create(startX + mainPanelWidth / 2 + 5, topY + 25, mainPanelWidth / 2 - 5, 20, Component.translatable("gui.betterroulette.config.use_vault"));
        this.addRenderableWidget(this.useVaultButton);

        int listY = topY + 55;
        int listHeight = this.height - listY - 40;
        this.entryList = new RouletteEntryList(this, this.minecraft, mainPanelWidth, listHeight, listY, 24);
        this.entryList.setLeftPos(startX);
        this.addRenderableWidget(this.entryList);

        this.addEntryButton = Button.builder(Component.literal("+"), b -> {
            this.entries.add(new RouletteEntry("New Entry", 0xAAAAAA, new ArrayList<>(), false));
            this.entryList.updateEntries(this.entries);
        }).bounds(startX, listY + listHeight + 5, 20, 20).build();
        this.addRenderableWidget(this.addEntryButton);

        this.removeEntryButton = Button.builder(Component.literal("-"), b -> {
            if (this.selectedEntry != null) {
                this.entries.remove(this.selectedEntry.rouletteEntry);
                setSelectedEntry(null);
                this.entryList.updateEntries(this.entries);
            }
        }).bounds(startX + 25, listY + listHeight + 5, 20, 20).build();
        this.addRenderableWidget(this.removeEntryButton);

        int rightX = startX + mainPanelWidth + panelGap;

        this.entryNameBox = new EditBox(this.font, rightX, topY, detailPanelWidth, 20, Component.literal("Entry Name"));
        this.entryNameBox.setResponder(s -> { if(this.selectedEntry != null) this.selectedEntry.rouletteEntry.setName(s); });
        this.detailPanelWidgets.add(this.entryNameBox);

        this.entryColorBox = new EditBox(this.font, rightX, topY + 25, detailPanelWidth - 25, 20, Component.literal("Color Code"));
        this.entryColorBox.setFilter(s -> s.matches("[0-9a-fA-F]*") && s.length() <= 6);
        this.entryColorBox.setResponder(s -> {
            if (this.selectedEntry != null) {
                try {
                    this.selectedEntry.rouletteEntry.setColor(Integer.parseInt(s, 16));
                } catch (NumberFormatException e) { }
            }
        });
        this.detailPanelWidgets.add(this.entryColorBox);

        this.isJackpotButton = CycleButton.onOffBuilder(false)
                .create(rightX, topY + 50, detailPanelWidth, 20, Component.literal("Is Jackpot"), (btn, val) -> {
                    if (this.selectedEntry != null) this.selectedEntry.rouletteEntry.setJackpot(val);
                });
        this.detailPanelWidgets.add(this.isJackpotButton);

        int cmdListY = topY + 80;
        int cmdListHeight = listHeight - 80;
        this.commandList = new CommandEntryList(this, Lists.newArrayList(), this.minecraft, detailPanelWidth, cmdListHeight, cmdListY, 16);
        this.commandList.setLeftPos(rightX);
        this.addRenderableWidget(this.commandList);

        this.commandBox = new EditBox(this.font, rightX, cmdListY + cmdListHeight + 5, detailPanelWidth - 45, 20, Component.literal("Command"));
        this.detailPanelWidgets.add(this.commandBox);

        this.addCommandButton = Button.builder(Component.literal("+"), b -> {
            if (this.selectedEntry != null && !this.commandBox.getValue().isBlank()) {
                this.selectedEntry.rouletteEntry.getCommands().add(this.commandBox.getValue());
                this.commandList.updateEntries();
                this.commandBox.setValue("");
            }
        }).bounds(rightX + detailPanelWidth - 42, cmdListY + cmdListHeight + 5, 20, 20).build();
        this.detailPanelWidgets.add(this.addCommandButton);

        this.removeCommandButton = Button.builder(Component.literal("-"), b -> {
            if (this.selectedEntry != null && this.selectedCommand != null) {
                this.selectedEntry.rouletteEntry.getCommands().remove(this.selectedCommand);
                this.selectedCommand = null;
                this.commandList.updateEntries();
            }
        }).bounds(rightX + detailPanelWidth - 20, cmdListY + cmdListHeight + 5, 20, 20).build();
        this.detailPanelWidgets.add(this.removeCommandButton);

        this.detailPanelWidgets.forEach(this::addRenderableWidget);

        Button saveButton = Button.builder(Component.translatable("gui.betterroulette.config.save"), b -> this.onClose())
                .bounds(this.width / 2 - 75, this.height - 25, 150, 20).build();
        this.addRenderableWidget(saveButton);

        this.updateDetailPanel(false);
    }

    private void updateDetailPanel(boolean visible) {
        this.detailPanelWidgets.forEach(w -> w.visible = visible);
        this.commandList.setVisible(visible);
        if (visible && this.selectedEntry != null) {
            RouletteEntry entry = this.selectedEntry.rouletteEntry;
            this.entryNameBox.setValue(entry.getName());
            this.entryColorBox.setValue(String.format("%06X", entry.getColor()));
            this.isJackpotButton.setValue(entry.isJackpot());
            this.commandList.commands.clear();
            this.commandList.commands.addAll(entry.getCommands());
            this.commandList.updateEntries();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 背景の土を描画しない (renderDirtBackgroundを削除)
        // 画面全体を半透明の黒で塗りつぶす
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        if (this.selectedEntry != null && this.entryColorBox.isVisible()) {
            int rightX = this.entryColorBox.getX();
            int topY = this.entryColorBox.getY();
            guiGraphics.fill(rightX + this.entryColorBox.getWidth() + 3, topY, rightX + this.entryColorBox.getWidth() + 23, topY + 20, 0xFF000000 | this.selectedEntry.rouletteEntry.getColor());
        }
    }

    @Override
    public void onClose() {
        this.nbt.putString("Name", Component.Serializer.toJson(Component.literal(this.nameBox.getValue())));
        try {
            this.nbt.putInt("Cost", Integer.parseInt(this.costBox.getValue()));
        } catch (NumberFormatException e) {
            this.nbt.putInt("Cost", 0);
        }
        this.nbt.putBoolean("UseVault", this.useVaultButton.getValue());
        saveEntriesToNbt();
        ModPackets.sendToServer(new CPacketUpdateRoulette(this.entityId, this.nbt));
        super.onClose();
    }

    public void setSelectedEntry(RouletteEntryList.Entry entry) {
        this.selectedEntry = entry;
        this.entryList.setSelected(entry);
        this.selectedCommand = null;
        updateDetailPanel(entry != null);
    }

    public void setSelectedCommand(String command) {
        this.selectedCommand = command;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}