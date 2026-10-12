package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.data.ResidenceData;
import br.com.residenciasaldeoes.data.SkinApplicationMode;
import br.com.residenciasaldeoes.menu.AnchorMenu;
import br.com.residenciasaldeoes.network.ModNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.io.IOException;
import java.util.ArrayList;

public class AnchorScreen extends AbstractContainerScreen<AnchorMenu> {
    private static final int ROW_TOP=77,ROW_HEIGHT=13,VISIBLE_ROWS=5,LIST_HEIGHT=VISIBLE_ROWS*ROW_HEIGHT;
    // Skin dialog preview viewport, local to the dialog rather than to the screen.
    private static final int SKIN_PREVIEW_LEFT=154,SKIN_PREVIEW_RIGHT=213,SKIN_PREVIEW_TOP=22,SKIN_PREVIEW_BOTTOM=111;
    private static final int SKIN_HINT_LEFT=8,SKIN_HINT_RIGHT=149,SKIN_HINT_TOP=112,SKIN_HINT_BOTTOM=129;
    // The preview camera is flipped 180 degrees on Z, so negative model yaw turns the face toward screen-right.
    private static final float SKIN_PREVIEW_DEFAULT_YAW=-25.0F;
    private static final int REGISTER_BUTTON_Y=151,UNLINK_BUTTON_Y=175,PREVIEW_BUTTON_Y=199;
    private static final Map<String,Snapshot> CACHE=new HashMap<>();
    private List<ModNetwork.ResidentEntry> residents=List.of();
    private int scroll;
    private UUID selected;
    private boolean previewOn,confirmAll,renameOpen,personalizationOpen,skinOpen;
    private boolean draggingScrollbar;
    private double scrollbarDragStartY;
    private int scrollbarDragStart;
    private Button registerButton,renameButton,unlinkResidentButton,unlinkAllButton,previewButton;
    private final List<Button> personalizationButtons=new ArrayList<>(),skinButtons=new ArrayList<>();
    private final List<SkinChoice> skinChoices=new ArrayList<>();
    private Button skinConfirm,skinCancel;
    private Button skinPreviewCenter;
    private int skinScroll,skinDialogLeft,skinDialogTop;
    private float skinPreviewYaw=SKIN_PREVIEW_DEFAULT_YAW,skinPreviewPitch;
    private boolean draggingSkinPreview;
    private double skinPreviewDragX,skinPreviewDragY;
    private String pendingSkin="";
    private Villager skinPreviewVillager;
    private EditBox renameField;
    private Button renameConfirm,renameCancel;
    private int dialogLeft,dialogTop;

    private record Snapshot(List<ModNetwork.ResidentEntry> entries,boolean preview) {}
    private record SkinChoice(String id,String label,SkinFiles.SkinFile file) {}

    public AnchorScreen(AnchorMenu m,Inventory i,Component t){super(m,i,t);imageWidth=246;imageHeight=226;}
    private static String cacheKey(ResourceLocation dimension,BlockPos pos){return dimension+"|"+pos.asLong();}

    public static void receiveResidents(ResourceLocation dimension,BlockPos pos,List<ModNetwork.ResidentEntry> entries,boolean preview){
        List<ModNetwork.ResidentEntry> copy=List.copyOf(entries);CACHE.put(cacheKey(dimension,pos),new Snapshot(copy,preview));
        Minecraft mc=Minecraft.getInstance();
        if(mc.level!=null&&mc.level.dimension().location().equals(dimension)&&mc.screen instanceof AnchorScreen s&&s.menu.getPos().equals(pos))s.applySnapshot(copy,preview);
    }
    /** Keeps the button label aligned with the same authoritative state the renderer receives. */
    public static void receivePreviewStates(List<ModNetwork.PreviewEntry> entries){
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||!(mc.screen instanceof AnchorScreen s))return;
        for(var e:entries){if(mc.level.dimension().location().equals(e.dimension())&&s.menu.getPos().equals(e.pos())){
            s.applyPreviewState(e.permanent());String key=cacheKey(e.dimension(),e.pos());Snapshot old=CACHE.get(key);if(old!=null)CACHE.put(key,new Snapshot(old.entries(),e.permanent()));return;
        }}
    }
    private void applyPreviewState(boolean enabled){previewOn=enabled;if(previewButton!=null)previewButton.setMessage(previewLabel());}
    private void applySnapshot(List<ModNetwork.ResidentEntry> entries,boolean preview){
        residents=List.copyOf(entries);scroll=Math.min(scroll,Math.max(0,residents.size()-VISIBLE_ROWS));previewOn=preview;
        if(previewButton!=null)previewButton.setMessage(previewLabel());
        if(selected!=null&&residents.stream().noneMatch(v->v.id().equals(selected))){selected=null;if(renameButton!=null)renameButton.active=false;if(renameOpen)closeRename(false);if(personalizationOpen)closeCustomization();if(skinOpen)closeSkinDialog();}
        if(renameButton!=null)renameButton.active=selected!=null&&!renameOpen;
        if(unlinkResidentButton!=null)unlinkResidentButton.active=selected!=null&&!renameOpen;
    }
    private Component previewLabel(){return Component.literal(previewOn?"Mostrar Limites (Ligado)":"Mostrar Limites (Desligado)");}
    private String residentName(UUID id){return residents.stream().filter(v->v.id().equals(id)).map(ModNetwork.ResidentEntry::name).findFirst().orElse("");}

    @Override protected void init(){
        super.init();titleLabelX=(imageWidth-font.width(title))/2;titleLabelY=7;inventoryLabelY=1000;
        if(minecraft.level!=null){Snapshot s=CACHE.get(cacheKey(minecraft.level.dimension().location(),menu.getPos()));if(s!=null)applySnapshot(s.entries(),s.preview());}
        for(int i=0;i<5;i++){final int size=ResidenceData.DIMENSIONS[i];addStyledButton(leftPos+8+i*46,topPos+33,43,17,Component.literal(size+"x"+size),b->ModNetwork.action(menu.getPos(),0,size),0xFF0D4B25,0xFF70C982);}
        registerButton=addStyledButton(leftPos+8,topPos+REGISTER_BUTTON_Y,112,20,Component.translatable("gui.residenciasaldeoes.register"),b->ModNetwork.action(menu.getPos(),1,0),0xFF0D4B25,0xFF70C982);
        renameButton=addStyledButton(leftPos+126,topPos+REGISTER_BUTTON_Y,112,20,Component.translatable("gui.residenciasaldeoes.customization"),b->openCustomization(),0xFF0D4B25,0xFF70C982);renameButton.active=selected!=null;
        unlinkResidentButton=addStyledButton(leftPos+8,topPos+UNLINK_BUTTON_Y,112,20,Component.literal("Desvincular Residente"),b->{if(selected!=null)ModNetwork.unlinkResident(menu.getPos(),selected);},0xFF0D4B25,0xFF70C982);unlinkResidentButton.active=selected!=null;
        unlinkAllButton=addStyledButton(leftPos+126,topPos+UNLINK_BUTTON_Y,112,20,unlinkAllLabel(),b->{if(confirmAll){ModNetwork.unlinkAll(menu.getPos());confirmAll=false;}else confirmAll=true;b.setMessage(unlinkAllLabel());},0xFF0D4B25,0xFF70C982);
        previewButton=addStyledButton(leftPos+8,topPos+PREVIEW_BUTTON_Y,230,20,previewLabel(),b->{previewOn=!previewOn;b.setMessage(previewLabel());ModNetwork.setPreview(menu.getPos(),previewOn);},0xFF0D4B25,0xFF70C982);
        // Pull an authoritative snapshot when a screen opens; the client cache is
        // only a fast initial display and may belong to an earlier menu session.
        ModNetwork.requestResidents(menu.getPos());
    }
    private Button addStyledButton(int x,int y,int width,int height,Component message,Button.OnPress action,int faceColor,int edgeColor){
        Button button=new StyledButton(x,y,width,height,message,action,faceColor,edgeColor);addRenderableWidget(button);return button;
    }
    private Component unlinkAllLabel(){return Component.literal(confirmAll?"Confirmar Todos?":"Desvincular Todos");}

    private void openCustomization(){
        if(selected==null||personalizationOpen||renameOpen||skinOpen)return;
        personalizationOpen=true;dialogLeft=leftPos+(imageWidth-180)/2;dialogTop=topPos+(imageHeight-112)/2;
        registerButton.active=false;renameButton.active=false;unlinkResidentButton.active=false;unlinkAllButton.active=false;previewButton.active=false;
        Component[] labels={Component.translatable("gui.residenciasaldeoes.rename"),Component.translatable("gui.residenciasaldeoes.change_skin"),Component.translatable("gui.residenciasaldeoes.back")};
        for(int i=0;i<labels.length;i++){
            final int option=i;
            personalizationButtons.add(addStyledButton(dialogLeft+12,dialogTop+27+i*25,156,20,labels[i],b->{
                if(option==0){closeCustomization();openRename();}
                else if(option==1)openSkinDialog();
                else closeCustomization();
            },0xFF0D4B25,0xFF70C982));
        }
    }
    private void closeCustomization(){
        if(!personalizationOpen)return;
        for(Button b:personalizationButtons)removeWidget(b);personalizationButtons.clear();personalizationOpen=false;
        setMainButtonsActive(true);
    }
    private void setMainButtonsActive(boolean active){
        if(registerButton!=null)registerButton.active=active;
        if(renameButton!=null)renameButton.active=active&&selected!=null;
        if(unlinkResidentButton!=null)unlinkResidentButton.active=active&&selected!=null;
        if(unlinkAllButton!=null)unlinkAllButton.active=active;
        if(previewButton!=null)previewButton.active=active;
    }
    private void openSkinDialog(){
        if(selected==null||!personalizationOpen)return;
        closeCustomization();skinOpen=true;skinScroll=0;skinChoices.clear();skinChoices.add(new SkinChoice("",Component.translatable("gui.residenciasaldeoes.original_skin").getString(),null));
        ModNetwork.ResidentEntry selectedResident=residents.stream().filter(r->r.id().equals(selected)).findFirst().orElse(null);
        String current=selectedResident==null?"":selectedResident.skinKey();
        pendingSkin=current;
        resetSkinPreviewView();
        if(!current.isEmpty())ModNetwork.requestSkin(menu.getPos(),selected);
        for(SkinFiles.SkinFile file:SkinFiles.list())skinChoices.add(new SkinChoice(file.id(),file.label(),file));
        if(!current.isEmpty()&&skinChoices.stream().noneMatch(o->o.id().equals(current)))skinChoices.add(new SkinChoice(current,Component.translatable("gui.residenciasaldeoes.current_skin",current).getString(),null));
        skinDialogLeft=leftPos+(imageWidth-220)/2;skinDialogTop=topPos+(imageHeight-180)/2;
        skinPreviewVillager=minecraft.level==null?null:EntityType.VILLAGER.create(minecraft.level);
        updateSkinPreview();
        setMainButtonsActive(false);
        rebuildSkinButtons();
        skinConfirm=addStyledButton(skinDialogLeft+16,skinDialogTop+132,86,20,Component.literal("Confirmar"),b->confirmSkin(),0xFF0D4B25,0xFF70C982);
        skinCancel=addStyledButton(skinDialogLeft+118,skinDialogTop+132,86,20,Component.literal("Cancelar"),b->closeSkinDialog(),0xFF0D4B25,0xFF70C982);
        skinPreviewCenter=addStyledButton(skinDialogLeft+153,skinDialogTop+112,60,14,Component.literal("Centralizar"),b->resetSkinPreviewView(),0xFF0D4B25,0xFF70C982);
    }
    private void resetSkinPreviewView(){skinPreviewYaw=SKIN_PREVIEW_DEFAULT_YAW;skinPreviewPitch=0.0F;}
    private void rebuildSkinButtons(){
        for(Button b:skinButtons)removeWidget(b);skinButtons.clear();
        int visible=Math.min(5,Math.max(0,skinChoices.size()-skinScroll));
        for(int row=0;row<visible;row++){
            SkinChoice option=skinChoices.get(skinScroll+row);
            String marker=option.id().equals(pendingSkin)?"› ":"  ";
            Button b=addStyledButton(skinDialogLeft+10,skinDialogTop+25+row*17,136,16,Component.literal(marker+font.plainSubstrByWidth(option.label(),116)),w->{pendingSkin=option.id();updateSkinPreview();rebuildSkinButtons();},0xFF0D4B25,0xFF70C982);
            skinButtons.add(b);
        }
    }
    private void updateSkinPreview(){
        if(skinPreviewVillager==null)return;
        if(pendingSkin.isEmpty()){VillagerSkinTextures.clear(skinPreviewVillager.getUUID());return;}
        SkinChoice option=skinChoices.stream().filter(o->o.id().equals(pendingSkin)).findFirst().orElse(null);
        try{byte[] png=option!=null&&option.file()!=null?SkinFiles.read(option.file()):VillagerSkinTextures.getPng(selected);if(png!=null)VillagerSkinTextures.receive(skinPreviewVillager.getUUID(),pendingSkin,png,SkinApplicationMode.COMPLETE);}catch(IOException ignored){}
    }
    private void confirmSkin(){
        if(!skinOpen||selected==null)return;
        ModNetwork.ResidentEntry selectedResident=residents.stream().filter(r->r.id().equals(selected)).findFirst().orElse(null);
        String current=selectedResident==null?"":selectedResident.skinKey();
        SkinApplicationMode currentMode=selectedResident==null?SkinApplicationMode.COMPLETE:selectedResident.skinMode();
        if(pendingSkin.isEmpty()&&!current.isEmpty())ModNetwork.assignSkin(menu.getPos(),selected,"",new byte[0],SkinApplicationMode.COMPLETE);
        else if(!pendingSkin.isEmpty()&&!pendingSkin.equals(current)){
            SkinChoice choice=skinChoices.stream().filter(o->o.id().equals(pendingSkin)).findFirst().orElse(null);
            if(choice==null||choice.file()==null)return;
            try{ModNetwork.assignSkin(menu.getPos(),selected,choice.id(),SkinFiles.read(choice.file()),SkinApplicationMode.COMPLETE);}catch(IOException ignored){return;}
        }else if(!pendingSkin.isEmpty()&&currentMode!=SkinApplicationMode.COMPLETE){
            ModNetwork.assignSkin(menu.getPos(),selected,pendingSkin,new byte[0],SkinApplicationMode.COMPLETE);
        }
        closeSkinDialog();openCustomization();
    }
    private void closeSkinDialog(){
        if(!skinOpen)return;
        for(Button b:skinButtons)removeWidget(b);skinButtons.clear();
        if(skinConfirm!=null)removeWidget(skinConfirm);if(skinCancel!=null)removeWidget(skinCancel);if(skinPreviewCenter!=null)removeWidget(skinPreviewCenter);skinConfirm=null;skinCancel=null;skinPreviewCenter=null;
        skinOpen=false;if(skinPreviewVillager!=null)VillagerSkinTextures.clear(skinPreviewVillager.getUUID());skinPreviewVillager=null;setMainButtonsActive(true);openCustomization();
    }
    public static void skinReceived(UUID resident){Minecraft mc=Minecraft.getInstance();if(mc.screen instanceof AnchorScreen screen)screen.refreshPreviewSkin(resident);}
    private void refreshPreviewSkin(UUID resident){if(!skinOpen||skinPreviewVillager==null||!resident.equals(selected)||!Objects.equals(pendingSkin,residents.stream().filter(r->r.id().equals(resident)).map(ModNetwork.ResidentEntry::skinKey).findFirst().orElse("")))return;byte[] bytes=VillagerSkinTextures.getPng(resident);if(bytes!=null)VillagerSkinTextures.receive(skinPreviewVillager.getUUID(),pendingSkin,bytes,SkinApplicationMode.COMPLETE);}

    private void openRename(){
        if(selected==null||renameOpen)return;
        renameOpen=true;dialogLeft=leftPos+(imageWidth-196)/2;dialogTop=topPos+(imageHeight-82)/2;
        renameField=new EditBox(font,dialogLeft+14,dialogTop+25,168,18,Component.translatable("gui.residenciasaldeoes.rename_hint"));renameField.setMaxLength(64);renameField.setValue(residentName(selected));addRenderableWidget(renameField);
        renameConfirm=addStyledButton(dialogLeft+14,dialogTop+53,80,20,Component.literal("Confirmar"),b->confirmRename(),0xFF0D4B25,0xFF70C982);
        renameCancel=addStyledButton(dialogLeft+102,dialogTop+53,80,20,Component.literal("Cancelar"),b->closeRename(false),0xFF0D4B25,0xFF70C982);
        registerButton.active=false;renameButton.active=false;unlinkResidentButton.active=false;unlinkAllButton.active=false;previewButton.active=false;
        setFocused(renameField);renameField.setFocus(true);
    }
    private void confirmRename(){
        if(!renameOpen||selected==null||renameField==null)return;
        String newName=renameField.getValue().trim();
        if(newName.isEmpty())return;
        ModNetwork.renameResident(menu.getPos(),selected,newName);
        List<ModNetwork.ResidentEntry> next=residents.stream().map(e->e.id().equals(selected)?new ModNetwork.ResidentEntry(e.id(),newName,e.bed(),e.work(),e.chest(),e.skinKey(),e.skinMode()):e).toList();
        if(minecraft.level!=null)receiveResidents(minecraft.level.dimension().location(),menu.getPos(),next,previewOn);
        closeRename(true);
    }
    private void closeRename(boolean saved){
        if(!renameOpen)return;
        setFocused(null);
        if(renameField!=null)removeWidget(renameField);if(renameConfirm!=null)removeWidget(renameConfirm);if(renameCancel!=null)removeWidget(renameCancel);
        renameField=null;renameConfirm=null;renameCancel=null;renameOpen=false;
        setMainButtonsActive(true);
    }

    @Override protected void renderLabels(PoseStack p,int mouseX,int mouseY){
        if(!skinOpen)font.draw(p,title,titleLabelX,titleLabelY,0xFFE9D5B2);
    }

    @Override protected void renderBg(PoseStack p,float q,int x,int y){
        fill(p,leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF30251C);fill(p,leftPos+3,topPos+3,leftPos+imageWidth-3,topPos+imageHeight-3,0xFF59442E);
        fill(p,leftPos+6,topPos+30,leftPos+imageWidth-6,topPos+31,0xFF876A45);fill(p,leftPos+7,topPos+74,leftPos+imageWidth-7,topPos+ROW_TOP+LIST_HEIGHT+2,0xFF393125);
        fill(p,leftPos+8,topPos+75,leftPos+imageWidth-8,topPos+76,0xFF6B5438);
    }
    @Override public void render(PoseStack p,int x,int y,float q){
        renderBackground(p);super.render(p,x,y,q);
        font.draw(p,Component.translatable("gui.residenciasaldeoes.current",menu.getSize(minecraft.player),menu.getSize(minecraft.player),menu.getSize(minecraft.player)),leftPos+8,topPos+19,0xFFF2E5C9);
        font.draw(p,Component.translatable("gui.residenciasaldeoes.occupancy",menu.getCount(minecraft.player),capacity(menu.getSize(minecraft.player))),leftPos+8,topPos+54,0xFFE4D7BD);
        font.draw(p,Component.translatable("gui.residenciasaldeoes.residents"),leftPos+8,topPos+64,0xFFFFEBCB);
        enableResidentClip();
        if(residents.isEmpty())font.draw(p,Component.translatable("gui.residenciasaldeoes.no_residents"),leftPos+12,topPos+82,0xFFD3C8AF);
        for(int i=scroll;i<Math.min(residents.size(),scroll+VISIBLE_ROWS);i++){
            int yy=topPos+ROW_TOP+(i-scroll)*ROW_HEIGHT;var e=residents.get(i);
            if(e.id().equals(selected)){fill(p,leftPos+7,yy-2,listClipRight(),yy+10,0xFF9A8058);fill(p,leftPos+8,yy-1,listClipRight()-1,yy+9,0xFF4A3B2C);}
            font.draw(p,font.plainSubstrByWidth(e.name(),69),leftPos+10,yy,e.id().equals(selected)?0xFFFFE7BF:0xFFF2EEE4);
            int sx=leftPos+82;sx=drawStatus(p,"Cama",e.bed(),sx,yy);sx=drawSeparator(p,sx,yy);sx=drawStatus(p,"Trabalho",e.work(),sx,yy);sx=drawSeparator(p,sx,yy);drawStatus(p,"Baú",e.chest(),sx,yy);
        }
        RenderSystem.disableScissor();
        if(hasScrollbar())renderScrollbar(p);
        if(renameOpen)renderRenameDialog(p,x,y,q);
        if(personalizationOpen)renderCustomizationDialog(p,x,y,q);
        if(skinOpen)renderSkinDialog(p,x,y,q);
        renderTooltip(p,x,y);
    }
    private int drawStatus(PoseStack p,String label,boolean ready,int x,int y){font.draw(p,Component.literal(label),x,y,0xFFE5DFD2);x+=font.width(label)+2;font.draw(p,Component.literal(ready?"✓":"×"),x,y,ready?0xFF59D982:0xFFE96A5C);return x+font.width(ready?"✓":"×")+4;}
    private int drawSeparator(PoseStack p,int x,int y){font.draw(p,Component.literal("|"),x,y,0xFFAA9878);return x+font.width("|")+4;}
    private boolean hasScrollbar(){return residents.size()>VISIBLE_ROWS;}
    private int scrollbarX(){return leftPos+imageWidth-11;}
    private int listClipRight(){return hasScrollbar()?scrollbarX()-2:leftPos+imageWidth-7;}
    private int scrollbarThumbHeight(){return Math.max(12,LIST_HEIGHT*VISIBLE_ROWS/residents.size());}
    private int scrollbarThumbTop(){int max= Math.max(1,residents.size()-VISIBLE_ROWS);return topPos+ROW_TOP+(LIST_HEIGHT-scrollbarThumbHeight())*scroll/max;}
    private void enableResidentClip(){double scale=minecraft.getWindow().getGuiScale();int x=(int)Math.floor((leftPos+7)*scale),right=(int)Math.ceil(listClipRight()*scale);int top=(int)Math.floor((topPos+ROW_TOP)*scale),bottom=(int)Math.ceil((topPos+ROW_TOP+LIST_HEIGHT)*scale);RenderSystem.enableScissor(x,minecraft.getWindow().getHeight()-bottom,right-x,bottom-top);}
    private void renderScrollbar(PoseStack p){int x=scrollbarX(),y=topPos+ROW_TOP;fill(p,x,y,x+5,y+LIST_HEIGHT,0xFF29231D);fill(p,x+1,y,x+4,y+LIST_HEIGHT,0xFF493B2B);int thumbTop=scrollbarThumbTop(),thumbBottom=thumbTop+scrollbarThumbHeight();fill(p,x,thumbTop,x+5,thumbBottom,0xFF896B46);fill(p,x+1,thumbTop+1,x+4,thumbBottom-1,0xFF62492F);}
    private void setScrollFromPointer(double y){int trackTop=topPos+ROW_TOP,travel=LIST_HEIGHT-scrollbarThumbHeight(),max=residents.size()-VISIBLE_ROWS;if(travel<=0||max<=0){scroll=0;return;}double thumbOffset=Math.max(0,Math.min(travel,y-trackTop-scrollbarThumbHeight()/2.0));scroll=(int)Math.round(thumbOffset*max/travel);}
    private void renderRenameDialog(PoseStack p,int x,int y,float q){
        fill(p,leftPos+3,topPos+3,leftPos+imageWidth-3,topPos+imageHeight-3,0x99000000);
        fill(p,dialogLeft,dialogTop,dialogLeft+196,dialogTop+82,0xFF30251C);fill(p,dialogLeft+2,dialogTop+2,dialogLeft+194,dialogTop+80,0xFF644A30);
        font.draw(p,Component.literal("Renomear: "+font.plainSubstrByWidth(residentName(selected),110)),dialogLeft+10,dialogTop+9,0xFFFFEBCB);
        if(renameField!=null)renameField.render(p,x,y,q);if(renameConfirm!=null)renameConfirm.render(p,x,y,q);if(renameCancel!=null)renameCancel.render(p,x,y,q);
    }
    private void renderCustomizationDialog(PoseStack p,int x,int y,float q){
        fill(p,leftPos+3,topPos+3,leftPos+imageWidth-3,topPos+imageHeight-3,0x99000000);
        fill(p,dialogLeft,dialogTop,dialogLeft+180,dialogTop+112,0xFF30251C);fill(p,dialogLeft+2,dialogTop+2,dialogLeft+178,dialogTop+110,0xFF644A30);
        Component heading=Component.translatable("gui.residenciasaldeoes.personalization_title");font.draw(p,heading,dialogLeft+(180-font.width(heading))/2,dialogTop+10,0xFFFFEBCB);
        for(Button b:personalizationButtons)b.render(p,x,y,q);
    }
    private void renderSkinDialog(PoseStack p,int x,int y,float q){
        fill(p,leftPos+3,topPos+3,leftPos+imageWidth-3,topPos+imageHeight-3,0x99000000);
        fill(p,skinDialogLeft,skinDialogTop,skinDialogLeft+220,skinDialogTop+180,0xFF30251C);fill(p,skinDialogLeft+2,skinDialogTop+2,skinDialogLeft+218,skinDialogTop+178,0xFF644A30);
        Component heading=Component.translatable("gui.residenciasaldeoes.skin_title");font.draw(p,heading,skinDialogLeft+(220-font.width(heading))/2,skinDialogTop+9,0xFFFFEBCB);
        fill(p,skinDialogLeft+7,skinDialogTop+22,skinDialogLeft+213,skinDialogTop+111,0xFF393125);
        for(Button b:skinButtons)b.render(p,x,y,q);
        if(skinChoices.isEmpty())font.draw(p,Component.translatable("gui.residenciasaldeoes.no_skins"),skinDialogLeft+12,skinDialogTop+32,0xFFD3C8AF);
        if(skinChoices.size()>5)renderSkinScrollbar(p);
        if(skinPreviewVillager!=null)renderSkinPreview(p);
        if(skinPreviewCenter!=null)skinPreviewCenter.render(p,x,y,q);
        if(skinConfirm!=null)skinConfirm.render(p,x,y,q);if(skinCancel!=null)skinCancel.render(p,x,y,q);
        renderSkinFolderHint(p);
    }
    private void renderSkinPreview(PoseStack poses){
        int x1=skinDialogLeft+SKIN_PREVIEW_LEFT,x2=skinDialogLeft+SKIN_PREVIEW_RIGHT;
        int y1=skinDialogTop+SKIN_PREVIEW_TOP,y2=skinDialogTop+SKIN_PREVIEW_BOTTOM;
        double guiScale=minecraft.getWindow().getGuiScale();
        int sx=(int)Math.floor(x1*guiScale),ex=(int)Math.ceil(x2*guiScale);
        int sy=(int)Math.floor(y1*guiScale),ey=(int)Math.ceil(y2*guiScale);
        int viewportWidth=x2-x1,viewportHeight=y2-y1;
        float modelHeight=Math.max(1.0F,skinPreviewVillager.getBbHeight());
        float modelWidth=Math.max(0.8F,skinPreviewVillager.getBbWidth()*1.35F);
        int baseScale=Math.max(1,(int)Math.floor(Math.min(36.0F,Math.min((viewportWidth-8.0F)/modelWidth,(viewportHeight-12.0F)/modelHeight))));
        int scale=fitPreviewScale(baseScale,viewportWidth,viewportHeight,modelWidth,modelHeight,skinPreviewYaw,skinPreviewPitch);
        int centerX=x1+viewportWidth/2;
        int centerY=y1+viewportHeight/2;
        RenderSystem.enableScissor(sx,minecraft.getWindow().getHeight()-ey,ex-sx,ey-sy);
        try{renderRotatingPreview(poses,centerX,centerY,scale,modelHeight);}finally{RenderSystem.disableScissor();}
    }
    private int fitPreviewScale(int baseScale,int viewportWidth,int viewportHeight,float modelWidth,float modelHeight,float yaw,float pitch){
        double y=Math.toRadians(yaw),p=Math.toRadians(pitch);
        double cosY=Math.abs(Math.cos(y)),sinY=Math.abs(Math.sin(y));
        double cosP=Math.abs(Math.cos(p)),sinP=Math.abs(Math.sin(p));
        double halfWidth=modelWidth/2.0,halfDepth=modelWidth/2.0,halfHeight=modelHeight/2.0;
        // Project a 3D bounding box through the same Y-then-X rotation order used below.
        double projectedHalfWidth=cosY*halfWidth+sinY*(sinP*halfHeight+cosP*halfDepth);
        double projectedHalfHeight=cosP*halfHeight+sinP*halfDepth;
        int fitWidth=(int)Math.floor((viewportWidth-8.0)/(2.0*projectedHalfWidth));
        int fitHeight=(int)Math.floor((viewportHeight-12.0)/(2.0*projectedHalfHeight));
        return Math.max(1,Math.min(baseScale,Math.min(fitWidth,fitHeight)));
    }
    private void renderSkinFolderHint(PoseStack p){
        int x=skinDialogLeft+SKIN_HINT_LEFT,y=skinDialogTop+SKIN_HINT_TOP;
        int clipRight=skinDialogLeft+SKIN_HINT_RIGHT,clipBottom=skinDialogTop+SKIN_HINT_BOTTOM;
        double guiScale=minecraft.getWindow().getGuiScale();
        int sx=(int)Math.floor(x*guiScale),ex=(int)Math.ceil(clipRight*guiScale);
        int sy=(int)Math.floor(y*guiScale),ey=(int)Math.ceil(clipBottom*guiScale);
        RenderSystem.enableScissor(sx,minecraft.getWindow().getHeight()-ey,ex-sx,ey-sy);
        List<net.minecraft.util.FormattedCharSequence> lines=font.split(Component.translatable("gui.residenciasaldeoes.skin_folder_hint"),SKIN_HINT_RIGHT-SKIN_HINT_LEFT-2);
        for(int i=0;i<Math.min(2,lines.size());i++)font.draw(p,lines.get(i),x,skinDialogTop+SKIN_HINT_TOP+i*8,0xFFD3C8AF);
        RenderSystem.disableScissor();
    }
    private void renderRotatingPreview(PoseStack poses,int x,int y,int scale,float modelHeight){
        Lighting.setupForEntityInInventory();var dispatcher=minecraft.getEntityRenderDispatcher();poses.pushPose();poses.translate(x,y,1050.0D);poses.scale(1.0F,1.0F,-1.0F);poses.translate(0.0D,0.0D,1000.0D);poses.scale(scale,scale,scale);
        var camera=Vector3f.ZP.rotationDegrees(180.0F);poses.mulPose(camera);dispatcher.overrideCameraOrientation(camera);dispatcher.setRenderShadow(false);
        // The mesh center is about 0.43 entity-heights above the renderer origin.
        // Translate that center to the pivot, then apply both rotations around it.
        poses.mulPose(Vector3f.YP.rotationDegrees(skinPreviewYaw));
        poses.mulPose(Vector3f.XP.rotationDegrees(skinPreviewPitch));
        poses.translate(0.0D,-modelHeight*0.43D,0.0D);
        // Vanilla inventory previews use a 180-degree entity yaw to face the viewer.
        // The additional yaw and pitch above are screen-only transforms on this preview entity.
        skinPreviewVillager.setYRot(180);skinPreviewVillager.yBodyRot=180;skinPreviewVillager.yBodyRotO=180;skinPreviewVillager.yHeadRot=180;skinPreviewVillager.yHeadRotO=180;
        var buffers=minecraft.renderBuffers().bufferSource();dispatcher.render(skinPreviewVillager,0,0,0,0,1.0F,poses,buffers,15728880);buffers.endBatch();dispatcher.overrideCameraOrientation(null);dispatcher.setRenderShadow(true);Lighting.setupFor3DItems();poses.popPose();
    }
    private boolean hasSkinScrollbar(){return skinChoices.size()>5;}
    private int skinScrollbarX(){return skinDialogLeft+149;}
    private int skinThumbHeight(){return Math.max(12,85*5/skinChoices.size());}
    private int skinThumbTop(){int max=Math.max(1,skinChoices.size()-5);return skinDialogTop+25+(85-skinThumbHeight())*skinScroll/max;}
    private void renderSkinScrollbar(PoseStack p){int x=skinScrollbarX(),y=skinDialogTop+25;fill(p,x,y,x+4,y+85,0xFF29231D);int thumb=skinThumbTop();fill(p,x,thumb,x+4,thumb+skinThumbHeight(),0xFF896B46);}
    private void setSkinScrollFromPointer(double y){int travel=85-skinThumbHeight(),max=skinChoices.size()-5;if(travel<=0||max<=0){skinScroll=0;return;}double offset=Math.max(0,Math.min(travel,y-(skinDialogTop+25)-skinThumbHeight()/2.0));skinScroll=(int)Math.round(offset*max/travel);rebuildSkinButtons();}

    @Override public boolean mouseClicked(double x,double y,int button){
        if(renameOpen){if(x<dialogLeft||x>=dialogLeft+196||y<dialogTop||y>=dialogTop+82)return true;return super.mouseClicked(x,y,button);}
        if(personalizationOpen){if(x<dialogLeft||x>=dialogLeft+180||y<dialogTop||y>=dialogTop+112)return true;return super.mouseClicked(x,y,button);}
        if(skinOpen){
            setFocused(null);
            if(x<skinDialogLeft||x>=skinDialogLeft+220||y<skinDialogTop||y>=skinDialogTop+180)return true;
            if(button==0&&x>=skinDialogLeft+SKIN_PREVIEW_LEFT&&x<skinDialogLeft+SKIN_PREVIEW_RIGHT&&y>=skinDialogTop+SKIN_PREVIEW_TOP&&y<skinDialogTop+SKIN_PREVIEW_BOTTOM){draggingSkinPreview=true;skinPreviewDragX=x;skinPreviewDragY=y;return true;}
            if(button==0&&hasSkinScrollbar()&&x>=skinScrollbarX()-2&&x<skinScrollbarX()+7&&y>=skinDialogTop+25&&y<skinDialogTop+110){setSkinScrollFromPointer(y);return true;}
            return dispatchSkinDialogClick(x,y,button);
        }
        if(button==0&&hasScrollbar()&&x>=scrollbarX()-2&&x<scrollbarX()+7&&y>=topPos+ROW_TOP&&y<topPos+ROW_TOP+LIST_HEIGHT){
            draggingScrollbar=true;scrollbarDragStartY=y;scrollbarDragStart=scroll;
            int thumbTop=scrollbarThumbTop();if(y<thumbTop||y>=thumbTop+scrollbarThumbHeight()){setScrollFromPointer(y);scrollbarDragStart=scroll;scrollbarDragStartY=y;}
            return true;
        }
        if(button==0&&x>=leftPos+7&&x<listClipRight()&&y>=topPos+ROW_TOP&&y<topPos+ROW_TOP+LIST_HEIGHT){
            int i=scroll+(int)(y-(topPos+ROW_TOP))/ROW_HEIGHT;
            if(i<residents.size()){selected=residents.get(i).id();if(renameButton!=null)renameButton.active=true;if(unlinkResidentButton!=null)unlinkResidentButton.active=true;setFocused(null);return true;}
        }
        return super.mouseClicked(x,y,button);
    }
    private boolean dispatchSkinDialogClick(double x,double y,int mouseButton){
        // Route only to skin-dialog widgets. The main-screen unlink buttons are
        // underneath this modal and share its footer coordinates.
        List<Button> overlay=new ArrayList<>(skinButtons);
        if(skinPreviewCenter!=null)overlay.add(skinPreviewCenter);
        if(skinConfirm!=null)overlay.add(skinConfirm);
        if(skinCancel!=null)overlay.add(skinCancel);
        for(int i=overlay.size()-1;i>=0;i--){
            Button widget=overlay.get(i);
            if(widget.isMouseOver(x,y)){
                if(widget.mouseClicked(x,y,mouseButton)){
                    setFocused(widget);
                    if(mouseButton==0)setDragging(true);
                }
                return true;
            }
        }
        setFocused(null);
        return true;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(draggingSkinPreview&&button==0){skinPreviewYaw=(skinPreviewYaw+(float)(x-skinPreviewDragX)*3.0F)%360.0F;skinPreviewPitch=Math.max(-90.0F,Math.min(90.0F,skinPreviewPitch+(float)(y-skinPreviewDragY)*2.0F));skinPreviewDragX=x;skinPreviewDragY=y;return true;}if(draggingScrollbar&&button==0){int travel=LIST_HEIGHT-scrollbarThumbHeight(),max=residents.size()-VISIBLE_ROWS;if(travel>0&&max>0)scroll=Math.max(0,Math.min(max,scrollbarDragStart+(int)Math.round((y-scrollbarDragStartY)*max/travel)));return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){if(button==0&&draggingSkinPreview){draggingSkinPreview=false;return true;}if(button==0&&draggingScrollbar){draggingScrollbar=false;return true;}return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double delta){if(skinOpen&&x>=skinDialogLeft+7&&x<skinDialogLeft+213&&y>=skinDialogTop+22&&y<skinDialogTop+111&&skinChoices.size()>5){skinScroll=Math.max(0,Math.min(skinChoices.size()-5,skinScroll-(int)Math.signum(delta)));rebuildSkinButtons();return true;}if(!renameOpen&&!personalizationOpen&&x>=leftPos+6&&x<leftPos+imageWidth-6&&y>=topPos+ROW_TOP&&y<topPos+ROW_TOP+LIST_HEIGHT&&residents.size()>VISIBLE_ROWS){scroll=Math.max(0,Math.min(residents.size()-VISIBLE_ROWS,scroll-(int)Math.signum(delta)));return true;}return super.mouseScrolled(x,y,delta);}
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
        if(personalizationOpen&&keyCode==GLFW.GLFW_KEY_ESCAPE){closeCustomization();return true;}
        if(skinOpen&&keyCode==GLFW.GLFW_KEY_ESCAPE){closeSkinDialog();return true;}
        if((personalizationOpen||skinOpen)&&keyCode!=GLFW.GLFW_KEY_ESCAPE)return true;
        if(renameOpen&&renameField!=null&&renameField.isFocused()){
            if(keyCode==GLFW.GLFW_KEY_ESCAPE){closeRename(false);return true;}
            if(keyCode==GLFW.GLFW_KEY_ENTER||keyCode==GLFW.GLFW_KEY_KP_ENTER){confirmRename();return true;}
            if(renameField.keyPressed(keyCode,scanCode,modifiers))return true;
            if(isTextKey(keyCode))return true;
        }
        return super.keyPressed(keyCode,scanCode,modifiers);
    }
    @Override public boolean charTyped(char character,int modifiers){if(renameOpen&&renameField!=null&&renameField.isFocused()){renameField.charTyped(character,modifiers);return true;}return super.charTyped(character,modifiers);}
    private boolean isTextKey(int k){return (k>=GLFW.GLFW_KEY_A&&k<=GLFW.GLFW_KEY_Z)||(k>=GLFW.GLFW_KEY_0&&k<=GLFW.GLFW_KEY_9)||k==GLFW.GLFW_KEY_SPACE||(k>=GLFW.GLFW_KEY_KP_0&&k<=GLFW.GLFW_KEY_KP_9)||k==GLFW.GLFW_KEY_KP_DECIMAL||k==GLFW.GLFW_KEY_KP_DIVIDE||k==GLFW.GLFW_KEY_KP_MULTIPLY||k==GLFW.GLFW_KEY_KP_SUBTRACT||k==GLFW.GLFW_KEY_KP_ADD||k==GLFW.GLFW_KEY_MINUS||k==GLFW.GLFW_KEY_EQUAL||k==GLFW.GLFW_KEY_LEFT_BRACKET||k==GLFW.GLFW_KEY_RIGHT_BRACKET||k==GLFW.GLFW_KEY_SEMICOLON||k==GLFW.GLFW_KEY_APOSTROPHE||k==GLFW.GLFW_KEY_GRAVE_ACCENT||k==GLFW.GLFW_KEY_BACKSLASH||k==GLFW.GLFW_KEY_COMMA||k==GLFW.GLFW_KEY_PERIOD||k==GLFW.GLFW_KEY_SLASH;}
    private int capacity(int s){for(int i=0;i<5;i++)if(ResidenceData.DIMENSIONS[i]==s)return ResidenceData.CAPACITIES[i];return 1;}

    /** Visual-only widget skin. Coordinates, size, message and callback remain unchanged. */
    private class StyledButton extends Button {
        private final int faceColor,edgeColor;
        StyledButton(int x,int y,int width,int height,Component message,OnPress action,int faceColor,int edgeColor){super(x,y,width,height,message,action);this.faceColor=faceColor;this.edgeColor=edgeColor;}
        @Override public void renderButton(PoseStack p,int mouseX,int mouseY,float partialTicks){
            boolean hover=isHoveredOrFocused();int face=active?faceColor:darken(faceColor,9);int edge=hover?lighten(edgeColor,15):edgeColor;
            if(hover)face=lighten(face,10);
            fill(p,x,y,x+width,y+height,edge);
            fill(p,x+1,y+1,x+width-1,y+height-1,face);
            fill(p,x+2,y+2,x+width-2,y+3,lighten(face,11));
            fill(p,x+2,y+height-3,x+width-2,y+height-2,darken(face,13));
            Component message=getMessage();int color=active?0xFFB8F2C3:0xFF77A982;Minecraft mc=Minecraft.getInstance();
            mc.font.drawShadow(p,message,x+(width-mc.font.width(message))/2.0f,y+(height-8)/2.0f,color);
        }
    }
    private int lighten(int color,int amount){int r=Math.min(255,((color>>16)&255)+amount),g=Math.min(255,((color>>8)&255)+amount),b=Math.min(255,(color&255)+amount);return (color&0xFF000000)|(r<<16)|(g<<8)|b;}
    private int darken(int color,int amount){int r=Math.max(0,((color>>16)&255)-amount),g=Math.max(0,((color>>8)&255)-amount),b=Math.max(0,(color&255)-amount);return (color&0xFF000000)|(r<<16)|(g<<8)|b;}
}
