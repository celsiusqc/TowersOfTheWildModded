package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractOptionSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.OptionEnum;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public final class OptionInstance<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final OptionInstance.Enum<Boolean> BOOLEAN_VALUES = new OptionInstance.Enum<>(ImmutableList.of(Boolean.TRUE, Boolean.FALSE), Codec.BOOL);
    public static final OptionInstance.CaptionBasedToString<Boolean> BOOLEAN_TO_STRING = (p_231544_, p_231545_) -> p_231545_
            ? CommonComponents.OPTION_ON
            : CommonComponents.OPTION_OFF;
    private final OptionInstance.TooltipSupplier<T> tooltip;
    final Function<T, Component> toString;
    private final OptionInstance.ValueSet<T> values;
    private final Codec<T> codec;
    private final T initialValue;
    private final Consumer<T> onValueUpdate;
    final Component caption;
    T value;

    public static OptionInstance<Boolean> createBoolean(String pKey, boolean pInitialValue, Consumer<Boolean> pOnValueUpdate) {
        return createBoolean(pKey, noTooltip(), pInitialValue, pOnValueUpdate);
    }

    public static OptionInstance<Boolean> createBoolean(String pKey, boolean pInitialValue) {
        return createBoolean(pKey, noTooltip(), pInitialValue, p_231548_ -> {
        });
    }

    public static OptionInstance<Boolean> createBoolean(String pCaption, OptionInstance.TooltipSupplier<Boolean> pTooltip, boolean pInitialValue) {
        return createBoolean(pCaption, pTooltip, pInitialValue, p_231513_ -> {
        });
    }

    public static OptionInstance<Boolean> createBoolean(
        String pCaption, OptionInstance.TooltipSupplier<Boolean> pTooltip, boolean pInitialValue, Consumer<Boolean> pOnValueUpdate
    ) {
        return createBoolean(pCaption, pTooltip, BOOLEAN_TO_STRING, pInitialValue, pOnValueUpdate);
    }

    public static OptionInstance<Boolean> createBoolean(
        String pCaption,
        OptionInstance.TooltipSupplier<Boolean> pTooltip,
        OptionInstance.CaptionBasedToString<Boolean> pValueStringifier,
        boolean pInitialValue,
        Consumer<Boolean> pOnValueUpdate
    ) {
        return new OptionInstance<>(pCaption, pTooltip, pValueStringifier, BOOLEAN_VALUES, pInitialValue, pOnValueUpdate);
    }

    public OptionInstance(
        String pCaption,
        OptionInstance.TooltipSupplier<T> pTooltip,
        OptionInstance.CaptionBasedToString<T> pValueStringifier,
        OptionInstance.ValueSet<T> pValues,
        T pInitialValue,
        Consumer<T> pOnValueUpdate
    ) {
        this(pCaption, pTooltip, pValueStringifier, pValues, pValues.codec(), pInitialValue, pOnValueUpdate);
    }

    public OptionInstance(
        String pCaption,
        OptionInstance.TooltipSupplier<T> pTooltip,
        OptionInstance.CaptionBasedToString<T> pValueStringifier,
        OptionInstance.ValueSet<T> pValues,
        Codec<T> pCodec,
        T pInitialValue,
        Consumer<T> pOnValueUpdate
    ) {
        this.caption = Component.translatable(pCaption);
        this.tooltip = pTooltip;
        this.toString = p_231506_ -> pValueStringifier.toString(this.caption, p_231506_);
        this.values = pValues;
        this.codec = pCodec;
        this.initialValue = pInitialValue;
        this.onValueUpdate = pOnValueUpdate;
        this.value = this.initialValue;
    }

    public static <T> OptionInstance.TooltipSupplier<T> noTooltip() {
        return p_258114_ -> null;
    }

    public static <T> OptionInstance.TooltipSupplier<T> cachedConstantTooltip(Component pMessage) {
        return p_258116_ -> Tooltip.create(pMessage);
    }

    public static <T extends OptionEnum> OptionInstance.CaptionBasedToString<T> forOptionEnum() {
        return (p_231538_, p_231539_) -> p_231539_.getCaption();
    }

    public AbstractWidget createButton(Options pOptions) {
        return this.createButton(pOptions, 0, 0, 150);
    }

    public AbstractWidget createButton(Options pOptions, int pX, int pY, int pWidth) {
        return this.createButton(pOptions, pX, pY, pWidth, p_261336_ -> {
        });
    }

    public AbstractWidget createButton(Options pOptions, int pX, int pY, int pWidth, Consumer<T> pOnValueChanged) {
        return this.values.createButton(this.tooltip, pOptions, pX, pY, pWidth, pOnValueChanged).apply(this);
    }

    public T get() {
        return this.value;
    }

    public Codec<T> codec() {
        return this.codec;
    }

    @Override
    public String toString() {
        return this.caption.getString();
    }

    public void set(T pValue) {
        T t = this.values.validateValue(pValue).orElseGet(() -> {
            LOGGER.error("Illegal option value " + pValue + " for " + this.caption);
            return this.initialValue;
        });
        if (!Minecraft.getInstance().isRunning()) {
            this.value = t;
        } else {
            if (!Objects.equals(this.value, t)) {
                this.value = t;
                this.onValueUpdate.accept(this.value);
            }
        }
    }

    public OptionInstance.ValueSet<T> values() {
        return this.values;
    }

    @OnlyIn(Dist.CLIENT)
    public static record AltEnum<T>(
        List<T> values, List<T> altValues, BooleanSupplier altCondition, OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter, Codec<T> codec
    ) implements OptionInstance.CycleableValueSet<T> {
        @Override
        public CycleButton.ValueListSupplier<T> valueListSupplier() {
            return CycleButton.ValueListSupplier.create(this.altCondition, this.values, this.altValues);
        }

        @Override
        public Optional<T> validateValue(T p_231570_) {
            return (this.altCondition.getAsBoolean() ? this.altValues : this.values).contains(p_231570_) ? Optional.of(p_231570_) : Optional.empty();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface CaptionBasedToString<T> {
        Component toString(Component pCaption, T pValue);
    }

    @OnlyIn(Dist.CLIENT)
    public static record ClampingLazyMaxIntRange(int minInclusive, IntSupplier maxSupplier, int encodableMaxInclusive)
        implements OptionInstance.IntRangeBase,
        OptionInstance.SliderableOrCyclableValueSet<Integer> {
        public Optional<Integer> validateValue(Integer pValue) {
            return Optional.of(Mth.clamp(pValue, this.minInclusive(), this.maxInclusive()));
        }

        @Override
        public int maxInclusive() {
            return this.maxSupplier.getAsInt();
        }

        @Override
        public Codec<Integer> codec() {
            return Codec.INT
                .validate(
                    p_276098_ -> {
                        int i = this.encodableMaxInclusive + 1;
                        return p_276098_.compareTo(this.minInclusive) >= 0 && p_276098_.compareTo(i) <= 0
                            ? DataResult.success(p_276098_)
                            : DataResult.error(() -> "Value " + p_276098_ + " outside of range [" + this.minInclusive + ":" + i + "]", p_276098_);
                    }
                );
        }

        @Override
        public boolean createCycleButton() {
            return true;
        }

        @Override
        public CycleButton.ValueListSupplier<Integer> valueListSupplier() {
            return CycleButton.ValueListSupplier.create(IntStream.range(this.minInclusive, this.maxInclusive() + 1).boxed().toList());
        }
    }

    @OnlyIn(Dist.CLIENT)
    interface CycleableValueSet<T> extends OptionInstance.ValueSet<T> {
        CycleButton.ValueListSupplier<T> valueListSupplier();

        default OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter() {
            return OptionInstance::set;
        }

        @Override
        default Function<OptionInstance<T>, AbstractWidget> createButton(
            OptionInstance.TooltipSupplier<T> pTooltipSupplier, Options pOptions, int pX, int pY, int pWidth, Consumer<T> pOnValueChanged
        ) {
            return p_261343_ -> CycleButton.builder(p_261343_.toString)
                    .withValues(this.valueListSupplier())
                    .withTooltip(pTooltipSupplier)
                    .withInitialValue(p_261343_.value)
                    .create(pX, pY, pWidth, 20, p_261343_.caption, (p_261347_, p_261348_) -> {
                        this.valueSetter().set(p_261343_, p_261348_);
                        pOptions.save();
                        pOnValueChanged.accept(p_261348_);
                    });
        }

        @OnlyIn(Dist.CLIENT)
        public interface ValueSetter<T> {
            void set(OptionInstance<T> pInstance, T pValue);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record Enum<T>(List<T> values, Codec<T> codec) implements OptionInstance.CycleableValueSet<T> {
        @Override
        public Optional<T> validateValue(T p_231632_) {
            return this.values.contains(p_231632_) ? Optional.of(p_231632_) : Optional.empty();
        }

        @Override
        public CycleButton.ValueListSupplier<T> valueListSupplier() {
            return CycleButton.ValueListSupplier.create(this.values);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record IntRange(int minInclusive, int maxInclusive, boolean applyValueImmediately) implements OptionInstance.IntRangeBase {
        public IntRange(int p_231642_, int p_231643_) {
            this(p_231642_, p_231643_, true);
        }

        public Optional<Integer> validateValue(Integer pValue) {
            return pValue.compareTo(this.minInclusive()) >= 0 && pValue.compareTo(this.maxInclusive()) <= 0 ? Optional.of(pValue) : Optional.empty();
        }

        @Override
        public Codec<Integer> codec() {
            return Codec.intRange(this.minInclusive, this.maxInclusive + 1);
        }
    }

    @OnlyIn(Dist.CLIENT)
    interface IntRangeBase extends OptionInstance.SliderableValueSet<Integer> {
        int minInclusive();

        int maxInclusive();

        default double toSliderValue(Integer pValue) {
            if (pValue == this.minInclusive()) {
                return 0.0;
            } else {
                return pValue == this.maxInclusive()
                    ? 1.0
                    : Mth.map((double)pValue.intValue() + 0.5, (double)this.minInclusive(), (double)this.maxInclusive() + 1.0, 0.0, 1.0);
            }
        }

        default Integer fromSliderValue(double pValue) {
            if (pValue >= 1.0) {
                pValue = 0.99999F;
            }

            return Mth.floor(Mth.map(pValue, 0.0, 1.0, (double)this.minInclusive(), (double)this.maxInclusive() + 1.0));
        }

        default <R> OptionInstance.SliderableValueSet<R> xmap(final IntFunction<? extends R> pTo, final ToIntFunction<? super R> pFrom) {
            return new OptionInstance.SliderableValueSet<R>() {
                @Override
                public Optional<R> validateValue(R p_231674_) {
                    return IntRangeBase.this.validateValue(Integer.valueOf(pFrom.applyAsInt(p_231674_))).map(pTo::apply);
                }

                @Override
                public double toSliderValue(R p_231678_) {
                    return IntRangeBase.this.toSliderValue(pFrom.applyAsInt(p_231678_));
                }

                @Override
                public R fromSliderValue(double p_231676_) {
                    return (R)pTo.apply(IntRangeBase.this.fromSliderValue(p_231676_));
                }

                @Override
                public Codec<R> codec() {
                    return IntRangeBase.this.codec().xmap(pTo::apply, pFrom::applyAsInt);
                }
            };
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record LazyEnum<T>(Supplier<List<T>> values, Function<T, Optional<T>> validateValue, Codec<T> codec)
        implements OptionInstance.CycleableValueSet<T> {
        @Override
        public Optional<T> validateValue(T p_231689_) {
            return this.validateValue.apply(p_231689_);
        }

        @Override
        public CycleButton.ValueListSupplier<T> valueListSupplier() {
            return CycleButton.ValueListSupplier.create(this.values.get());
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class OptionInstanceSliderButton<N> extends AbstractOptionSliderButton {
        private final OptionInstance<N> instance;
        private final OptionInstance.SliderableValueSet<N> values;
        private final OptionInstance.TooltipSupplier<N> tooltipSupplier;
        private final Consumer<N> onValueChanged;
        @Nullable
        private Long delayedApplyAt;
        private final boolean applyValueImmediately;

        OptionInstanceSliderButton(
            Options pOptions,
            int pX,
            int pY,
            int pWidth,
            int pHeight,
            OptionInstance<N> pInstance,
            OptionInstance.SliderableValueSet<N> pValues,
            OptionInstance.TooltipSupplier<N> pTooltipSupplier,
            Consumer<N> pOnValueChanged,
            boolean pApplyValueImmediately
        ) {
            super(pOptions, pX, pY, pWidth, pHeight, pValues.toSliderValue(pInstance.get()));
            this.instance = pInstance;
            this.values = pValues;
            this.tooltipSupplier = pTooltipSupplier;
            this.onValueChanged = pOnValueChanged;
            this.applyValueImmediately = pApplyValueImmediately;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(this.instance.toString.apply(this.values.fromSliderValue(this.value)));
            this.setTooltip(this.tooltipSupplier.apply(this.values.fromSliderValue(this.value)));
        }

        @Override
        protected void applyValue() {
            if (this.applyValueImmediately) {
                this.applyUnsavedValue();
            } else {
                this.delayedApplyAt = Util.getMillis() + 600L;
            }
        }

        public void applyUnsavedValue() {
            N n = this.values.fromSliderValue(this.value);
            if (!Objects.equals(n, this.instance.get())) {
                this.instance.set(n);
                this.options.save();
                this.onValueChanged.accept(this.instance.get());
            }
        }

        @Override
        public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            if (this.delayedApplyAt != null && Util.getMillis() >= this.delayedApplyAt) {
                this.delayedApplyAt = null;
                this.applyUnsavedValue();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    interface SliderableOrCyclableValueSet<T> extends OptionInstance.CycleableValueSet<T>, OptionInstance.SliderableValueSet<T> {
        boolean createCycleButton();

        @Override
        default Function<OptionInstance<T>, AbstractWidget> createButton(
            OptionInstance.TooltipSupplier<T> p_261786_, Options p_262030_, int p_261940_, int p_262149_, int p_261495_, Consumer<T> p_261881_
        ) {
            return this.createCycleButton()
                ? OptionInstance.CycleableValueSet.super.createButton(p_261786_, p_262030_, p_261940_, p_262149_, p_261495_, p_261881_)
                : OptionInstance.SliderableValueSet.super.createButton(p_261786_, p_262030_, p_261940_, p_262149_, p_261495_, p_261881_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    interface SliderableValueSet<T> extends OptionInstance.ValueSet<T> {
        double toSliderValue(T pValue);

        T fromSliderValue(double pValue);

        default boolean applyValueImmediately() {
            return true;
        }

        @Override
        default Function<OptionInstance<T>, AbstractWidget> createButton(
            OptionInstance.TooltipSupplier<T> pTooltipSupplier, Options pOptions, int pX, int pY, int pWidth, Consumer<T> pOnValueChanged
        ) {
            return p_341831_ -> new OptionInstance.OptionInstanceSliderButton<>(
                    pOptions, pX, pY, pWidth, 20, p_341831_, this, pTooltipSupplier, pOnValueChanged, this.applyValueImmediately()
                );
        }
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface TooltipSupplier<T> {
        @Nullable
        Tooltip apply(T pValue);
    }

    @OnlyIn(Dist.CLIENT)
    public static enum UnitDouble implements OptionInstance.SliderableValueSet<Double> {
        INSTANCE;

        public Optional<Double> validateValue(Double pValue) {
            return pValue >= 0.0 && pValue <= 1.0 ? Optional.of(pValue) : Optional.empty();
        }

        public double toSliderValue(Double pValue) {
            return pValue;
        }

        public Double fromSliderValue(double pValue) {
            return pValue;
        }

        public <R> OptionInstance.SliderableValueSet<R> xmap(final DoubleFunction<? extends R> pEncoder, final ToDoubleFunction<? super R> pDecoder) {
            return new OptionInstance.SliderableValueSet<R>() {
                @Override
                public Optional<R> validateValue(R p_231773_) {
                    return UnitDouble.this.validateValue(pDecoder.applyAsDouble(p_231773_)).map(pEncoder::apply);
                }

                @Override
                public double toSliderValue(R p_231777_) {
                    return UnitDouble.this.toSliderValue(pDecoder.applyAsDouble(p_231777_));
                }

                @Override
                public R fromSliderValue(double p_231775_) {
                    return (R)pEncoder.apply(UnitDouble.this.fromSliderValue(p_231775_));
                }

                @Override
                public Codec<R> codec() {
                    return UnitDouble.this.codec().xmap(pEncoder::apply, pDecoder::applyAsDouble);
                }
            };
        }

        @Override
        public Codec<Double> codec() {
            return Codec.withAlternative(Codec.doubleRange(0.0, 1.0), Codec.BOOL, p_231745_ -> p_231745_ ? 1.0 : 0.0);
        }
    }

    @OnlyIn(Dist.CLIENT)
    interface ValueSet<T> {
        Function<OptionInstance<T>, AbstractWidget> createButton(
            OptionInstance.TooltipSupplier<T> pTooltipSupplier, Options pOptions, int pX, int pY, int pWidth, Consumer<T> pOnValueChanged
        );

        Optional<T> validateValue(T pValue);

        Codec<T> codec();
    }
}
