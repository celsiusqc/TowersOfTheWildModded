package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;

public class ServerPackCommand {
    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
        pDispatcher.register(
            Commands.literal("serverpack")
                .requires(p_314426_ -> p_314426_.hasPermission(2))
                .then(
                    Commands.literal("push")
                        .then(
                            Commands.argument("url", StringArgumentType.string())
                                .then(
                                    Commands.argument("uuid", UuidArgument.uuid())
                                        .then(
                                            Commands.argument("hash", StringArgumentType.word())
                                                .executes(
                                                    p_314625_ -> pushPack(
                                                            p_314625_.getSource(),
                                                            StringArgumentType.getString(p_314625_, "url"),
                                                            Optional.of(UuidArgument.getUuid(p_314625_, "uuid")),
                                                            Optional.of(StringArgumentType.getString(p_314625_, "hash"))
                                                        )
                                                )
                                        )
                                        .executes(
                                            p_314483_ -> pushPack(
                                                    p_314483_.getSource(),
                                                    StringArgumentType.getString(p_314483_, "url"),
                                                    Optional.of(UuidArgument.getUuid(p_314483_, "uuid")),
                                                    Optional.empty()
                                                )
                                        )
                                )
                                .executes(
                                    p_314643_ -> pushPack(
                                            p_314643_.getSource(), StringArgumentType.getString(p_314643_, "url"), Optional.empty(), Optional.empty()
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("pop")
                        .then(
                            Commands.argument("uuid", UuidArgument.uuid())
                                .executes(p_314561_ -> popPack(p_314561_.getSource(), UuidArgument.getUuid(p_314561_, "uuid")))
                        )
                )
        );
    }

    private static void sendToAllConnections(CommandSourceStack pSource, Packet<?> pPacket) {
        pSource.getServer().getConnection().getConnections().forEach(p_314597_ -> p_314597_.send(pPacket));
    }

    private static int pushPack(CommandSourceStack pSource, String pUrl, Optional<UUID> pUuid, Optional<String> pHash) {
        UUID uuid = pUuid.orElseGet(() -> UUID.nameUUIDFromBytes(pUrl.getBytes(StandardCharsets.UTF_8)));
        String s = pHash.orElse("");
        ClientboundResourcePackPushPacket clientboundresourcepackpushpacket = new ClientboundResourcePackPushPacket(uuid, pUrl, s, false, null);
        sendToAllConnections(pSource, clientboundresourcepackpushpacket);
        return 0;
    }

    private static int popPack(CommandSourceStack pSource, UUID pUuid) {
        ClientboundResourcePackPopPacket clientboundresourcepackpoppacket = new ClientboundResourcePackPopPacket(Optional.of(pUuid));
        sendToAllConnections(pSource, clientboundresourcepackpoppacket);
        return 0;
    }
}
