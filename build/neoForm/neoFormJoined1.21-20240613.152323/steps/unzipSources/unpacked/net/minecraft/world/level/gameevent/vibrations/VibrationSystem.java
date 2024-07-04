package net.minecraft.world.level.gameevent.vibrations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public interface VibrationSystem {
    List<ResourceKey<GameEvent>> RESONANCE_EVENTS = List.of(
        GameEvent.RESONATE_1.key(),
        GameEvent.RESONATE_2.key(),
        GameEvent.RESONATE_3.key(),
        GameEvent.RESONATE_4.key(),
        GameEvent.RESONATE_5.key(),
        GameEvent.RESONATE_6.key(),
        GameEvent.RESONATE_7.key(),
        GameEvent.RESONATE_8.key(),
        GameEvent.RESONATE_9.key(),
        GameEvent.RESONATE_10.key(),
        GameEvent.RESONATE_11.key(),
        GameEvent.RESONATE_12.key(),
        GameEvent.RESONATE_13.key(),
        GameEvent.RESONATE_14.key(),
        GameEvent.RESONATE_15.key()
    );
    int DEFAULT_VIBRATION_FREQUENCY = 0;
    /** @deprecated Neo: use the {@link net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps#VIBRATION_FREQUENCIES data map} instead. */
    @Deprecated
    ToIntFunction<ResourceKey<GameEvent>> VIBRATION_FREQUENCY_FOR_EVENT = Util.make(new Reference2IntOpenHashMap<>(), p_316653_ -> {
        p_316653_.defaultReturnValue(0);
        p_316653_.put(GameEvent.STEP.key(), 1);
        p_316653_.put(GameEvent.SWIM.key(), 1);
        p_316653_.put(GameEvent.FLAP.key(), 1);
        p_316653_.put(GameEvent.PROJECTILE_LAND.key(), 2);
        p_316653_.put(GameEvent.HIT_GROUND.key(), 2);
        p_316653_.put(GameEvent.SPLASH.key(), 2);
        p_316653_.put(GameEvent.ITEM_INTERACT_FINISH.key(), 3);
        p_316653_.put(GameEvent.PROJECTILE_SHOOT.key(), 3);
        p_316653_.put(GameEvent.INSTRUMENT_PLAY.key(), 3);
        p_316653_.put(GameEvent.ENTITY_ACTION.key(), 4);
        p_316653_.put(GameEvent.ELYTRA_GLIDE.key(), 4);
        p_316653_.put(GameEvent.UNEQUIP.key(), 4);
        p_316653_.put(GameEvent.ENTITY_DISMOUNT.key(), 5);
        p_316653_.put(GameEvent.EQUIP.key(), 5);
        p_316653_.put(GameEvent.ENTITY_INTERACT.key(), 6);
        p_316653_.put(GameEvent.SHEAR.key(), 6);
        p_316653_.put(GameEvent.ENTITY_MOUNT.key(), 6);
        p_316653_.put(GameEvent.ENTITY_DAMAGE.key(), 7);
        p_316653_.put(GameEvent.DRINK.key(), 8);
        p_316653_.put(GameEvent.EAT.key(), 8);
        p_316653_.put(GameEvent.CONTAINER_CLOSE.key(), 9);
        p_316653_.put(GameEvent.BLOCK_CLOSE.key(), 9);
        p_316653_.put(GameEvent.BLOCK_DEACTIVATE.key(), 9);
        p_316653_.put(GameEvent.BLOCK_DETACH.key(), 9);
        p_316653_.put(GameEvent.CONTAINER_OPEN.key(), 10);
        p_316653_.put(GameEvent.BLOCK_OPEN.key(), 10);
        p_316653_.put(GameEvent.BLOCK_ACTIVATE.key(), 10);
        p_316653_.put(GameEvent.BLOCK_ATTACH.key(), 10);
        p_316653_.put(GameEvent.PRIME_FUSE.key(), 10);
        p_316653_.put(GameEvent.NOTE_BLOCK_PLAY.key(), 10);
        p_316653_.put(GameEvent.BLOCK_CHANGE.key(), 11);
        p_316653_.put(GameEvent.BLOCK_DESTROY.key(), 12);
        p_316653_.put(GameEvent.FLUID_PICKUP.key(), 12);
        p_316653_.put(GameEvent.BLOCK_PLACE.key(), 13);
        p_316653_.put(GameEvent.FLUID_PLACE.key(), 13);
        p_316653_.put(GameEvent.ENTITY_PLACE.key(), 14);
        p_316653_.put(GameEvent.LIGHTNING_STRIKE.key(), 14);
        p_316653_.put(GameEvent.TELEPORT.key(), 14);
        p_316653_.put(GameEvent.ENTITY_DIE.key(), 15);
        p_316653_.put(GameEvent.EXPLODE.key(), 15);

        for (int i = 1; i <= 15; i++) {
            p_316653_.put(getResonanceEventByFrequency(i), i);
        }
    });

    VibrationSystem.Data getVibrationData();

    VibrationSystem.User getVibrationUser();

    static int getGameEventFrequency(Holder<GameEvent> pGameEvent) {
        var data = pGameEvent.getData(net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps.VIBRATION_FREQUENCIES);
        return data != null ? data.frequency() : 0;
    }

    static int getGameEventFrequency(ResourceKey<GameEvent> p_316800_) {
        var holder = net.minecraft.core.registries.BuiltInRegistries.GAME_EVENT.getHolder(p_316800_);
        return holder.map(VibrationSystem::getGameEventFrequency).orElse(0);
    }

    static ResourceKey<GameEvent> getResonanceEventByFrequency(int pFrequency) {
        return RESONANCE_EVENTS.get(pFrequency - 1);
    }

    static int getRedstoneStrengthForDistance(float pDistance, int pMaxDistance) {
        double d0 = 15.0 / (double)pMaxDistance;
        return Math.max(1, 15 - Mth.floor(d0 * (double)pDistance));
    }

    public static final class Data {
        public static Codec<VibrationSystem.Data> CODEC = RecordCodecBuilder.create(
            p_338090_ -> p_338090_.group(
                        VibrationInfo.CODEC.lenientOptionalFieldOf("event").forGetter(p_281665_ -> Optional.ofNullable(p_281665_.currentVibration)),
                        VibrationSelector.CODEC.fieldOf("selector").forGetter(VibrationSystem.Data::getSelectionStrategy),
                        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("event_delay").orElse(0).forGetter(VibrationSystem.Data::getTravelTimeInTicks)
                    )
                    .apply(p_338090_, (p_281934_, p_282381_, p_282931_) -> new VibrationSystem.Data(p_281934_.orElse(null), p_282381_, p_282931_, true))
        );
        public static final String NBT_TAG_KEY = "listener";
        @Nullable
        VibrationInfo currentVibration;
        private int travelTimeInTicks;
        final VibrationSelector selectionStrategy;
        private boolean reloadVibrationParticle;

        private Data(@Nullable VibrationInfo pCurrentVibration, VibrationSelector pSelectionStrategy, int pTravelTimeInTicks, boolean pReloadVibrationParticle) {
            this.currentVibration = pCurrentVibration;
            this.travelTimeInTicks = pTravelTimeInTicks;
            this.selectionStrategy = pSelectionStrategy;
            this.reloadVibrationParticle = pReloadVibrationParticle;
        }

        public Data() {
            this(null, new VibrationSelector(), 0, false);
        }

        public VibrationSelector getSelectionStrategy() {
            return this.selectionStrategy;
        }

        @Nullable
        public VibrationInfo getCurrentVibration() {
            return this.currentVibration;
        }

        public void setCurrentVibration(@Nullable VibrationInfo pCurrentVibration) {
            this.currentVibration = pCurrentVibration;
        }

        public int getTravelTimeInTicks() {
            return this.travelTimeInTicks;
        }

        public void setTravelTimeInTicks(int pTravelTimeInTicks) {
            this.travelTimeInTicks = pTravelTimeInTicks;
        }

        public void decrementTravelTime() {
            this.travelTimeInTicks = Math.max(0, this.travelTimeInTicks - 1);
        }

        public boolean shouldReloadVibrationParticle() {
            return this.reloadVibrationParticle;
        }

        public void setReloadVibrationParticle(boolean pReloadVibrationParticle) {
            this.reloadVibrationParticle = pReloadVibrationParticle;
        }
    }

    public static class Listener implements GameEventListener {
        private final VibrationSystem system;

        public Listener(VibrationSystem pSystem) {
            this.system = pSystem;
        }

        @Override
        public PositionSource getListenerSource() {
            return this.system.getVibrationUser().getPositionSource();
        }

        @Override
        public int getListenerRadius() {
            return this.system.getVibrationUser().getListenerRadius();
        }

        @Override
        public boolean handleGameEvent(ServerLevel pLevel, Holder<GameEvent> pGameEvent, GameEvent.Context pContext, Vec3 pPos) {
            VibrationSystem.Data vibrationsystem$data = this.system.getVibrationData();
            VibrationSystem.User vibrationsystem$user = this.system.getVibrationUser();
            if (vibrationsystem$data.getCurrentVibration() != null) {
                return false;
            } else if (!vibrationsystem$user.isValidVibration(pGameEvent, pContext)) {
                return false;
            } else {
                Optional<Vec3> optional = vibrationsystem$user.getPositionSource().getPosition(pLevel);
                if (optional.isEmpty()) {
                    return false;
                } else {
                    Vec3 vec3 = optional.get();
                    if (!vibrationsystem$user.canReceiveVibration(pLevel, BlockPos.containing(pPos), pGameEvent, pContext)) {
                        return false;
                    } else if (isOccluded(pLevel, pPos, vec3)) {
                        return false;
                    } else {
                        this.scheduleVibration(pLevel, vibrationsystem$data, pGameEvent, pContext, pPos, vec3);
                        return true;
                    }
                }
            }
        }

        public void forceScheduleVibration(ServerLevel pLevel, Holder<GameEvent> pGameEvent, GameEvent.Context pContext, Vec3 pPos) {
            this.system
                .getVibrationUser()
                .getPositionSource()
                .getPosition(pLevel)
                .ifPresent(p_316103_ -> this.scheduleVibration(pLevel, this.system.getVibrationData(), pGameEvent, pContext, pPos, p_316103_));
        }

        private void scheduleVibration(
            ServerLevel pLevel, VibrationSystem.Data pData, Holder<GameEvent> pGameEvent, GameEvent.Context pContext, Vec3 pPos, Vec3 pSensorPos
        ) {
            pData.selectionStrategy
                .addCandidate(
                    new VibrationInfo(pGameEvent, (float)pPos.distanceTo(pSensorPos), pPos, pContext.sourceEntity()), pLevel.getGameTime()
                );
        }

        public static float distanceBetweenInBlocks(BlockPos pPos1, BlockPos pPos2) {
            return (float)Math.sqrt(pPos1.distSqr(pPos2));
        }

        private static boolean isOccluded(Level pLevel, Vec3 pEventPos, Vec3 pVibrationUserPos) {
            Vec3 vec3 = new Vec3((double)Mth.floor(pEventPos.x) + 0.5, (double)Mth.floor(pEventPos.y) + 0.5, (double)Mth.floor(pEventPos.z) + 0.5);
            Vec3 vec31 = new Vec3((double)Mth.floor(pVibrationUserPos.x) + 0.5, (double)Mth.floor(pVibrationUserPos.y) + 0.5, (double)Mth.floor(pVibrationUserPos.z) + 0.5);

            for (Direction direction : Direction.values()) {
                Vec3 vec32 = vec3.relative(direction, 1.0E-5F);
                if (pLevel.isBlockInLine(new ClipBlockStateContext(vec32, vec31, p_283608_ -> p_283608_.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS))).getType()
                    != HitResult.Type.BLOCK) {
                    return false;
                }
            }

            return true;
        }
    }

    public interface Ticker {
        static void tick(Level pLevel, VibrationSystem.Data pData, VibrationSystem.User pUser) {
            if (pLevel instanceof ServerLevel serverlevel) {
                if (pData.currentVibration == null) {
                    trySelectAndScheduleVibration(serverlevel, pData, pUser);
                }

                if (pData.currentVibration != null) {
                    boolean flag = pData.getTravelTimeInTicks() > 0;
                    tryReloadVibrationParticle(serverlevel, pData, pUser);
                    pData.decrementTravelTime();
                    if (pData.getTravelTimeInTicks() <= 0) {
                        flag = receiveVibration(serverlevel, pData, pUser, pData.currentVibration);
                    }

                    if (flag) {
                        pUser.onDataChanged();
                    }
                }
            }
        }

        private static void trySelectAndScheduleVibration(ServerLevel pLevel, VibrationSystem.Data pData, VibrationSystem.User pUser) {
            pData.getSelectionStrategy()
                .chosenCandidate(pLevel.getGameTime())
                .ifPresent(
                    p_282059_ -> {
                        pData.setCurrentVibration(p_282059_);
                        Vec3 vec3 = p_282059_.pos();
                        pData.setTravelTimeInTicks(pUser.calculateTravelTimeInTicks(p_282059_.distance()));
                        pLevel.sendParticles(
                            new VibrationParticleOption(pUser.getPositionSource(), pData.getTravelTimeInTicks()),
                            vec3.x,
                            vec3.y,
                            vec3.z,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0
                        );
                        pUser.onDataChanged();
                        pData.getSelectionStrategy().startOver();
                    }
                );
        }

        private static void tryReloadVibrationParticle(ServerLevel pLevel, VibrationSystem.Data pData, VibrationSystem.User pUser) {
            if (pData.shouldReloadVibrationParticle()) {
                if (pData.currentVibration == null) {
                    pData.setReloadVibrationParticle(false);
                } else {
                    Vec3 vec3 = pData.currentVibration.pos();
                    PositionSource positionsource = pUser.getPositionSource();
                    Vec3 vec31 = positionsource.getPosition(pLevel).orElse(vec3);
                    int i = pData.getTravelTimeInTicks();
                    int j = pUser.calculateTravelTimeInTicks(pData.currentVibration.distance());
                    double d0 = 1.0 - (double)i / (double)j;
                    double d1 = Mth.lerp(d0, vec3.x, vec31.x);
                    double d2 = Mth.lerp(d0, vec3.y, vec31.y);
                    double d3 = Mth.lerp(d0, vec3.z, vec31.z);
                    boolean flag = pLevel.sendParticles(new VibrationParticleOption(positionsource, i), d1, d2, d3, 1, 0.0, 0.0, 0.0, 0.0) > 0;
                    if (flag) {
                        pData.setReloadVibrationParticle(false);
                    }
                }
            }
        }

        private static boolean receiveVibration(ServerLevel pLevel, VibrationSystem.Data pData, VibrationSystem.User pUser, VibrationInfo pVibrationInfo) {
            BlockPos blockpos = BlockPos.containing(pVibrationInfo.pos());
            BlockPos blockpos1 = pUser.getPositionSource().getPosition(pLevel).map(BlockPos::containing).orElse(blockpos);
            if (pUser.requiresAdjacentChunksToBeTicking() && !areAdjacentChunksTicking(pLevel, blockpos1)) {
                return false;
            } else {
                pUser.onReceiveVibration(
                    pLevel,
                    blockpos,
                    pVibrationInfo.gameEvent(),
                    pVibrationInfo.getEntity(pLevel).orElse(null),
                    pVibrationInfo.getProjectileOwner(pLevel).orElse(null),
                    VibrationSystem.Listener.distanceBetweenInBlocks(blockpos, blockpos1)
                );
                pData.setCurrentVibration(null);
                return true;
            }
        }

        private static boolean areAdjacentChunksTicking(Level pLevel, BlockPos pPos) {
            ChunkPos chunkpos = new ChunkPos(pPos);

            for (int i = chunkpos.x - 1; i <= chunkpos.x + 1; i++) {
                for (int j = chunkpos.z - 1; j <= chunkpos.z + 1; j++) {
                    if (!pLevel.shouldTickBlocksAt(ChunkPos.asLong(i, j)) || pLevel.getChunkSource().getChunkNow(i, j) == null) {
                        return false;
                    }
                }
            }

            return true;
        }
    }

    public interface User {
        int getListenerRadius();

        PositionSource getPositionSource();

        boolean canReceiveVibration(ServerLevel pLevel, BlockPos pPos, Holder<GameEvent> pGameEvent, GameEvent.Context pContext);

        void onReceiveVibration(
            ServerLevel pLevel, BlockPos pPos, Holder<GameEvent> pGameEvent, @Nullable Entity pEntity, @Nullable Entity pPlayerEntity, float pDistance
        );

        default TagKey<GameEvent> getListenableEvents() {
            return GameEventTags.VIBRATIONS;
        }

        default boolean canTriggerAvoidVibration() {
            return false;
        }

        default boolean requiresAdjacentChunksToBeTicking() {
            return false;
        }

        default int calculateTravelTimeInTicks(float pDistance) {
            return Mth.floor(pDistance);
        }

        default boolean isValidVibration(Holder<GameEvent> pGameEvent, GameEvent.Context pContext) {
            if (!pGameEvent.is(this.getListenableEvents())) {
                return false;
            } else {
                Entity entity = pContext.sourceEntity();
                if (entity != null) {
                    if (entity.isSpectator()) {
                        return false;
                    }

                    if (entity.isSteppingCarefully() && pGameEvent.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) {
                        if (this.canTriggerAvoidVibration() && entity instanceof ServerPlayer serverplayer) {
                            CriteriaTriggers.AVOID_VIBRATION.trigger(serverplayer);
                        }

                        return false;
                    }

                    if (entity.dampensVibrations()) {
                        return false;
                    }
                }

                return pContext.affectedState() != null ? !pContext.affectedState().is(BlockTags.DAMPENS_VIBRATIONS) : true;
            }
        }

        default void onDataChanged() {
        }
    }
}
