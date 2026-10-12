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
assert "for(int slot=0;slot<5;slot++)" in menu and "mayPlace(ItemStack stack){return false;}" in menu
assert "implements Container" in be and "ContainerHelper.saveAllItems(tag,outputItems)" in be and "ContainerHelper.loadAllItems(tag,outputItems)" in be
assert "setOutputItem(int slot,ItemStack stack)" in be
assert "RETURN_TIMEOUT_TICKS=300L" in events and "THREAT_RETURN_TIMEOUT_TICKS=150L" in events
assert "MemoryModuleType.NEAREST_HOSTILE" in events and "villager.hasLineOfSight(entity)" in events
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
from PIL import Image
image=Image.open(tex).convert("RGBA")
assert image.getpixel((0,0))[3]==0 and any(pixel[3]>0 for pixel in image.get_flattened_data())
recipe_files=list((root/"src/main/resources/data/residenciasaldeoes/recipes").glob("*.json"))
fragment_recipes=[p for p in recipe_files if "emerald_fragment" in p.read_text()]
assert fragment_recipes==[root/"src/main/resources/data/residenciasaldeoes/recipes/emerald_from_fragment.json"]
assert 'VERSION="8"' in (root/"src/main/java/br/com/residenciasaldeoes/network/ModNetwork.java").read_text()
print("Residence food, output slots, item, recipe, localization, and threat timeout checks passed.")
