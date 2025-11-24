package shake1227.betterroulette.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import shake1227.betterroulette.common.entity.RouletteEntity;
import shake1227.betterroulette.core.init.EntityInit;

public class RouletteItem extends Item {
    public RouletteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (!level.isClientSide && player != null) {
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());

            RouletteEntity roulette = EntityInit.ROULETTE.get().create(level);
            if (roulette != null) {
                roulette.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                roulette.setYRot(player.getYRot());
                roulette.setOwner(player);
                level.addFreshEntity(roulette);

                if (!player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}