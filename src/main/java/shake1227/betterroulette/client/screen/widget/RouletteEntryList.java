package shake1227.betterroulette.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import shake1227.betterroulette.client.screen.RouletteConfigScreen;
import shake1227.betterroulette.common.data.RouletteEntry;

import java.util.Collections;
import java.util.List;

public class RouletteEntryList extends ContainerObjectSelectionList<RouletteEntryList.Entry> {
    private final RouletteConfigScreen parent;

    public RouletteEntryList(RouletteConfigScreen parent, Minecraft mc, int width, int height, int y, int itemHeight) {
        super(mc, width, height, y, itemHeight);
        this.parent = parent;
        this.centerListVertically = false;
    }

    public void updateEntries(List<RouletteEntry> entries) {
        this.clearEntries();
        entries.forEach(entry -> this.addEntry(new Entry(entry)));
    }

    @Override
    public int getScrollbarPosition() {
        return this.getX0() + this.width;
    }

    @Override
    public int getRowWidth() {
        return this.width;
    }

    public class Entry extends ContainerObjectSelectionList.Entry<RouletteEntryList.Entry> {
        public final RouletteEntry rouletteEntry;

        public Entry(RouletteEntry rouletteEntry) {
            this.rouletteEntry = rouletteEntry;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovering, float partialTicks) {
            if (isHovering || this.isMouseOver(mouseX, mouseY)) {
                guiGraphics.fill(left, top, left + width, top + height, 0x44FFFFFF);
            } else {
                guiGraphics.fill(left, top, left + width, top + height, 0x22FFFFFF);
            }

            guiGraphics.fill(left + 2, top + 2, left + 18, top + height - 2, 0xFF000000 | this.rouletteEntry.getColor());
            guiGraphics.drawString(minecraft.font, this.rouletteEntry.getName(), left + 24, top + (height - minecraft.font.lineHeight) / 2, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                RouletteEntryList.this.setSelected(this);
                parent.setSelectedEntry(this);
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.rouletteEntry.getName());
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