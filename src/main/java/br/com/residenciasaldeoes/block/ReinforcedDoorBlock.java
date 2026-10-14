package br.com.residenciasaldeoes.block;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

/** Iron-door durability and redstone behavior with vanilla wooden-door interaction. */
public final class ReinforcedDoorBlock extends DoorBlock {
    public ReinforcedDoorBlock() {
        super(BlockBehaviour.Properties.of(Material.WOOD)
                .strength(5.0F, 5.0F)
                .sound(SoundType.WOOD)
                .noOcclusion());
    }

    // Keep DoorBlock's vanilla placement, hinge, open/closed collision and selection shapes.

}
