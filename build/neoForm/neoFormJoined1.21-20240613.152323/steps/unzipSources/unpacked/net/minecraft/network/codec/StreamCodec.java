package net.minecraft.network.codec;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import io.netty.buffer.ByteBuf;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {
    static <B, V> StreamCodec<B, V> of(final StreamEncoder<B, V> pEncoder, final StreamDecoder<B, V> pDecoder) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(B p_319945_) {
                return pDecoder.decode(p_319945_);
            }

            @Override
            public void encode(B p_320538_, V p_320754_) {
                pEncoder.encode(p_320538_, p_320754_);
            }
        };
    }

    static <B, V> StreamCodec<B, V> ofMember(final StreamMemberEncoder<B, V> pEncoder, final StreamDecoder<B, V> pDecoder) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(B p_320797_) {
                return pDecoder.decode(p_320797_);
            }

            @Override
            public void encode(B p_319939_, V p_320568_) {
                pEncoder.encode(p_320568_, p_319939_);
            }
        };
    }

    static <B, V> StreamCodec<B, V> unit(final V p_320438_) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(B p_320572_) {
                return p_320438_;
            }

            @Override
            public void encode(B p_320044_, V p_320328_) {
                if (!p_320328_.equals(p_320438_)) {
                    throw new IllegalStateException("Can't encode '" + p_320328_ + "', expected '" + p_320438_ + "'");
                }
            }
        };
    }

    default <O> StreamCodec<B, O> apply(StreamCodec.CodecOperation<B, V, O> pOperation) {
        return pOperation.apply(this);
    }

    default <O> StreamCodec<B, O> map(final Function<? super V, ? extends O> pFactory, final Function<? super O, ? extends V> pGetter) {
        return new StreamCodec<B, O>() {
            @Override
            public O decode(B p_320534_) {
                return (O)pFactory.apply(StreamCodec.this.decode(p_320534_));
            }

            @Override
            public void encode(B p_319798_, O p_320273_) {
                StreamCodec.this.encode(p_319798_, (V)pGetter.apply(p_320273_));
            }
        };
    }

    default <O extends ByteBuf> StreamCodec<O, V> mapStream(final Function<O, ? extends B> pBufferFactory) {
        return new StreamCodec<O, V>() {
            public V decode(O p_319818_) {
                B b = (B)pBufferFactory.apply(p_319818_);
                return StreamCodec.this.decode(b);
            }

            public void encode(O p_319973_, V p_319843_) {
                B b = (B)pBufferFactory.apply(p_319973_);
                StreamCodec.this.encode(b, p_319843_);
            }
        };
    }

    default <U> StreamCodec<B, U> dispatch(
        final Function<? super U, ? extends V> pKeyGetter, final Function<? super V, ? extends StreamCodec<? super B, ? extends U>> pCodecGetter
    ) {
        return new StreamCodec<B, U>() {
            @Override
            public U decode(B p_320094_) {
                V v = StreamCodec.this.decode(p_320094_);
                StreamCodec<? super B, ? extends U> streamcodec = (StreamCodec<? super B, ? extends U>)pCodecGetter.apply(v);
                return (U)streamcodec.decode(p_320094_);
            }

            @Override
            public void encode(B p_320767_, U p_320010_) {
                V v = (V)pKeyGetter.apply(p_320010_);
                StreamCodec<B, U> streamcodec = (StreamCodec<B, U>)pCodecGetter.apply(v);
                StreamCodec.this.encode(p_320767_, v);
                streamcodec.encode(p_320767_, p_320010_);
            }
        };
    }

    static <B, C, T1> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> pCodec, final Function<C, T1> pGetter, final Function<T1, C> pFactory) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_320924_) {
                T1 t1 = pCodec.decode(p_320924_);
                return pFactory.apply(t1);
            }

            @Override
            public void encode(B p_320798_, C p_320749_) {
                pCodec.encode(p_320798_, pGetter.apply(p_320749_));
            }
        };
    }

    static <B, C, T1, T2> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> pCodec1,
        final Function<C, T1> pGetter1,
        final StreamCodec<? super B, T2> pCodec2,
        final Function<C, T2> pGetter2,
        final BiFunction<T1, T2, C> pFactory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_320168_) {
                T1 t1 = pCodec1.decode(p_320168_);
                T2 t2 = pCodec2.decode(p_320168_);
                return pFactory.apply(t1, t2);
            }

            @Override
            public void encode(B p_320592_, C p_320163_) {
                pCodec1.encode(p_320592_, pGetter1.apply(p_320163_));
                pCodec2.encode(p_320592_, pGetter2.apply(p_320163_));
            }
        };
    }

    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> pCodec1,
        final Function<C, T1> pGetter1,
        final StreamCodec<? super B, T2> pCodec2,
        final Function<C, T2> pGetter2,
        final StreamCodec<? super B, T3> pCodec3,
        final Function<C, T3> pGetter3,
        final Function3<T1, T2, T3, C> pFactory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_320842_) {
                T1 t1 = pCodec1.decode(p_320842_);
                T2 t2 = pCodec2.decode(p_320842_);
                T3 t3 = pCodec3.decode(p_320842_);
                return pFactory.apply(t1, t2, t3);
            }

            @Override
            public void encode(B p_320737_, C p_320439_) {
                pCodec1.encode(p_320737_, pGetter1.apply(p_320439_));
                pCodec2.encode(p_320737_, pGetter2.apply(p_320439_));
                pCodec3.encode(p_320737_, pGetter3.apply(p_320439_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> pCodec1,
        final Function<C, T1> pGetter1,
        final StreamCodec<? super B, T2> pCodec2,
        final Function<C, T2> pGetter2,
        final StreamCodec<? super B, T3> pCodec3,
        final Function<C, T3> pGetter3,
        final StreamCodec<? super B, T4> pCodec4,
        final Function<C, T4> pGetter4,
        final Function4<T1, T2, T3, T4, C> pFactory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_323859_) {
                T1 t1 = pCodec1.decode(p_323859_);
                T2 t2 = pCodec2.decode(p_323859_);
                T3 t3 = pCodec3.decode(p_323859_);
                T4 t4 = pCodec4.decode(p_323859_);
                return pFactory.apply(t1, t2, t3, t4);
            }

            @Override
            public void encode(B p_323667_, C p_323469_) {
                pCodec1.encode(p_323667_, pGetter1.apply(p_323469_));
                pCodec2.encode(p_323667_, pGetter2.apply(p_323469_));
                pCodec3.encode(p_323667_, pGetter3.apply(p_323469_));
                pCodec4.encode(p_323667_, pGetter4.apply(p_323469_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> pCodec1,
        final Function<C, T1> pGetter1,
        final StreamCodec<? super B, T2> pCodec2,
        final Function<C, T2> pGetter2,
        final StreamCodec<? super B, T3> pCodec3,
        final Function<C, T3> pGetter3,
        final StreamCodec<? super B, T4> pCodec4,
        final Function<C, T4> pGetter4,
        final StreamCodec<? super B, T5> pCodec5,
        final Function<C, T5> pGetter5,
        final Function5<T1, T2, T3, T4, T5, C> pFactory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_324610_) {
                T1 t1 = pCodec1.decode(p_324610_);
                T2 t2 = pCodec2.decode(p_324610_);
                T3 t3 = pCodec3.decode(p_324610_);
                T4 t4 = pCodec4.decode(p_324610_);
                T5 t5 = pCodec5.decode(p_324610_);
                return pFactory.apply(t1, t2, t3, t4, t5);
            }

            @Override
            public void encode(B p_323786_, C p_323619_) {
                pCodec1.encode(p_323786_, pGetter1.apply(p_323619_));
                pCodec2.encode(p_323786_, pGetter2.apply(p_323619_));
                pCodec3.encode(p_323786_, pGetter3.apply(p_323619_));
                pCodec4.encode(p_323786_, pGetter4.apply(p_323619_));
                pCodec5.encode(p_323786_, pGetter5.apply(p_323619_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> pCodec1,
        final Function<C, T1> pGetter1,
        final StreamCodec<? super B, T2> pCodec2,
        final Function<C, T2> pGetter2,
        final StreamCodec<? super B, T3> pCodec3,
        final Function<C, T3> pGetter3,
        final StreamCodec<? super B, T4> pCodec4,
        final Function<C, T4> pGetter4,
        final StreamCodec<? super B, T5> pCodec5,
        final Function<C, T5> pGetter5,
        final StreamCodec<? super B, T6> pCodec6,
        final Function<C, T6> pGetter6,
        final Function6<T1, T2, T3, T4, T5, T6, C> pFactory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_330310_) {
                T1 t1 = pCodec1.decode(p_330310_);
                T2 t2 = pCodec2.decode(p_330310_);
                T3 t3 = pCodec3.decode(p_330310_);
                T4 t4 = pCodec4.decode(p_330310_);
                T5 t5 = pCodec5.decode(p_330310_);
                T6 t6 = pCodec6.decode(p_330310_);
                return pFactory.apply(t1, t2, t3, t4, t5, t6);
            }

            @Override
            public void encode(B p_332052_, C p_331912_) {
                pCodec1.encode(p_332052_, pGetter1.apply(p_331912_));
                pCodec2.encode(p_332052_, pGetter2.apply(p_331912_));
                pCodec3.encode(p_332052_, pGetter3.apply(p_331912_));
                pCodec4.encode(p_332052_, pGetter4.apply(p_331912_));
                pCodec5.encode(p_332052_, pGetter5.apply(p_331912_));
                pCodec6.encode(p_332052_, pGetter6.apply(p_331912_));
            }
        };
    }

    static <B, T> StreamCodec<B, T> recursive(final UnaryOperator<StreamCodec<B, T>> pModifier) {
        return new StreamCodec<B, T>() {
            private final Supplier<StreamCodec<B, T>> inner = Suppliers.memoize(() -> pModifier.apply(this));

            @Override
            public T decode(B p_330903_) {
                return this.inner.get().decode(p_330903_);
            }

            @Override
            public void encode(B p_331641_, T p_330634_) {
                this.inner.get().encode(p_331641_, p_330634_);
            }
        };
    }

    default <S extends B> StreamCodec<S, V> cast() {
        return (StreamCodec<S, V>)this;
    }

    @FunctionalInterface
    public interface CodecOperation<B, S, T> {
        StreamCodec<B, T> apply(StreamCodec<B, S> pCodec);
    }
}
