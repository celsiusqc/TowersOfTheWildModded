package net.minecraft.world.effect;

import com.google.common.collect.ComparisonChain;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

public class MobEffectInstance implements Comparable<MobEffectInstance> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int INFINITE_DURATION = -1;
    public static final int MIN_AMPLIFIER = 0;
    public static final int MAX_AMPLIFIER = 255;
    public static final Codec<MobEffectInstance> CODEC = RecordCodecBuilder.create(
        p_348152_ -> p_348152_.group(
                    MobEffect.CODEC.fieldOf("id").forGetter(MobEffectInstance::getEffect),
                    MobEffectInstance.Details.MAP_CODEC.forGetter(MobEffectInstance::asDetails)
                )
                .apply(p_348152_, MobEffectInstance::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MobEffectInstance> STREAM_CODEC = StreamCodec.composite(
        MobEffect.STREAM_CODEC, MobEffectInstance::getEffect, MobEffectInstance.Details.STREAM_CODEC, MobEffectInstance::asDetails, MobEffectInstance::new
    );
    private final Holder<MobEffect> effect;
    private int duration;
    private int amplifier;
    private boolean ambient;
    private boolean visible;
    private boolean showIcon;
    /**
     * A hidden effect which is not shown to the player.
     */
    @Nullable
    private MobEffectInstance hiddenEffect;
    private final MobEffectInstance.BlendState blendState = new MobEffectInstance.BlendState();

    public MobEffectInstance(Holder<MobEffect> pEffect) {
        this(pEffect, 0, 0);
    }

    public MobEffectInstance(Holder<MobEffect> pEffect, int pDuration) {
        this(pEffect, pDuration, 0);
    }

    public MobEffectInstance(Holder<MobEffect> pEffect, int pDuration, int pAmplifier) {
        this(pEffect, pDuration, pAmplifier, false, true);
    }

    public MobEffectInstance(Holder<MobEffect> pEffect, int pDuration, int pAmplifier, boolean pAmbient, boolean pVisible) {
        this(pEffect, pDuration, pAmplifier, pAmbient, pVisible, pVisible);
    }

    public MobEffectInstance(Holder<MobEffect> pEffect, int pDuration, int pAmplifier, boolean pAmbient, boolean pVisible, boolean pShowIcon) {
        this(pEffect, pDuration, pAmplifier, pAmbient, pVisible, pShowIcon, null);
    }

    public MobEffectInstance(
        Holder<MobEffect> pEffect, int pDuration, int pAmplifier, boolean pAmbient, boolean pVisible, boolean pShowIcon, @Nullable MobEffectInstance pHiddenEffect
    ) {
        this.effect = pEffect;
        this.duration = pDuration;
        this.amplifier = Mth.clamp(pAmplifier, 0, 255);
        this.ambient = pAmbient;
        this.visible = pVisible;
        this.showIcon = pShowIcon;
        this.hiddenEffect = pHiddenEffect;
        this.effect.value().fillEffectCures(this.cures, this);
    }

    public MobEffectInstance(MobEffectInstance pOther) {
        this.effect = pOther.effect;
        this.setDetailsFrom(pOther);
    }

    private MobEffectInstance(Holder<MobEffect> pEffect, MobEffectInstance.Details p_324529_) {
        this(
            pEffect,
            p_324529_.duration(),
            p_324529_.amplifier(),
            p_324529_.ambient(),
            p_324529_.showParticles(),
            p_324529_.showIcon(),
            p_324529_.hiddenEffect().map(p_323227_ -> new MobEffectInstance(pEffect, p_323227_)).orElse(null)
        );
        this.cures.clear();
        p_324529_.cures().ifPresent(this.cures::addAll);
    }

    private MobEffectInstance.Details asDetails() {
        return new MobEffectInstance.Details(
            this.getAmplifier(),
            this.getDuration(),
            this.isAmbient(),
            this.isVisible(),
            this.showIcon(),
            Optional.ofNullable(this.hiddenEffect).map(MobEffectInstance::asDetails),
            Optional.of(this.getCures()).filter(cures -> !cures.isEmpty())
        );
    }

    public float getBlendFactor(LivingEntity pEntity, float pDelta) {
        return this.blendState.getFactor(pEntity, pDelta);
    }

    public ParticleOptions getParticleOptions() {
        return this.effect.value().createParticleOptions(this);
    }

    void setDetailsFrom(MobEffectInstance pEffectInstance) {
        this.duration = pEffectInstance.duration;
        this.amplifier = pEffectInstance.amplifier;
        this.ambient = pEffectInstance.ambient;
        this.visible = pEffectInstance.visible;
        this.showIcon = pEffectInstance.showIcon;
        this.cures.clear();
        this.cures.addAll(pEffectInstance.cures);
    }

    public boolean update(MobEffectInstance pOther) {
        if (!this.effect.equals(pOther.effect)) {
            LOGGER.warn("This method should only be called for matching effects!");
        }

        boolean flag = false;
        if (pOther.amplifier > this.amplifier) {
            if (pOther.isShorterDurationThan(this)) {
                MobEffectInstance mobeffectinstance = this.hiddenEffect;
                this.hiddenEffect = new MobEffectInstance(this);
                this.hiddenEffect.hiddenEffect = mobeffectinstance;
            }

            this.amplifier = pOther.amplifier;
            this.duration = pOther.duration;
            flag = true;
        } else if (this.isShorterDurationThan(pOther)) {
            if (pOther.amplifier == this.amplifier) {
                this.duration = pOther.duration;
                flag = true;
            } else if (this.hiddenEffect == null) {
                this.hiddenEffect = new MobEffectInstance(pOther);
            } else {
                this.hiddenEffect.update(pOther);
            }
        }

        if (!pOther.ambient && this.ambient || flag) {
            this.ambient = pOther.ambient;
            flag = true;
        }

        if (pOther.visible != this.visible) {
            this.visible = pOther.visible;
            flag = true;
        }

        if (pOther.showIcon != this.showIcon) {
            this.showIcon = pOther.showIcon;
            flag = true;
        }

        return flag;
    }

    private boolean isShorterDurationThan(MobEffectInstance pOther) {
        return !this.isInfiniteDuration() && (this.duration < pOther.duration || pOther.isInfiniteDuration());
    }

    public boolean isInfiniteDuration() {
        return this.duration == -1;
    }

    public boolean endsWithin(int pDuration) {
        return !this.isInfiniteDuration() && this.duration <= pDuration;
    }

    public int mapDuration(Int2IntFunction pMapper) {
        return !this.isInfiniteDuration() && this.duration != 0 ? pMapper.applyAsInt(this.duration) : this.duration;
    }

    public Holder<MobEffect> getEffect() {
        return this.effect;
    }

    public int getDuration() {
        return this.duration;
    }

    public int getAmplifier() {
        return this.amplifier;
    }

    public boolean isAmbient() {
        return this.ambient;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public boolean showIcon() {
        return this.showIcon;
    }

    public boolean tick(LivingEntity pEntity, Runnable pOnExpirationRunnable) {
        if (this.hasRemainingDuration()) {
            int i = this.isInfiniteDuration() ? pEntity.tickCount : this.duration;
            if (this.effect.value().shouldApplyEffectTickThisTick(i, this.amplifier) && !this.effect.value().applyEffectTick(pEntity, this.amplifier)) {
                pEntity.removeEffect(this.effect);
            }

            this.tickDownDuration();
            if (this.duration == 0 && this.hiddenEffect != null) {
                this.setDetailsFrom(this.hiddenEffect);
                this.hiddenEffect = this.hiddenEffect.hiddenEffect;
                pOnExpirationRunnable.run();
            }
        }

        this.blendState.tick(this);
        return this.hasRemainingDuration();
    }

    private boolean hasRemainingDuration() {
        return this.isInfiniteDuration() || this.duration > 0;
    }

    private int tickDownDuration() {
        if (this.hiddenEffect != null) {
            this.hiddenEffect.tickDownDuration();
        }

        return this.duration = this.mapDuration(p_267916_ -> p_267916_ - 1);
    }

    public void onEffectStarted(LivingEntity pEntity) {
        this.effect.value().onEffectStarted(pEntity, this.amplifier);
    }

    public void onMobRemoved(LivingEntity pLivingEntity, Entity.RemovalReason pReason) {
        this.effect.value().onMobRemoved(pLivingEntity, this.amplifier, pReason);
    }

    public void onMobHurt(LivingEntity pLivingEntity, DamageSource pDamageSource, float pAmount) {
        this.effect.value().onMobHurt(pLivingEntity, this.amplifier, pDamageSource, pAmount);
    }

    public String getDescriptionId() {
        return this.effect.value().getDescriptionId();
    }

    @Override
    public String toString() {
        String s;
        if (this.amplifier > 0) {
            s = this.getDescriptionId() + " x " + (this.amplifier + 1) + ", Duration: " + this.describeDuration();
        } else {
            s = this.getDescriptionId() + ", Duration: " + this.describeDuration();
        }

        if (!this.visible) {
            s = s + ", Particles: false";
        }

        if (!this.showIcon) {
            s = s + ", Show Icon: false";
        }

        return s;
    }

    private String describeDuration() {
        return this.isInfiniteDuration() ? "infinite" : Integer.toString(this.duration);
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return !(pOther instanceof MobEffectInstance mobeffectinstance)
                ? false
                : this.duration == mobeffectinstance.duration
                    && this.amplifier == mobeffectinstance.amplifier
                    && this.ambient == mobeffectinstance.ambient
                    && this.visible == mobeffectinstance.visible
                    && this.showIcon == mobeffectinstance.showIcon
                    && this.effect.equals(mobeffectinstance.effect);
        }
    }

    @Override
    public int hashCode() {
        int i = this.effect.hashCode();
        i = 31 * i + this.duration;
        i = 31 * i + this.amplifier;
        i = 31 * i + (this.ambient ? 1 : 0);
        i = 31 * i + (this.visible ? 1 : 0);
        return 31 * i + (this.showIcon ? 1 : 0);
    }

    public Tag save() {
        return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
    }

    /**
     * Read a custom potion effect from a potion item's NBT data.
     */
    @Nullable
    public static MobEffectInstance load(CompoundTag pNbt) {
        return CODEC.parse(NbtOps.INSTANCE, pNbt).resultOrPartial(LOGGER::error).orElse(null);
    }

    public int compareTo(MobEffectInstance pOther) {
        int i = 32147;
        return (this.getDuration() <= 32147 || pOther.getDuration() <= 32147) && (!this.isAmbient() || !pOther.isAmbient())
            ? ComparisonChain.start()
                .compareFalseFirst(this.isAmbient(), pOther.isAmbient())
                .compareFalseFirst(this.isInfiniteDuration(), pOther.isInfiniteDuration())
                .compare(this.getDuration(), pOther.getDuration())
                .compare(this.getEffect().value().getSortOrder(this), pOther.getEffect().value().getSortOrder(pOther))
                .result()
            : ComparisonChain.start()
                .compare(this.isAmbient(), pOther.isAmbient())
                .compare(this.getEffect().value().getSortOrder(this), pOther.getEffect().value().getSortOrder(pOther))
                .result();
    }

    public void onEffectAdded(LivingEntity pLivingEntity) {
        this.effect.value().onEffectAdded(pLivingEntity, this.amplifier);
    }

    public boolean is(Holder<MobEffect> pEffect) {
        return this.effect.equals(pEffect);
    }

    public void copyBlendState(MobEffectInstance pEffectInstance) {
        this.blendState.copyFrom(pEffectInstance.blendState);
    }

    public void skipBlending() {
        this.blendState.setImmediate(this);
    }

    private final java.util.Set<net.neoforged.neoforge.common.EffectCure> cures = com.google.common.collect.Sets.newIdentityHashSet();

    /**
     * {@return the {@link net.neoforged.neoforge.common.EffectCure}s which can cure the {@link MobEffect} held by this {@link MobEffectInstance}}
     */
    public java.util.Set<net.neoforged.neoforge.common.EffectCure> getCures() {
        return cures;
    }

    static class BlendState {
        private float factor;
        private float factorPreviousFrame;

        public void setImmediate(MobEffectInstance pEffectInstance) {
            this.factor = computeTarget(pEffectInstance);
            this.factorPreviousFrame = this.factor;
        }

        public void copyFrom(MobEffectInstance.BlendState pBlendState) {
            this.factor = pBlendState.factor;
            this.factorPreviousFrame = pBlendState.factorPreviousFrame;
        }

        public void tick(MobEffectInstance pEffect) {
            this.factorPreviousFrame = this.factor;
            int i = getBlendDuration(pEffect);
            if (i == 0) {
                this.factor = 1.0F;
            } else {
                float f = computeTarget(pEffect);
                if (this.factor != f) {
                    float f1 = 1.0F / (float)i;
                    this.factor = this.factor + Mth.clamp(f - this.factor, -f1, f1);
                }
            }
        }

        private static float computeTarget(MobEffectInstance pEffect) {
            boolean flag = !pEffect.endsWithin(getBlendDuration(pEffect));
            return flag ? 1.0F : 0.0F;
        }

        private static int getBlendDuration(MobEffectInstance pEffect) {
            return pEffect.getEffect().value().getBlendDurationTicks();
        }

        public float getFactor(LivingEntity pEntity, float pDelta) {
            if (pEntity.isRemoved()) {
                this.factorPreviousFrame = this.factor;
            }

            return Mth.lerp(pDelta, this.factorPreviousFrame, this.factor);
        }
    }

    static record Details(
        int amplifier, int duration, boolean ambient, boolean showParticles, boolean showIcon, Optional<MobEffectInstance.Details> hiddenEffect, Optional<java.util.Set<net.neoforged.neoforge.common.EffectCure>> cures) {
        public static final MapCodec<MobEffectInstance.Details> MAP_CODEC = MapCodec.recursive(
            "MobEffectInstance.Details",
            p_323465_ -> RecordCodecBuilder.mapCodec(
                    p_324063_ -> p_324063_.group(
                                ExtraCodecs.UNSIGNED_BYTE.optionalFieldOf("amplifier", 0).forGetter(MobEffectInstance.Details::amplifier),
                                Codec.INT.optionalFieldOf("duration", Integer.valueOf(0)).forGetter(MobEffectInstance.Details::duration),
                                Codec.BOOL.optionalFieldOf("ambient", Boolean.valueOf(false)).forGetter(MobEffectInstance.Details::ambient),
                                Codec.BOOL.optionalFieldOf("show_particles", Boolean.valueOf(true)).forGetter(MobEffectInstance.Details::showParticles),
                                Codec.BOOL.optionalFieldOf("show_icon").forGetter(p_323788_ -> Optional.of(p_323788_.showIcon())),
                                p_323465_.optionalFieldOf("hidden_effect").forGetter(MobEffectInstance.Details::hiddenEffect)
                                // Neo: Add additional serialization logic for custom EffectCure(s)
                                , net.neoforged.neoforge.common.util.NeoForgeExtraCodecs.setOf(net.neoforged.neoforge.common.EffectCure.CODEC).optionalFieldOf("neoforge:cures").forGetter(MobEffectInstance.Details::cures)
                            )
                            .apply(p_324063_, MobEffectInstance.Details::create)
                )
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, MobEffectInstance.Details> STREAM_CODEC = StreamCodec.recursive(
            p_329990_ -> net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs.composite(
                    ByteBufCodecs.VAR_INT,
                    MobEffectInstance.Details::amplifier,
                    ByteBufCodecs.VAR_INT,
                    MobEffectInstance.Details::duration,
                    ByteBufCodecs.BOOL,
                    MobEffectInstance.Details::ambient,
                    ByteBufCodecs.BOOL,
                    MobEffectInstance.Details::showParticles,
                    ByteBufCodecs.BOOL,
                    MobEffectInstance.Details::showIcon,
                    p_329990_.apply(ByteBufCodecs::optional),
                    MobEffectInstance.Details::hiddenEffect,
                    // Neo: Add additional serialization logic for custom EffectCure(s)
                    net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs.connectionAware(
                            ByteBufCodecs.optional(net.neoforged.neoforge.common.EffectCure.STREAM_CODEC.apply(ByteBufCodecs.collection(java.util.HashSet::new))),
                            net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs.uncheckedUnit(Optional.empty())
                    ),
                    MobEffectInstance.Details::cures,
                    MobEffectInstance.Details::new
                )
        );

        private static MobEffectInstance.Details create(
            int p_323657_, int p_324205_, boolean p_324263_, boolean p_324000_, Optional<Boolean> p_323607_, Optional<MobEffectInstance.Details> p_324604_
        ) {
            return new MobEffectInstance.Details(p_323657_, p_324205_, p_324263_, p_324000_, p_323607_.orElse(p_324000_), p_324604_);
        }

        private static MobEffectInstance.Details create(
            int p_323657_, int p_324205_, boolean p_324263_, boolean p_324000_, Optional<Boolean> p_323607_, Optional<MobEffectInstance.Details> p_324604_, Optional<java.util.Set<net.neoforged.neoforge.common.EffectCure>> cures
        ) {
            return new MobEffectInstance.Details(p_323657_, p_324205_, p_324263_, p_324000_, p_323607_.orElse(p_324000_), p_324604_, cures);
        }

        @Deprecated
        Details(int amplifier, int duration, boolean ambient, boolean showParticles, boolean showIcon, Optional<MobEffectInstance.Details> hiddenEffect) {
            this(amplifier, duration, ambient, showParticles, showIcon, hiddenEffect, Optional.empty());
        }

    }
}
