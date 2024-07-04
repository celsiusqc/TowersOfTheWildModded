package net.minecraft.client.resources.server;

import java.util.UUID;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface PackLoadFeedback {
    void reportUpdate(UUID pId, PackLoadFeedback.Update pUpdate);

    void reportFinalResult(UUID pId, PackLoadFeedback.FinalResult pResult);

    @OnlyIn(Dist.CLIENT)
    public static enum FinalResult {
        DECLINED,
        APPLIED,
        DISCARDED,
        DOWNLOAD_FAILED,
        ACTIVATION_FAILED;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Update {
        ACCEPTED,
        DOWNLOADED;
    }
}
