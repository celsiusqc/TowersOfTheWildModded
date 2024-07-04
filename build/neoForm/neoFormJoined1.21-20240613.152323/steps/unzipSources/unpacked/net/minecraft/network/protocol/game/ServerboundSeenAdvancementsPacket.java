package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceLocation;

public class ServerboundSeenAdvancementsPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundSeenAdvancementsPacket> STREAM_CODEC = Packet.codec(
        ServerboundSeenAdvancementsPacket::write, ServerboundSeenAdvancementsPacket::new
    );
    private final ServerboundSeenAdvancementsPacket.Action action;
    @Nullable
    private final ResourceLocation tab;

    public ServerboundSeenAdvancementsPacket(ServerboundSeenAdvancementsPacket.Action pAction, @Nullable ResourceLocation pTab) {
        this.action = pAction;
        this.tab = pTab;
    }

    public static ServerboundSeenAdvancementsPacket openedTab(AdvancementHolder pAdvancement) {
        return new ServerboundSeenAdvancementsPacket(ServerboundSeenAdvancementsPacket.Action.OPENED_TAB, pAdvancement.id());
    }

    public static ServerboundSeenAdvancementsPacket closedScreen() {
        return new ServerboundSeenAdvancementsPacket(ServerboundSeenAdvancementsPacket.Action.CLOSED_SCREEN, null);
    }

    private ServerboundSeenAdvancementsPacket(FriendlyByteBuf p_179744_) {
        this.action = p_179744_.readEnum(ServerboundSeenAdvancementsPacket.Action.class);
        if (this.action == ServerboundSeenAdvancementsPacket.Action.OPENED_TAB) {
            this.tab = p_179744_.readResourceLocation();
        } else {
            this.tab = null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134446_) {
        p_134446_.writeEnum(this.action);
        if (this.action == ServerboundSeenAdvancementsPacket.Action.OPENED_TAB) {
            p_134446_.writeResourceLocation(this.tab);
        }
    }

    @Override
    public PacketType<ServerboundSeenAdvancementsPacket> type() {
        return GamePacketTypes.SERVERBOUND_SEEN_ADVANCEMENTS;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleSeenAdvancements(this);
    }

    public ServerboundSeenAdvancementsPacket.Action getAction() {
        return this.action;
    }

    @Nullable
    public ResourceLocation getTab() {
        return this.tab;
    }

    public static enum Action {
        OPENED_TAB,
        CLOSED_SCREEN;
    }
}
