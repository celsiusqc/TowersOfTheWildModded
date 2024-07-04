package net.minecraft.commands.functions;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.resources.ResourceLocation;

class FunctionBuilder<T extends ExecutionCommandSource<T>> {
    @Nullable
    private List<UnboundEntryAction<T>> plainEntries = new ArrayList<>();
    @Nullable
    private List<MacroFunction.Entry<T>> macroEntries;
    private final List<String> macroArguments = new ArrayList<>();

    public void addCommand(UnboundEntryAction<T> pCommand) {
        if (this.macroEntries != null) {
            this.macroEntries.add(new MacroFunction.PlainTextEntry<>(pCommand));
        } else {
            this.plainEntries.add(pCommand);
        }
    }

    private int getArgumentIndex(String pArgument) {
        int i = this.macroArguments.indexOf(pArgument);
        if (i == -1) {
            i = this.macroArguments.size();
            this.macroArguments.add(pArgument);
        }

        return i;
    }

    private IntList convertToIndices(List<String> pArguments) {
        IntArrayList intarraylist = new IntArrayList(pArguments.size());

        for (String s : pArguments) {
            intarraylist.add(this.getArgumentIndex(s));
        }

        return intarraylist;
    }

    public void addMacro(String pName, int pLineNumber, T pCompilationContext) {
        StringTemplate stringtemplate = StringTemplate.fromString(pName, pLineNumber);
        if (this.plainEntries != null) {
            this.macroEntries = new ArrayList<>(this.plainEntries.size() + 1);

            for (UnboundEntryAction<T> unboundentryaction : this.plainEntries) {
                this.macroEntries.add(new MacroFunction.PlainTextEntry<>(unboundentryaction));
            }

            this.plainEntries = null;
        }

        this.macroEntries.add(new MacroFunction.MacroEntry<>(stringtemplate, this.convertToIndices(stringtemplate.variables()), pCompilationContext));
    }

    public CommandFunction<T> build(ResourceLocation pId) {
        return (CommandFunction<T>)(this.macroEntries != null
            ? new MacroFunction<>(pId, this.macroEntries, this.macroArguments)
            : new PlainTextFunction<>(pId, this.plainEntries));
    }
}
