package shake1227.betterroulette.client.screen;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import shake1227.betterroulette.client.renderer.util.RenderUtil;
import shake1227.betterroulette.client.screen.widget.CommandEntryList;
import shake1227.betterroulette.client.screen.widget.RouletteEntryList;
import shake1227.betterroulette.common.data.RouletteEntry;
import shake1227.betterroulette.network.ModPackets;
import shake1227.betterroulette.network.packet.CPacketDeleteRoulette;
import shake1227.betterroulette.network.packet.CPacketUpdateRoulette;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class RouletteConfigScreen extends Screen {
    private final int entityId;
    private final CompoundTag nbt;
    private final List<RouletteEntry> entries = new ArrayList<>();

    private EditBox nameBox;
    private EditBox costBox;
    private CycleButton<Boolean> useVaultButton;
    private CycleButton<Boolean> useItemCostButton;
    private Button setItemCostButton;
    private ItemStack pendingCostItem = ItemStack.EMPTY;

    // 新設定
    private EditBox coastingBox;
    private CycleButton<Boolean> stopModeButton; // true=Auto, false=Manual
    private EditBox autoTimeMinBox;
    private EditBox autoTimeMaxBox;
    private CycleButton<Boolean> mixModeButton; // Mixモードボタン
    private EditBox mixSlotCountBox; // 追加: マス目数設定

    private RouletteEntryList entryList;
    private Button addEntryButton;
    private Button removeEntryButton;

    private EditBox entryNameBox;
    private EditBox entryDescBox; // 追加
    private EditBox entryWeightBox;
    private EditBox entryColorBox;
    private Button colorPreviewButton;
    private ColorPickerWidget colorPicker;

    private EditBox commandBox;
    private CommandEntryList commandList;
    private Button addCommandButton;
    private Button removeCommandButton;
    private CycleButton<Boolean> isJackpotButton;

    private Button deleteRouletteButton;

    private final List<AbstractWidget> detailPanelWidgets = new ArrayList<>();

    private RouletteEntryList.Entry selectedEntry;
    private String selectedCommand;

    private boolean showColorPicker = false;

    public RouletteConfigScreen(int entityId, CompoundTag nbt) {
        super(Component.translatable("gui.betterroulette.config.title"));
        this.entityId = entityId;
        this.nbt = nbt;
        loadEntriesFromNbt();

        if (nbt.contains("CostItem")) {
            this.pendingCostItem = ItemStack.of(nbt.getCompound("CostItem"));
        }
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

        int mainPanelWidth = 160;
        int detailPanelWidth = 160;
        int panelGap = 20;
        int totalWidth = mainPanelWidth + panelGap + detailPanelWidth;
        int startX = (this.width - totalWidth) / 2;
        int topY = 30;

        // --- 左側: 全体設定 ---
        int leftY = topY;

        this.nameBox = new EditBox(this.font, startX, leftY, mainPanelWidth, 20, Component.translatable("gui.betterroulette.config.name"));
        this.nameBox.setValue(Component.Serializer.fromJson(this.nbt.getString("Name")).getString());
        this.nameBox.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.name")));
        this.addRenderableWidget(this.nameBox);
        leftY += 25;

        this.costBox = new EditBox(this.font, startX, leftY, mainPanelWidth / 2 - 5, 20, Component.translatable("gui.betterroulette.config.cost"));
        this.costBox.setValue(String.valueOf(this.nbt.getInt("Cost")));
        this.costBox.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.costBox.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.cost")));
        this.addRenderableWidget(this.costBox);

        this.useVaultButton = CycleButton.onOffBuilder(this.nbt.getBoolean("UseVault"))
                .create(startX + mainPanelWidth / 2 + 5, leftY, mainPanelWidth / 2 - 5, 20, Component.translatable("gui.betterroulette.config.use_vault"));
        this.useVaultButton.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.use_vault")));
        this.addRenderableWidget(this.useVaultButton);
        leftY += 25;

        this.useItemCostButton = CycleButton.onOffBuilder(this.nbt.getBoolean("UseItemCost"))
                .create(startX, leftY, mainPanelWidth / 2 - 5, 20, Component.translatable("gui.betterroulette.config.use_item"));
        this.useItemCostButton.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.use_item")));
        this.addRenderableWidget(this.useItemCostButton);

        this.setItemCostButton = Button.builder(Component.translatable("gui.betterroulette.config.set_item"), b -> {
                    if (this.minecraft.player != null) {
                        this.pendingCostItem = this.minecraft.player.getMainHandItem().copy();
                    }
                }).bounds(startX + mainPanelWidth / 2 + 5, leftY, mainPanelWidth / 2 - 5, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.set_item")))
                .build();
        this.addRenderableWidget(this.setItemCostButton);
        leftY += 25;

        this.coastingBox = new EditBox(this.font, startX, leftY, mainPanelWidth, 20, Component.literal("Coasting"));
        float coasting = this.nbt.contains("Coasting") ? this.nbt.getFloat("Coasting") : 1.0f;
        this.coastingBox.setValue(String.valueOf(coasting));
        this.coastingBox.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.coasting")));
        this.addRenderableWidget(this.coastingBox);
        leftY += 25;

        boolean isAuto = !this.nbt.contains("IsAutoStop") || this.nbt.getBoolean("IsAutoStop");
        this.stopModeButton = CycleButton.booleanBuilder(Component.translatable("gui.betterroulette.config.stop_mode.auto"), Component.translatable("gui.betterroulette.config.stop_mode.manual"))
                .displayOnlyValue()
                .withInitialValue(isAuto)
                .create(startX, leftY, mainPanelWidth, 20, Component.translatable("gui.betterroulette.config.stop_mode"), (btn, val) -> {
                    this.autoTimeMinBox.visible = val;
                    this.autoTimeMaxBox.visible = val;
                });
        this.stopModeButton.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.stop_mode")));
        this.addRenderableWidget(this.stopModeButton);
        leftY += 25;

        int min = this.nbt.contains("AutoMin") ? this.nbt.getInt("AutoMin") : 5;
        int max = this.nbt.contains("AutoMax") ? this.nbt.getInt("AutoMax") : 10;

        this.autoTimeMinBox = new EditBox(this.font, startX, leftY, mainPanelWidth/2 - 5, 20, Component.literal("Min Sec"));
        this.autoTimeMinBox.setValue(String.valueOf(min));
        this.autoTimeMinBox.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.autoTimeMinBox.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.config.auto_time_min")));
        this.autoTimeMinBox.visible = isAuto;
        this.addRenderableWidget(this.autoTimeMinBox);

        this.autoTimeMaxBox = new EditBox(this.font, startX + mainPanelWidth/2 + 5, leftY, mainPanelWidth/2 - 5, 20, Component.literal("Max Sec"));
        this.autoTimeMaxBox.setValue(String.valueOf(max));
        this.autoTimeMaxBox.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.autoTimeMaxBox.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.config.auto_time_max")));
        this.autoTimeMaxBox.visible = isAuto;
        this.addRenderableWidget(this.autoTimeMaxBox);
        leftY += 25;

        // Mix Mode & Slot Count
        boolean isMix = this.nbt.contains("IsMixMode") && this.nbt.getBoolean("IsMixMode");
        this.mixModeButton = CycleButton.booleanBuilder(Component.translatable("gui.betterroulette.config.mix_mode"), Component.translatable("gui.betterroulette.config.normal_mode"))
                .displayOnlyValue()
                .withInitialValue(isMix)
                .create(startX, leftY, mainPanelWidth, 20, Component.literal("Mix Mode"), (btn, val) -> {
                    this.mixSlotCountBox.visible = val;
                });
        this.mixModeButton.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.mix_mode")));
        this.addRenderableWidget(this.mixModeButton);
        leftY += 25;

        // Mix Slot Count Box
        int slotCount = this.nbt.contains("MixSlotCount") ? this.nbt.getInt("MixSlotCount") : 60;
        this.mixSlotCountBox = new EditBox(this.font, startX, leftY, mainPanelWidth, 20, Component.translatable("gui.betterroulette.config.mix_slots"));
        this.mixSlotCountBox.setValue(String.valueOf(slotCount));
        this.mixSlotCountBox.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.mixSlotCountBox.visible = isMix;
        this.mixSlotCountBox.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.mix_slots")));
        this.addRenderableWidget(this.mixSlotCountBox);
        leftY += 30;

        // Entry List
        int listHeight = this.height - leftY - 40;
        this.entryList = new RouletteEntryList(this, this.minecraft, mainPanelWidth, listHeight, leftY, 24);
        this.entryList.setLeftPos(startX);
        this.addRenderableWidget(this.entryList);
        this.entryList.updateEntries(this.entries);

        this.addEntryButton = Button.builder(Component.literal("+"), b -> {
                    this.entries.add(new RouletteEntry("New Entry", 0xAAAAAA, new ArrayList<>(), false, 10));
                    this.entryList.updateEntries(this.entries);
                }).bounds(startX, leftY + listHeight + 5, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.add_entry")))
                .build();
        this.addRenderableWidget(this.addEntryButton);

        this.removeEntryButton = Button.builder(Component.literal("-"), b -> {
                    if (this.selectedEntry != null) {
                        this.entries.remove(this.selectedEntry.rouletteEntry);
                        setSelectedEntry(null);
                        this.entryList.updateEntries(this.entries);
                    }
                }).bounds(startX + 25, leftY + listHeight + 5, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.remove_entry")))
                .build();
        this.addRenderableWidget(this.removeEntryButton);


        // --- 右側: 詳細設定 ---
        int rightX = startX + mainPanelWidth + panelGap;
        int currentDetailY = topY;

        // 1. 名前
        this.entryNameBox = new EditBox(this.font, rightX, currentDetailY, detailPanelWidth, 20, Component.translatable("gui.betterroulette.config.entry_name"));
        this.entryNameBox.setResponder(s -> {
            if(this.selectedEntry != null) this.selectedEntry.rouletteEntry.setName(s);
        });
        this.entryNameBox.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.entry_name")));
        this.detailPanelWidgets.add(this.entryNameBox);
        currentDetailY += 24;

        // 2. 詳細 (Description)
        this.entryDescBox = new EditBox(this.font, rightX, currentDetailY, detailPanelWidth, 20, Component.translatable("gui.betterroulette.config.entry_desc"));
        this.entryDescBox.setResponder(s -> {
            if(this.selectedEntry != null) this.selectedEntry.rouletteEntry.setDesc(s);
        });
        this.entryDescBox.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.entry_desc")));
        this.detailPanelWidgets.add(this.entryDescBox);
        currentDetailY += 24;

        // 3. Weight & Color Hex
        this.entryWeightBox = new EditBox(this.font, rightX, currentDetailY, detailPanelWidth / 2 - 2, 20, Component.translatable("gui.betterroulette.config.weight"));
        this.entryWeightBox.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.entryWeightBox.setResponder(s -> {
            if (this.selectedEntry != null && !s.isEmpty()) {
                try {
                    this.selectedEntry.rouletteEntry.setWeight(Math.max(1, Integer.parseInt(s)));
                } catch(NumberFormatException ignored) {}
            }
        });
        this.entryWeightBox.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.weight")));
        this.detailPanelWidgets.add(this.entryWeightBox);

        this.entryColorBox = new EditBox(this.font, rightX + detailPanelWidth / 2 + 2, currentDetailY, detailPanelWidth / 2 - 2, 20, Component.translatable("gui.betterroulette.config.hex"));
        this.entryColorBox.setFilter(s -> s.matches("[0-9a-fA-F]*") && s.length() <= 6);
        this.entryColorBox.setResponder(s -> {
            if (this.selectedEntry != null) {
                try {
                    int c = Integer.parseInt(s, 16);
                    this.selectedEntry.rouletteEntry.setColor(c);
                    if (this.colorPicker != null) this.colorPicker.setColorFromHex(c);
                } catch (NumberFormatException e) { }
            }
        });
        this.entryColorBox.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.hex")));
        this.detailPanelWidgets.add(this.entryColorBox);
        currentDetailY += 24;

        // 4. 色選択ボタン
        this.colorPreviewButton = Button.builder(Component.empty(), b -> {
                    toggleColorPicker();
                }).bounds(rightX, currentDetailY, detailPanelWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.color_picker")))
                .build();
        this.detailPanelWidgets.add(this.colorPreviewButton);
        currentDetailY += 24;

        // 5. Jackpotボタン
        this.isJackpotButton = CycleButton.onOffBuilder(false)
                .create(rightX, currentDetailY, detailPanelWidth, 20, Component.translatable("gui.betterroulette.config.is_jackpot"), (btn, val) -> {
                    if (this.selectedEntry != null) this.selectedEntry.rouletteEntry.setJackpot(val);
                });
        this.isJackpotButton.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.is_jackpot")));
        this.detailPanelWidgets.add(this.isJackpotButton);
        currentDetailY += 24;

        // 6. コマンドリスト
        int bottomPadding = 45;
        int commandInputHeight = 24;
        int cmdListHeight = this.height - currentDetailY - bottomPadding - commandInputHeight;

        this.commandList = new CommandEntryList(this, Lists.newArrayList(), this.minecraft, detailPanelWidth, cmdListHeight, currentDetailY, 16);
        this.commandList.setLeftPos(rightX);
        this.addRenderableWidget(this.commandList);

        // 7. コマンド入力欄
        int inputY = currentDetailY + cmdListHeight + 2;
        this.commandBox = new EditBox(this.font, rightX, inputY, detailPanelWidth - 45, 20, Component.translatable("gui.betterroulette.config.command"));
        this.commandBox.setTooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.command")));
        this.detailPanelWidgets.add(this.commandBox);

        this.addCommandButton = Button.builder(Component.literal("+"), b -> {
            if (this.selectedEntry != null && !this.commandBox.getValue().isBlank()) {
                String cmd = this.commandBox.getValue();
                this.selectedEntry.rouletteEntry.getCommands().add(cmd);
                syncCommandList();
                this.commandBox.setValue("");
            }
        }).bounds(rightX + detailPanelWidth - 42, inputY, 20, 20).build();
        this.detailPanelWidgets.add(this.addCommandButton);

        this.removeCommandButton = Button.builder(Component.literal("-"), b -> {
            if (this.selectedEntry != null && this.selectedCommand != null) {
                this.selectedEntry.rouletteEntry.getCommands().remove(this.selectedCommand);
                syncCommandList();
                this.selectedCommand = null;
            }
        }).bounds(rightX + detailPanelWidth - 20, inputY, 20, 20).build();
        this.detailPanelWidgets.add(this.removeCommandButton);

        this.detailPanelWidgets.forEach(this::addRenderableWidget);

        // 保存ボタン
        int buttonWidth = 120;
        int buttonGap = 10;
        int buttonY = this.height - 25;
        int totalButtonWidth = (buttonWidth * 2) + buttonGap;
        int startButtonX = (this.width - totalButtonWidth) / 2;

        Button saveButton = Button.builder(Component.translatable("gui.betterroulette.config.save"), b -> this.onClose())
                .bounds(startButtonX, buttonY, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.save")))
                .build();
        this.addRenderableWidget(saveButton);

        // 削除ボタン
        this.deleteRouletteButton = Button.builder(Component.translatable("gui.betterroulette.config.delete"), b -> {
                    ModPackets.sendToServer(new CPacketDeleteRoulette(this.entityId));
                    super.onClose();
                }).bounds(startButtonX + buttonWidth + buttonGap, buttonY, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.betterroulette.tooltip.delete")))
                .build();
        this.addRenderableWidget(this.deleteRouletteButton);

        // カラーピッカー (Widgetとして登録)
        this.colorPicker = new ColorPickerWidget(rightX + (detailPanelWidth - 140) / 2, this.colorPreviewButton.getY() + 25, 50);
        this.colorPicker.visible = false;
        this.addWidget(this.colorPicker);

        this.updateDetailPanel(false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.showColorPicker && this.colorPicker.visible) {
            if (this.colorPicker.mouseClicked(mouseX, mouseY, button)) return true;
            if (!this.colorPicker.isMouseOver(mouseX, mouseY) && this.colorPreviewButton != null && !this.colorPreviewButton.isMouseOver(mouseX, mouseY)) {
                this.showColorPicker = false;
                this.colorPicker.visible = false;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.showColorPicker && this.colorPicker.visible) {
            if (this.colorPicker.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.showColorPicker && this.colorPicker.visible) {
            if (this.colorPicker.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.showColorPicker && this.colorPicker.visible && this.colorPicker.isMouseOver(mouseX, mouseY)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void syncCommandList() {
        if (this.selectedEntry != null) {
            this.commandList.commands.clear();
            this.commandList.commands.addAll(this.selectedEntry.rouletteEntry.getCommands());
            this.commandList.updateEntries();
        }
    }

    private void updateDetailPanel(boolean visible) {
        this.detailPanelWidgets.forEach(w -> w.visible = visible);
        this.commandList.setVisible(visible);
        this.showColorPicker = false;
        if (this.colorPicker != null) this.colorPicker.visible = false;

        if (visible && this.selectedEntry != null) {
            RouletteEntry entry = this.selectedEntry.rouletteEntry;
            this.entryNameBox.setValue(entry.getName());
            this.entryDescBox.setValue(entry.getDesc()); // 値セット
            this.entryWeightBox.setValue(String.valueOf(entry.getWeight()));
            this.entryColorBox.setValue(String.format("%06X", entry.getColor()));
            this.isJackpotButton.setValue(entry.isJackpot());
            syncCommandList();
        }
    }

    private void toggleColorPicker() {
        this.showColorPicker = !this.showColorPicker;
        this.colorPicker.visible = this.showColorPicker;
        if (this.showColorPicker && this.selectedEntry != null) {
            this.colorPicker.setColorFromHex(this.selectedEntry.rouletteEntry.getColor());
            this.colorPicker.setX(this.colorPreviewButton.getX() + (this.colorPreviewButton.getWidth() - this.colorPicker.getWidth()) / 2);
            this.colorPicker.setY(this.colorPreviewButton.getY() + 25);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        if (this.setItemCostButton.isHovered() && !this.pendingCostItem.isEmpty()) {
            guiGraphics.renderTooltip(this.font, this.pendingCostItem, mouseX, mouseY);
        }

        if (this.selectedEntry != null && this.entryColorBox.isVisible()) {
            int x = this.colorPreviewButton.getX();
            int y = this.colorPreviewButton.getY();
            int w = this.colorPreviewButton.getWidth();
            int h = this.colorPreviewButton.getHeight();
            int color = 0xFF000000 | this.selectedEntry.rouletteEntry.getColor();
            guiGraphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, color);
            int textColor = (((color >> 16) & 0xFF) * 0.299 + ((color >> 8) & 0xFF) * 0.587 + (color & 0xFF) * 0.114) > 128 ? 0x000000 : 0xFFFFFF;
            guiGraphics.drawCenteredString(this.font, "Select Color", x + w / 2, y + (h - 8) / 2, textColor);
        }

        if (this.colorPicker.visible) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 500);
            this.colorPicker.render(guiGraphics, mouseX, mouseY, partialTicks);
            guiGraphics.pose().popPose();
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
        this.nbt.putBoolean("UseItemCost", this.useItemCostButton.getValue());
        this.nbt.put("CostItem", this.pendingCostItem.save(new CompoundTag()));

        try {
            this.nbt.putFloat("Coasting", Float.parseFloat(this.coastingBox.getValue()));
        } catch(NumberFormatException e) { this.nbt.putFloat("Coasting", 1.0f); }

        this.nbt.putBoolean("IsAutoStop", this.stopModeButton.getValue());

        try {
            this.nbt.putInt("AutoMin", Integer.parseInt(this.autoTimeMinBox.getValue()));
        } catch(NumberFormatException e) { this.nbt.putInt("AutoMin", 5); }

        try {
            this.nbt.putInt("AutoMax", Integer.parseInt(this.autoTimeMaxBox.getValue()));
        } catch(NumberFormatException e) { this.nbt.putInt("AutoMax", 10); }

        this.nbt.putBoolean("IsMixMode", this.mixModeButton.getValue());
        try {
            this.nbt.putInt("MixSlotCount", Integer.parseInt(this.mixSlotCountBox.getValue()));
        } catch(NumberFormatException e) { this.nbt.putInt("MixSlotCount", 60); }

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

    private class ColorPickerWidget extends AbstractWidget {
        private final int radius;
        private final int sliderWidth = 20;
        private final int padding = 5;

        private float hue = 0.0f;
        private float saturation = 0.0f;
        private float brightness = 1.0f;

        private boolean isDraggingWheel = false;
        private boolean isDraggingSlider = false;

        public ColorPickerWidget(int x, int y, int radius) {
            super(x, y, (radius * 2) + 20 + 15, radius * 2 + 10, Component.empty());
            this.radius = radius;
        }

        public void setColorFromHex(int rgb) {
            float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
            this.hue = hsb[0];
            this.saturation = hsb[1];
            this.brightness = hsb[2];
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) return;

            int startX = this.getX();
            int startY = this.getY();
            int wheelCenterX = startX + radius + padding;
            int wheelCenterY = startY + radius + padding;

            guiGraphics.fill(startX, startY, startX + width, startY + height, 0xFF202020);
            guiGraphics.renderOutline(startX, startY, width, height, 0xFFFFFFFF);

            for (int y = -radius; y <= radius; y++) {
                for (int x = -radius; x <= radius; x++) {
                    double dist = Math.sqrt(x * x + y * y);
                    if (dist <= radius) {
                        double angle = Math.atan2(y, x);
                        float h = (float) ((angle / (2 * Math.PI)) + 0.5);
                        float s = (float) (dist / radius);
                        int color = Color.HSBtoRGB(h, s, this.brightness);
                        guiGraphics.fill(wheelCenterX + x, wheelCenterY + y, wheelCenterX + x + 1, wheelCenterY + y + 1, color);
                    }
                }
            }

            double cursorAngle = (this.hue - 0.5) * 2 * Math.PI;
            double cursorDist = this.saturation * radius;
            int cursorX = wheelCenterX + (int) (Math.cos(cursorAngle) * cursorDist);
            int cursorY = wheelCenterY + (int) (Math.sin(cursorAngle) * cursorDist);
            guiGraphics.renderOutline(cursorX - 2, cursorY - 2, 4, 4, 0xFF000000);
            guiGraphics.fill(cursorX - 1, cursorY - 1, cursorX + 2, cursorY + 2, 0xFFFFFFFF);

            int sliderX = startX + (radius * 2) + (padding * 2);
            int sliderY = startY + padding;
            int sliderH = radius * 2;

            int topColor = Color.HSBtoRGB(this.hue, this.saturation, 1.0f);
            int bottomColor = Color.HSBtoRGB(this.hue, this.saturation, 0.0f);
            guiGraphics.fillGradient(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderH, topColor, bottomColor);
            guiGraphics.renderOutline(sliderX, sliderY, sliderWidth, sliderH, 0xFF888888);

            int knobY = sliderY + (int)((1.0f - this.brightness) * sliderH);
            guiGraphics.fill(sliderX - 1, knobY - 1, sliderX + sliderWidth + 1, knobY + 2, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!this.visible) return false;
            if (!this.isMouseOver(mouseX, mouseY)) return false;

            int startX = this.getX();
            int startY = this.getY();
            int wheelCenterX = startX + radius + padding;
            int wheelCenterY = startY + radius + padding;

            double dx = mouseX - wheelCenterX;
            double dy = mouseY - wheelCenterY;
            if (Math.sqrt(dx * dx + dy * dy) <= radius) {
                this.isDraggingWheel = true;
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                updateColorFromMouse(mouseX, mouseY);
                return true;
            }

            int sliderX = startX + (radius * 2) + (padding * 2);
            int sliderY = startY + padding;
            if (mouseX >= sliderX - 5 && mouseX <= sliderX + sliderWidth + 5 &&
                    mouseY >= sliderY && mouseY <= sliderY + (radius * 2)) {
                this.isDraggingSlider = true;
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                updateColorFromMouse(mouseX, mouseY);
                return true;
            }

            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            this.isDraggingWheel = false;
            this.isDraggingSlider = false;
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (this.visible && (this.isDraggingWheel || this.isDraggingSlider)) {
                updateColorFromMouse(mouseX, mouseY);
                return true;
            }
            return false;
        }

        private void updateColorFromMouse(double mouseX, double mouseY) {
            int startX = this.getX();
            int startY = this.getY();

            if (this.isDraggingWheel) {
                int wheelCenterX = startX + radius + padding;
                int wheelCenterY = startY + radius + padding;
                double dx = mouseX - wheelCenterX;
                double dy = mouseY - wheelCenterY;
                double dist = Math.sqrt(dx * dx + dy * dy);

                this.saturation = Mth.clamp((float)(dist / radius), 0.0f, 1.0f);
                this.hue = (float) ((Math.atan2(dy, dx) / (2 * Math.PI)) + 0.5);
            }
            if (this.isDraggingSlider) {
                int sliderY = startY + padding;
                int sliderH = radius * 2;
                this.brightness = Mth.clamp(1.0f - (float)((mouseY - sliderY) / sliderH), 0.0f, 1.0f);
            }

            int rgb = Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 0xFFFFFF;
            if (selectedEntry != null) {
                selectedEntry.rouletteEntry.setColor(rgb);
                entryColorBox.setValue(String.format("%06X", rgb));
            }
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {}
    }
}