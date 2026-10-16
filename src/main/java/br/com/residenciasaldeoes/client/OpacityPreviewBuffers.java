package br.com.residenciasaldeoes.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** Applies the selected alpha to every preview model pass, including the vanilla skin. */
final class OpacityPreviewBuffers implements MultiBufferSource {
    private final MultiBufferSource.BufferSource delegate;
    private final int opacity;

    OpacityPreviewBuffers(MultiBufferSource.BufferSource delegate,int opacity){
        this.delegate=delegate;
        this.opacity=Math.max(0,Math.min(100,opacity));
    }

    @Override public VertexConsumer getBuffer(RenderType type){
        VertexConsumer target=delegate.getBuffer(type);
        if(opacity>=100)return target;
        return new VertexConsumer(){
            @Override public VertexConsumer vertex(double x,double y,double z){target.vertex(x,y,z);return this;}
            @Override public VertexConsumer color(int red,int green,int blue,int alpha){
                int adjusted=alpha;
                if(!ResidentOpacity.renderingNameTag()){
                    adjusted=Math.max(0,Math.min(255,Math.round(alpha*(opacity/100.0F))));
                    ResidentOpacity.beginPreviewAlphaOverride();
                }
                try{target.color(red,green,blue,adjusted);}finally{ResidentOpacity.endPreviewAlphaOverride();}
                return this;
            }
            @Override public VertexConsumer uv(float u,float v){target.uv(u,v);return this;}
            @Override public VertexConsumer overlayCoords(int u,int v){target.overlayCoords(u,v);return this;}
            @Override public VertexConsumer uv2(int u,int v){target.uv2(u,v);return this;}
            @Override public VertexConsumer normal(float x,float y,float z){target.normal(x,y,z);return this;}
            @Override public void endVertex(){target.endVertex();}
            @Override public void defaultColor(int red,int green,int blue,int alpha){
                if(ResidentOpacity.renderingNameTag()){target.defaultColor(red,green,blue,alpha);return;}
                int adjusted=Math.max(0,Math.min(255,Math.round(alpha*(opacity/100.0F))));
                ResidentOpacity.beginPreviewAlphaOverride();
                try{target.defaultColor(red,green,blue,adjusted);}finally{ResidentOpacity.endPreviewAlphaOverride();}
            }
            @Override public void unsetDefaultColor(){target.unsetDefaultColor();}
        };
    }
}
