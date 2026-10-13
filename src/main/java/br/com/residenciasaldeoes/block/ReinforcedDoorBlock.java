package br.com.residenciasaldeoes.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

/** Iron-door durability and redstone behavior with a hand-operated wooden door interaction. */
public final class ReinforcedDoorBlock extends DoorBlock {
    public ReinforcedDoorBlock() {
        super(BlockBehaviour.Properties.of(Material.WOOD)
                .strength(5.0F, 5.0F)
                .sound(SoundType.WOOD)
                .noOcclusion());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockState lower = level.getBlockState(lowerPos);
        if (!(lower.getBlock() instanceof ReinforcedDoorBlock)) return InteractionResult.PASS;
        BlockState changed = lower.cycle(OPEN);
        boolean opening = changed.getValue(OPEN);
        level.setBlock(lowerPos, changed, 10);
        BlockPos upperPos = lowerPos.above();
        BlockState upper = level.getBlockState(upperPos);
        if (upper.getBlock() instanceof ReinforcedDoorBlock && upper.getValue(HALF) == DoubleBlockHalf.UPPER)
            level.setBlock(upperPos, upper.setValue(OPEN, opening), 10);
        if (!level.isClientSide)
            level.playSound(null, pos, opening ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
