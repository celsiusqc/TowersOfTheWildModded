package net.minecraft.network.protocol.game;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.scores.PlayerTeam;

public class ClientboundSetPlayerTeamPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSetPlayerTeamPacket> STREAM_CODEC = Packet.codec(
        ClientboundSetPlayerTeamPacket::write, ClientboundSetPlayerTeamPacket::new
    );
    private static final int METHOD_ADD = 0;
    private static final int METHOD_REMOVE = 1;
    private static final int METHOD_CHANGE = 2;
    private static final int METHOD_JOIN = 3;
    private static final int METHOD_LEAVE = 4;
    private static final int MAX_VISIBILITY_LENGTH = 40;
    private static final int MAX_COLLISION_LENGTH = 40;
    private final int method;
    private final String name;
    private final Collection<String> players;
    private final Optional<ClientboundSetPlayerTeamPacket.Parameters> parameters;

    private ClientboundSetPlayerTeamPacket(
        String pName, int pMethod, Optional<ClientboundSetPlayerTeamPacket.Parameters> pParameters, Collection<String> pPlayers
    ) {
        this.name = pName;
        this.method = pMethod;
        this.parameters = pParameters;
        this.players = ImmutableList.copyOf(pPlayers);
    }

    public static ClientboundSetPlayerTeamPacket createAddOrModifyPacket(PlayerTeam pTeam, boolean pUseAdd) {
        return new ClientboundSetPlayerTeamPacket(
            pTeam.getName(),
            pUseAdd ? 0 : 2,
            Optional.of(new ClientboundSetPlayerTeamPacket.Parameters(pTeam)),
            (Collection<String>)(pUseAdd ? pTeam.getPlayers() : ImmutableList.of())
        );
    }

    public static ClientboundSetPlayerTeamPacket createRemovePacket(PlayerTeam pTeam) {
        return new ClientboundSetPlayerTeamPacket(pTeam.getName(), 1, Optional.empty(), ImmutableList.of());
    }

    public static ClientboundSetPlayerTeamPacket createPlayerPacket(PlayerTeam pTeam, String pPlayerName, ClientboundSetPlayerTeamPacket.Action pAction) {
        return new ClientboundSetPlayerTeamPacket(
            pTeam.getName(), pAction == ClientboundSetPlayerTeamPacket.Action.ADD ? 3 : 4, Optional.empty(), ImmutableList.of(pPlayerName)
        );
    }

    private ClientboundSetPlayerTeamPacket(RegistryFriendlyByteBuf p_323831_) {
        this.name = p_323831_.readUtf();
        this.method = p_323831_.readByte();
        if (shouldHaveParameters(this.method)) {
            this.parameters = Optional.of(new ClientboundSetPlayerTeamPacket.Parameters(p_323831_));
        } else {
            this.parameters = Optional.empty();
        }

        if (shouldHavePlayerList(this.method)) {
            this.players = p_323831_.readList(FriendlyByteBuf::readUtf);
        } else {
            this.players = ImmutableList.of();
        }
    }

    private void write(RegistryFriendlyByteBuf p_323648_) {
        p_323648_.writeUtf(this.name);
        p_323648_.writeByte(this.method);
        if (shouldHaveParameters(this.method)) {
            this.parameters.orElseThrow(() -> new IllegalStateException("Parameters not present, but method is" + this.method)).write(p_323648_);
        }

        if (shouldHavePlayerList(this.method)) {
            p_323648_.writeCollection(this.players, FriendlyByteBuf::writeUtf);
        }
    }

    private static boolean shouldHavePlayerList(int pMethod) {
        return pMethod == 0 || pMethod == 3 || pMethod == 4;
    }

    private static boolean shouldHaveParameters(int pMethod) {
        return pMethod == 0 || pMethod == 2;
    }

    @Nullable
    public ClientboundSetPlayerTeamPacket.Action getPlayerAction() {
        return switch (this.method) {
            case 0, 3 -> ClientboundSetPlayerTeamPacket.Action.ADD;
            default -> null;
            case 4 -> ClientboundSetPlayerTeamPacket.Action.REMOVE;
        };
    }

    @Nullable
    public ClientboundSetPlayerTeamPacket.Action getTeamAction() {
        return switch (this.method) {
            case 0 -> ClientboundSetPlayerTeamPacket.Action.ADD;
            case 1 -> ClientboundSetPlayerTeamPacket.Action.REMOVE;
            default -> null;
        };
    }

    @Override
    public PacketType<ClientboundSetPlayerTeamPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_PLAYER_TEAM;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetPlayerTeamPacket(this);
    }

    public String getName() {
        return this.name;
    }

    public Collection<String> getPlayers() {
        return this.players;
    }

    public Optional<ClientboundSetPlayerTeamPacket.Parameters> getParameters() {
        return this.parameters;
    }

    public static enum Action {
        ADD,
        REMOVE;
    }

    public static class Parameters {
        private final Component displayName;
        private final Component playerPrefix;
        private final Component playerSuffix;
        private final String nametagVisibility;
        private final String collisionRule;
        private final ChatFormatting color;
        private final int options;

        public Parameters(PlayerTeam pTeam) {
            this.displayName = pTeam.getDisplayName();
            this.options = pTeam.packOptions();
            this.nametagVisibility = pTeam.getNameTagVisibility().name;
            this.collisionRule = pTeam.getCollisionRule().name;
            this.color = pTeam.getColor();
            this.playerPrefix = pTeam.getPlayerPrefix();
            this.playerSuffix = pTeam.getPlayerSuffix();
        }

        public Parameters(RegistryFriendlyByteBuf pBuffer) {
            this.displayName = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(pBuffer);
            this.options = pBuffer.readByte();
            this.nametagVisibility = pBuffer.readUtf(40);
            this.collisionRule = pBuffer.readUtf(40);
            this.color = pBuffer.readEnum(ChatFormatting.class);
            this.playerPrefix = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(pBuffer);
            this.playerSuffix = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(pBuffer);
        }

        public Component getDisplayName() {
            return this.displayName;
        }

        public int getOptions() {
            return this.options;
        }

        public ChatFormatting getColor() {
            return this.color;
        }

        public String getNametagVisibility() {
            return this.nametagVisibility;
        }

        public String getCollisionRule() {
            return this.collisionRule;
        }

        public Component getPlayerPrefix() {
            return this.playerPrefix;
        }

        public Component getPlayerSuffix() {
            return this.playerSuffix;
        }

        public void write(RegistryFriendlyByteBuf pBuffer) {
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(pBuffer, this.displayName);
            pBuffer.writeByte(this.options);
            pBuffer.writeUtf(this.nametagVisibility);
            pBuffer.writeUtf(this.collisionRule);
            pBuffer.writeEnum(this.color);
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(pBuffer, this.playerPrefix);
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(pBuffer, this.playerSuffix);
        }
    }
}
