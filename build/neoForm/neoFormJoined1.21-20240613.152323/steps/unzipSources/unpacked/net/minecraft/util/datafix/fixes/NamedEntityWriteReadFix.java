package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.View;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.functions.PointFreeRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.BitSet;
import net.minecraft.Util;

public abstract class NamedEntityWriteReadFix extends DataFix {
    private final String name;
    private final String entityName;
    private final TypeReference type;

    public NamedEntityWriteReadFix(Schema pOutputSchema, boolean pChangesType, String pName, TypeReference pType, String pEntityName) {
        super(pOutputSchema, pChangesType);
        this.name = pName;
        this.type = pType;
        this.entityName = pEntityName;
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(this.type);
        Type<?> type1 = this.getInputSchema().getChoiceType(this.type, this.entityName);
        Type<?> type2 = this.getOutputSchema().getType(this.type);
        Type<?> type3 = this.getOutputSchema().getChoiceType(this.type, this.entityName);
        OpticFinder<?> opticfinder = DSL.namedChoice(this.entityName, type1);
        Type<?> type4 = type1.all(typePatcher(type, type2), true, false).view().newType();
        return this.fix(type, type2, opticfinder, type3, type4);
    }

    private <S, T, A, B> TypeRewriteRule fix(Type<S> pInputType, Type<T> pOutputType, OpticFinder<A> pFinder, Type<B> pOutputChoiceType, Type<?> pNewType) {
        return this.fixTypeEverywhere(this.name, pInputType, pOutputType, p_323223_ -> p_323218_ -> {
                Typed<S> typed = new Typed<>(pInputType, p_323223_, p_323218_);
                return (T)typed.update(pFinder, pOutputChoiceType, p_323212_ -> {
                    Typed<A> typed1 = new Typed<>((Type<A>)pNewType, p_323223_, p_323212_);
                    return Util.<A, B>writeAndReadTypedOrThrow(typed1, pOutputChoiceType, this::fix).getValue();
                }).getValue();
            });
    }

    private static <A, B> TypeRewriteRule typePatcher(Type<A> pType, Type<B> pNewType) {
        RewriteResult<A, B> rewriteresult = RewriteResult.create(View.create("Patcher", pType, pNewType, p_323208_ -> p_323224_ -> {
                throw new UnsupportedOperationException();
            }), new BitSet());
        return TypeRewriteRule.everywhere(TypeRewriteRule.ifSame(pType, rewriteresult), PointFreeRule.nop(), true, true);
    }

    protected abstract <T> Dynamic<T> fix(Dynamic<T> p_307318_);
}
