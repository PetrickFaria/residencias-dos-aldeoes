package br.com.residenciasaldeoes;

import br.com.residenciasaldeoes.block.ResidentialAnchorBlock;
import br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity;
import br.com.residenciasaldeoes.menu.AnchorMenu;
import br.com.residenciasaldeoes.network.ModNetwork;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.extensions.IForgeMenuType;

@Mod(ResidenciasDosAldeoes.MODID)
public class ResidenciasDosAldeoes {
    public static final String MODID = "residenciasaldeoes";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final RegistryObject<ResidentialAnchorBlock> ANCHOR = BLOCKS.register("ancora_residencial", ResidentialAnchorBlock::new);
    public static final RegistryObject<Item> ANCHOR_ITEM = ITEMS.register("ancora_residencial", () -> new BlockItem(ANCHOR.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<BlockEntityType<ResidentialAnchorBlockEntity>> ANCHOR_BE = BLOCK_ENTITIES.register("ancora_residencial", () -> BlockEntityType.Builder.of(ResidentialAnchorBlockEntity::new, ANCHOR.get()).build(null));
    public static final RegistryObject<MenuType<AnchorMenu>> ANCHOR_MENU = MENUS.register("ancora_residencial", () -> IForgeMenuType.create(AnchorMenu::new));

    public ResidenciasDosAldeoes() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus); ITEMS.register(bus); BLOCK_ENTITIES.register(bus); MENUS.register(bus);
        ModNetwork.register();
        MinecraftForge.EVENT_BUS.register(new VillagerResidenceEvents());
    }
}
