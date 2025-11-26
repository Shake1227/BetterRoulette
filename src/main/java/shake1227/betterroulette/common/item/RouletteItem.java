package shake1227.betterroulette.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
            Direction face = context.getClickedFace();
            BlockPos pos = context.getClickedPos().relative(face);

            RouletteEntity roulette = EntityInit.ROULETTE.get().create(level);
            if (roulette != null) {
                double x = pos.getX() + 0.5 - (face.getStepX() * 0.5);
                double y = pos.getY();
                double z = pos.getZ() + 0.5 - (face.getStepZ() * 0.5);

                if (face.getAxis() == Direction.Axis.Y) {
                    y = pos.getY() + 0.5 - (face.getStepY() * 0.5);
                }

                roulette.setPos(x, y, z);
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