package net.minecraft.client.sounds;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.audio.ListenerTransform;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.MultipliedFloats;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

/**
 * The SoundManager class is responsible for managing sound events and playing sounds.
 * It handles sound event registrations, caching of sound resources, and sound playback.
 */
@OnlyIn(Dist.CLIENT)
public class SoundManager extends SimplePreparableReloadListener<SoundManager.Preparations> {
    public static final ResourceLocation EMPTY_SOUND_LOCATION = ResourceLocation.withDefaultNamespace("empty");
    public static final Sound EMPTY_SOUND = new Sound(
        EMPTY_SOUND_LOCATION, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, false, false, 16
    );
    public static final ResourceLocation INTENTIONALLY_EMPTY_SOUND_LOCATION = ResourceLocation.withDefaultNamespace("intentionally_empty");
    public static final WeighedSoundEvents INTENTIONALLY_EMPTY_SOUND_EVENT = new WeighedSoundEvents(INTENTIONALLY_EMPTY_SOUND_LOCATION, null);
    public static final Sound INTENTIONALLY_EMPTY_SOUND = new Sound(
        INTENTIONALLY_EMPTY_SOUND_LOCATION, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, false, false, 16
    );
    static final Logger LOGGER = LogUtils.getLogger();
    private static final String SOUNDS_PATH = "sounds.json";
    private static final Gson GSON = new GsonBuilder()
        .registerTypeHierarchyAdapter(Component.class, new Component.SerializerAdapter(RegistryAccess.EMPTY))
        .registerTypeAdapter(SoundEventRegistration.class, new SoundEventRegistrationSerializer())
        .create();
    private static final TypeToken<Map<String, SoundEventRegistration>> SOUND_EVENT_REGISTRATION_TYPE = new TypeToken<Map<String, SoundEventRegistration>>() {
    };
    private final Map<ResourceLocation, WeighedSoundEvents> registry = Maps.newHashMap();
    private final SoundEngine soundEngine;
    private final Map<ResourceLocation, Resource> soundCache = new HashMap<>();

    public SoundManager(Options pOptions) {
        this.soundEngine = new SoundEngine(this, pOptions, ResourceProvider.fromMap(this.soundCache));
    }

    /**
     * Performs any reloading that can be done off-thread, such as file IO
     *
     * @param pResourceManager The resource manager in-charge of loading sound files
     * @param pProfiler        The sound profiler
     */
    protected SoundManager.Preparations prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        SoundManager.Preparations soundmanager$preparations = new SoundManager.Preparations();
        pProfiler.startTick();
        pProfiler.push("list");
        soundmanager$preparations.listResources(pResourceManager);
        pProfiler.pop();

        for (String s : pResourceManager.getNamespaces()) {
            pProfiler.push(s);

            try {
                for (Resource resource : pResourceManager.getResourceStack(ResourceLocation.fromNamespaceAndPath(s, "sounds.json"))) {
                    pProfiler.push(resource.sourcePackId());

                    try (Reader reader = resource.openAsReader()) {
                        pProfiler.push("parse");
                        Map<String, SoundEventRegistration> map = GsonHelper.fromJson(GSON, reader, SOUND_EVENT_REGISTRATION_TYPE);
                        pProfiler.popPush("register");

                        for (Entry<String, SoundEventRegistration> entry : map.entrySet()) {
                            soundmanager$preparations.handleRegistration(ResourceLocation.fromNamespaceAndPath(s, entry.getKey()), entry.getValue());
                        }

                        pProfiler.pop();
                    } catch (RuntimeException runtimeexception) {
                        LOGGER.warn("Invalid {} in resourcepack: '{}'", "sounds.json", resource.sourcePackId(), runtimeexception);
                    }

                    pProfiler.pop();
                }
            } catch (IOException ioexception) {
            }

            pProfiler.pop();
        }

        pProfiler.endTick();
        return soundmanager$preparations;
    }

    /**
     * Applies the prepared sound event registrations and caches to the sound manager.
     *
     * @param pObject          The prepared sound event registrations and caches
     * @param pResourceManager The resource manager
     * @param pProfiler        The profiler
     */
    protected void apply(SoundManager.Preparations pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        pObject.apply(this.registry, this.soundCache, this.soundEngine);
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            for (ResourceLocation resourcelocation : this.registry.keySet()) {
                WeighedSoundEvents weighedsoundevents = this.registry.get(resourcelocation);
                if (!ComponentUtils.isTranslationResolvable(weighedsoundevents.getSubtitle()) && BuiltInRegistries.SOUND_EVENT.containsKey(resourcelocation)) {
                    LOGGER.error("Missing subtitle {} for sound event: {}", weighedsoundevents.getSubtitle(), resourcelocation);
                }
            }
        }

        if (LOGGER.isDebugEnabled()) {
            for (ResourceLocation resourcelocation1 : this.registry.keySet()) {
                if (!BuiltInRegistries.SOUND_EVENT.containsKey(resourcelocation1)) {
                    LOGGER.debug("Not having sound event for: {}", resourcelocation1);
                }
            }
        }

        this.soundEngine.reload();
    }

    public List<String> getAvailableSoundDevices() {
        return this.soundEngine.getAvailableSoundDevices();
    }

    public ListenerTransform getListenerTransform() {
        return this.soundEngine.getListenerTransform();
    }

    /**
     * Validates a sound resource
     * <p>
     * @return {@code true} if the sound resource is valid, {@code false} otherwise
     *
     * @param pSound            The sound to validate
     * @param pLocation         The location of the sound event
     * @param pResourceProvider The resource provider
     */
    static boolean validateSoundResource(Sound pSound, ResourceLocation pLocation, ResourceProvider pResourceProvider) {
        ResourceLocation resourcelocation = pSound.getPath();
        if (pResourceProvider.getResource(resourcelocation).isEmpty()) {
            LOGGER.warn("File {} does not exist, cannot add it to event {}", resourcelocation, pLocation);
            return false;
        } else {
            return true;
        }
    }

    /**
     * {@return The sound event associated with the specific {@linkplain ResourceLocation}, or {@code null} if not found}
     *
     * @param pLocation The location of the sound event
     */
    @Nullable
    public WeighedSoundEvents getSoundEvent(ResourceLocation pLocation) {
        return this.registry.get(pLocation);
    }

    public Collection<ResourceLocation> getAvailableSounds() {
        return this.registry.keySet();
    }

    /**
     * Queues a ticking sound to be played.
     *
     * @param pTickableSound The ticking sound instance
     */
    public void queueTickingSound(TickableSoundInstance pTickableSound) {
        this.soundEngine.queueTickingSound(pTickableSound);
    }

    /**
     * Play a sound
     */
    public void play(SoundInstance pSound) {
        this.soundEngine.play(pSound);
    }

    /**
     * Plays a sound with a delay in ticks.
     *
     * @param pSound The sound instance to play
     * @param pDelay The delay in ticks before playing the sound
     */
    public void playDelayed(SoundInstance pSound, int pDelay) {
        this.soundEngine.playDelayed(pSound, pDelay);
    }

    /**
     * Updates the sound source position based on the active render info.
     *
     * @param pActiveRenderInfo The active render info
     */
    public void updateSource(Camera pActiveRenderInfo) {
        this.soundEngine.updateSource(pActiveRenderInfo);
    }

    public void pause() {
        this.soundEngine.pause();
    }

    public void stop() {
        this.soundEngine.stopAll();
    }

    public void destroy() {
        this.soundEngine.destroy();
    }

    public void emergencyShutdown() {
        this.soundEngine.emergencyShutdown();
    }

    /**
     * Updates the sound manager's tick state.
     *
     * @param pIsGamePaused {@code true} if the game is paused, {@code false}
     *                      otherwise
     */
    public void tick(boolean pIsGamePaused) {
        this.soundEngine.tick(pIsGamePaused);
    }

    public void resume() {
        this.soundEngine.resume();
    }

    /**
     * Updates the volume of the specified sound source category.
     *
     * @param pCategory The sound source category
     * @param pVolume   The new volume
     */
    public void updateSourceVolume(SoundSource pCategory, float pVolume) {
        if (pCategory == SoundSource.MASTER && pVolume <= 0.0F) {
            this.stop();
        }

        this.soundEngine.updateCategoryVolume(pCategory, pVolume);
    }

    public void stop(SoundInstance pSound) {
        this.soundEngine.stop(pSound);
    }

    /**
     * Checks if the specified sound is active (playing or scheduled to be played).
     * @return {@code true} if the sound is active, {@code false} otherwise
     *
     * @param pSound The sound instance to check
     */
    public boolean isActive(SoundInstance pSound) {
        return this.soundEngine.isActive(pSound);
    }

    public void addListener(SoundEventListener pListener) {
        this.soundEngine.addEventListener(pListener);
    }

    public void removeListener(SoundEventListener pListener) {
        this.soundEngine.removeEventListener(pListener);
    }

    /**
     * Stops all sounds associated with the specified ID and category.
     *
     * @param pId       The ID of the sounds to stop, or null to stop all sounds
     * @param pCategory The category of the sounds to stop, or null to stop sounds
     *                  from all categories
     */
    public void stop(@Nullable ResourceLocation pId, @Nullable SoundSource pCategory) {
        this.soundEngine.stop(pId, pCategory);
    }

    public String getDebugString() {
        return this.soundEngine.getDebugString();
    }

    public void reload() {
        this.soundEngine.reload();
    }

    /**
     * The Preparations class represents the prepared sound event registrations and caches for applying to the sound manager.
     */
    @OnlyIn(Dist.CLIENT)
    protected static class Preparations {
        final Map<ResourceLocation, WeighedSoundEvents> registry = Maps.newHashMap();
        private Map<ResourceLocation, Resource> soundCache = Map.of();

        void listResources(ResourceManager pResourceManager) {
            this.soundCache = Sound.SOUND_LISTER.listMatchingResources(pResourceManager);
        }

        void handleRegistration(ResourceLocation pLocation, SoundEventRegistration pRegistration) {
            WeighedSoundEvents weighedsoundevents = this.registry.get(pLocation);
            boolean flag = weighedsoundevents == null;
            if (flag || pRegistration.isReplace()) {
                if (!flag) {
                    SoundManager.LOGGER.debug("Replaced sound event location {}", pLocation);
                }

                weighedsoundevents = new WeighedSoundEvents(pLocation, pRegistration.getSubtitle());
                this.registry.put(pLocation, weighedsoundevents);
            }

            ResourceProvider resourceprovider = ResourceProvider.fromMap(this.soundCache);

            for (final Sound sound : pRegistration.getSounds()) {
                final ResourceLocation resourcelocation = sound.getLocation();
                Weighted<Sound> weighted;
                switch (sound.getType()) {
                    case FILE:
                        if (!SoundManager.validateSoundResource(sound, pLocation, resourceprovider)) {
                            continue;
                        }

                        weighted = sound;
                        break;
                    case SOUND_EVENT:
                        weighted = new Weighted<Sound>() {
                            @Override
                            public int getWeight() {
                                WeighedSoundEvents weighedsoundevents1 = Preparations.this.registry.get(resourcelocation);
                                return weighedsoundevents1 == null ? 0 : weighedsoundevents1.getWeight();
                            }

                            public Sound getSound(RandomSource p_235261_) {
                                WeighedSoundEvents weighedsoundevents1 = Preparations.this.registry.get(resourcelocation);
                                if (weighedsoundevents1 == null) {
                                    return SoundManager.EMPTY_SOUND;
                                } else {
                                    Sound sound1 = weighedsoundevents1.getSound(p_235261_);
                                    return new Sound(
                                        sound1.getLocation(),
                                        new MultipliedFloats(sound1.getVolume(), sound.getVolume()),
                                        new MultipliedFloats(sound1.getPitch(), sound.getPitch()),
                                        sound.getWeight(),
                                        Sound.Type.FILE,
                                        sound1.shouldStream() || sound.shouldStream(),
                                        sound1.shouldPreload(),
                                        sound1.getAttenuationDistance()
                                    );
                                }
                            }

                            @Override
                            public void preloadIfRequired(SoundEngine p_120438_) {
                                WeighedSoundEvents weighedsoundevents1 = Preparations.this.registry.get(resourcelocation);
                                if (weighedsoundevents1 != null) {
                                    weighedsoundevents1.preloadIfRequired(p_120438_);
                                }
                            }
                        };
                        break;
                    default:
                        throw new IllegalStateException("Unknown SoundEventRegistration type: " + sound.getType());
                }

                weighedsoundevents.addSound(weighted);
            }
        }

        /**
         * Applies the prepared sound event registrations and caches to the sound manager.
         *
         * @param pSoundRegistry The sound registry to apply to
         * @param pCache         The sound cache to apply to
         * @param pSoundEngine   The sound engine to apply to
         */
        public void apply(Map<ResourceLocation, WeighedSoundEvents> pSoundRegistry, Map<ResourceLocation, Resource> pCache, SoundEngine pSoundEngine) {
            pSoundRegistry.clear();
            pCache.clear();
            pCache.putAll(this.soundCache);

            for (Entry<ResourceLocation, WeighedSoundEvents> entry : this.registry.entrySet()) {
                pSoundRegistry.put(entry.getKey(), entry.getValue());
                entry.getValue().preloadIfRequired(pSoundEngine);
            }
        }
    }
}
