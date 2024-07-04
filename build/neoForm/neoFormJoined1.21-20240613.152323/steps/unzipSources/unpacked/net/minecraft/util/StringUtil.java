package net.minecraft.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public class StringUtil {
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");
    private static final Pattern LINE_PATTERN = Pattern.compile("\\r\\n|\\v");
    private static final Pattern LINE_END_PATTERN = Pattern.compile("(?:\\r\\n|\\v)$");

    public static String formatTickDuration(int pTicks, float pTicksPerSecond) {
        int i = Mth.floor((float)pTicks / pTicksPerSecond);
        int j = i / 60;
        i %= 60;
        int k = j / 60;
        j %= 60;
        return k > 0 ? String.format(Locale.ROOT, "%02d:%02d:%02d", k, j, i) : String.format(Locale.ROOT, "%02d:%02d", j, i);
    }

    public static String stripColor(String pText) {
        return STRIP_COLOR_PATTERN.matcher(pText).replaceAll("");
    }

    /**
     * Returns a value indicating whether the given string is null or empty.
     */
    public static boolean isNullOrEmpty(@Nullable String pString) {
        return StringUtils.isEmpty(pString);
    }

    public static String truncateStringIfNecessary(String pString, int pMaxSize, boolean pAddEllipsis) {
        if (pString.length() <= pMaxSize) {
            return pString;
        } else {
            return pAddEllipsis && pMaxSize > 3 ? pString.substring(0, pMaxSize - 3) + "..." : pString.substring(0, pMaxSize);
        }
    }

    public static int lineCount(String pString) {
        if (pString.isEmpty()) {
            return 0;
        } else {
            Matcher matcher = LINE_PATTERN.matcher(pString);
            int i = 1;

            while (matcher.find()) {
                i++;
            }

            return i;
        }
    }

    public static boolean endsWithNewLine(String pString) {
        return LINE_END_PATTERN.matcher(pString).find();
    }

    public static String trimChatMessage(String pString) {
        return truncateStringIfNecessary(pString, 256, false);
    }

    public static boolean isAllowedChatCharacter(char pCharacter) {
        return pCharacter != 167 && pCharacter >= ' ' && pCharacter != 127;
    }

    public static boolean isValidPlayerName(String pPlayerName) {
        return pPlayerName.length() > 16 ? false : pPlayerName.chars().filter(p_332111_ -> p_332111_ <= 32 || p_332111_ >= 127).findAny().isEmpty();
    }

    public static String filterText(String pText) {
        return filterText(pText, false);
    }

    public static String filterText(String pText, boolean pAllowLineBreaks) {
        StringBuilder stringbuilder = new StringBuilder();

        for (char c0 : pText.toCharArray()) {
            if (isAllowedChatCharacter(c0)) {
                stringbuilder.append(c0);
            } else if (pAllowLineBreaks && c0 == '\n') {
                stringbuilder.append(c0);
            }
        }

        return stringbuilder.toString();
    }

    public static boolean isWhitespace(int p_331212_) {
        return Character.isWhitespace(p_331212_) || Character.isSpaceChar(p_331212_);
    }

    public static boolean isBlank(@Nullable String pString) {
        return pString != null && pString.length() != 0 ? pString.chars().allMatch(StringUtil::isWhitespace) : true;
    }
}
