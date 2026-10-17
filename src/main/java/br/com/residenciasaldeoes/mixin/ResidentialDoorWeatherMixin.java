package br.com.residenciasaldeoes.mixin;

import br.com.residenciasaldeoes.VillagerResidenceEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents only a registered resident from reopening a controlled door during its closure period. */
@Mixin(DoorBlock.class)
public abstract class ResidentialDoorWeatherMixin {
    @Inject(method = "setOpen", at = @At("HEAD"), cancellable = true)
    private void residenciasaldeoes$keepResidentialDoorClosed(Entity entity, Level level, BlockState state,
                                                              BlockPos pos, boolean open, CallbackInfo callback) {
        if (open && entity instanceof Villager villager && level instanceof ServerLevel serverLevel
                && VillagerResidenceEvents.shouldBlockResidentDoorOpening(serverLevel, villager, pos)) {
            callback.cancel();
        }
    }
}
