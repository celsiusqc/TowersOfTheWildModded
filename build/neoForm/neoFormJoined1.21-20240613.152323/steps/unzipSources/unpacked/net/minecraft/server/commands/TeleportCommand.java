package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class TeleportCommand {
    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(
        Component.translatable("commands.teleport.invalidPosition")
    );

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
        LiteralCommandNode<CommandSourceStack> literalcommandnode = pDispatcher.register(
            Commands.literal("teleport")
                .requires(p_139039_ -> p_139039_.hasPermission(2))
                .then(
                    Commands.argument("location", Vec3Argument.vec3())
                        .executes(
                            p_139051_ -> teleportToPos(
                                    p_139051_.getSource(),
                                    Collections.singleton(p_139051_.getSource().getEntityOrException()),
                                    p_139051_.getSource().getLevel(),
                                    Vec3Argument.getCoordinates(p_139051_, "location"),
                                    WorldCoordinates.current(),
                                    null
                                )
                        )
                )
                .then(
                    Commands.argument("destination", EntityArgument.entity())
                        .executes(
                            p_139049_ -> teleportToEntity(
                                    p_139049_.getSource(),
                                    Collections.singleton(p_139049_.getSource().getEntityOrException()),
                                    EntityArgument.getEntity(p_139049_, "destination")
                                )
                        )
                )
                .then(
                    Commands.argument("targets", EntityArgument.entities())
                        .then(
                            Commands.argument("location", Vec3Argument.vec3())
                                .executes(
                                    p_139047_ -> teleportToPos(
                                            p_139047_.getSource(),
                                            EntityArgument.getEntities(p_139047_, "targets"),
                                            p_139047_.getSource().getLevel(),
                                            Vec3Argument.getCoordinates(p_139047_, "location"),
                                            null,
                                            null
                                        )
                                )
                                .then(
                                    Commands.argument("rotation", RotationArgument.rotation())
                                        .executes(
                                            p_139045_ -> teleportToPos(
                                                    p_139045_.getSource(),
                                                    EntityArgument.getEntities(p_139045_, "targets"),
                                                    p_139045_.getSource().getLevel(),
                                                    Vec3Argument.getCoordinates(p_139045_, "location"),
                                                    RotationArgument.getRotation(p_139045_, "rotation"),
                                                    null
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("facing")
                                        .then(
                                            Commands.literal("entity")
                                                .then(
                                                    Commands.argument("facingEntity", EntityArgument.entity())
                                                        .executes(
                                                            p_326749_ -> teleportToPos(
                                                                    p_326749_.getSource(),
                                                                    EntityArgument.getEntities(p_326749_, "targets"),
                                                                    p_326749_.getSource().getLevel(),
                                                                    Vec3Argument.getCoordinates(p_326749_, "location"),
                                                                    null,
                                                                    new TeleportCommand.LookAtEntity(
                                                                        EntityArgument.getEntity(p_326749_, "facingEntity"), EntityAnchorArgument.Anchor.FEET
                                                                    )
                                                                )
                                                        )
                                                        .then(
                                                            Commands.argument("facingAnchor", EntityAnchorArgument.anchor())
                                                                .executes(
                                                                    p_326750_ -> teleportToPos(
                                                                            p_326750_.getSource(),
                                                                            EntityArgument.getEntities(p_326750_, "targets"),
                                                                            p_326750_.getSource().getLevel(),
                                                                            Vec3Argument.getCoordinates(p_326750_, "location"),
                                                                            null,
                                                                            new TeleportCommand.LookAtEntity(
                                                                                EntityArgument.getEntity(p_326750_, "facingEntity"),
                                                                                EntityAnchorArgument.getAnchor(p_326750_, "facingAnchor")
                                                                            )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                        .then(
                                            Commands.argument("facingLocation", Vec3Argument.vec3())
                                                .executes(
                                                    p_326751_ -> teleportToPos(
                                                            p_326751_.getSource(),
                                                            EntityArgument.getEntities(p_326751_, "targets"),
                                                            p_326751_.getSource().getLevel(),
                                                            Vec3Argument.getCoordinates(p_326751_, "location"),
                                                            null,
                                                            new TeleportCommand.LookAtPosition(Vec3Argument.getVec3(p_326751_, "facingLocation"))
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.argument("destination", EntityArgument.entity())
                                .executes(
                                    p_139011_ -> teleportToEntity(
                                            p_139011_.getSource(),
                                            EntityArgument.getEntities(p_139011_, "targets"),
                                            EntityArgument.getEntity(p_139011_, "destination")
                                        )
                                )
                        )
                )
        );
        pDispatcher.register(Commands.literal("tp").requires(p_139013_ -> p_139013_.hasPermission(2)).redirect(literalcommandnode));
    }

    private static int teleportToEntity(CommandSourceStack pSource, Collection<? extends Entity> pTargets, Entity pDestination) throws CommandSyntaxException {
        for (Entity entity : pTargets) {
            performTeleport(
                pSource,
                entity,
                (ServerLevel)pDestination.level(),
                pDestination.getX(),
                pDestination.getY(),
                pDestination.getZ(),
                EnumSet.noneOf(RelativeMovement.class),
                pDestination.getYRot(),
                pDestination.getXRot(),
                null
            );
        }

        if (pTargets.size() == 1) {
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.teleport.success.entity.single", pTargets.iterator().next().getDisplayName(), pDestination.getDisplayName()
                    ),
                true
            );
        } else {
            pSource.sendSuccess(() -> Component.translatable("commands.teleport.success.entity.multiple", pTargets.size(), pDestination.getDisplayName()), true);
        }

        return pTargets.size();
    }

    private static int teleportToPos(
        CommandSourceStack pSource,
        Collection<? extends Entity> pTargets,
        ServerLevel pLevel,
        Coordinates pPosition,
        @Nullable Coordinates pRotation,
        @Nullable TeleportCommand.LookAt pFacing
    ) throws CommandSyntaxException {
        Vec3 vec3 = pPosition.getPosition(pSource);
        Vec2 vec2 = pRotation == null ? null : pRotation.getRotation(pSource);
        Set<RelativeMovement> set = EnumSet.noneOf(RelativeMovement.class);
        if (pPosition.isXRelative()) {
            set.add(RelativeMovement.X);
        }

        if (pPosition.isYRelative()) {
            set.add(RelativeMovement.Y);
        }

        if (pPosition.isZRelative()) {
            set.add(RelativeMovement.Z);
        }

        if (pRotation == null) {
            set.add(RelativeMovement.X_ROT);
            set.add(RelativeMovement.Y_ROT);
        } else {
            if (pRotation.isXRelative()) {
                set.add(RelativeMovement.X_ROT);
            }

            if (pRotation.isYRelative()) {
                set.add(RelativeMovement.Y_ROT);
            }
        }

        for (Entity entity : pTargets) {
            if (pRotation == null) {
                performTeleport(pSource, entity, pLevel, vec3.x, vec3.y, vec3.z, set, entity.getYRot(), entity.getXRot(), pFacing);
            } else {
                performTeleport(pSource, entity, pLevel, vec3.x, vec3.y, vec3.z, set, vec2.y, vec2.x, pFacing);
            }
        }

        if (pTargets.size() == 1) {
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.teleport.success.location.single",
                        pTargets.iterator().next().getDisplayName(),
                        formatDouble(vec3.x),
                        formatDouble(vec3.y),
                        formatDouble(vec3.z)
                    ),
                true
            );
        } else {
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.teleport.success.location.multiple", pTargets.size(), formatDouble(vec3.x), formatDouble(vec3.y), formatDouble(vec3.z)
                    ),
                true
            );
        }

        return pTargets.size();
    }

    private static String formatDouble(double pValue) {
        return String.format(Locale.ROOT, "%f", pValue);
    }

    private static void performTeleport(
        CommandSourceStack pSource,
        Entity pEntity,
        ServerLevel pLevel,
        double pX,
        double pY,
        double pZ,
        Set<RelativeMovement> pRelativeList,
        float pYaw,
        float pPitch,
        @Nullable TeleportCommand.LookAt pFacing
    ) throws CommandSyntaxException {
        net.neoforged.neoforge.event.entity.EntityTeleportEvent.TeleportCommand event = net.neoforged.neoforge.event.EventHooks.onEntityTeleportCommand(pEntity, pX, pY, pZ);
        if (event.isCanceled()) {
             return;
        }
        pX = event.getTargetX();
        pY = event.getTargetY();
        pZ = event.getTargetZ();

        BlockPos blockpos = BlockPos.containing(pX, pY, pZ);
        if (!Level.isInSpawnableBounds(blockpos)) {
            throw INVALID_POSITION.create();
        } else {
            float f = Mth.wrapDegrees(pYaw);
            float f1 = Mth.wrapDegrees(pPitch);
            if (pEntity.teleportTo(pLevel, pX, pY, pZ, pRelativeList, f, f1)) {
                if (pFacing != null) {
                    pFacing.perform(pSource, pEntity);
                }

                if (!(pEntity instanceof LivingEntity livingentity) || !livingentity.isFallFlying()) {
                    pEntity.setDeltaMovement(pEntity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                    pEntity.setOnGround(true);
                }

                if (pEntity instanceof PathfinderMob pathfindermob) {
                    pathfindermob.getNavigation().stop();
                }
            }
        }
    }

    @FunctionalInterface
    interface LookAt {
        void perform(CommandSourceStack pSource, Entity pEntity);
    }

    static record LookAtEntity(Entity entity, EntityAnchorArgument.Anchor anchor) implements TeleportCommand.LookAt {
        @Override
        public void perform(CommandSourceStack p_326864_, Entity p_326807_) {
            if (p_326807_ instanceof ServerPlayer serverplayer) {
                serverplayer.lookAt(p_326864_.getAnchor(), this.entity, this.anchor);
            } else {
                p_326807_.lookAt(p_326864_.getAnchor(), this.anchor.apply(this.entity));
            }
        }
    }

    static record LookAtPosition(Vec3 position) implements TeleportCommand.LookAt {
        @Override
        public void perform(CommandSourceStack p_326870_, Entity p_326894_) {
            p_326894_.lookAt(p_326870_.getAnchor(), this.position);
        }
    }
}
