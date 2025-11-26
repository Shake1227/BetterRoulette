package shake1227.betterroulette.core.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import shake1227.betterroulette.BetterRoulette;
import shake1227.betterroulette.common.entity.RouletteEntity;

public class EntityInit {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BetterRoulette.MOD_ID);

    public static final RegistryObject<EntityType<RouletteEntity>> ROULETTE =
            ENTITIES.register("roulette", () -> EntityType.Builder.of(RouletteEntity::new, MobCategory.MISC)
                    .sized(2.0f, 2.5f)
                    .build("roulette"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}