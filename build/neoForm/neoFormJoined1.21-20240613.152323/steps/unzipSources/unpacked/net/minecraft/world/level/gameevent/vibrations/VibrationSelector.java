package net.minecraft.world.level.gameevent.vibrations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;

public class VibrationSelector {
    public static final Codec<VibrationSelector> CODEC = RecordCodecBuilder.create(
        p_338089_ -> p_338089_.group(
                    VibrationInfo.CODEC.lenientOptionalFieldOf("event").forGetter(p_251862_ -> p_251862_.currentVibrationData.map(Pair::getLeft)),
                    Codec.LONG.fieldOf("tick").forGetter(p_251458_ -> p_251458_.currentVibrationData.map(Pair::getRight).orElse(-1L))
                )
                .apply(p_338089_, VibrationSelector::new)
    );
    private Optional<Pair<VibrationInfo, Long>> currentVibrationData;

    public VibrationSelector(Optional<VibrationInfo> p_251736_, long p_251649_) {
        this.currentVibrationData = p_251736_.map(p_251571_ -> Pair.of(p_251571_, p_251649_));
    }

    public VibrationSelector() {
        this.currentVibrationData = Optional.empty();
    }

    public void addCandidate(VibrationInfo pVibrationInfo, long pTick) {
        if (this.shouldReplaceVibration(pVibrationInfo, pTick)) {
            this.currentVibrationData = Optional.of(Pair.of(pVibrationInfo, pTick));
        }
    }

    private boolean shouldReplaceVibration(VibrationInfo pVibrationInfo, long pTick) {
        if (this.currentVibrationData.isEmpty()) {
            return true;
        } else {
            Pair<VibrationInfo, Long> pair = this.currentVibrationData.get();
            long i = pair.getRight();
            if (pTick != i) {
                return false;
            } else {
                VibrationInfo vibrationinfo = pair.getLeft();
                if (pVibrationInfo.distance() < vibrationinfo.distance()) {
                    return true;
                } else {
                    return pVibrationInfo.distance() > vibrationinfo.distance()
                        ? false
                        : VibrationSystem.getGameEventFrequency(pVibrationInfo.gameEvent()) > VibrationSystem.getGameEventFrequency(vibrationinfo.gameEvent());
                }
            }
        }
    }

    public Optional<VibrationInfo> chosenCandidate(long pTick) {
        if (this.currentVibrationData.isEmpty()) {
            return Optional.empty();
        } else {
            return this.currentVibrationData.get().getRight() < pTick ? Optional.of(this.currentVibrationData.get().getLeft()) : Optional.empty();
        }
    }

    public void startOver() {
        this.currentVibrationData = Optional.empty();
    }
}
