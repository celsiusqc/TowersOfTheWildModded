package net.minecraft.client.renderer.texture;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpriteTicker extends AutoCloseable {
    void tickAndUpload(int pX, int pY);

    @Override
    void close();
}
