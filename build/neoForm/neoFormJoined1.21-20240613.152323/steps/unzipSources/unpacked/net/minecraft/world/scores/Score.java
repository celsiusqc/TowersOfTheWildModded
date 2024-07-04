package net.minecraft.world.scores;

import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatTypes;

public class Score implements ReadOnlyScoreInfo {
    private static final String TAG_SCORE = "Score";
    private static final String TAG_LOCKED = "Locked";
    private static final String TAG_DISPLAY = "display";
    private static final String TAG_FORMAT = "format";
    private int value;
    private boolean locked = true;
    @Nullable
    private Component display;
    @Nullable
    private NumberFormat numberFormat;

    @Override
    public int value() {
        return this.value;
    }

    public void value(int pValue) {
        this.value = pValue;
    }

    @Override
    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean pLocked) {
        this.locked = pLocked;
    }

    @Nullable
    public Component display() {
        return this.display;
    }

    public void display(@Nullable Component pDisplay) {
        this.display = pDisplay;
    }

    @Nullable
    @Override
    public NumberFormat numberFormat() {
        return this.numberFormat;
    }

    public void numberFormat(@Nullable NumberFormat pNumberFormat) {
        this.numberFormat = pNumberFormat;
    }

    public CompoundTag write(HolderLookup.Provider pLevelRegistry) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putInt("Score", this.value);
        compoundtag.putBoolean("Locked", this.locked);
        if (this.display != null) {
            compoundtag.putString("display", Component.Serializer.toJson(this.display, pLevelRegistry));
        }

        if (this.numberFormat != null) {
            NumberFormatTypes.CODEC
                .encodeStart(pLevelRegistry.createSerializationContext(NbtOps.INSTANCE), this.numberFormat)
                .ifSuccess(p_313666_ -> compoundtag.put("format", p_313666_));
        }

        return compoundtag;
    }

    public static Score read(CompoundTag pTag, HolderLookup.Provider pLevelRegistry) {
        Score score = new Score();
        score.value = pTag.getInt("Score");
        score.locked = pTag.getBoolean("Locked");
        if (pTag.contains("display", 8)) {
            score.display = Component.Serializer.fromJson(pTag.getString("display"), pLevelRegistry);
        }

        if (pTag.contains("format", 10)) {
            NumberFormatTypes.CODEC
                .parse(pLevelRegistry.createSerializationContext(NbtOps.INSTANCE), pTag.get("format"))
                .ifSuccess(p_313664_ -> score.numberFormat = p_313664_);
        }

        return score;
    }
}
