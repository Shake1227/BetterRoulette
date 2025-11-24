package shake1227.betterroulette.common.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.ArrayList;
import java.util.List;

public class RouletteEntry {
    private String name;
    private int color; // RGB (no alpha)
    private List<String> commands;
    private boolean isJackpot;

    public RouletteEntry(String name, int color, List<String> commands, boolean isJackpot) {
        this.name = name;
        this.color = color;
        this.commands = commands;
        this.isJackpot = isJackpot;
    }

    public static RouletteEntry fromNBT(CompoundTag nbt) {
        List<String> commands = new ArrayList<>();
        ListTag commandsTag = nbt.getList("Commands", 8); // 8 is the NBT type for String
        for (int i = 0; i < commandsTag.size(); i++) {
            commands.add(commandsTag.getString(i));
        }

        return new RouletteEntry(
                nbt.getString("Name"),
                nbt.getInt("Color"),
                commands,
                nbt.getBoolean("IsJackpot")
        );
    }

    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Name", this.name);
        nbt.putInt("Color", this.color);

        ListTag commandsTag = new ListTag();
        for (String command : this.commands) {
            commandsTag.add(StringTag.valueOf(command));
        }
        nbt.put("Commands", commandsTag);
        nbt.putBoolean("IsJackpot", this.isJackpot);
        return nbt;
    }

    public String getName() { return name; }
    public int getColor() { return color; }
    public List<String> getCommands() { return commands; }
    public boolean isJackpot() { return isJackpot; }

    public void setName(String name) { this.name = name; }
    public void setColor(int color) { this.color = color; }
    public void setCommands(List<String> commands) { this.commands = commands; }
    public void setJackpot(boolean jackpot) { isJackpot = jackpot; }
}