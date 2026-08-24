package town.lampas.modernplayerladder.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import town.lampas.modernplayerladder.ladder.PlayerLadderHandler;

import java.util.function.Predicate;

/**
 * ProjectileUtil mixin to filter out recursive passengers from entity hit-testing.
 * Enables carriers to interact, attack, and shoot through entities riding them.
 * Fabric API does not provide a hook for vanilla ProjectileUtil entity raycasting.
 */
@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilMixin {

    @ModifyVariable(
        method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
        at = @At("HEAD"),
        argsOnly = true
    )
    private static Predicate<Entity> modernPlayerLadder$filterPassengers(Predicate<Entity> predicate, Entity shooter) {
        return PlayerLadderHandler.filterHitResultPredicate(predicate, shooter);
    }
}
