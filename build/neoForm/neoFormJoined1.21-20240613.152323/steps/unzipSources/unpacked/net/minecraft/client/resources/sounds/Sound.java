package net.minecraft.client.resources.sounds;

import javax.annotation.Nullable;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.SampledFloat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Sound implements Weighted<Sound> {
    public static final FileToIdConverter SOUND_LISTER = new FileToIdConverter("sounds", ".ogg");
    private final ResourceLocation location;
    private final SampledFloat volume;
    private final SampledFloat pitch;
    private final int weight;
    private final Sound.Type type;
    private final boolean stream;
    private final boolean preload;
    private final int attenuationDistance;

    public Sound(
        ResourceLocation p_350972_,
        SampledFloat p_235135_,
        SampledFloat p_235136_,
        int p_235137_,
        Sound.Type p_235138_,
        boolean p_235139_,
        boolean p_235140_,
        int p_235141_
    ) {
        this.location = p_350972_;
        this.volume = p_235135_;
        this.pitch = p_235136_;
        this.weight = p_235137_;
        this.type = p_235138_;
        this.stream = p_235139_;
        this.preload = p_235140_;
        this.attenuationDistance = p_235141_;
    }

    public ResourceLocation getLocation() {
        return this.location;
    }

    public ResourceLocation getPath() {
        return SOUND_LISTER.idToFile(this.location);
    }

    public SampledFloat getVolume() {
        return this.volume;
    }

    public SampledFloat getPitch() {
        return this.pitch;
    }

    @Override
    public int getWeight() {
        return this.weight;
    }

    /**
     * Retrieves the sound associated with the element.
     * The sound is obtained using the provided random source.
     * <p>
     * @return The sound associated with the element
     *
     * @param pRandomSource the random source used for sound selection
     */
    public Sound getSound(RandomSource pRandomSource) {
        return this;
    }

    /**
     * Preloads the sound if required by the sound engine.
     * This method is called to preload the sound associated with the element into the sound engine, ensuring it is ready for playback.
     *
     * @param pEngine the sound engine used for sound preloading
     */
    @Override
    public void preloadIfRequired(SoundEngine pEngine) {
        if (this.preload) {
            pEngine.requestPreload(this);
        }
    }

    public Sound.Type getType() {
        return this.type;
    }

    public boolean shouldStream() {
        return this.stream;
    }

    public boolean shouldPreload() {
        return this.preload;
    }

    public int getAttenuationDistance() {
        return this.attenuationDistance;
    }

    @Override
    public String toString() {
        return "Sound[" + this.location + "]";
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Type {
        FILE("file"),
        SOUND_EVENT("event");

        private final String name;

        private Type(String pName) {
            this.name = pName;
        }

        @Nullable
        public static Sound.Type getByName(String pName) {
            for (Sound.Type sound$type : values()) {
                if (sound$type.name.equals(pName)) {
                    return sound$type;
                }
            }

            return null;
        }
    }
}
