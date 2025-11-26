package shake1227.betterroulette.common.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.ArrayList;
import java.util.List;

public class RouletteEntry {
    private String name;
    private String desc;
    private int color;
    private List<String> commands;
    private boolean isJackpot;
    private int weight;

    public RouletteEntry(String name, String desc, int color, List<String> commands, boolean isJackpot, int weight) {
        this.name = name;
        this.desc = desc;
        this.color = color;
        this.commands = commands;
        this.isJackpot = isJackpot;
        this.weight = weight;
    }

    // 互換用
    public RouletteEntry(String name, int color, List<String> commands, boolean isJackpot, int weight) {
        this(name, "", color, commands, isJackpot, weight);
    }

    public static RouletteEntry fromNBT(CompoundTag nbt) {
        List<String> commands = new ArrayList<>();
        ListTag commandsTag = nbt.getList("Commands", 8);
        for (int i = 0; i < commandsTag.size(); i++) {
            commands.add(commandsTag.getString(i));
        }

        int w = nbt.contains("Weight") ? nbt.getInt("Weight") : 10;
        String d = nbt.contains("Desc") ? nbt.getString("Desc") : "";

        return new RouletteEntry(
                nbt.getString("Name"),
                d,
                nbt.getInt("Color"),
                commands,
                nbt.getBoolean("IsJackpot"),
                w
        );
    }

    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Name", this.name);
        nbt.putString("Desc", this.desc);
        nbt.putInt("Color", this.color);
        nbt.putInt("Weight", this.weight);

        ListTag commandsTag = new ListTag();
        for (String command : this.commands) {
            commandsTag.add(StringTag.valueOf(command));
        }
        nbt.put("Commands", commandsTag);
        nbt.putBoolean("IsJackpot", this.isJackpot);
        return nbt;
    }

    public String getName() { return name; }
    public String getDesc() { return desc; }
    public int getColor() { return color; }
    public List<String> getCommands() { return commands; }
    public boolean isJackpot() { return isJackpot; }
    public int getWeight() { return weight; }

    public void setName(String name) { this.name = name; }
    public void setDesc(String desc) { this.desc = desc; }
    public void setColor(int color) { this.color = color; }
    public void setCommands(List<String> commands) { this.commands = commands; }
    public void setJackpot(boolean jackpot) { isJackpot = jackpot; }
    public void setWeight(int weight) { this.weight = weight; }
}