package net.minecraft.world.scores;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;

public interface ReadOnlyScoreInfo {
    int value();

    boolean isLocked();

    @Nullable
    NumberFormat numberFormat();

    default MutableComponent formatValue(NumberFormat pFormat) {
        return Objects.requireNonNullElse(this.numberFormat(), pFormat).format(this.value());
    }

    static MutableComponent safeFormatValue(@Nullable ReadOnlyScoreInfo pScoreInfo, NumberFormat pFormat) {
        return pScoreInfo != null ? pScoreInfo.formatValue(pFormat) : pFormat.format(0);
    }
}
