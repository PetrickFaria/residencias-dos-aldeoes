from pathlib import Path
import json, struct
root=Path(".")
menu=(root/"src/main/java/br/com/residenciasaldeoes/menu/AnchorMenu.java").read_text()
be=(root/"src/main/java/br/com/residenciasaldeoes/block/ResidentialAnchorBlockEntity.java").read_text()
events=(root/"src/main/java/br/com/residenciasaldeoes/VillagerResidenceEvents.java").read_text()
screen=(root/"src/main/java/br/com/residenciasaldeoes/client/AnchorScreen.java").read_text()
network=(root/"src/main/java/br/com/residenciasaldeoes/network/ModNetwork.java").read_text()
main=(root/"src/main/java/br/com/residenciasaldeoes/ResidenciasDosAldeoes.java").read_text()
lang=json.loads((root/"src/main/resources/assets/residenciasaldeoes/lang/pt_br.json").read_text())
assert "if(b==Blocks.CAKE)cakes++" in menu and "idx<resources[3]*2" in menu
assert "drawStatus(p,\"Comida\",e.food()" in screen and "boolean food" in network
assert "for(int slot=0;slot<ResidentialAnchorBlockEntity.OUTPUT_SLOTS;slot++)" in menu and "mayPlace(ItemStack stack){return false;}" in menu
assert "implements Container" in be and "ContainerHelper.saveAllItems(tag,outputItems)" in be and "ContainerHelper.loadAllItems(tag,outputItems)" in be
assert "setOutputItem(int slot,ItemStack stack)" in be
assert "RETURN_TIMEOUT_TICKS=300L" in events and "THREAT_RETURN_TIMEOUT_TICKS=150L" in events
assert "MemoryModuleType.NEAREST_HOSTILE" in events and "villager.hasLineOfSight(entity)" in events
assert "if(night||forced||weather)" in events, "Night and rain must use the same home-return path"
assert "LivingConversionEvent.Post" in events and "unlinkConverted(level,villager,event.getOutcome())" in events
register_start=events.index("@SubscribeEvent public void onRegister(PlayerInteractEvent.EntityInteract event)")
register_end=events.index("@SubscribeEvent public void onVillagerConversion",register_start)
register_handler=events[register_start:register_end]
assert register_handler.index("event.setCanceled(true)") < register_handler.index("ResidenceData.get(level).link(level,pos,villager)")
assert "imageWidth=272;imageHeight=232" in screen, "Refined panel must remain compact while fitting four resource indicators"
assert "drawStatus(p,\"Comida\",e.food()" in screen and "listClipRight()" in screen
assert "int outputRowLeft=(272-(ResidentialAnchorBlockEntity.OUTPUT_SLOTS*18+(ResidentialAnchorBlockEntity.OUTPUT_SLOTS-1)*3))/2;" in menu
assert "fill(p,sx,sy,sx+18,sy+18,0xFF342419)" in screen and "fill(p,sx+1,sy+1,sx+17,sy+17,0xFFB18A5E)" in screen
assert "mayPlace(ItemStack stack){return false;}" in menu, "The ten output slots remain output only"
assert lang["gui.residenciasaldeoes.anchor_title"]=="Bloco Residencial"
assert lang["item.residenciasaldeoes.emerald_fragment"]=="Fragmento de Esmeralda"
assert 'EMERALD_FRAGMENT = ITEMS.register("emerald_fragment"' in main
model=json.loads((root/"src/main/resources/assets/residenciasaldeoes/models/item/emerald_fragment.json").read_text())
assert model["textures"]["layer0"]=="residenciasaldeoes:item/emerald_fragment"
recipe=json.loads((root/"src/main/resources/data/residenciasaldeoes/recipes/emerald_from_fragment.json").read_text())
assert recipe["pattern"]==["###","###","###"] and recipe["result"]=={"item":"minecraft:emerald","count":1}
tex=root/"src/main/resources/assets/residenciasaldeoes/textures/item/emerald_fragment.png"
data=tex.read_bytes()
assert data[:8]==bytes.fromhex("89504e470d0a1a0a")
assert struct.unpack(">II",data[16:24])==(16,16)
assert data[24]==8 and data[25]==6, "Texture must be 8-bit RGBA so alpha transparency is supported"
recipe_files=list((root/"src/main/resources/data/residenciasaldeoes/recipes").glob("*.json"))
fragment_recipes=[p for p in recipe_files if "emerald_fragment" in p.read_text()]
assert fragment_recipes==[root/"src/main/resources/data/residenciasaldeoes/recipes/emerald_from_fragment.json"]
assert 'VERSION="8"' in (root/"src/main/java/br/com/residenciasaldeoes/network/ModNetwork.java").read_text()
print("Residence food, output slots, item, recipe, localization, and threat timeout checks passed.")
