package shake1227.betterroulette.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import shake1227.betterroulette.client.screen.RouletteConfigScreen;

import java.util.Collections;
import java.util.List;

public class CommandEntryList extends ContainerObjectSelectionList<CommandEntryList.Entry> {
    private final RouletteConfigScreen parent;
    public final List<String> commands;

    public CommandEntryList(RouletteConfigScreen parent, List<String> commands, Minecraft mc, int width, int height, int y, int itemHeight) {
        super(mc, width, height, y, itemHeight);
        this.parent = parent;
        this.commands = commands;
        this.updateEntries();
        this.centerListVertically = false;
    }

    public void updateEntries() {
        this.clearEntries();
        this.commands.forEach(cmd -> this.addEntry(new Entry(cmd)));
    }

    @Override
    public int getScrollbarPosition() {
        return this.getX0() + this.width;
    }

    @Override
    public int getRowWidth() {
        return this.width;
    }

    public class Entry extends ContainerObjectSelectionList.Entry<CommandEntryList.Entry> {
        private final String command;

        public Entry(String command) {
            this.command = command;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovering, float partialTicks) {
            if (isHovering || this.isMouseOver(mouseX, mouseY)) {
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

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return Collections.singletonList(new net.minecraft.client.gui.narration.NarratableEntry() {
                public net.minecraft.client.gui.narration.NarrationPriority narrationPriority() {
                    return net.minecraft.client.gui.narration.NarrationPriority.FOCUSED;
                }

                public void updateNarration(NarrationElementOutput p_169152_) {
                    p_169152_.add(net.minecraft.client.gui.narration.NarrationPart.TITLE, getNarration());
                }
            });
        }
    }
}