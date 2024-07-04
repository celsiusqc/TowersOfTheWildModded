package com.mojang.realmsclient.dto;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ServerData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsServer extends ValueObject {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int NO_VALUE = -1;
    public long id;
    public String remoteSubscriptionId;
    public String name;
    public String motd;
    public RealmsServer.State state;
    public String owner;
    public UUID ownerUUID = Util.NIL_UUID;
    public List<PlayerInfo> players;
    public Map<Integer, RealmsWorldOptions> slots;
    public boolean expired;
    public boolean expiredTrial;
    public int daysLeft;
    public RealmsServer.WorldType worldType;
    public int activeSlot;
    @Nullable
    public String minigameName;
    public int minigameId;
    public String minigameImage;
    public long parentRealmId = -1L;
    @Nullable
    public String parentWorldName;
    public String activeVersion = "";
    public RealmsServer.Compatibility compatibility = RealmsServer.Compatibility.UNVERIFIABLE;

    public String getDescription() {
        return this.motd;
    }

    public String getName() {
        return this.name;
    }

    @Nullable
    public String getMinigameName() {
        return this.minigameName;
    }

    public void setName(String pName) {
        this.name = pName;
    }

    public void setDescription(String pMotd) {
        this.motd = pMotd;
    }

    public static RealmsServer parse(JsonObject pJson) {
        RealmsServer realmsserver = new RealmsServer();

        try {
            realmsserver.id = JsonUtils.getLongOr("id", pJson, -1L);
            realmsserver.remoteSubscriptionId = JsonUtils.getStringOr("remoteSubscriptionId", pJson, null);
            realmsserver.name = JsonUtils.getStringOr("name", pJson, null);
            realmsserver.motd = JsonUtils.getStringOr("motd", pJson, null);
            realmsserver.state = getState(JsonUtils.getStringOr("state", pJson, RealmsServer.State.CLOSED.name()));
            realmsserver.owner = JsonUtils.getStringOr("owner", pJson, null);
            if (pJson.get("players") != null && pJson.get("players").isJsonArray()) {
                realmsserver.players = parseInvited(pJson.get("players").getAsJsonArray());
                sortInvited(realmsserver);
            } else {
                realmsserver.players = Lists.newArrayList();
            }

            realmsserver.daysLeft = JsonUtils.getIntOr("daysLeft", pJson, 0);
            realmsserver.expired = JsonUtils.getBooleanOr("expired", pJson, false);
            realmsserver.expiredTrial = JsonUtils.getBooleanOr("expiredTrial", pJson, false);
            realmsserver.worldType = getWorldType(JsonUtils.getStringOr("worldType", pJson, RealmsServer.WorldType.NORMAL.name()));
            realmsserver.ownerUUID = JsonUtils.getUuidOr("ownerUUID", pJson, Util.NIL_UUID);
            if (pJson.get("slots") != null && pJson.get("slots").isJsonArray()) {
                realmsserver.slots = parseSlots(pJson.get("slots").getAsJsonArray());
            } else {
                realmsserver.slots = createEmptySlots();
            }

            realmsserver.minigameName = JsonUtils.getStringOr("minigameName", pJson, null);
            realmsserver.activeSlot = JsonUtils.getIntOr("activeSlot", pJson, -1);
            realmsserver.minigameId = JsonUtils.getIntOr("minigameId", pJson, -1);
            realmsserver.minigameImage = JsonUtils.getStringOr("minigameImage", pJson, null);
            realmsserver.parentRealmId = JsonUtils.getLongOr("parentWorldId", pJson, -1L);
            realmsserver.parentWorldName = JsonUtils.getStringOr("parentWorldName", pJson, null);
            realmsserver.activeVersion = JsonUtils.getStringOr("activeVersion", pJson, "");
            realmsserver.compatibility = getCompatibility(JsonUtils.getStringOr("compatibility", pJson, RealmsServer.Compatibility.UNVERIFIABLE.name()));
        } catch (Exception exception) {
            LOGGER.error("Could not parse McoServer: {}", exception.getMessage());
        }

        return realmsserver;
    }

    private static void sortInvited(RealmsServer pRealmsServer) {
        pRealmsServer.players
            .sort(
                (p_87502_, p_87503_) -> ComparisonChain.start()
                        .compareFalseFirst(p_87503_.getAccepted(), p_87502_.getAccepted())
                        .compare(p_87502_.getName().toLowerCase(Locale.ROOT), p_87503_.getName().toLowerCase(Locale.ROOT))
                        .result()
            );
    }

    private static List<PlayerInfo> parseInvited(JsonArray pJsonArray) {
        List<PlayerInfo> list = Lists.newArrayList();

        for (JsonElement jsonelement : pJsonArray) {
            try {
                JsonObject jsonobject = jsonelement.getAsJsonObject();
                PlayerInfo playerinfo = new PlayerInfo();
                playerinfo.setName(JsonUtils.getStringOr("name", jsonobject, null));
                playerinfo.setUuid(JsonUtils.getUuidOr("uuid", jsonobject, Util.NIL_UUID));
                playerinfo.setOperator(JsonUtils.getBooleanOr("operator", jsonobject, false));
                playerinfo.setAccepted(JsonUtils.getBooleanOr("accepted", jsonobject, false));
                playerinfo.setOnline(JsonUtils.getBooleanOr("online", jsonobject, false));
                list.add(playerinfo);
            } catch (Exception exception) {
            }
        }

        return list;
    }

    private static Map<Integer, RealmsWorldOptions> parseSlots(JsonArray pJsonArray) {
        Map<Integer, RealmsWorldOptions> map = Maps.newHashMap();

        for (JsonElement jsonelement : pJsonArray) {
            try {
                JsonObject jsonobject = jsonelement.getAsJsonObject();
                JsonParser jsonparser = new JsonParser();
                JsonElement jsonelement1 = jsonparser.parse(jsonobject.get("options").getAsString());
                RealmsWorldOptions realmsworldoptions;
                if (jsonelement1 == null) {
                    realmsworldoptions = RealmsWorldOptions.createDefaults();
                } else {
                    realmsworldoptions = RealmsWorldOptions.parse(jsonelement1.getAsJsonObject());
                }

                int i = JsonUtils.getIntOr("slotId", jsonobject, -1);
                map.put(i, realmsworldoptions);
            } catch (Exception exception) {
            }
        }

        for (int j = 1; j <= 3; j++) {
            if (!map.containsKey(j)) {
                map.put(j, RealmsWorldOptions.createEmptyDefaults());
            }
        }

        return map;
    }

    private static Map<Integer, RealmsWorldOptions> createEmptySlots() {
        Map<Integer, RealmsWorldOptions> map = Maps.newHashMap();
        map.put(1, RealmsWorldOptions.createEmptyDefaults());
        map.put(2, RealmsWorldOptions.createEmptyDefaults());
        map.put(3, RealmsWorldOptions.createEmptyDefaults());
        return map;
    }

    public static RealmsServer parse(String pJson) {
        try {
            return parse(new JsonParser().parse(pJson).getAsJsonObject());
        } catch (Exception exception) {
            LOGGER.error("Could not parse McoServer: {}", exception.getMessage());
            return new RealmsServer();
        }
    }

    private static RealmsServer.State getState(String pName) {
        try {
            return RealmsServer.State.valueOf(pName);
        } catch (Exception exception) {
            return RealmsServer.State.CLOSED;
        }
    }

    private static RealmsServer.WorldType getWorldType(String pName) {
        try {
            return RealmsServer.WorldType.valueOf(pName);
        } catch (Exception exception) {
            return RealmsServer.WorldType.NORMAL;
        }
    }

    public static RealmsServer.Compatibility getCompatibility(@Nullable String pId) {
        try {
            return RealmsServer.Compatibility.valueOf(pId);
        } catch (Exception exception) {
            return RealmsServer.Compatibility.UNVERIFIABLE;
        }
    }

    public boolean isCompatible() {
        return this.compatibility.isCompatible();
    }

    public boolean needsUpgrade() {
        return this.compatibility.needsUpgrade();
    }

    public boolean needsDowngrade() {
        return this.compatibility.needsDowngrade();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.name, this.motd, this.state, this.owner, this.expired);
    }

    @Override
    public boolean equals(Object pOther) {
        if (pOther == null) {
            return false;
        } else if (pOther == this) {
            return true;
        } else if (pOther.getClass() != this.getClass()) {
            return false;
        } else {
            RealmsServer realmsserver = (RealmsServer)pOther;
            return new EqualsBuilder()
                .append(this.id, realmsserver.id)
                .append(this.name, realmsserver.name)
                .append(this.motd, realmsserver.motd)
                .append(this.state, realmsserver.state)
                .append(this.owner, realmsserver.owner)
                .append(this.expired, realmsserver.expired)
                .append(this.worldType, this.worldType)
                .isEquals();
        }
    }

    public RealmsServer clone() {
        RealmsServer realmsserver = new RealmsServer();
        realmsserver.id = this.id;
        realmsserver.remoteSubscriptionId = this.remoteSubscriptionId;
        realmsserver.name = this.name;
        realmsserver.motd = this.motd;
        realmsserver.state = this.state;
        realmsserver.owner = this.owner;
        realmsserver.players = this.players;
        realmsserver.slots = this.cloneSlots(this.slots);
        realmsserver.expired = this.expired;
        realmsserver.expiredTrial = this.expiredTrial;
        realmsserver.daysLeft = this.daysLeft;
        realmsserver.worldType = this.worldType;
        realmsserver.ownerUUID = this.ownerUUID;
        realmsserver.minigameName = this.minigameName;
        realmsserver.activeSlot = this.activeSlot;
        realmsserver.minigameId = this.minigameId;
        realmsserver.minigameImage = this.minigameImage;
        realmsserver.parentWorldName = this.parentWorldName;
        realmsserver.parentRealmId = this.parentRealmId;
        realmsserver.activeVersion = this.activeVersion;
        realmsserver.compatibility = this.compatibility;
        return realmsserver;
    }

    public Map<Integer, RealmsWorldOptions> cloneSlots(Map<Integer, RealmsWorldOptions> pSlots) {
        Map<Integer, RealmsWorldOptions> map = Maps.newHashMap();

        for (Entry<Integer, RealmsWorldOptions> entry : pSlots.entrySet()) {
            map.put(entry.getKey(), entry.getValue().clone());
        }

        return map;
    }

    public boolean isSnapshotRealm() {
        return this.parentRealmId != -1L;
    }

    public boolean isMinigameActive() {
        return this.worldType == RealmsServer.WorldType.MINIGAME;
    }

    public String getWorldName(int pSlot) {
        return this.name + " (" + this.slots.get(pSlot).getSlotName(pSlot) + ")";
    }

    public ServerData toServerData(String pIp) {
        return new ServerData(this.name, pIp, ServerData.Type.REALM);
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Compatibility {
        UNVERIFIABLE,
        INCOMPATIBLE,
        RELEASE_TYPE_INCOMPATIBLE,
        NEEDS_DOWNGRADE,
        NEEDS_UPGRADE,
        COMPATIBLE;

        public boolean isCompatible() {
            return this == COMPATIBLE;
        }

        public boolean needsUpgrade() {
            return this == NEEDS_UPGRADE;
        }

        public boolean needsDowngrade() {
            return this == NEEDS_DOWNGRADE;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class McoServerComparator implements Comparator<RealmsServer> {
        private final String refOwner;

        public McoServerComparator(String pRefOwner) {
            this.refOwner = pRefOwner;
        }

        public int compare(RealmsServer pFirst, RealmsServer pSecond) {
            return ComparisonChain.start()
                .compareTrueFirst(pFirst.isSnapshotRealm(), pSecond.isSnapshotRealm())
                .compareTrueFirst(pFirst.state == RealmsServer.State.UNINITIALIZED, pSecond.state == RealmsServer.State.UNINITIALIZED)
                .compareTrueFirst(pFirst.expiredTrial, pSecond.expiredTrial)
                .compareTrueFirst(pFirst.owner.equals(this.refOwner), pSecond.owner.equals(this.refOwner))
                .compareFalseFirst(pFirst.expired, pSecond.expired)
                .compareTrueFirst(pFirst.state == RealmsServer.State.OPEN, pSecond.state == RealmsServer.State.OPEN)
                .compare(pFirst.id, pSecond.id)
                .result();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum State {
        CLOSED,
        OPEN,
        UNINITIALIZED;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum WorldType {
        NORMAL,
        MINIGAME,
        ADVENTUREMAP,
        EXPERIENCE,
        INSPIRATION;
    }
}
