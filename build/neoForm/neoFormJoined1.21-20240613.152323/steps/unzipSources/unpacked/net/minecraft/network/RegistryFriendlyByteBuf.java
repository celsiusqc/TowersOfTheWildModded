package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import net.minecraft.core.RegistryAccess;

public class RegistryFriendlyByteBuf extends FriendlyByteBuf {
    private final RegistryAccess registryAccess;
    private final net.neoforged.neoforge.network.connection.ConnectionType connectionType;

    /**
     * @deprecated Neo: use overload with ConnectionType context
     */
    @Deprecated
    public RegistryFriendlyByteBuf(ByteBuf pSource, RegistryAccess pRegistryAccess) {
        super(pSource);
        this.registryAccess = pRegistryAccess;
        this.connectionType = net.neoforged.neoforge.network.connection.ConnectionType.OTHER;
    }

    public RegistryFriendlyByteBuf(ByteBuf pSource, RegistryAccess pRegistryAccess, net.neoforged.neoforge.network.connection.ConnectionType connectionType) {
        super(pSource);
        this.registryAccess = pRegistryAccess;
        this.connectionType = connectionType;
    }

    public net.neoforged.neoforge.network.connection.ConnectionType getConnectionType() {
        return this.connectionType;
    }

    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }

    public static Function<ByteBuf, RegistryFriendlyByteBuf> decorator(RegistryAccess pRegistry, net.neoforged.neoforge.network.connection.ConnectionType connectionType) {
        return p_320793_ -> new RegistryFriendlyByteBuf(p_320793_, pRegistry, connectionType);
    }

    /**
     * @deprecated Neo: use overload with ConnectionType context
     */
    @Deprecated
    public static Function<ByteBuf, RegistryFriendlyByteBuf> decorator(RegistryAccess pRegistry) {
        return p_320793_ -> new RegistryFriendlyByteBuf(p_320793_, pRegistry);
    }
}
