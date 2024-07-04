package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.GameTestAddMarkerDebugPayload;
import net.minecraft.network.protocol.common.custom.GameTestClearMarkersDebugPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Nameable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class DebugPackets {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void sendGameTestAddMarker(ServerLevel pLevel, BlockPos pPos, String pText, int pColor, int pLifetimeMillis) {
        sendPacketToAllPlayers(pLevel, new GameTestAddMarkerDebugPayload(pPos, pColor, pText, pLifetimeMillis));
    }

    public static void sendGameTestClearPacket(ServerLevel pLevel) {
        sendPacketToAllPlayers(pLevel, new GameTestClearMarkersDebugPayload());
    }

    public static void sendPoiPacketsForChunk(ServerLevel pLevel, ChunkPos pChunkPos) {
    }

    public static void sendPoiAddedPacket(ServerLevel pLevel, BlockPos pPos) {
        sendVillageSectionsPacket(pLevel, pPos);
    }

    public static void sendPoiRemovedPacket(ServerLevel pLevel, BlockPos pPos) {
        sendVillageSectionsPacket(pLevel, pPos);
    }

    public static void sendPoiTicketCountPacket(ServerLevel pLevel, BlockPos pPos) {
        sendVillageSectionsPacket(pLevel, pPos);
    }

    private static void sendVillageSectionsPacket(ServerLevel pLevel, BlockPos pPos) {
    }

    public static void sendPathFindingPacket(Level pLevel, Mob pMob, @Nullable Path pPath, float pMaxDistanceToWaypoint) {
    }

    public static void sendNeighborsUpdatePacket(Level pLevel, BlockPos pPos) {
    }

    public static void sendStructurePacket(WorldGenLevel pLevel, StructureStart pStructureStart) {
    }

    public static void sendGoalSelector(Level pLevel, Mob pMob, GoalSelector pGoalSelector) {
    }

    public static void sendRaids(ServerLevel pLevel, Collection<Raid> pRaids) {
    }

    public static void sendEntityBrain(LivingEntity pLivingEntity) {
    }

    public static void sendBeeInfo(Bee pBee) {
    }

    public static void sendBreezeInfo(Breeze pBreeze) {
    }

    public static void sendGameEventInfo(Level pLevel, Holder<GameEvent> pGameEvent, Vec3 pPos) {
    }

    public static void sendGameEventListenerInfo(Level pLevel, GameEventListener pGameEventListener) {
    }

    public static void sendHiveInfo(Level pLevel, BlockPos pPos, BlockState pBlockState, BeehiveBlockEntity pHiveBlockEntity) {
    }

    private static List<String> getMemoryDescriptions(LivingEntity pEntity, long pGameTime) {
        Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> map = pEntity.getBrain().getMemories();
        List<String> list = Lists.newArrayList();

        for (Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : map.entrySet()) {
            MemoryModuleType<?> memorymoduletype = entry.getKey();
            Optional<? extends ExpirableValue<?>> optional = entry.getValue();
            String s;
            if (optional.isPresent()) {
                ExpirableValue<?> expirablevalue = (ExpirableValue<?>)optional.get();
                Object object = expirablevalue.getValue();
                if (memorymoduletype == MemoryModuleType.HEARD_BELL_TIME) {
                    long i = pGameTime - (Long)object;
                    s = i + " ticks ago";
                } else if (expirablevalue.canExpire()) {
                    s = getShortDescription((ServerLevel)pEntity.level(), object) + " (ttl: " + expirablevalue.getTimeToLive() + ")";
                } else {
                    s = getShortDescription((ServerLevel)pEntity.level(), object);
                }
            } else {
                s = "-";
            }

            list.add(BuiltInRegistries.MEMORY_MODULE_TYPE.getKey(memorymoduletype).getPath() + ": " + s);
        }

        list.sort(String::compareTo);
        return list;
    }

    private static String getShortDescription(ServerLevel pLevel, @Nullable Object pObject) {
        if (pObject == null) {
            return "-";
        } else if (pObject instanceof UUID) {
            return getShortDescription(pLevel, pLevel.getEntity((UUID)pObject));
        } else if (pObject instanceof LivingEntity) {
            Entity entity1 = (Entity)pObject;
            return DebugEntityNameGenerator.getEntityName(entity1);
        } else if (pObject instanceof Nameable) {
            return ((Nameable)pObject).getName().getString();
        } else if (pObject instanceof WalkTarget) {
            return getShortDescription(pLevel, ((WalkTarget)pObject).getTarget());
        } else if (pObject instanceof EntityTracker) {
            return getShortDescription(pLevel, ((EntityTracker)pObject).getEntity());
        } else if (pObject instanceof GlobalPos) {
            return getShortDescription(pLevel, ((GlobalPos)pObject).pos());
        } else if (pObject instanceof BlockPosTracker) {
            return getShortDescription(pLevel, ((BlockPosTracker)pObject).currentBlockPosition());
        } else if (pObject instanceof DamageSource) {
            Entity entity = ((DamageSource)pObject).getEntity();
            return entity == null ? pObject.toString() : getShortDescription(pLevel, entity);
        } else if (!(pObject instanceof Collection)) {
            return pObject.toString();
        } else {
            List<String> list = Lists.newArrayList();

            for (Object object : (Iterable)pObject) {
                list.add(getShortDescription(pLevel, object));
            }

            return list.toString();
        }
    }

    private static void sendPacketToAllPlayers(ServerLevel pLevel, CustomPacketPayload pPayload) {
        Packet<?> packet = new ClientboundCustomPayloadPacket(pPayload);

        for (ServerPlayer serverplayer : pLevel.players()) {
            serverplayer.connection.send(packet);
        }
    }
}
