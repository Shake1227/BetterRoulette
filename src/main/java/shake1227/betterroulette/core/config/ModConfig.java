package shake1227.betterroulette.core.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import shake1227.betterroulette.BetterRoulette;

@Mod.EventBusSubscriber(modid = BetterRoulette.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        SERVER = new Server(builder);
        SERVER_SPEC = builder.build();
    }

    public static class Server {
        public final ForgeConfigSpec.ConfigValue<String> jackpotCommand;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.push("general");
            jackpotCommand = builder
                    .comment("Command to execute when a player hits a jackpot. Use @p for player name.")
                    .define("jackpotCommand", "say @p hit the jackpot!");
            builder.pop();
        }
    }
}