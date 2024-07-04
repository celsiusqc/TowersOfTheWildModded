package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.util.Date;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;

public class UserBanListEntry extends BanListEntry<GameProfile> {
    public UserBanListEntry(@Nullable GameProfile pUser) {
        this(pUser, null, null, null, null);
    }

    public UserBanListEntry(
        @Nullable GameProfile pProfile, @Nullable Date pCreated, @Nullable String pSource, @Nullable Date pExpires, @Nullable String pReason
    ) {
        super(pProfile, pCreated, pSource, pExpires, pReason);
    }

    public UserBanListEntry(JsonObject pEntryData) {
        super(createGameProfile(pEntryData), pEntryData);
    }

    @Override
    protected void serialize(JsonObject pData) {
        if (this.getUser() != null) {
            pData.addProperty("uuid", this.getUser().getId().toString());
            pData.addProperty("name", this.getUser().getName());
            super.serialize(pData);
        }
    }

    @Override
    public Component getDisplayName() {
        GameProfile gameprofile = this.getUser();
        return gameprofile != null ? Component.literal(gameprofile.getName()) : Component.translatable("commands.banlist.entry.unknown");
    }

    /**
     * Convert a {@linkplain com.google.gson.JsonObject JsonObject} into a {@linkplain com.mojang.authlib.GameProfile}. The json object must have {@code uuid} and {@code name} attributes or {@code null} will be returned.
     */
    @Nullable
    private static GameProfile createGameProfile(JsonObject pJson) {
        if (pJson.has("uuid") && pJson.has("name")) {
            String s = pJson.get("uuid").getAsString();

            UUID uuid;
            try {
                uuid = UUID.fromString(s);
            } catch (Throwable throwable) {
                return null;
            }

            return new GameProfile(uuid, pJson.get("name").getAsString());
        } else {
            return null;
        }
    }
}
