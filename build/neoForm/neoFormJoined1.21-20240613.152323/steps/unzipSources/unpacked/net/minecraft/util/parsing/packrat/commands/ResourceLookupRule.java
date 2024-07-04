package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;

public abstract class ResourceLookupRule<C, V> implements Rule<StringReader, V>, ResourceSuggestion {
    private final Atom<ResourceLocation> idParser;
    protected final C context;

    protected ResourceLookupRule(Atom<ResourceLocation> pIdParser, C pContext) {
        this.idParser = pIdParser;
        this.context = pContext;
    }

    @Override
    public Optional<V> parse(ParseState<StringReader> pParseState) {
        pParseState.input().skipWhitespace();
        int i = pParseState.mark();
        Optional<ResourceLocation> optional = pParseState.parse(this.idParser);
        if (optional.isPresent()) {
            try {
                return Optional.of(this.validateElement(pParseState.input(), optional.get()));
            } catch (Exception exception) {
                pParseState.errorCollector().store(i, this, exception);
                return Optional.empty();
            }
        } else {
            pParseState.errorCollector().store(i, this, ResourceLocation.ERROR_INVALID.createWithContext(pParseState.input()));
            return Optional.empty();
        }
    }

    protected abstract V validateElement(ImmutableStringReader pReader, ResourceLocation pElementType) throws Exception;
}
