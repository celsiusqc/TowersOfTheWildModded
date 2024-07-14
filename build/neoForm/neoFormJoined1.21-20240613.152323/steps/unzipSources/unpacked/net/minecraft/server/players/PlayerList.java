package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.io.File;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.FileUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.slf4j.Logger;

public abstract class PlayerList {
    public static final File USERBANLIST_FILE = new File("banned-players.json");
    public static final File IPBANLIST_FILE = new File("banned-ips.json");
    public static final File OPLIST_FILE = new File("ops.json");
    public static final File WHITELIST_FILE = new File("whitelist.json");
    public static final Component CHAT_FILTERED_FULL = Component.translatable("chat.filtered_full");
    public static final Component DUPLICATE_LOGIN_DISCONNECT_MESSAGE = Component.translatable("multiplayer.disconnect.duplicate_login");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SEND_PLAYER_INFO_INTERVAL = 600;
    private static final SimpleDateFormat BAN_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    private final MinecraftServer server;
    private final List<ServerPlayer> players = Lists.newArrayList();
    /**
     * A map containing the key-value pairs for UUIDs and their EntityPlayerMP objects.
     */
    private final Map<UUID, ServerPlayer> playersByUUID = Maps.newHashMap();
    private final UserBanList bans = new UserBanList(USERBANLIST_FILE);
    private final IpBanList ipBans = new IpBanList(IPBANLIST_FILE);
    private final ServerOpList ops = new ServerOpList(OPLIST_FILE);
    private final UserWhiteList whitelist = new UserWhiteList(WHITELIST_FILE);
    private final Map<UUID, ServerStatsCounter> stats = Maps.newHashMap();
    private final Map<UUID, PlayerAdvancements> advancements = Maps.newHashMap();
    private final PlayerDataStorage playerIo;
    private boolean doWhiteList;
    private final LayeredRegistryAccess<RegistryLayer> registries;
    protected final int maxPlayers;
    private int viewDistance;
    private int simulationDistance;
    private boolean allowCommandsForAllPlayers;
    private static final boolean ALLOW_LOGOUTIVATOR = false;
    private int sendAllPlayerInfoIn;
    private final List<ServerPlayer> playersView = java.util.Collections.unmodifiableList(players);

    public PlayerList(MinecraftServer pServer, LayeredRegistryAccess<RegistryLayer> pRegistries, PlayerDataStorage pPlayerIo, int pMaxPlayers) {
        this.server = pServer;
        this.registries = pRegistries;
        this.maxPlayers = pMaxPlayers;
        this.playerIo = pPlayerIo;
    }

    public void placeNewPlayer(Connection pConnection, ServerPlayer pPlayer, CommonListenerCookie pCookie) {
        GameProfile gameprofile = pPlayer.getGameProfile();
        GameProfileCache gameprofilecache = this.server.getProfileCache();
        String s;
        if (gameprofilecache != null) {
            Optional<GameProfile> optional = gameprofilecache.get(gameprofile.getId());
            s = optional.map(GameProfile::getName).orElse(gameprofile.getName());
            gameprofilecache.add(gameprofile);
        } else {
            s = gameprofile.getName();
        }

        Optional<CompoundTag> optional1 = this.load(pPlayer);
        ResourceKey<Level> resourcekey = optional1.<ResourceKey<Level>>flatMap(
                p_337568_ -> DimensionType.parseLegacy(new Dynamic<>(NbtOps.INSTANCE, p_337568_.get("Dimension"))).resultOrPartial(LOGGER::error)
            )
            .orElse(Level.OVERWORLD);
        ServerLevel serverlevel = this.server.getLevel(resourcekey);
        ServerLevel serverlevel1;
        if (serverlevel == null) {
            LOGGER.warn("Unknown respawn dimension {}, defaulting to overworld", resourcekey);
            serverlevel1 = this.server.overworld();
        } else {
            serverlevel1 = serverlevel;
        }

        pPlayer.setServerLevel(serverlevel1);
        String s1 = pConnection.getLoggableAddress(this.server.logIPs());
        LOGGER.info(
            "{}[{}] logged in with entity id {} at ({}, {}, {})",
            pPlayer.getName().getString(),
            s1,
            pPlayer.getId(),
            pPlayer.getX(),
            pPlayer.getY(),
            pPlayer.getZ()
        );
        LevelData leveldata = serverlevel1.getLevelData();
        pPlayer.loadGameTypes(optional1.orElse(null));
        ServerGamePacketListenerImpl servergamepacketlistenerimpl = new ServerGamePacketListenerImpl(this.server, pConnection, pPlayer, pCookie);
        pConnection.setupInboundProtocol(
            GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(this.server.registryAccess(), servergamepacketlistenerimpl.getConnectionType())), servergamepacketlistenerimpl
        );
        GameRules gamerules = serverlevel1.getGameRules();
        boolean flag = gamerules.getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN);
        boolean flag1 = gamerules.getBoolean(GameRules.RULE_REDUCEDDEBUGINFO);
        boolean flag2 = gamerules.getBoolean(GameRules.RULE_LIMITED_CRAFTING);
        servergamepacketlistenerimpl.send(
            new ClientboundLoginPacket(
                pPlayer.getId(),
                leveldata.isHardcore(),
                this.server.levelKeys(),
                this.getMaxPlayers(),
                this.viewDistance,
                this.simulationDistance,
                flag1,
                !flag,
                flag2,
                pPlayer.createCommonSpawnInfo(serverlevel1),
                this.server.enforceSecureProfile()
            )
        );
        servergamepacketlistenerimpl.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
        servergamepacketlistenerimpl.send(new ClientboundPlayerAbilitiesPacket(pPlayer.getAbilities()));
        servergamepacketlistenerimpl.send(new ClientboundSetCarriedItemPacket(pPlayer.getInventory().selected));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.OnDatapackSyncEvent(this, pPlayer));
        servergamepacketlistenerimpl.send(new ClientboundUpdateRecipesPacket(this.server.getRecipeManager().getOrderedRecipes()));
        this.sendPlayerPermissionLevel(pPlayer);
        pPlayer.getStats().markAllDirty();
        pPlayer.getRecipeBook().sendInitialRecipeBook(pPlayer);
        this.updateEntireScoreboard(serverlevel1.getScoreboard(), pPlayer);
        this.server.invalidateStatus();
        MutableComponent mutablecomponent;
        if (pPlayer.getGameProfile().getName().equalsIgnoreCase(s)) {
            mutablecomponent = Component.translatable("multiplayer.player.joined", pPlayer.getDisplayName());
        } else {
            mutablecomponent = Component.translatable("multiplayer.player.joined.renamed", pPlayer.getDisplayName(), s);
        }

        this.broadcastSystemMessage(mutablecomponent.withStyle(ChatFormatting.YELLOW), false);
        servergamepacketlistenerimpl.teleport(pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), pPlayer.getYRot(), pPlayer.getXRot());
        ServerStatus serverstatus = this.server.getStatus();
        if (serverstatus != null && !pCookie.transferred()) {
            pPlayer.sendServerStatus(serverstatus);
        }

        pPlayer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(this.players));
        this.players.add(pPlayer);
        this.playersByUUID.put(pPlayer.getUUID(), pPlayer);
        this.broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(pPlayer)));
        this.sendLevelInfo(pPlayer, serverlevel1);
        serverlevel1.addNewPlayer(pPlayer);
        this.server.getCustomBossEvents().onPlayerConnect(pPlayer);
        this.sendActivePlayerEffects(pPlayer);
        if (optional1.isPresent() && optional1.get().contains("RootVehicle", 10)) {
            CompoundTag compoundtag = optional1.get().getCompound("RootVehicle");
            Entity entity = EntityType.loadEntityRecursive(
                compoundtag.getCompound("Entity"), serverlevel1, p_215603_ -> !serverlevel1.addWithUUID(p_215603_) ? null : p_215603_
            );
            if (entity != null) {
                UUID uuid;
                if (compoundtag.hasUUID("Attach")) {
                    uuid = compoundtag.getUUID("Attach");
                } else {
                    uuid = null;
                }

                if (entity.getUUID().equals(uuid)) {
                    pPlayer.startRiding(entity, true);
                } else {
                    for (Entity entity1 : entity.getIndirectPassengers()) {
                        if (entity1.getUUID().equals(uuid)) {
                            pPlayer.startRiding(entity1, true);
                            break;
                        }
                    }
                }

                if (!pPlayer.isPassenger()) {
                    LOGGER.warn("Couldn't reattach entity to player");
                    entity.discard();

                    for (Entity entity2 : entity.getIndirectPassengers()) {
                        entity2.discard();
                    }
                }
            }
        }

        pPlayer.initInventoryMenu();
        net.neoforged.neoforge.event.EventHooks.firePlayerLoggedIn( pPlayer );
    }

    protected void updateEntireScoreboard(ServerScoreboard pScoreboard, ServerPlayer pPlayer) {
        Set<Objective> set = Sets.newHashSet();

        for (PlayerTeam playerteam : pScoreboard.getPlayerTeams()) {
            pPlayer.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerteam, true));
        }

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            Objective objective = pScoreboard.getDisplayObjective(displayslot);
            if (objective != null && !set.contains(objective)) {
                for (Packet<?> packet : pScoreboard.getStartTrackingPackets(objective)) {
                    pPlayer.connection.send(packet);
                }

                set.add(objective);
            }
        }
    }

    public void addWorldborderListener(ServerLevel pLevel) {
        pLevel.getWorldBorder().addListener(new BorderChangeListener() {
            @Override
            public void onBorderSizeSet(WorldBorder p_11321_, double p_11322_) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderSizePacket(p_11321_));
            }

            @Override
            public void onBorderSizeLerping(WorldBorder p_11328_, double p_11329_, double p_11330_, long p_11331_) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderLerpSizePacket(p_11328_));
            }

            @Override
            public void onBorderCenterSet(WorldBorder p_11324_, double p_11325_, double p_11326_) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderCenterPacket(p_11324_));
            }

            @Override
            public void onBorderSetWarningTime(WorldBorder p_11333_, int p_11334_) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDelayPacket(p_11333_));
            }

            @Override
            public void onBorderSetWarningBlocks(WorldBorder p_11339_, int p_11340_) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDistancePacket(p_11339_));
            }

            @Override
            public void onBorderSetDamagePerBlock(WorldBorder p_11336_, double p_11337_) {
            }

            @Override
            public void onBorderSetDamageSafeZOne(WorldBorder p_11342_, double p_11343_) {
            }
        });
    }

    public Optional<CompoundTag> load(ServerPlayer pPlayer) {
        CompoundTag compoundtag = this.server.getWorldData().getLoadedPlayerTag();
        Optional<CompoundTag> optional;
        if (this.server.isSingleplayerOwner(pPlayer.getGameProfile()) && compoundtag != null) {
            optional = Optional.of(compoundtag);
            pPlayer.load(compoundtag);
            LOGGER.debug("loading single player");
            net.neoforged.neoforge.event.EventHooks.firePlayerLoadingEvent(pPlayer, this.playerIo, pPlayer.getUUID().toString());
        } else {
            optional = this.playerIo.load(pPlayer);
        }

        return optional;
    }

    /**
     * Also stores the NBTTags if this is an IntegratedPlayerList.
     */
    protected void save(ServerPlayer pPlayer) {
        if (pPlayer.connection == null) return;
        this.playerIo.save(pPlayer);
        ServerStatsCounter serverstatscounter = this.stats.get(pPlayer.getUUID());
        if (serverstatscounter != null) {
            serverstatscounter.save();
        }

        PlayerAdvancements playeradvancements = this.advancements.get(pPlayer.getUUID());
        if (playeradvancements != null) {
            playeradvancements.save();
        }
    }

    /**
     * Called when a player disconnects from the game. Writes player data to disk and removes them from the world.
     */
    public void remove(ServerPlayer pPlayer) {
        net.neoforged.neoforge.event.EventHooks.firePlayerLoggedOut(pPlayer);
        ServerLevel serverlevel = pPlayer.serverLevel();
        pPlayer.awardStat(Stats.LEAVE_GAME);
        this.save(pPlayer);
        if (pPlayer.isPassenger()) {
            Entity entity = pPlayer.getRootVehicle();
            if (entity.hasExactlyOnePlayerPassenger()) {
                LOGGER.debug("Removing player mount");
                pPlayer.stopRiding();
                entity.getPassengersAndSelf().forEach(p_215620_ -> p_215620_.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER));
            }
        }

        pPlayer.unRide();
        serverlevel.removePlayerImmediately(pPlayer, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        pPlayer.getAdvancements().stopListening();
        this.players.remove(pPlayer);
        this.server.getCustomBossEvents().onPlayerDisconnect(pPlayer);
        UUID uuid = pPlayer.getUUID();
        ServerPlayer serverplayer = this.playersByUUID.get(uuid);
        if (serverplayer == pPlayer) {
            this.playersByUUID.remove(uuid);
            this.stats.remove(uuid);
            this.advancements.remove(uuid);
        }

        this.broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(pPlayer.getUUID())));
    }

    @Nullable
    public Component canPlayerLogin(SocketAddress pSocketAddress, GameProfile pGameProfile) {
        if (this.bans.isBanned(pGameProfile)) {
            UserBanListEntry userbanlistentry = this.bans.get(pGameProfile);
            MutableComponent mutablecomponent1 = Component.translatable("multiplayer.disconnect.banned.reason", userbanlistentry.getReason());
            if (userbanlistentry.getExpires() != null) {
                mutablecomponent1.append(
                    Component.translatable("multiplayer.disconnect.banned.expiration", BAN_DATE_FORMAT.format(userbanlistentry.getExpires()))
                );
            }

            return mutablecomponent1;
        } else if (!this.isWhiteListed(pGameProfile)) {
            return Component.translatable("multiplayer.disconnect.not_whitelisted");
        } else if (this.ipBans.isBanned(pSocketAddress)) {
            IpBanListEntry ipbanlistentry = this.ipBans.get(pSocketAddress);
            MutableComponent mutablecomponent = Component.translatable("multiplayer.disconnect.banned_ip.reason", ipbanlistentry.getReason());
            if (ipbanlistentry.getExpires() != null) {
                mutablecomponent.append(
                    Component.translatable("multiplayer.disconnect.banned_ip.expiration", BAN_DATE_FORMAT.format(ipbanlistentry.getExpires()))
                );
            }

            return mutablecomponent;
        } else {
            return this.players.size() >= this.maxPlayers && !this.canBypassPlayerLimit(pGameProfile)
                ? Component.translatable("multiplayer.disconnect.server_full")
                : null;
        }
    }

    public ServerPlayer getPlayerForLogin(GameProfile pGameProfile, ClientInformation pClientInformation) {
        return new ServerPlayer(this.server, this.server.overworld(), pGameProfile, pClientInformation);
    }

    public boolean disconnectAllPlayersWithProfile(GameProfile pGameProfile) {
        UUID uuid = pGameProfile.getId();
        Set<ServerPlayer> set = Sets.newIdentityHashSet();

        for (ServerPlayer serverplayer : this.players) {
            if (serverplayer.getUUID().equals(uuid)) {
                set.add(serverplayer);
            }
        }

        ServerPlayer serverplayer2 = this.playersByUUID.get(pGameProfile.getId());
        if (serverplayer2 != null) {
            set.add(serverplayer2);
        }

        for (ServerPlayer serverplayer1 : set) {
            serverplayer1.connection.disconnect(DUPLICATE_LOGIN_DISCONNECT_MESSAGE);
        }

        return !set.isEmpty();
    }

    public ServerPlayer respawn(ServerPlayer pPlayer, boolean pKeepEverything, Entity.RemovalReason p_348558_) {
        this.players.remove(pPlayer);
        pPlayer.serverLevel().removePlayerImmediately(pPlayer, p_348558_);
        DimensionTransition dimensiontransition = pPlayer.findRespawnPositionAndUseSpawnBlock(pKeepEverything, DimensionTransition.DO_NOTHING);

        // Neo: Allow changing the respawn position of players. The local dimension transition is updated with the new target.
        var event = net.neoforged.neoforge.event.EventHooks.firePlayerRespawnPositionEvent(pPlayer, dimensiontransition, pKeepEverything);
        dimensiontransition = event.getDimensionTransition();

        ServerLevel serverlevel = dimensiontransition.newLevel();
        ServerPlayer serverplayer = new ServerPlayer(this.server, serverlevel, pPlayer.getGameProfile(), pPlayer.clientInformation());
        serverplayer.connection = pPlayer.connection;
        serverplayer.restoreFrom(pPlayer, pKeepEverything);
        serverplayer.setId(pPlayer.getId());
        serverplayer.setMainArm(pPlayer.getMainArm());

        // Neo: Allow the event to control if the original spawn position is copied
        if (event.copyOriginalSpawnPosition()) {
            serverplayer.copyRespawnPosition(pPlayer);
        }

        for (String s : pPlayer.getTags()) {
            serverplayer.addTag(s);
        }

        Vec3 vec3 = dimensiontransition.pos();
        serverplayer.moveTo(vec3.x, vec3.y, vec3.z, dimensiontransition.yRot(), dimensiontransition.xRot());
        if (dimensiontransition.missingRespawnBlock()) {
            serverplayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
        }

        byte b0 = (byte)(pKeepEverything ? 1 : 0);
        ServerLevel serverlevel1 = serverplayer.serverLevel();
        LevelData leveldata = serverlevel1.getLevelData();
        serverplayer.connection.send(new ClientboundRespawnPacket(serverplayer.createCommonSpawnInfo(serverlevel1), b0));
        serverplayer.connection.teleport(serverplayer.getX(), serverplayer.getY(), serverplayer.getZ(), serverplayer.getYRot(), serverplayer.getXRot());
        serverplayer.connection.send(new ClientboundSetDefaultSpawnPositionPacket(serverlevel.getSharedSpawnPos(), serverlevel.getSharedSpawnAngle()));
        serverplayer.connection.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
        serverplayer.connection
            .send(new ClientboundSetExperiencePacket(serverplayer.experienceProgress, serverplayer.totalExperience, serverplayer.experienceLevel));
        this.sendActivePlayerEffects(serverplayer);
        this.sendLevelInfo(serverplayer, serverlevel);
        this.sendPlayerPermissionLevel(serverplayer);
        serverlevel.addRespawnedPlayer(serverplayer);
        this.players.add(serverplayer);
        this.playersByUUID.put(serverplayer.getUUID(), serverplayer);
        serverplayer.initInventoryMenu();
        serverplayer.setHealth(serverplayer.getHealth());
        net.neoforged.neoforge.event.EventHooks.firePlayerRespawnEvent(serverplayer, pKeepEverything);
        if (!pKeepEverything) {
            BlockPos blockpos = BlockPos.containing(dimensiontransition.pos());
            BlockState blockstate = serverlevel.getBlockState(blockpos);
            if (blockstate.is(Blocks.RESPAWN_ANCHOR)) {
                serverplayer.connection
                    .send(
                        new ClientboundSoundPacket(
                            SoundEvents.RESPAWN_ANCHOR_DEPLETE,
                            SoundSource.BLOCKS,
                            (double)blockpos.getX(),
                            (double)blockpos.getY(),
                            (double)blockpos.getZ(),
                            1.0F,
                            1.0F,
                            serverlevel.getRandom().nextLong()
                        )
                    );
            }
        }

        return serverplayer;
    }

    public void sendActivePlayerEffects(ServerPlayer p_348494_) {
        this.sendActiveEffects(p_348494_, p_348494_.connection);
    }

    public void sendActiveEffects(LivingEntity p_348624_, ServerGamePacketListenerImpl p_348496_) {
        for (MobEffectInstance mobeffectinstance : p_348624_.getActiveEffects()) {
            p_348496_.send(new ClientboundUpdateMobEffectPacket(p_348624_.getId(), mobeffectinstance, false));
        }
    }

    public void sendPlayerPermissionLevel(ServerPlayer pPlayer) {
        GameProfile gameprofile = pPlayer.getGameProfile();
        int i = this.server.getProfilePermissions(gameprofile);
        this.sendPlayerPermissionLevel(pPlayer, i);
    }

    public void tick() {
        if (++this.sendAllPlayerInfoIn > 600) {
            this.broadcastAll(new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY), this.players));
            this.sendAllPlayerInfoIn = 0;
        }
    }

    public void broadcastAll(Packet<?> pPacket) {
        for (ServerPlayer serverplayer : this.players) {
            serverplayer.connection.send(pPacket);
        }
    }

    public void broadcastAll(Packet<?> pPacket, ResourceKey<Level> pDimension) {
        for (ServerPlayer serverplayer : this.players) {
            if (serverplayer.level().dimension() == pDimension) {
                serverplayer.connection.send(pPacket);
            }
        }
    }

    public void broadcastSystemToTeam(Player pPlayer, Component pMessage) {
        Team team = pPlayer.getTeam();
        if (team != null) {
            for (String s : team.getPlayers()) {
                ServerPlayer serverplayer = this.getPlayerByName(s);
                if (serverplayer != null && serverplayer != pPlayer) {
                    serverplayer.sendSystemMessage(pMessage);
                }
            }
        }
    }

    public void broadcastSystemToAllExceptTeam(Player pPlayer, Component pMessage) {
        Team team = pPlayer.getTeam();
        if (team == null) {
            this.broadcastSystemMessage(pMessage, false);
        } else {
            for (int i = 0; i < this.players.size(); i++) {
                ServerPlayer serverplayer = this.players.get(i);
                if (serverplayer.getTeam() != team) {
                    serverplayer.sendSystemMessage(pMessage);
                }
            }
        }
    }

    public String[] getPlayerNamesArray() {
        String[] astring = new String[this.players.size()];

        for (int i = 0; i < this.players.size(); i++) {
            astring[i] = this.players.get(i).getGameProfile().getName();
        }

        return astring;
    }

    public UserBanList getBans() {
        return this.bans;
    }

    public IpBanList getIpBans() {
        return this.ipBans;
    }

    public void op(GameProfile pProfile) {
        if (net.neoforged.neoforge.event.EventHooks.onPermissionChanged(pProfile, this.server.getOperatorUserPermissionLevel(), this)) return;
        this.ops.add(new ServerOpListEntry(pProfile, this.server.getOperatorUserPermissionLevel(), this.ops.canBypassPlayerLimit(pProfile)));
        ServerPlayer serverplayer = this.getPlayer(pProfile.getId());
        if (serverplayer != null) {
            this.sendPlayerPermissionLevel(serverplayer);
        }
    }

    public void deop(GameProfile pProfile) {
        if (net.neoforged.neoforge.event.EventHooks.onPermissionChanged(pProfile, 0, this)) return;
        this.ops.remove(pProfile);
        ServerPlayer serverplayer = this.getPlayer(pProfile.getId());
        if (serverplayer != null) {
            this.sendPlayerPermissionLevel(serverplayer);
        }
    }

    private void sendPlayerPermissionLevel(ServerPlayer pPlayer, int pPermLevel) {
        if (pPlayer.connection != null) {
            byte b0;
            if (pPermLevel <= 0) {
                b0 = 24;
            } else if (pPermLevel >= 4) {
                b0 = 28;
            } else {
                b0 = (byte)(24 + pPermLevel);
            }

            pPlayer.connection.send(new ClientboundEntityEventPacket(pPlayer, b0));
        }

        this.server.getCommands().sendCommands(pPlayer);
    }

    public boolean isWhiteListed(GameProfile pProfile) {
        return !this.doWhiteList || this.ops.contains(pProfile) || this.whitelist.contains(pProfile);
    }

    public boolean isOp(GameProfile pProfile) {
        return this.ops.contains(pProfile)
            || this.server.isSingleplayerOwner(pProfile) && this.server.getWorldData().isAllowCommands()
            || this.allowCommandsForAllPlayers;
    }

    @Nullable
    public ServerPlayer getPlayerByName(String pUsername) {
        int i = this.players.size();

        for (int j = 0; j < i; j++) {
            ServerPlayer serverplayer = this.players.get(j);
            if (serverplayer.getGameProfile().getName().equalsIgnoreCase(pUsername)) {
                return serverplayer;
            }
        }

        return null;
    }

    public void broadcast(
        @Nullable Player pExcept, double pX, double pY, double pZ, double pRadius, ResourceKey<Level> pDimension, Packet<?> pPacket
    ) {
        for (int i = 0; i < this.players.size(); i++) {
            ServerPlayer serverplayer = this.players.get(i);
            if (serverplayer != pExcept && serverplayer.level().dimension() == pDimension) {
                double d0 = pX - serverplayer.getX();
                double d1 = pY - serverplayer.getY();
                double d2 = pZ - serverplayer.getZ();
                if (d0 * d0 + d1 * d1 + d2 * d2 < pRadius * pRadius) {
                    serverplayer.connection.send(pPacket);
                }
            }
        }
    }

    public void saveAll() {
        for (int i = 0; i < this.players.size(); i++) {
            this.save(this.players.get(i));
        }
    }

    public UserWhiteList getWhiteList() {
        return this.whitelist;
    }

    public String[] getWhiteListNames() {
        return this.whitelist.getUserList();
    }

    public ServerOpList getOps() {
        return this.ops;
    }

    public String[] getOpNames() {
        return this.ops.getUserList();
    }

    public void reloadWhiteList() {
    }

    /**
     * Updates the time and weather for the given player to those of the given world
     */
    public void sendLevelInfo(ServerPlayer pPlayer, ServerLevel pLevel) {
        WorldBorder worldborder = this.server.overworld().getWorldBorder();
        pPlayer.connection.send(new ClientboundInitializeBorderPacket(worldborder));
        pPlayer.connection
            .send(new ClientboundSetTimePacket(pLevel.getGameTime(), pLevel.getDayTime(), pLevel.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)));
        pPlayer.connection.send(new ClientboundSetDefaultSpawnPositionPacket(pLevel.getSharedSpawnPos(), pLevel.getSharedSpawnAngle()));
        if (pLevel.isRaining()) {
            pPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
            pPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, pLevel.getRainLevel(1.0F)));
            pPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, pLevel.getThunderLevel(1.0F)));
        }

        pPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START, 0.0F));
        this.server.tickRateManager().updateJoiningPlayer(pPlayer);
    }

    /**
     * Sends the players inventory to himself.
     */
    public void sendAllPlayerInfo(ServerPlayer pPlayer) {
        pPlayer.inventoryMenu.sendAllDataToRemote();
        pPlayer.resetSentInfo();
        pPlayer.connection.send(new ClientboundSetCarriedItemPacket(pPlayer.getInventory().selected));
    }

    public int getPlayerCount() {
        return this.players.size();
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public boolean isUsingWhitelist() {
        return this.doWhiteList;
    }

    public void setUsingWhiteList(boolean pWhitelistEnabled) {
        this.doWhiteList = pWhitelistEnabled;
    }

    public List<ServerPlayer> getPlayersWithAddress(String pAddress) {
        List<ServerPlayer> list = Lists.newArrayList();

        for (ServerPlayer serverplayer : this.players) {
            if (serverplayer.getIpAddress().equals(pAddress)) {
                list.add(serverplayer);
            }
        }

        return list;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public int getSimulationDistance() {
        return this.simulationDistance;
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    @Nullable
    public CompoundTag getSingleplayerData() {
        return null;
    }

    public void setAllowCommandsForAllPlayers(boolean pAllowCommandsForAllPlayers) {
        this.allowCommandsForAllPlayers = pAllowCommandsForAllPlayers;
    }

    public void removeAll() {
        for (int i = 0; i < this.players.size(); i++) {
            this.players.get(i).connection.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
        }
    }

    public void broadcastSystemMessage(Component pMessage, boolean pBypassHiddenChat) {
        this.broadcastSystemMessage(pMessage, p_215639_ -> pMessage, pBypassHiddenChat);
    }

    public void broadcastSystemMessage(Component pServerMessage, Function<ServerPlayer, Component> pPlayerMessageFactory, boolean pBypassHiddenChat) {
        this.server.sendSystemMessage(pServerMessage);

        for (ServerPlayer serverplayer : this.players) {
            Component component = pPlayerMessageFactory.apply(serverplayer);
            if (component != null) {
                serverplayer.sendSystemMessage(component, pBypassHiddenChat);
            }
        }
    }

    public void broadcastChatMessage(PlayerChatMessage pMessage, CommandSourceStack pSender, ChatType.Bound pBoundChatType) {
        this.broadcastChatMessage(pMessage, pSender::shouldFilterMessageTo, pSender.getPlayer(), pBoundChatType);
    }

    public void broadcastChatMessage(PlayerChatMessage pMessage, ServerPlayer pSender, ChatType.Bound pBoundChatType) {
        this.broadcastChatMessage(pMessage, pSender::shouldFilterMessageTo, pSender, pBoundChatType);
    }

    private void broadcastChatMessage(
        PlayerChatMessage pMessage, Predicate<ServerPlayer> pShouldFilterMessageTo, @Nullable ServerPlayer pSender, ChatType.Bound pBoundChatType
    ) {
        boolean flag = this.verifyChatTrusted(pMessage);
        this.server.logChatMessage(pMessage.decoratedContent(), pBoundChatType, flag ? null : "Not Secure");
        OutgoingChatMessage outgoingchatmessage = OutgoingChatMessage.create(pMessage);
        boolean flag1 = false;

        for (ServerPlayer serverplayer : this.players) {
            boolean flag2 = pShouldFilterMessageTo.test(serverplayer);
            serverplayer.sendChatMessage(outgoingchatmessage, flag2, pBoundChatType);
            flag1 |= flag2 && pMessage.isFullyFiltered();
        }

        if (flag1 && pSender != null) {
            pSender.sendSystemMessage(CHAT_FILTERED_FULL);
        }
    }

    private boolean verifyChatTrusted(PlayerChatMessage pMessage) {
        return pMessage.hasSignature() && !pMessage.hasExpiredServer(Instant.now());
    }

    public ServerStatsCounter getPlayerStats(Player pPlayer) {
        UUID uuid = pPlayer.getUUID();
        ServerStatsCounter serverstatscounter = this.stats.get(uuid);
        if (serverstatscounter == null) {
            File file1 = this.server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile();
            File file2 = new File(file1, uuid + ".json");

            serverstatscounter = new ServerStatsCounter(this.server, file2);
            this.stats.put(uuid, serverstatscounter);
        }

        return serverstatscounter;
    }

    public PlayerAdvancements getPlayerAdvancements(ServerPlayer pPlayer) {
        UUID uuid = pPlayer.getUUID();
        PlayerAdvancements playeradvancements = this.advancements.get(uuid);
        if (playeradvancements == null) {
            Path path = this.server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(uuid + ".json");
            playeradvancements = new PlayerAdvancements(this.server.getFixerUpper(), this, this.server.getAdvancements(), path, pPlayer);
            this.advancements.put(uuid, playeradvancements);
        }

        // Forge: don't overwrite active player with a fake one.
        if (!(pPlayer instanceof net.neoforged.neoforge.common.util.FakePlayer))
        playeradvancements.setPlayer(pPlayer);
        return playeradvancements;
    }

    public void setViewDistance(int pViewDistance) {
        this.viewDistance = pViewDistance;
        this.broadcastAll(new ClientboundSetChunkCacheRadiusPacket(pViewDistance));

        for (ServerLevel serverlevel : this.server.getAllLevels()) {
            if (serverlevel != null) {
                serverlevel.getChunkSource().setViewDistance(pViewDistance);
            }
        }
    }

    public void setSimulationDistance(int pSimulationDistance) {
        this.simulationDistance = pSimulationDistance;
        this.broadcastAll(new ClientboundSetSimulationDistancePacket(pSimulationDistance));

        for (ServerLevel serverlevel : this.server.getAllLevels()) {
            if (serverlevel != null) {
                serverlevel.getChunkSource().setSimulationDistance(pSimulationDistance);
            }
        }
    }

    public List<ServerPlayer> getPlayers() {
        return this.playersView; //Unmodifiable view, we don't want people removing things without us knowing.
    }

    /**
     * Gets the ServerPlayer object representing the player with the UUID.
     */
    @Nullable
    public ServerPlayer getPlayer(UUID pPlayerUUID) {
        return this.playersByUUID.get(pPlayerUUID);
    }

    public boolean canBypassPlayerLimit(GameProfile pProfile) {
        return false;
    }

    public void reloadResources() {
        for (PlayerAdvancements playeradvancements : this.advancements.values()) {
            playeradvancements.reload(this.server.getAdvancements());
        }

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.OnDatapackSyncEvent(this, null));
        this.broadcastAll(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registries)));
        ClientboundUpdateRecipesPacket clientboundupdaterecipespacket = new ClientboundUpdateRecipesPacket(this.server.getRecipeManager().getOrderedRecipes());

        for (ServerPlayer serverplayer : this.players) {
            serverplayer.connection.send(clientboundupdaterecipespacket);
            serverplayer.getRecipeBook().sendInitialRecipeBook(serverplayer);
        }
    }

    public boolean isAllowCommandsForAllPlayers() {
        return this.allowCommandsForAllPlayers;
    }
}
