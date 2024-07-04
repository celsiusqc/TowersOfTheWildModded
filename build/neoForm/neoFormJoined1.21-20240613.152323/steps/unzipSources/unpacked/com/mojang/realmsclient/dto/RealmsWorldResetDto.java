package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Set;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsWorldResetDto extends ValueObject implements ReflectionBasedSerialization {
    @SerializedName("seed")
    private final String seed;
    @SerializedName("worldTemplateId")
    private final long worldTemplateId;
    @SerializedName("levelType")
    private final int levelType;
    @SerializedName("generateStructures")
    private final boolean generateStructures;
    @SerializedName("experiments")
    private final Set<String> experiments;

    public RealmsWorldResetDto(String pSeed, long pWorldTemplateId, int pLevelType, boolean pGenerateStructures, Set<String> pExperiments) {
        this.seed = pSeed;
        this.worldTemplateId = pWorldTemplateId;
        this.levelType = pLevelType;
        this.generateStructures = pGenerateStructures;
        this.experiments = pExperiments;
    }
}
