package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.util.PngInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_ICON_SIZE = 1024;
    public String name;
    public String ip;
    public Component status;
    public Component motd;
    @Nullable
    public ServerStatus.Players players;
    public long ping;
    public int protocol = SharedConstants.getCurrentVersion().getProtocolVersion();
    public Component version = Component.literal(SharedConstants.getCurrentVersion().getName());
    public List<Component> playerList = Collections.emptyList();
    private ServerData.ServerPackStatus packStatus = ServerData.ServerPackStatus.PROMPT;
    @Nullable
    private byte[] iconBytes;
    private ServerData.Type type;
    private ServerData.State state = ServerData.State.INITIAL;
    public net.neoforged.neoforge.client.ExtendedServerListData neoForgeData = null;

    public ServerData(String pName, String pIp, ServerData.Type pType) {
        this.name = pName;
        this.ip = pIp;
        this.type = pType;
    }

    public CompoundTag write() {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("name", this.name);
        compoundtag.putString("ip", this.ip);
        if (this.iconBytes != null) {
            compoundtag.putString("icon", Base64.getEncoder().encodeToString(this.iconBytes));
        }

        if (this.packStatus == ServerData.ServerPackStatus.ENABLED) {
            compoundtag.putBoolean("acceptTextures", true);
        } else if (this.packStatus == ServerData.ServerPackStatus.DISABLED) {
            compoundtag.putBoolean("acceptTextures", false);
        }

        return compoundtag;
    }

    public ServerData.ServerPackStatus getResourcePackStatus() {
        return this.packStatus;
    }

    public void setResourcePackStatus(ServerData.ServerPackStatus pPackStatus) {
        this.packStatus = pPackStatus;
    }

    /**
     * Takes an NBTTagCompound with 'name' and 'ip' keys, returns a ServerData instance.
     */
    public static ServerData read(CompoundTag pNbtCompound) {
        ServerData serverdata = new ServerData(pNbtCompound.getString("name"), pNbtCompound.getString("ip"), ServerData.Type.OTHER);
        if (pNbtCompound.contains("icon", 8)) {
            try {
                byte[] abyte = Base64.getDecoder().decode(pNbtCompound.getString("icon"));
                serverdata.setIconBytes(validateIcon(abyte));
            } catch (IllegalArgumentException illegalargumentexception) {
                LOGGER.warn("Malformed base64 server icon", (Throwable)illegalargumentexception);
            }
        }

        if (pNbtCompound.contains("acceptTextures", 1)) {
            if (pNbtCompound.getBoolean("acceptTextures")) {
                serverdata.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
            } else {
                serverdata.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);
            }
        } else {
            serverdata.setResourcePackStatus(ServerData.ServerPackStatus.PROMPT);
        }

        return serverdata;
    }

    @Nullable
    public byte[] getIconBytes() {
        return this.iconBytes;
    }

    public void setIconBytes(@Nullable byte[] pIconBytes) {
        this.iconBytes = pIconBytes;
    }

    public boolean isLan() {
        return this.type == ServerData.Type.LAN;
    }

    public boolean isRealm() {
        return this.type == ServerData.Type.REALM;
    }

    public ServerData.Type type() {
        return this.type;
    }

    public void copyNameIconFrom(ServerData pOther) {
        this.ip = pOther.ip;
        this.name = pOther.name;
        this.iconBytes = pOther.iconBytes;
    }

    public void copyFrom(ServerData pServerData) {
        this.copyNameIconFrom(pServerData);
        this.setResourcePackStatus(pServerData.getResourcePackStatus());
        this.type = pServerData.type;
    }

    public ServerData.State state() {
        return this.state;
    }

    public void setState(ServerData.State pState) {
        this.state = pState;
    }

    @Nullable
    public static byte[] validateIcon(@Nullable byte[] pIcon) {
        if (pIcon != null) {
            try {
                PngInfo pnginfo = PngInfo.fromBytes(pIcon);
                if (pnginfo.width() <= 1024 && pnginfo.height() <= 1024) {
                    return pIcon;
                }
            } catch (IOException ioexception) {
                LOGGER.warn("Failed to decode server icon", (Throwable)ioexception);
            }
        }

        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum ServerPackStatus {
        ENABLED("enabled"),
        DISABLED("disabled"),
        PROMPT("prompt");

        private final Component name;

        private ServerPackStatus(String pName) {
            this.name = Component.translatable("addServer.resourcePack." + pName);
        }

        public Component getName() {
            return this.name;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum State {
        INITIAL,
        PINGING,
        UNREACHABLE,
        INCOMPATIBLE,
        SUCCESSFUL;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Type {
        LAN,
        REALM,
        OTHER;
    }
}
