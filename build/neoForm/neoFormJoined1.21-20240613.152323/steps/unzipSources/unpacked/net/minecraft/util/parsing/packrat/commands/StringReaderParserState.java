package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.ErrorCollector;
import net.minecraft.util.parsing.packrat.ParseState;

public class StringReaderParserState extends ParseState<StringReader> {
    private final StringReader input;

    public StringReaderParserState(Dictionary<StringReader> pDictionary, ErrorCollector<StringReader> pErrorCollector, StringReader pInput) {
        super(pDictionary, pErrorCollector);
        this.input = pInput;
    }

    public StringReader input() {
        return this.input;
    }

    @Override
    public int mark() {
        return this.input.getCursor();
    }

    @Override
    public void restore(int pCursor) {
        this.input.setCursor(pCursor);
    }
}
