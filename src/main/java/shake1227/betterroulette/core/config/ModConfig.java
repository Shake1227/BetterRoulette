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
        public final ForgeConfigSpec.ConfigValue<String> currencyUnit;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.push("general");
            jackpotCommand = builder
                    .comment("Command to execute when a player hits a jackpot. Use @p or @dp for player name.")
                    .define("jackpotCommand", "/execute at @dp run summon firework_rocket ~ ~1 ~ {Silent:1b,LifeTime:0,Motion:[0.0d,1.0d,0.0d],FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:4,Flicker:0b,Trail:1b,Colors:[I;15790320,6719955,14188952]}],FadeColors:[I;15790320],Flight:1}}}}");

            currencyUnit = builder
                    .comment("Currency unit suffix displayed after the cost value (e.g. 'G', ' Yen').")
                    .define("currencyUnit", "G");

            builder.pop();
        }
    }
}