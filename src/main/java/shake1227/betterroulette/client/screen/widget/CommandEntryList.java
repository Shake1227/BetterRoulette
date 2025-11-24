package shake1227.betterroulette.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import shake1227.betterroulette.client.screen.RouletteConfigScreen;

import java.util.List;

public class CommandEntryList extends ObjectSelectionList<CommandEntryList.Entry> {
    private final RouletteConfigScreen parent;
    public final List<String> commands;
    private boolean visible = true;

    public CommandEntryList(RouletteConfigScreen parent, List<String> commands, Minecraft mc, int width, int height, int y, int itemHeight) {
        super(mc, width, parent.height, y, y + height, itemHeight);
        this.parent = parent;
        this.commands = commands;
        this.updateEntries();
        this.centerListVertically = false;
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
    }

    public void updateEntries() {
        this.clearEntries();
        this.commands.forEach(cmd -> this.addEntry(new Entry(cmd)));
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.visible) {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public int getScrollbarPosition() {
        return this.x0 + this.width;
    }

    @Override
    public int getRowWidth() {
        return this.width;
    }

    public class Entry extends ObjectSelectionList.Entry<CommandEntryList.Entry> {
        private final String command;

        public Entry(String command) {
            this.command = command;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovering, float partialTicks) {
            if (isHovering || CommandEntryList.this.getSelected() == this) {
                guiGraphics.fill(left, top, left + width, top + height, 0x44FFFFFF);
            }
            guiGraphics.drawString(minecraft.font, this.command, left + 5, top + (height - minecraft.font.lineHeight) / 2, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                CommandEntryList.this.setSelected(this);
                parent.setSelectedCommand(this.command);
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.command);
        }
    }
}