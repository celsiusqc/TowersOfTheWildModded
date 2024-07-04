package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.File;
import java.util.Objects;

public class UserWhiteList extends StoredUserList<GameProfile, UserWhiteListEntry> {
    public UserWhiteList(File pFile) {
        super(pFile);
    }

    @Override
    protected StoredUserEntry<GameProfile> createEntry(JsonObject pEntryData) {
        return new UserWhiteListEntry(pEntryData);
    }

    /**
     * Returns {@code true} if the profile is in the whitelist.
     */
    public boolean isWhiteListed(GameProfile pProfile) {
        return this.contains(pProfile);
    }

    @Override
    public String[] getUserList() {
        return this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(GameProfile::getName).toArray(String[]::new);
    }

    /**
     * Gets the key value for the given object
     */
    protected String getKeyForUser(GameProfile pObj) {
        return pObj.getId().toString();
    }
}
