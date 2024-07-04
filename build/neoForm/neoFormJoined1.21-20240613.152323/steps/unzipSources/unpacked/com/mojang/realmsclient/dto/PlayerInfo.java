package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import java.util.UUID;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerInfo extends ValueObject implements ReflectionBasedSerialization {
    @SerializedName("name")
    private String name;
    @SerializedName("uuid")
    private UUID uuid;
    @SerializedName("operator")
    private boolean operator;
    @SerializedName("accepted")
    private boolean accepted;
    @SerializedName("online")
    private boolean online;

    public String getName() {
        return this.name;
    }

    public void setName(String pName) {
        this.name = pName;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public void setUuid(UUID pUuid) {
        this.uuid = pUuid;
    }

    public boolean isOperator() {
        return this.operator;
    }

    public void setOperator(boolean pOperator) {
        this.operator = pOperator;
    }

    public boolean getAccepted() {
        return this.accepted;
    }

    public void setAccepted(boolean pAccepted) {
        this.accepted = pAccepted;
    }

    public boolean getOnline() {
        return this.online;
    }

    public void setOnline(boolean pOnline) {
        this.online = pOnline;
    }
}
