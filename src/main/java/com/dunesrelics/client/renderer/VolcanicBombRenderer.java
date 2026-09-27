package com.dunesrelics.client.renderer;

import com.dunesrelics.entity.volcanic.VolcanicBomb;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/** Renders volcanic bombs as a large, fully lit magma block. */
public class VolcanicBombRenderer extends ThrownItemRenderer<VolcanicBomb> {
    public VolcanicBombRenderer(EntityRendererProvider.Context context) {
        super(context, 2.0F, true);
    }
}
