package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsWorldOptions extends ValueObject {
    public final boolean pvp;
    public final boolean spawnAnimals;
    public final boolean spawnMonsters;
    public final boolean spawnNPCs;
    public final int spawnProtection;
    public final boolean commandBlocks;
    public final boolean forceGameMode;
    public final int difficulty;
    public final int gameMode;
    private final String slotName;
    public final String version;
    public final RealmsServer.Compatibility compatibility;
    public long templateId;
    @Nullable
    public String templateImage;
    public boolean empty;
    private static final boolean DEFAULT_FORCE_GAME_MODE = false;
    private static final boolean DEFAULT_PVP = true;
    private static final boolean DEFAULT_SPAWN_ANIMALS = true;
    private static final boolean DEFAULT_SPAWN_MONSTERS = true;
    private static final boolean DEFAULT_SPAWN_NPCS = true;
    private static final int DEFAULT_SPAWN_PROTECTION = 0;
    private static final boolean DEFAULT_COMMAND_BLOCKS = false;
    private static final int DEFAULT_DIFFICULTY = 2;
    private static final int DEFAULT_GAME_MODE = 0;
    private static final String DEFAULT_SLOT_NAME = "";
    private static final String DEFAULT_VERSION = "";
    private static final RealmsServer.Compatibility DEFAULT_COMPATIBILITY = RealmsServer.Compatibility.UNVERIFIABLE;
    private static final long DEFAULT_TEMPLATE_ID = -1L;
    private static final String DEFAULT_TEMPLATE_IMAGE = null;

    public RealmsWorldOptions(
        boolean pPvp,
        boolean pSpawnAnimals,
        boolean pSpawnMonsters,
        boolean pSpawnNPCs,
        int pSpawnProtection,
        boolean pCommandBlocks,
        int pDifficulty,
        int pGameMode,
        boolean pForceGameMode,
        String pSlotName,
        String pVersion,
        RealmsServer.Compatibility pCompatibility
    ) {
        this.pvp = pPvp;
        this.spawnAnimals = pSpawnAnimals;
        this.spawnMonsters = pSpawnMonsters;
        this.spawnNPCs = pSpawnNPCs;
        this.spawnProtection = pSpawnProtection;
        this.commandBlocks = pCommandBlocks;
        this.difficulty = pDifficulty;
        this.gameMode = pGameMode;
        this.forceGameMode = pForceGameMode;
        this.slotName = pSlotName;
        this.version = pVersion;
        this.compatibility = pCompatibility;
    }

    public static RealmsWorldOptions createDefaults() {
        return new RealmsWorldOptions(true, true, true, true, 0, false, 2, 0, false, "", "", DEFAULT_COMPATIBILITY);
    }

    public static RealmsWorldOptions createEmptyDefaults() {
        RealmsWorldOptions realmsworldoptions = createDefaults();
        realmsworldoptions.setEmpty(true);
        return realmsworldoptions;
    }

    public void setEmpty(boolean pEmpty) {
        this.empty = pEmpty;
    }

    public static RealmsWorldOptions parse(JsonObject pJson) {
        RealmsWorldOptions realmsworldoptions = new RealmsWorldOptions(
            JsonUtils.getBooleanOr("pvp", pJson, true),
            JsonUtils.getBooleanOr("spawnAnimals", pJson, true),
            JsonUtils.getBooleanOr("spawnMonsters", pJson, true),
            JsonUtils.getBooleanOr("spawnNPCs", pJson, true),
            JsonUtils.getIntOr("spawnProtection", pJson, 0),
            JsonUtils.getBooleanOr("commandBlocks", pJson, false),
            JsonUtils.getIntOr("difficulty", pJson, 2),
            JsonUtils.getIntOr("gameMode", pJson, 0),
            JsonUtils.getBooleanOr("forceGameMode", pJson, false),
            JsonUtils.getRequiredStringOr("slotName", pJson, ""),
            JsonUtils.getRequiredStringOr("version", pJson, ""),
            RealmsServer.getCompatibility(JsonUtils.getRequiredStringOr("compatibility", pJson, RealmsServer.Compatibility.UNVERIFIABLE.name()))
        );
        realmsworldoptions.templateId = JsonUtils.getLongOr("worldTemplateId", pJson, -1L);
        realmsworldoptions.templateImage = JsonUtils.getStringOr("worldTemplateImage", pJson, DEFAULT_TEMPLATE_IMAGE);
        return realmsworldoptions;
    }

    public String getSlotName(int pSlotIndex) {
        if (StringUtil.isBlank(this.slotName)) {
            return this.empty ? I18n.get("mco.configure.world.slot.empty") : this.getDefaultSlotName(pSlotIndex);
        } else {
            return this.slotName;
        }
    }

    public String getDefaultSlotName(int pSlotIndex) {
        return I18n.get("mco.configure.world.slot", pSlotIndex);
    }

    public String toJson() {
        JsonObject jsonobject = new JsonObject();
        if (!this.pvp) {
            jsonobject.addProperty("pvp", this.pvp);
        }

        if (!this.spawnAnimals) {
            jsonobject.addProperty("spawnAnimals", this.spawnAnimals);
        }

        if (!this.spawnMonsters) {
            jsonobject.addProperty("spawnMonsters", this.spawnMonsters);
        }

        if (!this.spawnNPCs) {
            jsonobject.addProperty("spawnNPCs", this.spawnNPCs);
        }

        if (this.spawnProtection != 0) {
            jsonobject.addProperty("spawnProtection", this.spawnProtection);
        }

        if (this.commandBlocks) {
            jsonobject.addProperty("commandBlocks", this.commandBlocks);
        }

        if (this.difficulty != 2) {
            jsonobject.addProperty("difficulty", this.difficulty);
        }

        if (this.gameMode != 0) {
            jsonobject.addProperty("gameMode", this.gameMode);
        }

        if (this.forceGameMode) {
            jsonobject.addProperty("forceGameMode", this.forceGameMode);
        }

        if (!Objects.equals(this.slotName, "")) {
            jsonobject.addProperty("slotName", this.slotName);
        }

        if (!Objects.equals(this.version, "")) {
            jsonobject.addProperty("version", this.version);
        }

        if (this.compatibility != DEFAULT_COMPATIBILITY) {
            jsonobject.addProperty("compatibility", this.compatibility.name());
        }

        return jsonobject.toString();
    }

    public RealmsWorldOptions clone() {
        return new RealmsWorldOptions(
            this.pvp,
            this.spawnAnimals,
            this.spawnMonsters,
            this.spawnNPCs,
            this.spawnProtection,
            this.commandBlocks,
            this.difficulty,
            this.gameMode,
            this.forceGameMode,
            this.slotName,
            this.version,
            this.compatibility
        );
    }
}
