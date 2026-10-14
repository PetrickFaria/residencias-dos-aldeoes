package br.com.residenciasaldeoes;

import br.com.residenciasaldeoes.block.ResidentialAnchorBlock;
import br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity;
import br.com.residenciasaldeoes.block.ReinforcedDoorBlock;
import br.com.residenciasaldeoes.block.EmeraldChestBlock;
import br.com.residenciasaldeoes.block.EmeraldChestBodyBlock;
import br.com.residenciasaldeoes.block.EmeraldChestBlockEntity;
import br.com.residenciasaldeoes.menu.AnchorMenu;
import br.com.residenciasaldeoes.network.ModNetwork;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
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
    public static final RegistryObject<ReinforcedDoorBlock> REINFORCED_DOOR = BLOCKS.register("reinforced_door", ReinforcedDoorBlock::new);
    public static final RegistryObject<EmeraldChestBlock> EMERALD_CHEST = BLOCKS.register("emerald_chest", EmeraldChestBlock::new);
    public static final RegistryObject<EmeraldChestBodyBlock> EMERALD_CHEST_BODY = BLOCKS.register("emerald_chest_body", EmeraldChestBodyBlock::new);
    public static final RegistryObject<Item> ANCHOR_ITEM = ITEMS.register("ancora_residencial", () -> new BlockItem(ANCHOR.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<Item> REINFORCED_DOOR_ITEM = ITEMS.register("reinforced_door", () -> new BlockItem(REINFORCED_DOOR.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<Item> EMERALD_CHEST_ITEM = ITEMS.register("emerald_chest", () -> new BlockItem(EMERALD_CHEST.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<Item> EMERALD_FRAGMENT = ITEMS.register("emerald_fragment", () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MATERIALS).stacksTo(64)));
    public static final RegistryObject<BlockEntityType<ResidentialAnchorBlockEntity>> ANCHOR_BE = BLOCK_ENTITIES.register("ancora_residencial", () -> BlockEntityType.Builder.of(ResidentialAnchorBlockEntity::new, ANCHOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<EmeraldChestBlockEntity>> EMERALD_CHEST_BE = BLOCK_ENTITIES.register("emerald_chest", () -> BlockEntityType.Builder.of(EmeraldChestBlockEntity::new, EMERALD_CHEST.get()).build(null));
    public static final RegistryObject<MenuType<AnchorMenu>> ANCHOR_MENU = MENUS.register("ancora_residencial", () -> IForgeMenuType.create(AnchorMenu::new));

    public ResidenciasDosAldeoes() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus); ITEMS.register(bus); BLOCK_ENTITIES.register(bus); MENUS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ResidenceConfig.SPEC, "residenciasaldeoes-common.toml");
        ModNetwork.register();
        MinecraftForge.EVENT_BUS.register(new VillagerResidenceEvents());
    }
}
