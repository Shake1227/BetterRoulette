package shake1227.betterroulette;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import shake1227.betterroulette.core.config.ModConfig;
import shake1227.betterroulette.core.init.EntityInit;
import shake1227.betterroulette.core.init.ItemInit;
import shake1227.betterroulette.core.setup.ModSetup;

@Mod(BetterRoulette.MOD_ID)
public class BetterRoulette {
    public static final String MOD_ID = "betterroulette";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BetterRoulette() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(Type.SERVER, ModConfig.SERVER_SPEC);

        ItemInit.register(modEventBus);
        EntityInit.register(modEventBus);

        modEventBus.addListener(ModSetup::init);

        MinecraftForge.EVENT_BUS.register(this);
    }
}