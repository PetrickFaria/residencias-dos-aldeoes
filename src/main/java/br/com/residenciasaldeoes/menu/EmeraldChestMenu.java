package br.com.residenciasaldeoes.menu;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Reject invalid clicks and shift-clicks before the player's source stack is consumed. */
public final class EmeraldChestMenu extends ChestMenu {
    public EmeraldChestMenu(int id,Inventory inventory,Container container) {
        super(MenuType.GENERIC_9x3,id,inventory,container,3);
        for(int i=0;i<27;i++) {
            Slot old=slots.get(i);
            Slot filtered=new Slot(container,i,old.x,old.y) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return stack.is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get());
                }
            };
            filtered.index=i;
            slots.set(i,filtered);
        }
    }
}
