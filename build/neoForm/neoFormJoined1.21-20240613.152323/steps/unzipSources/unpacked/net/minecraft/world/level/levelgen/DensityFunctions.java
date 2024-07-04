package net.minecraft.world.level.levelgen;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.ToFloatFunction;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.slf4j.Logger;

public final class DensityFunctions {
    private static final Codec<DensityFunction> CODEC = BuiltInRegistries.DENSITY_FUNCTION_TYPE
        .byNameCodec()
        .dispatch(p_338092_ -> p_338092_.codec().codec(), Function.identity());
    protected static final double MAX_REASONABLE_NOISE_VALUE = 1000000.0;
    static final Codec<Double> NOISE_VALUE_CODEC = Codec.doubleRange(-1000000.0, 1000000.0);
    public static final Codec<DensityFunction> DIRECT_CODEC = Codec.either(NOISE_VALUE_CODEC, CODEC)
        .xmap(
            p_224023_ -> p_224023_.map(DensityFunctions::constant, Function.identity()),
            p_224051_ -> p_224051_ instanceof DensityFunctions.Constant densityfunctions$constant
                    ? Either.left(densityfunctions$constant.value())
                    : Either.right(p_224051_)
        );

    public static MapCodec<? extends DensityFunction> bootstrap(Registry<MapCodec<? extends DensityFunction>> pRegistry) {
        register(pRegistry, "blend_alpha", DensityFunctions.BlendAlpha.CODEC);
        register(pRegistry, "blend_offset", DensityFunctions.BlendOffset.CODEC);
        register(pRegistry, "beardifier", DensityFunctions.BeardifierMarker.CODEC);
        register(pRegistry, "old_blended_noise", BlendedNoise.CODEC);

        for (DensityFunctions.Marker.Type densityfunctions$marker$type : DensityFunctions.Marker.Type.values()) {
            register(pRegistry, densityfunctions$marker$type.getSerializedName(), densityfunctions$marker$type.codec);
        }

        register(pRegistry, "noise", DensityFunctions.Noise.CODEC);
        register(pRegistry, "end_islands", DensityFunctions.EndIslandDensityFunction.CODEC);
        register(pRegistry, "weird_scaled_sampler", DensityFunctions.WeirdScaledSampler.CODEC);
        register(pRegistry, "shifted_noise", DensityFunctions.ShiftedNoise.CODEC);
        register(pRegistry, "range_choice", DensityFunctions.RangeChoice.CODEC);
        register(pRegistry, "shift_a", DensityFunctions.ShiftA.CODEC);
        register(pRegistry, "shift_b", DensityFunctions.ShiftB.CODEC);
        register(pRegistry, "shift", DensityFunctions.Shift.CODEC);
        register(pRegistry, "blend_density", DensityFunctions.BlendDensity.CODEC);
        register(pRegistry, "clamp", DensityFunctions.Clamp.CODEC);

        for (DensityFunctions.Mapped.Type densityfunctions$mapped$type : DensityFunctions.Mapped.Type.values()) {
            register(pRegistry, densityfunctions$mapped$type.getSerializedName(), densityfunctions$mapped$type.codec);
        }

        for (DensityFunctions.TwoArgumentSimpleFunction.Type densityfunctions$twoargumentsimplefunction$type : DensityFunctions.TwoArgumentSimpleFunction.Type.values()) {
            register(pRegistry, densityfunctions$twoargumentsimplefunction$type.getSerializedName(), densityfunctions$twoargumentsimplefunction$type.codec);
        }

        register(pRegistry, "spline", DensityFunctions.Spline.CODEC);
        register(pRegistry, "constant", DensityFunctions.Constant.CODEC);
        return register(pRegistry, "y_clamped_gradient", DensityFunctions.YClampedGradient.CODEC);
    }

    private static MapCodec<? extends DensityFunction> register(
        Registry<MapCodec<? extends DensityFunction>> pRegistry, String pName, KeyDispatchDataCodec<? extends DensityFunction> pCodec
    ) {
        return Registry.register(pRegistry, pName, pCodec.codec());
    }

    static <A, O> KeyDispatchDataCodec<O> singleArgumentCodec(Codec<A> pCodec, Function<A, O> pFromFunction, Function<O, A> pToFunction) {
        return KeyDispatchDataCodec.of(pCodec.fieldOf("argument").xmap(pFromFunction, pToFunction));
    }

    static <O> KeyDispatchDataCodec<O> singleFunctionArgumentCodec(Function<DensityFunction, O> pFromFunction, Function<O, DensityFunction> pToFunction) {
        return singleArgumentCodec(DensityFunction.HOLDER_HELPER_CODEC, pFromFunction, pToFunction);
    }

    static <O> KeyDispatchDataCodec<O> doubleFunctionArgumentCodec(
        BiFunction<DensityFunction, DensityFunction, O> pFromFunction, Function<O, DensityFunction> pPrimary, Function<O, DensityFunction> pSecondary
    ) {
        return KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(
                p_224049_ -> p_224049_.group(
                            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument1").forGetter(pPrimary),
                            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument2").forGetter(pSecondary)
                        )
                        .apply(p_224049_, pFromFunction)
            )
        );
    }

    static <O> KeyDispatchDataCodec<O> makeCodec(MapCodec<O> pMapCodec) {
        return KeyDispatchDataCodec.of(pMapCodec);
    }

    private DensityFunctions() {
    }

    public static DensityFunction interpolated(DensityFunction pWrapped) {
        return new DensityFunctions.Marker(DensityFunctions.Marker.Type.Interpolated, pWrapped);
    }

    public static DensityFunction flatCache(DensityFunction pWrapped) {
        return new DensityFunctions.Marker(DensityFunctions.Marker.Type.FlatCache, pWrapped);
    }

    public static DensityFunction cache2d(DensityFunction pWrapped) {
        return new DensityFunctions.Marker(DensityFunctions.Marker.Type.Cache2D, pWrapped);
    }

    public static DensityFunction cacheOnce(DensityFunction pWrapped) {
        return new DensityFunctions.Marker(DensityFunctions.Marker.Type.CacheOnce, pWrapped);
    }

    public static DensityFunction cacheAllInCell(DensityFunction pWrapped) {
        return new DensityFunctions.Marker(DensityFunctions.Marker.Type.CacheAllInCell, pWrapped);
    }

    public static DensityFunction mappedNoise(
        Holder<NormalNoise.NoiseParameters> pNoiseData, @Deprecated double pXzScale, double pYScale, double pFromY, double pToY
    ) {
        return mapFromUnitTo(new DensityFunctions.Noise(new DensityFunction.NoiseHolder(pNoiseData), pXzScale, pYScale), pFromY, pToY);
    }

    public static DensityFunction mappedNoise(Holder<NormalNoise.NoiseParameters> pNoiseData, double pYScale, double pFromY, double pToY) {
        return mappedNoise(pNoiseData, 1.0, pYScale, pFromY, pToY);
    }

    public static DensityFunction mappedNoise(Holder<NormalNoise.NoiseParameters> pNoiseData, double pFromY, double pToY) {
        return mappedNoise(pNoiseData, 1.0, 1.0, pFromY, pToY);
    }

    public static DensityFunction shiftedNoise2d(
        DensityFunction pShiftX, DensityFunction pShiftZ, double pXzScale, Holder<NormalNoise.NoiseParameters> pNoiseData
    ) {
        return new DensityFunctions.ShiftedNoise(pShiftX, zero(), pShiftZ, pXzScale, 0.0, new DensityFunction.NoiseHolder(pNoiseData));
    }

    public static DensityFunction noise(Holder<NormalNoise.NoiseParameters> pNoiseData) {
        return noise(pNoiseData, 1.0, 1.0);
    }

    public static DensityFunction noise(Holder<NormalNoise.NoiseParameters> pNoiseData, double pXzScale, double pYScale) {
        return new DensityFunctions.Noise(new DensityFunction.NoiseHolder(pNoiseData), pXzScale, pYScale);
    }

    public static DensityFunction noise(Holder<NormalNoise.NoiseParameters> pNoiseData, double pYScale) {
        return noise(pNoiseData, 1.0, pYScale);
    }

    public static DensityFunction rangeChoice(
        DensityFunction pInput, double pMinInclusive, double pMaxExclusive, DensityFunction pWhenInRange, DensityFunction pWhenOutOfRange
    ) {
        return new DensityFunctions.RangeChoice(pInput, pMinInclusive, pMaxExclusive, pWhenInRange, pWhenOutOfRange);
    }

    public static DensityFunction shiftA(Holder<NormalNoise.NoiseParameters> pNoiseData) {
        return new DensityFunctions.ShiftA(new DensityFunction.NoiseHolder(pNoiseData));
    }

    public static DensityFunction shiftB(Holder<NormalNoise.NoiseParameters> pNoiseData) {
        return new DensityFunctions.ShiftB(new DensityFunction.NoiseHolder(pNoiseData));
    }

    public static DensityFunction shift(Holder<NormalNoise.NoiseParameters> pNoiseData) {
        return new DensityFunctions.Shift(new DensityFunction.NoiseHolder(pNoiseData));
    }

    public static DensityFunction blendDensity(DensityFunction pInput) {
        return new DensityFunctions.BlendDensity(pInput);
    }

    public static DensityFunction endIslands(long pSeed) {
        return new DensityFunctions.EndIslandDensityFunction(pSeed);
    }

    public static DensityFunction weirdScaledSampler(
        DensityFunction pInput, Holder<NormalNoise.NoiseParameters> pNoiseData, DensityFunctions.WeirdScaledSampler.RarityValueMapper pRarityValueMapper
    ) {
        return new DensityFunctions.WeirdScaledSampler(pInput, new DensityFunction.NoiseHolder(pNoiseData), pRarityValueMapper);
    }

    public static DensityFunction add(DensityFunction pArgument1, DensityFunction pArgument2) {
        return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.ADD, pArgument1, pArgument2);
    }

    public static DensityFunction mul(DensityFunction pArgument1, DensityFunction pArgument2) {
        return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.MUL, pArgument1, pArgument2);
    }

    public static DensityFunction min(DensityFunction pArgument1, DensityFunction pArgument2) {
        return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.MIN, pArgument1, pArgument2);
    }

    public static DensityFunction max(DensityFunction pArgument1, DensityFunction pArgument2) {
        return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.MAX, pArgument1, pArgument2);
    }

    public static DensityFunction spline(CubicSpline<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> pSpline) {
        return new DensityFunctions.Spline(pSpline);
    }

    public static DensityFunction zero() {
        return DensityFunctions.Constant.ZERO;
    }

    public static DensityFunction constant(double p_208265_) {
        return new DensityFunctions.Constant(p_208265_);
    }

    public static DensityFunction yClampedGradient(int pFromY, int pToY, double pFromValue, double pToValue) {
        return new DensityFunctions.YClampedGradient(pFromY, pToY, pFromValue, pToValue);
    }

    public static DensityFunction map(DensityFunction pInput, DensityFunctions.Mapped.Type pType) {
        return DensityFunctions.Mapped.create(pType, pInput);
    }

    private static DensityFunction mapFromUnitTo(DensityFunction pDensityFunction, double pFromY, double pToY) {
        double d0 = (pFromY + pToY) * 0.5;
        double d1 = (pToY - pFromY) * 0.5;
        return add(constant(d0), mul(constant(d1), pDensityFunction));
    }

    public static DensityFunction blendAlpha() {
        return DensityFunctions.BlendAlpha.INSTANCE;
    }

    public static DensityFunction blendOffset() {
        return DensityFunctions.BlendOffset.INSTANCE;
    }

    public static DensityFunction lerp(DensityFunction pDeltaFunction, DensityFunction pMinFunction, DensityFunction pMaxFunction) {
        if (pMinFunction instanceof DensityFunctions.Constant densityfunctions$constant) {
            return lerp(pDeltaFunction, densityfunctions$constant.value, pMaxFunction);
        } else {
            DensityFunction densityfunction = cacheOnce(pDeltaFunction);
            DensityFunction densityfunction1 = add(mul(densityfunction, constant(-1.0)), constant(1.0));
            return add(mul(pMinFunction, densityfunction1), mul(pMaxFunction, densityfunction));
        }
    }

    public static DensityFunction lerp(DensityFunction pDeltaFunction, double pMin, DensityFunction pMaxFunction) {
        return add(mul(pDeltaFunction, add(pMaxFunction, constant(-pMin))), constant(pMin));
    }

    static record Ap2(
        DensityFunctions.TwoArgumentSimpleFunction.Type type, DensityFunction argument1, DensityFunction argument2, double minValue, double maxValue
    ) implements DensityFunctions.TwoArgumentSimpleFunction {
        @Override
        public double compute(DensityFunction.FunctionContext p_208410_) {
            double d0 = this.argument1.compute(p_208410_);

            return switch (this.type) {
                case ADD -> d0 + this.argument2.compute(p_208410_);
                case MUL -> d0 == 0.0 ? 0.0 : d0 * this.argument2.compute(p_208410_);
                case MIN -> d0 < this.argument2.minValue() ? d0 : Math.min(d0, this.argument2.compute(p_208410_));
                case MAX -> d0 > this.argument2.maxValue() ? d0 : Math.max(d0, this.argument2.compute(p_208410_));
            };
        }

        @Override
        public void fillArray(double[] p_208414_, DensityFunction.ContextProvider p_208415_) {
            this.argument1.fillArray(p_208414_, p_208415_);
            switch (this.type) {
                case ADD:
                    double[] adouble = new double[p_208414_.length];
                    this.argument2.fillArray(adouble, p_208415_);

                    for (int k = 0; k < p_208414_.length; k++) {
                        p_208414_[k] += adouble[k];
                    }
                    break;
                case MUL:
                    for (int j = 0; j < p_208414_.length; j++) {
                        double d1 = p_208414_[j];
                        p_208414_[j] = d1 == 0.0 ? 0.0 : d1 * this.argument2.compute(p_208415_.forIndex(j));
                    }
                    break;
                case MIN:
                    double d3 = this.argument2.minValue();

                    for (int l = 0; l < p_208414_.length; l++) {
                        double d4 = p_208414_[l];
                        p_208414_[l] = d4 < d3 ? d4 : Math.min(d4, this.argument2.compute(p_208415_.forIndex(l)));
                    }
                    break;
                case MAX:
                    double d0 = this.argument2.maxValue();

                    for (int i = 0; i < p_208414_.length; i++) {
                        double d2 = p_208414_[i];
                        p_208414_[i] = d2 > d0 ? d2 : Math.max(d2, this.argument2.compute(p_208415_.forIndex(i)));
                    }
            }
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor p_208412_) {
            return p_208412_.apply(
                DensityFunctions.TwoArgumentSimpleFunction.create(this.type, this.argument1.mapAll(p_208412_), this.argument2.mapAll(p_208412_))
            );
        }
    }

    protected static enum BeardifierMarker implements DensityFunctions.BeardifierOrMarker {
        INSTANCE;

        @Override
        public double compute(DensityFunction.FunctionContext p_208515_) {
            return 0.0;
        }

        @Override
        public void fillArray(double[] p_208517_, DensityFunction.ContextProvider p_208518_) {
            Arrays.fill(p_208517_, 0.0);
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            return 0.0;
        }
    }

    public interface BeardifierOrMarker extends DensityFunction.SimpleFunction {
        KeyDispatchDataCodec<DensityFunction> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(DensityFunctions.BeardifierMarker.INSTANCE));

        @Override
        default KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected static enum BlendAlpha implements DensityFunction.SimpleFunction {
        INSTANCE;

        public static final KeyDispatchDataCodec<DensityFunction> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

        @Override
        public double compute(DensityFunction.FunctionContext p_208536_) {
            return 1.0;
        }

        @Override
        public void fillArray(double[] p_208538_, DensityFunction.ContextProvider p_208539_) {
            Arrays.fill(p_208538_, 1.0);
        }

        @Override
        public double minValue() {
            return 1.0;
        }

        @Override
        public double maxValue() {
            return 1.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    static record BlendDensity(DensityFunction input) implements DensityFunctions.TransformerWithContext {
        static final KeyDispatchDataCodec<DensityFunctions.BlendDensity> CODEC = DensityFunctions.singleFunctionArgumentCodec(
            DensityFunctions.BlendDensity::new, DensityFunctions.BlendDensity::input
        );

        @Override
        public double transform(DensityFunction.FunctionContext p_208553_, double p_208554_) {
            return p_208553_.getBlender().blendDensity(p_208553_, p_208554_);
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor p_208556_) {
            return p_208556_.apply(new DensityFunctions.BlendDensity(this.input.mapAll(p_208556_)));
        }

        @Override
        public double minValue() {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public double maxValue() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected static enum BlendOffset implements DensityFunction.SimpleFunction {
        INSTANCE;

        public static final KeyDispatchDataCodec<DensityFunction> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

        @Override
        public double compute(DensityFunction.FunctionContext p_208573_) {
            return 0.0;
        }

        @Override
        public void fillArray(double[] p_208575_, DensityFunction.ContextProvider p_208576_) {
            Arrays.fill(p_208575_, 0.0);
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            return 0.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected static record Clamp(DensityFunction input, double minValue, double maxValue) implements DensityFunctions.PureTransformer {
        private static final MapCodec<DensityFunctions.Clamp> DATA_CODEC = RecordCodecBuilder.mapCodec(
            p_208597_ -> p_208597_.group(
                        DensityFunction.DIRECT_CODEC.fieldOf("input").forGetter(DensityFunctions.Clamp::input),
                        DensityFunctions.NOISE_VALUE_CODEC.fieldOf("min").forGetter(DensityFunctions.Clamp::minValue),
                        DensityFunctions.NOISE_VALUE_CODEC.fieldOf("max").forGetter(DensityFunctions.Clamp::maxValue)
                    )
                    .apply(p_208597_, DensityFunctions.Clamp::new)
        );
        public static final KeyDispatchDataCodec<DensityFunctions.Clamp> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double transform(double pValue) {
            return Mth.clamp(pValue, this.minValue, this.maxValue);
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor pVisitor) {
            return new DensityFunctions.Clamp(this.input.mapAll(pVisitor), this.minValue, this.maxValue);
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    static record Constant(double value) implements DensityFunction.SimpleFunction {
        static final KeyDispatchDataCodec<DensityFunctions.Constant> CODEC = DensityFunctions.singleArgumentCodec(
            DensityFunctions.NOISE_VALUE_CODEC, DensityFunctions.Constant::new, DensityFunctions.Constant::value
        );
        static final DensityFunctions.Constant ZERO = new DensityFunctions.Constant(0.0);

        @Override
        public double compute(DensityFunction.FunctionContext p_208615_) {
            return this.value;
        }

        @Override
        public void fillArray(double[] p_208617_, DensityFunction.ContextProvider p_208618_) {
            Arrays.fill(p_208617_, this.value);
        }

        @Override
        public double minValue() {
            return this.value;
        }

        @Override
        public double maxValue() {
            return this.value;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected static final class EndIslandDensityFunction implements DensityFunction.SimpleFunction {
        public static final KeyDispatchDataCodec<DensityFunctions.EndIslandDensityFunction> CODEC = KeyDispatchDataCodec.of(
            MapCodec.unit(new DensityFunctions.EndIslandDensityFunction(0L))
        );
        private static final float ISLAND_THRESHOLD = -0.9F;
        private final SimplexNoise islandNoise;

        public EndIslandDensityFunction(long pSeed) {
            RandomSource randomsource = new LegacyRandomSource(pSeed);
            randomsource.consumeCount(17292);
            this.islandNoise = new SimplexNoise(randomsource);
        }

        private static float getHeightValue(SimplexNoise pNoise, int pX, int pZ) {
            int i = pX / 2;
            int j = pZ / 2;
            int k = pX % 2;
            int l = pZ % 2;
            float f = 100.0F - Mth.sqrt((float)(pX * pX + pZ * pZ)) * 8.0F;
            f = Mth.clamp(f, -100.0F, 80.0F);

            for (int i1 = -12; i1 <= 12; i1++) {
                for (int j1 = -12; j1 <= 12; j1++) {
                    long k1 = (long)(i + i1);
                    long l1 = (long)(j + j1);
                    if (k1 * k1 + l1 * l1 > 4096L && pNoise.getValue((double)k1, (double)l1) < -0.9F) {
                        float f1 = (Mth.abs((float)k1) * 3439.0F + Mth.abs((float)l1) * 147.0F) % 13.0F + 9.0F;
                        float f2 = (float)(k - i1 * 2);
                        float f3 = (float)(l - j1 * 2);
                        float f4 = 100.0F - Mth.sqrt(f2 * f2 + f3 * f3) * f1;
                        f4 = Mth.clamp(f4, -100.0F, 80.0F);
                        f = Math.max(f, f4);
                    }
                }
            }

            return f;
        }

        @Override
        public double compute(DensityFunction.FunctionContext pContext) {
            return ((double)getHeightValue(this.islandNoise, pContext.blockX() / 8, pContext.blockZ() / 8) - 8.0) / 128.0;
        }

        @Override
        public double minValue() {
            return -0.84375;
        }

        @Override
        public double maxValue() {
            return 0.5625;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    @VisibleForDebug
    public static record HolderHolder(Holder<DensityFunction> function) implements DensityFunction {
        @Override
        public double compute(DensityFunction.FunctionContext p_208641_) {
            return this.function.value().compute(p_208641_);
        }

        @Override
        public void fillArray(double[] p_208645_, DensityFunction.ContextProvider p_208646_) {
            this.function.value().fillArray(p_208645_, p_208646_);
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor p_208643_) {
            return p_208643_.apply(new DensityFunctions.HolderHolder(new Holder.Direct<>(this.function.value().mapAll(p_208643_))));
        }

        @Override
        public double minValue() {
            return this.function.isBound() ? this.function.value().minValue() : Double.NEGATIVE_INFINITY;
        }

        @Override
        public double maxValue() {
            return this.function.isBound() ? this.function.value().maxValue() : Double.POSITIVE_INFINITY;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException("Calling .codec() on HolderHolder");
        }
    }

    protected static record Mapped(DensityFunctions.Mapped.Type type, DensityFunction input, double minValue, double maxValue)
        implements DensityFunctions.PureTransformer {
        public static DensityFunctions.Mapped create(DensityFunctions.Mapped.Type pType, DensityFunction pInput) {
            double d0 = pInput.minValue();
            double d1 = transform(pType, d0);
            double d2 = transform(pType, pInput.maxValue());
            return pType != DensityFunctions.Mapped.Type.ABS && pType != DensityFunctions.Mapped.Type.SQUARE
                ? new DensityFunctions.Mapped(pType, pInput, d1, d2)
                : new DensityFunctions.Mapped(pType, pInput, Math.max(0.0, d0), Math.max(d1, d2));
        }

        private static double transform(DensityFunctions.Mapped.Type pType, double pValue) {
            return switch (pType) {
                case ABS -> Math.abs(pValue);
                case SQUARE -> pValue * pValue;
                case CUBE -> pValue * pValue * pValue;
                case HALF_NEGATIVE -> pValue > 0.0 ? pValue : pValue * 0.5;
                case QUARTER_NEGATIVE -> pValue > 0.0 ? pValue : pValue * 0.25;
                case SQUEEZE -> {
                    double d0 = Mth.clamp(pValue, -1.0, 1.0);
                    yield d0 / 2.0 - d0 * d0 * d0 / 24.0;
                }
            };
        }

        @Override
        public double transform(double pValue) {
            return transform(this.type, pValue);
        }

        public DensityFunctions.Mapped mapAll(DensityFunction.Visitor pVisitor) {
            return create(this.type, this.input.mapAll(pVisitor));
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return this.type.codec;
        }

        static enum Type implements StringRepresentable {
            ABS("abs"),
            SQUARE("square"),
            CUBE("cube"),
            HALF_NEGATIVE("half_negative"),
            QUARTER_NEGATIVE("quarter_negative"),
            SQUEEZE("squeeze");

            private final String name;
            final KeyDispatchDataCodec<DensityFunctions.Mapped> codec = DensityFunctions.singleFunctionArgumentCodec(
                p_208700_ -> DensityFunctions.Mapped.create(this, p_208700_), DensityFunctions.Mapped::input
            );

            private Type(String pName) {
                this.name = pName;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }
        }
    }

    protected static record Marker(DensityFunctions.Marker.Type type, DensityFunction wrapped) implements DensityFunctions.MarkerOrMarked {
        @Override
        public double compute(DensityFunction.FunctionContext pContext) {
            return this.wrapped.compute(pContext);
        }

        @Override
        public void fillArray(double[] pArray, DensityFunction.ContextProvider pContextProvider) {
            this.wrapped.fillArray(pArray, pContextProvider);
        }

        @Override
        public double minValue() {
            return this.wrapped.minValue();
        }

        @Override
        public double maxValue() {
            return this.wrapped.maxValue();
        }

        static enum Type implements StringRepresentable {
            Interpolated("interpolated"),
            FlatCache("flat_cache"),
            Cache2D("cache_2d"),
            CacheOnce("cache_once"),
            CacheAllInCell("cache_all_in_cell");

            private final String name;
            final KeyDispatchDataCodec<DensityFunctions.MarkerOrMarked> codec = DensityFunctions.singleFunctionArgumentCodec(
                p_208740_ -> new DensityFunctions.Marker(this, p_208740_), DensityFunctions.MarkerOrMarked::wrapped
            );

            private Type(String pName) {
                this.name = pName;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }
        }
    }

    public interface MarkerOrMarked extends DensityFunction {
        DensityFunctions.Marker.Type type();

        DensityFunction wrapped();

        @Override
        default KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return this.type().codec;
        }

        @Override
        default DensityFunction mapAll(DensityFunction.Visitor p_224070_) {
            return p_224070_.apply(new DensityFunctions.Marker(this.type(), this.wrapped().mapAll(p_224070_)));
        }
    }

    static record MulOrAdd(DensityFunctions.MulOrAdd.Type specificType, DensityFunction input, double minValue, double maxValue, double argument)
        implements DensityFunctions.PureTransformer,
        DensityFunctions.TwoArgumentSimpleFunction {
        @Override
        public DensityFunctions.TwoArgumentSimpleFunction.Type type() {
            return this.specificType == DensityFunctions.MulOrAdd.Type.MUL
                ? DensityFunctions.TwoArgumentSimpleFunction.Type.MUL
                : DensityFunctions.TwoArgumentSimpleFunction.Type.ADD;
        }

        @Override
        public DensityFunction argument1() {
            return DensityFunctions.constant(this.argument);
        }

        @Override
        public DensityFunction argument2() {
            return this.input;
        }

        @Override
        public double transform(double p_208759_) {
            return switch (this.specificType) {
                case MUL -> p_208759_ * this.argument;
                case ADD -> p_208759_ + this.argument;
            };
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor p_208761_) {
            DensityFunction densityfunction = this.input.mapAll(p_208761_);
            double d0 = densityfunction.minValue();
            double d1 = densityfunction.maxValue();
            double d2;
            double d3;
            if (this.specificType == DensityFunctions.MulOrAdd.Type.ADD) {
                d2 = d0 + this.argument;
                d3 = d1 + this.argument;
            } else if (this.argument >= 0.0) {
                d2 = d0 * this.argument;
                d3 = d1 * this.argument;
            } else {
                d2 = d1 * this.argument;
                d3 = d0 * this.argument;
            }

            return new DensityFunctions.MulOrAdd(this.specificType, densityfunction, d2, d3, this.argument);
        }

        static enum Type {
            MUL,
            ADD;
        }
    }

    protected static record Noise(DensityFunction.NoiseHolder noise, @Deprecated double xzScale, double yScale) implements DensityFunction {
        public static final MapCodec<DensityFunctions.Noise> DATA_CODEC = RecordCodecBuilder.mapCodec(
            p_208798_ -> p_208798_.group(
                        DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(DensityFunctions.Noise::noise),
                        Codec.DOUBLE.fieldOf("xz_scale").forGetter(DensityFunctions.Noise::xzScale),
                        Codec.DOUBLE.fieldOf("y_scale").forGetter(DensityFunctions.Noise::yScale)
                    )
                    .apply(p_208798_, DensityFunctions.Noise::new)
        );
        public static final KeyDispatchDataCodec<DensityFunctions.Noise> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(DensityFunction.FunctionContext pContext) {
            return this.noise
                .getValue((double)pContext.blockX() * this.xzScale, (double)pContext.blockY() * this.yScale, (double)pContext.blockZ() * this.xzScale);
        }

        @Override
        public void fillArray(double[] pArray, DensityFunction.ContextProvider pContextProvider) {
            pContextProvider.fillAllDirectly(pArray, this);
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor pVisitor) {
            return pVisitor.apply(new DensityFunctions.Noise(pVisitor.visitNoise(this.noise), this.xzScale, this.yScale));
        }

        @Override
        public double minValue() {
            return -this.maxValue();
        }

        @Override
        public double maxValue() {
            return this.noise.maxValue();
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    interface PureTransformer extends DensityFunction {
        DensityFunction input();

        @Override
        default double compute(DensityFunction.FunctionContext pContext) {
            return this.transform(this.input().compute(pContext));
        }

        @Override
        default void fillArray(double[] pArray, DensityFunction.ContextProvider pContextProvider) {
            this.input().fillArray(pArray, pContextProvider);

            for (int i = 0; i < pArray.length; i++) {
                pArray[i] = this.transform(pArray[i]);
            }
        }

        double transform(double pValue);
    }

    static record RangeChoice(DensityFunction input, double minInclusive, double maxExclusive, DensityFunction whenInRange, DensityFunction whenOutOfRange)
        implements DensityFunction {
        public static final MapCodec<DensityFunctions.RangeChoice> DATA_CODEC = RecordCodecBuilder.mapCodec(
            p_208837_ -> p_208837_.group(
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(DensityFunctions.RangeChoice::input),
                        DensityFunctions.NOISE_VALUE_CODEC.fieldOf("min_inclusive").forGetter(DensityFunctions.RangeChoice::minInclusive),
                        DensityFunctions.NOISE_VALUE_CODEC.fieldOf("max_exclusive").forGetter(DensityFunctions.RangeChoice::maxExclusive),
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("when_in_range").forGetter(DensityFunctions.RangeChoice::whenInRange),
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("when_out_of_range").forGetter(DensityFunctions.RangeChoice::whenOutOfRange)
                    )
                    .apply(p_208837_, DensityFunctions.RangeChoice::new)
        );
        public static final KeyDispatchDataCodec<DensityFunctions.RangeChoice> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(DensityFunction.FunctionContext pContext) {
            double d0 = this.input.compute(pContext);
            return d0 >= this.minInclusive && d0 < this.maxExclusive ? this.whenInRange.compute(pContext) : this.whenOutOfRange.compute(pContext);
        }

        @Override
        public void fillArray(double[] pArray, DensityFunction.ContextProvider pContextProvider) {
            this.input.fillArray(pArray, pContextProvider);

            for (int i = 0; i < pArray.length; i++) {
                double d0 = pArray[i];
                if (d0 >= this.minInclusive && d0 < this.maxExclusive) {
                    pArray[i] = this.whenInRange.compute(pContextProvider.forIndex(i));
                } else {
                    pArray[i] = this.whenOutOfRange.compute(pContextProvider.forIndex(i));
                }
            }
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor pVisitor) {
            return pVisitor.apply(
                new DensityFunctions.RangeChoice(
                    this.input.mapAll(pVisitor),
                    this.minInclusive,
                    this.maxExclusive,
                    this.whenInRange.mapAll(pVisitor),
                    this.whenOutOfRange.mapAll(pVisitor)
                )
            );
        }

        @Override
        public double minValue() {
            return Math.min(this.whenInRange.minValue(), this.whenOutOfRange.minValue());
        }

        @Override
        public double maxValue() {
            return Math.max(this.whenInRange.maxValue(), this.whenOutOfRange.maxValue());
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected static record Shift(DensityFunction.NoiseHolder offsetNoise) implements DensityFunctions.ShiftNoise {
        static final KeyDispatchDataCodec<DensityFunctions.Shift> CODEC = DensityFunctions.singleArgumentCodec(
            DensityFunction.NoiseHolder.CODEC, DensityFunctions.Shift::new, DensityFunctions.Shift::offsetNoise
        );

        @Override
        public double compute(DensityFunction.FunctionContext p_208864_) {
            return this.compute((double)p_208864_.blockX(), (double)p_208864_.blockY(), (double)p_208864_.blockZ());
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor p_224087_) {
            return p_224087_.apply(new DensityFunctions.Shift(p_224087_.visitNoise(this.offsetNoise)));
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected static record ShiftA(DensityFunction.NoiseHolder offsetNoise) implements DensityFunctions.ShiftNoise {
        static final KeyDispatchDataCodec<DensityFunctions.ShiftA> CODEC = DensityFunctions.singleArgumentCodec(
            DensityFunction.NoiseHolder.CODEC, DensityFunctions.ShiftA::new, DensityFunctions.ShiftA::offsetNoise
        );

        @Override
        public double compute(DensityFunction.FunctionContext p_208884_) {
            return this.compute((double)p_208884_.blockX(), 0.0, (double)p_208884_.blockZ());
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor p_224093_) {
            return p_224093_.apply(new DensityFunctions.ShiftA(p_224093_.visitNoise(this.offsetNoise)));
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected static record ShiftB(DensityFunction.NoiseHolder offsetNoise) implements DensityFunctions.ShiftNoise {
        static final KeyDispatchDataCodec<DensityFunctions.ShiftB> CODEC = DensityFunctions.singleArgumentCodec(
            DensityFunction.NoiseHolder.CODEC, DensityFunctions.ShiftB::new, DensityFunctions.ShiftB::offsetNoise
        );

        @Override
        public double compute(DensityFunction.FunctionContext p_208904_) {
            return this.compute((double)p_208904_.blockZ(), (double)p_208904_.blockX(), 0.0);
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor p_224099_) {
            return p_224099_.apply(new DensityFunctions.ShiftB(p_224099_.visitNoise(this.offsetNoise)));
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    interface ShiftNoise extends DensityFunction {
        DensityFunction.NoiseHolder offsetNoise();

        @Override
        default double minValue() {
            return -this.maxValue();
        }

        @Override
        default double maxValue() {
            return this.offsetNoise().maxValue() * 4.0;
        }

        default double compute(double pX, double pY, double pZ) {
            return this.offsetNoise().getValue(pX * 0.25, pY * 0.25, pZ * 0.25) * 4.0;
        }

        @Override
        default void fillArray(double[] pArray, DensityFunction.ContextProvider pContextProvider) {
            pContextProvider.fillAllDirectly(pArray, this);
        }
    }

    protected static record ShiftedNoise(
        DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, DensityFunction.NoiseHolder noise
    ) implements DensityFunction {
        private static final MapCodec<DensityFunctions.ShiftedNoise> DATA_CODEC = RecordCodecBuilder.mapCodec(
            p_208943_ -> p_208943_.group(
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_x").forGetter(DensityFunctions.ShiftedNoise::shiftX),
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_y").forGetter(DensityFunctions.ShiftedNoise::shiftY),
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_z").forGetter(DensityFunctions.ShiftedNoise::shiftZ),
                        Codec.DOUBLE.fieldOf("xz_scale").forGetter(DensityFunctions.ShiftedNoise::xzScale),
                        Codec.DOUBLE.fieldOf("y_scale").forGetter(DensityFunctions.ShiftedNoise::yScale),
                        DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(DensityFunctions.ShiftedNoise::noise)
                    )
                    .apply(p_208943_, DensityFunctions.ShiftedNoise::new)
        );
        public static final KeyDispatchDataCodec<DensityFunctions.ShiftedNoise> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(DensityFunction.FunctionContext pContext) {
            double d0 = (double)pContext.blockX() * this.xzScale + this.shiftX.compute(pContext);
            double d1 = (double)pContext.blockY() * this.yScale + this.shiftY.compute(pContext);
            double d2 = (double)pContext.blockZ() * this.xzScale + this.shiftZ.compute(pContext);
            return this.noise.getValue(d0, d1, d2);
        }

        @Override
        public void fillArray(double[] pArray, DensityFunction.ContextProvider pContextProvider) {
            pContextProvider.fillAllDirectly(pArray, this);
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor pVisitor) {
            return pVisitor.apply(
                new DensityFunctions.ShiftedNoise(
                    this.shiftX.mapAll(pVisitor),
                    this.shiftY.mapAll(pVisitor),
                    this.shiftZ.mapAll(pVisitor),
                    this.xzScale,
                    this.yScale,
                    pVisitor.visitNoise(this.noise)
                )
            );
        }

        @Override
        public double minValue() {
            return -this.maxValue();
        }

        @Override
        public double maxValue() {
            return this.noise.maxValue();
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    public static record Spline(CubicSpline<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> spline) implements DensityFunction {
        private static final Codec<CubicSpline<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate>> SPLINE_CODEC = CubicSpline.codec(
            DensityFunctions.Spline.Coordinate.CODEC
        );
        private static final MapCodec<DensityFunctions.Spline> DATA_CODEC = SPLINE_CODEC.fieldOf("spline")
            .xmap(DensityFunctions.Spline::new, DensityFunctions.Spline::spline);
        public static final KeyDispatchDataCodec<DensityFunctions.Spline> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(DensityFunction.FunctionContext pContext) {
            return (double)this.spline.apply(new DensityFunctions.Spline.Point(pContext));
        }

        @Override
        public double minValue() {
            return (double)this.spline.minValue();
        }

        @Override
        public double maxValue() {
            return (double)this.spline.maxValue();
        }

        @Override
        public void fillArray(double[] pArray, DensityFunction.ContextProvider pContextProvider) {
            pContextProvider.fillAllDirectly(pArray, this);
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor pVisitor) {
            return pVisitor.apply(new DensityFunctions.Spline(this.spline.mapAll(p_224119_ -> p_224119_.mapAll(pVisitor))));
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }

        public static record Coordinate(Holder<DensityFunction> function) implements ToFloatFunction<DensityFunctions.Spline.Point> {
            public static final Codec<DensityFunctions.Spline.Coordinate> CODEC = DensityFunction.CODEC
                .xmap(DensityFunctions.Spline.Coordinate::new, DensityFunctions.Spline.Coordinate::function);

            @Override
            public String toString() {
                Optional<ResourceKey<DensityFunction>> optional = this.function.unwrapKey();
                if (optional.isPresent()) {
                    ResourceKey<DensityFunction> resourcekey = optional.get();
                    if (resourcekey == NoiseRouterData.CONTINENTS) {
                        return "continents";
                    }

                    if (resourcekey == NoiseRouterData.EROSION) {
                        return "erosion";
                    }

                    if (resourcekey == NoiseRouterData.RIDGES) {
                        return "weirdness";
                    }

                    if (resourcekey == NoiseRouterData.RIDGES_FOLDED) {
                        return "ridges";
                    }
                }

                return "Coordinate[" + this.function + "]";
            }

            public float apply(DensityFunctions.Spline.Point pObject) {
                return (float)this.function.value().compute(pObject.context());
            }

            @Override
            public float minValue() {
                return this.function.isBound() ? (float)this.function.value().minValue() : Float.NEGATIVE_INFINITY;
            }

            @Override
            public float maxValue() {
                return this.function.isBound() ? (float)this.function.value().maxValue() : Float.POSITIVE_INFINITY;
            }

            public DensityFunctions.Spline.Coordinate mapAll(DensityFunction.Visitor pVisitor) {
                return new DensityFunctions.Spline.Coordinate(new Holder.Direct<>(this.function.value().mapAll(pVisitor)));
            }
        }

        public static record Point(DensityFunction.FunctionContext context) {
        }
    }

    interface TransformerWithContext extends DensityFunction {
        DensityFunction input();

        @Override
        default double compute(DensityFunction.FunctionContext pContext) {
            return this.transform(pContext, this.input().compute(pContext));
        }

        @Override
        default void fillArray(double[] pArray, DensityFunction.ContextProvider pContextProvider) {
            this.input().fillArray(pArray, pContextProvider);

            for (int i = 0; i < pArray.length; i++) {
                pArray[i] = this.transform(pContextProvider.forIndex(i), pArray[i]);
            }
        }

        double transform(DensityFunction.FunctionContext pContext, double pValue);
    }

    interface TwoArgumentSimpleFunction extends DensityFunction {
        Logger LOGGER = LogUtils.getLogger();

        static DensityFunctions.TwoArgumentSimpleFunction create(
            DensityFunctions.TwoArgumentSimpleFunction.Type pType, DensityFunction pArgument1, DensityFunction pArgument2
        ) {
            double d0 = pArgument1.minValue();
            double d1 = pArgument2.minValue();
            double d2 = pArgument1.maxValue();
            double d3 = pArgument2.maxValue();
            if (pType == DensityFunctions.TwoArgumentSimpleFunction.Type.MIN || pType == DensityFunctions.TwoArgumentSimpleFunction.Type.MAX) {
                boolean flag = d0 >= d3;
                boolean flag1 = d1 >= d2;
                if (flag || flag1) {
                    LOGGER.warn("Creating a " + pType + " function between two non-overlapping inputs: " + pArgument1 + " and " + pArgument2);
                }
            }
            double d5 = switch (pType) {
                case ADD -> d0 + d1;
                case MUL -> d0 > 0.0 && d1 > 0.0 ? d0 * d1 : (d2 < 0.0 && d3 < 0.0 ? d2 * d3 : Math.min(d0 * d3, d2 * d1));
                case MIN -> Math.min(d0, d1);
                case MAX -> Math.max(d0, d1);
            };

            double d4 = switch (pType) {
                case ADD -> d2 + d3;
                case MUL -> d0 > 0.0 && d1 > 0.0 ? d2 * d3 : (d2 < 0.0 && d3 < 0.0 ? d0 * d1 : Math.max(d0 * d1, d2 * d3));
                case MIN -> Math.min(d2, d3);
                case MAX -> Math.max(d2, d3);
            };
            if (pType == DensityFunctions.TwoArgumentSimpleFunction.Type.MUL || pType == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD) {
                if (pArgument1 instanceof DensityFunctions.Constant densityfunctions$constant1) {
                    return new DensityFunctions.MulOrAdd(
                        pType == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD
                            ? DensityFunctions.MulOrAdd.Type.ADD
                            : DensityFunctions.MulOrAdd.Type.MUL,
                        pArgument2,
                        d5,
                        d4,
                        densityfunctions$constant1.value
                    );
                }

                if (pArgument2 instanceof DensityFunctions.Constant densityfunctions$constant) {
                    return new DensityFunctions.MulOrAdd(
                        pType == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD
                            ? DensityFunctions.MulOrAdd.Type.ADD
                            : DensityFunctions.MulOrAdd.Type.MUL,
                        pArgument1,
                        d5,
                        d4,
                        densityfunctions$constant.value
                    );
                }
            }

            return new DensityFunctions.Ap2(pType, pArgument1, pArgument2, d5, d4);
        }

        DensityFunctions.TwoArgumentSimpleFunction.Type type();

        DensityFunction argument1();

        DensityFunction argument2();

        @Override
        default KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return this.type().codec;
        }

        public static enum Type implements StringRepresentable {
            ADD("add"),
            MUL("mul"),
            MIN("min"),
            MAX("max");

            final KeyDispatchDataCodec<DensityFunctions.TwoArgumentSimpleFunction> codec = DensityFunctions.doubleFunctionArgumentCodec(
                (p_209092_, p_209093_) -> DensityFunctions.TwoArgumentSimpleFunction.create(this, p_209092_, p_209093_),
                DensityFunctions.TwoArgumentSimpleFunction::argument1,
                DensityFunctions.TwoArgumentSimpleFunction::argument2
            );
            private final String name;

            private Type(String pName) {
                this.name = pName;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }
        }
    }

    protected static record WeirdScaledSampler(
        DensityFunction input, DensityFunction.NoiseHolder noise, DensityFunctions.WeirdScaledSampler.RarityValueMapper rarityValueMapper
    ) implements DensityFunctions.TransformerWithContext {
        private static final MapCodec<DensityFunctions.WeirdScaledSampler> DATA_CODEC = RecordCodecBuilder.mapCodec(
            p_208438_ -> p_208438_.group(
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(DensityFunctions.WeirdScaledSampler::input),
                        DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(DensityFunctions.WeirdScaledSampler::noise),
                        DensityFunctions.WeirdScaledSampler.RarityValueMapper.CODEC
                            .fieldOf("rarity_value_mapper")
                            .forGetter(DensityFunctions.WeirdScaledSampler::rarityValueMapper)
                    )
                    .apply(p_208438_, DensityFunctions.WeirdScaledSampler::new)
        );
        public static final KeyDispatchDataCodec<DensityFunctions.WeirdScaledSampler> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double transform(DensityFunction.FunctionContext pContext, double pValue) {
            double d0 = this.rarityValueMapper.mapper.get(pValue);
            return d0 * Math.abs(this.noise.getValue((double)pContext.blockX() / d0, (double)pContext.blockY() / d0, (double)pContext.blockZ() / d0));
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor pVisitor) {
            return pVisitor.apply(
                new DensityFunctions.WeirdScaledSampler(this.input.mapAll(pVisitor), pVisitor.visitNoise(this.noise), this.rarityValueMapper)
            );
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            return this.rarityValueMapper.maxRarity * this.noise.maxValue();
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }

        public static enum RarityValueMapper implements StringRepresentable {
            TYPE1("type_1", NoiseRouterData.QuantizedSpaghettiRarity::getSpaghettiRarity3D, 2.0),
            TYPE2("type_2", NoiseRouterData.QuantizedSpaghettiRarity::getSphaghettiRarity2D, 3.0);

            public static final Codec<DensityFunctions.WeirdScaledSampler.RarityValueMapper> CODEC = StringRepresentable.fromEnum(
                DensityFunctions.WeirdScaledSampler.RarityValueMapper::values
            );
            private final String name;
            final Double2DoubleFunction mapper;
            final double maxRarity;

            private RarityValueMapper(String pName, Double2DoubleFunction pMapper, double pMaxRarity) {
                this.name = pName;
                this.mapper = pMapper;
                this.maxRarity = pMaxRarity;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }
        }
    }

    static record YClampedGradient(int fromY, int toY, double fromValue, double toValue) implements DensityFunction.SimpleFunction {
        private static final MapCodec<DensityFunctions.YClampedGradient> DATA_CODEC = RecordCodecBuilder.mapCodec(
            p_208494_ -> p_208494_.group(
                        Codec.intRange(DimensionType.MIN_Y * 2, DimensionType.MAX_Y * 2).fieldOf("from_y").forGetter(DensityFunctions.YClampedGradient::fromY),
                        Codec.intRange(DimensionType.MIN_Y * 2, DimensionType.MAX_Y * 2).fieldOf("to_y").forGetter(DensityFunctions.YClampedGradient::toY),
                        DensityFunctions.NOISE_VALUE_CODEC.fieldOf("from_value").forGetter(DensityFunctions.YClampedGradient::fromValue),
                        DensityFunctions.NOISE_VALUE_CODEC.fieldOf("to_value").forGetter(DensityFunctions.YClampedGradient::toValue)
                    )
                    .apply(p_208494_, DensityFunctions.YClampedGradient::new)
        );
        public static final KeyDispatchDataCodec<DensityFunctions.YClampedGradient> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(DensityFunction.FunctionContext pContext) {
            return Mth.clampedMap((double)pContext.blockY(), (double)this.fromY, (double)this.toY, this.fromValue, this.toValue);
        }

        @Override
        public double minValue() {
            return Math.min(this.fromValue, this.toValue);
        }

        @Override
        public double maxValue() {
            return Math.max(this.fromValue, this.toValue);
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }
}
