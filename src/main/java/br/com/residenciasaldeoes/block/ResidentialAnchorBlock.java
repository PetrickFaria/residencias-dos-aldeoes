package br.com.residenciasaldeoes.block;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.data.ResidenceData;
import br.com.residenciasaldeoes.menu.AnchorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class ResidentialAnchorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public ResidentialAnchorBlock(){super(BlockBehaviour.Properties.of(Material.WOOD).strength(1.0F,2.0F).sound(SoundType.WOOD));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state){return new ResidentialAnchorBlockEntity(pos,state);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit){
        if(!level.isClientSide && player instanceof ServerPlayer sp){
            MenuProvider provider=new MenuProvider(){public Component getDisplayName(){return Component.translatable("gui.residenciasaldeoes.anchor_title");}public AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inv,Player p){return new AnchorMenu(id,inv,pos);}};
            NetworkHooks.openScreen(sp,provider,pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock()) && level instanceof ServerLevel sl) ResidenceData.get(sl).unlinkAll(sl,sl.dimension(),pos);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public boolean isSignalSource(BlockState state){return false;}
}
