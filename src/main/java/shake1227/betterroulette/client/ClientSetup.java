package shake1227.betterroulette.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shake1227.betterroulette.BetterRoulette;
import shake1227.betterroulette.client.renderer.RouletteRenderer;
import shake1227.betterroulette.core.init.EntityInit;

@Mod.EventBusSubscriber(modid = BetterRoulette.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityInit.ROULETTE.get(), RouletteRenderer::new);
    }
}