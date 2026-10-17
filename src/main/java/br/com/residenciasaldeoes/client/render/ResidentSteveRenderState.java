package br.com.residenciasaldeoes.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

import java.util.List;

/**
 * Renderer state saved while a resident is rendered with the player model.
 * This helper intentionally lives outside the package owned by the Mixin config.
 */
@SuppressWarnings("rawtypes")
public record ResidentSteveRenderState(EntityModel model, List<RenderLayer> layers) {
}
