package com.mojang.blaze3d.audio;

import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL10;

/**
 * The Listener class represents the listener in a 3D audio environment.
 *
 * The listener's position and orientation determine how sounds are perceived by the listener.
 */
@OnlyIn(Dist.CLIENT)
public class Listener {
    private float gain = 1.0F;
    private ListenerTransform transform = ListenerTransform.INITIAL;

    public void setTransform(ListenerTransform pTransform) {
        this.transform = pTransform;
        Vec3 vec3 = pTransform.position();
        Vec3 vec31 = pTransform.forward();
        Vec3 vec32 = pTransform.up();
        AL10.alListener3f(4100, (float)vec3.x, (float)vec3.y, (float)vec3.z);
        AL10.alListenerfv(4111, new float[]{(float)vec31.x, (float)vec31.y, (float)vec31.z, (float)vec32.x(), (float)vec32.y(), (float)vec32.z()});
    }

    /**
     * Sets the listener's gain.
     *
     * @param pGain The gain to set for the listener.
     */
    public void setGain(float pGain) {
        AL10.alListenerf(4106, pGain);
        this.gain = pGain;
    }

    public float getGain() {
        return this.gain;
    }

    public void reset() {
        this.setTransform(ListenerTransform.INITIAL);
    }

    public ListenerTransform getTransform() {
        return this.transform;
    }
}
