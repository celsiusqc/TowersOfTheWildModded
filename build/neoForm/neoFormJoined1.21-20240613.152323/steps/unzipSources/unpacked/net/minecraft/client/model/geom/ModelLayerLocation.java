package net.minecraft.client.model.geom;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ModelLayerLocation {
    private final ResourceLocation model;
    private final String layer;

    public ModelLayerLocation(ResourceLocation pModel, String pLayer) {
        this.model = pModel;
        this.layer = pLayer;
    }

    public ResourceLocation getModel() {
        return this.model;
    }

    public String getLayer() {
        return this.layer;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return !(pOther instanceof ModelLayerLocation modellayerlocation)
                ? false
                : this.model.equals(modellayerlocation.model) && this.layer.equals(modellayerlocation.layer);
        }
    }

    @Override
    public int hashCode() {
        int i = this.model.hashCode();
        return 31 * i + this.layer.hashCode();
    }

    @Override
    public String toString() {
        return this.model + "#" + this.layer;
    }
}
