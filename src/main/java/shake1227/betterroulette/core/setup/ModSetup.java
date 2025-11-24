package shake1227.betterroulette.core.setup;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.server.command.ConfigCommand;
import shake1227.betterroulette.BetterRoulette;
import shake1227.betterroulette.common.command.SpinCommand;
import shake1227.betterroulette.compats.VaultProxy;

@Mod.EventBusSubscriber(modid = BetterRoulette.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModSetup {
    public static void init(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            shake1227.betterroulette.network.ModPackets.register();
            VaultProxy.setupEconomy();
        });
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        new SpinCommand(event.getDispatcher());
        ConfigCommand.register(event.getDispatcher());
    }
}