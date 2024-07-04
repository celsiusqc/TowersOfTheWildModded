package net.minecraft.commands.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;

public record StringTemplate(List<String> segments, List<String> variables) {
    public static StringTemplate fromString(String pName, int pLineNumber) {
        Builder<String> builder = ImmutableList.builder();
        Builder<String> builder1 = ImmutableList.builder();
        int i = pName.length();
        int j = 0;
        int k = pName.indexOf(36);

        while (k != -1) {
            if (k != i - 1 && pName.charAt(k + 1) == '(') {
                builder.add(pName.substring(j, k));
                int l = pName.indexOf(41, k + 1);
                if (l == -1) {
                    throw new IllegalArgumentException("Unterminated macro variable in macro '" + pName + "' on line " + pLineNumber);
                }

                String s = pName.substring(k + 2, l);
                if (!isValidVariableName(s)) {
                    throw new IllegalArgumentException("Invalid macro variable name '" + s + "' on line " + pLineNumber);
                }

                builder1.add(s);
                j = l + 1;
                k = pName.indexOf(36, j);
            } else {
                k = pName.indexOf(36, k + 1);
            }
        }

        if (j == 0) {
            throw new IllegalArgumentException("Macro without variables on line " + pLineNumber);
        } else {
            if (j != i) {
                builder.add(pName.substring(j));
            }

            return new StringTemplate(builder.build(), builder1.build());
        }
    }

    private static boolean isValidVariableName(String pVariableName) {
        for (int i = 0; i < pVariableName.length(); i++) {
            char c0 = pVariableName.charAt(i);
            if (!Character.isLetterOrDigit(c0) && c0 != '_') {
                return false;
            }
        }

        return true;
    }

    public String substitute(List<String> pArguments) {
        StringBuilder stringbuilder = new StringBuilder();

        for (int i = 0; i < this.variables.size(); i++) {
            stringbuilder.append(this.segments.get(i)).append(pArguments.get(i));
            CommandFunction.checkCommandLineLength(stringbuilder);
        }

        if (this.segments.size() > this.variables.size()) {
            stringbuilder.append(this.segments.get(this.segments.size() - 1));
        }

        CommandFunction.checkCommandLineLength(stringbuilder);
        return stringbuilder.toString();
    }
}
