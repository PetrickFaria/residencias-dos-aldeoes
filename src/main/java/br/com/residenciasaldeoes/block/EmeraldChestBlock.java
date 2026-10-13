package br.com.residenciasaldeoes.block;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Single-tile animated chest with the vanilla player-facing chest menu. */
public final class EmeraldChestBlock extends ChestBlock {
    public EmeraldChestBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.CHEST).strength(2.5F), () ->
                (BlockEntityType<? extends net.minecraft.world.level.block.entity.ChestBlockEntity>)
                        (BlockEntityType<?>) ResidenciasDosAldeoes.EMERALD_CHEST_BE.get());
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EmeraldChestBlockEntity(pos, state);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(TYPE, ChestType.SINGLE);
    }

    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        return updated.hasProperty(TYPE) ? updated.setValue(TYPE, ChestType.SINGLE) : updated;
    }
}
