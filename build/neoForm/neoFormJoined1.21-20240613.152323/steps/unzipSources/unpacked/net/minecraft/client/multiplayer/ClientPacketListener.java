package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.time.Instant;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.DebugQueryHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DemoIntroScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerReconfigScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.debug.BrainDebugRenderer;
import net.minecraft.client.renderer.debug.NeighborsUpdateRenderer;
import net.minecraft.client.renderer.debug.VillageSectionsDebugRenderer;
import net.minecraft.client.renderer.debug.WorldGenAttemptRenderer;
import net.minecraft.client.resources.sounds.BeeAggressiveSoundInstance;
import net.minecraft.client.resources.sounds.BeeFlyingSoundInstance;
import net.minecraft.client.resources.sounds.BeeSoundInstance;
import net.minecraft.client.resources.sounds.GuardianAttackSoundInstance;
import net.minecraft.client.resources.sounds.MinecartSoundInstance;
import net.minecraft.client.resources.sounds.SnifferSoundInstance;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.LocalChatSession;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.chat.SignedMessageLink;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.custom.BeeDebugPayload;
import net.minecraft.network.protocol.common.custom.BrainDebugPayload;
import net.minecraft.network.protocol.common.custom.BreezeDebugPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.GameEventDebugPayload;
import net.minecraft.network.protocol.common.custom.GameEventListenerDebugPayload;
import net.minecraft.network.protocol.common.custom.GameTestAddMarkerDebugPayload;
import net.minecraft.network.protocol.common.custom.GameTestClearMarkersDebugPayload;
import net.minecraft.network.protocol.common.custom.GoalDebugPayload;
import net.minecraft.network.protocol.common.custom.HiveDebugPayload;
import net.minecraft.network.protocol.common.custom.NeighborUpdatesDebugPayload;
import net.minecraft.network.protocol.common.custom.PathfindingDebugPayload;
import net.minecraft.network.protocol.common.custom.PoiAddedDebugPayload;
import net.minecraft.network.protocol.common.custom.PoiRemovedDebugPayload;
import net.minecraft.network.protocol.common.custom.PoiTicketCountDebugPayload;
import net.minecraft.network.protocol.common.custom.RaidsDebugPayload;
import net.minecraft.network.protocol.common.custom.StructuresDebugPayload;
import net.minecraft.network.protocol.common.custom.VillageSectionsDebugPayload;
import net.minecraft.network.protocol.common.custom.WorldGenAttemptDebugPayload;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAddExperienceOrbPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundDebugSamplePacket;
import net.minecraft.network.protocol.game.ClientboundDeleteChatPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundProjectilePowerPacket;
import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTickingStatePacket;
import net.minecraft.network.protocol.game.ClientboundTickingStepPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerLinks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.Crypt;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SignatureValidator;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientPacketListener extends ClientCommonPacketListenerImpl implements ClientGamePacketListener, TickablePacketListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component UNSECURE_SERVER_TOAST_TITLE = Component.translatable("multiplayer.unsecureserver.toast.title");
    private static final Component UNSERURE_SERVER_TOAST = Component.translatable("multiplayer.unsecureserver.toast");
    private static final Component INVALID_PACKET = Component.translatable("multiplayer.disconnect.invalid_packet");
    private static final Component CHAT_VALIDATION_FAILED_ERROR = Component.translatable("multiplayer.disconnect.chat_validation_failed");
    private static final Component RECONFIGURE_SCREEN_MESSAGE = Component.translatable("connect.reconfiguring");
    private static final int PENDING_OFFSET_THRESHOLD = 64;
    private final GameProfile localGameProfile;
    /**
     * Reference to the current ClientWorld instance, which many handler methods operate on
     */
    private ClientLevel level;
    private ClientLevel.ClientLevelData levelData;
    /**
     * A mapping from player names to their respective GuiPlayerInfo (specifies the clients response time to the server)
     */
    private final Map<UUID, PlayerInfo> playerInfoMap = Maps.newHashMap();
    private final Set<PlayerInfo> listedPlayers = new ReferenceOpenHashSet<>();
    private final ClientAdvancements advancements;
    private final ClientSuggestionProvider suggestionsProvider;
    private final DebugQueryHandler debugQueryHandler = new DebugQueryHandler(this);
    private int serverChunkRadius = 3;
    private int serverSimulationDistance = 3;
    /**
     * Just an ordinary random number generator, used to randomize audio pitch of item/orb pickup and randomize both particlespawn offset and velocity
     */
    private final RandomSource random = RandomSource.createThreadSafe();
    public CommandDispatcher<SharedSuggestionProvider> commands = new CommandDispatcher<>();
    private final RecipeManager recipeManager;
    private final UUID id = UUID.randomUUID();
    private Set<ResourceKey<Level>> levels;
    private final RegistryAccess.Frozen registryAccess;
    private final FeatureFlagSet enabledFeatures;
    private final PotionBrewing potionBrewing;
    @Nullable
    private LocalChatSession chatSession;
    private SignedMessageChain.Encoder signedMessageEncoder = SignedMessageChain.Encoder.UNSIGNED;
    private LastSeenMessagesTracker lastSeenMessages = new LastSeenMessagesTracker(20);
    private MessageSignatureCache messageSignatureCache = MessageSignatureCache.createDefault();
    private final ChunkBatchSizeCalculator chunkBatchSizeCalculator = new ChunkBatchSizeCalculator();
    private final PingDebugMonitor pingDebugMonitor;
    private final DebugSampleSubscriber debugSampleSubscriber;
    private net.neoforged.neoforge.network.connection.ConnectionType connectionType;
    @Nullable
    private LevelLoadStatusManager levelLoadStatusManager;
    private boolean serverEnforcesSecureChat;
    private boolean seenInsecureChatWarning = false;
    private volatile boolean closed;
    private final Scoreboard scoreboard = new Scoreboard();
    private final SessionSearchTrees searchTrees = new SessionSearchTrees();

    public ClientPacketListener(Minecraft pMinecraft, Connection pConnection, CommonListenerCookie pCommonListenerCookie) {
        super(pMinecraft, pConnection, pCommonListenerCookie);
        this.localGameProfile = pCommonListenerCookie.localGameProfile();
        this.registryAccess = pCommonListenerCookie.receivedRegistries();
        this.enabledFeatures = pCommonListenerCookie.enabledFeatures();
        this.advancements = new ClientAdvancements(pMinecraft, this.telemetryManager);
        this.suggestionsProvider = new ClientSuggestionProvider(this, pMinecraft);
        this.pingDebugMonitor = new PingDebugMonitor(this, pMinecraft.getDebugOverlay().getPingLogger());
        this.recipeManager = new RecipeManager(this.registryAccess);
        this.debugSampleSubscriber = new DebugSampleSubscriber(this, pMinecraft.getDebugOverlay());
        if (pCommonListenerCookie.chatState() != null) {
            pMinecraft.gui.getChat().restoreState(pCommonListenerCookie.chatState());
        }

        this.connectionType = pCommonListenerCookie.connectionType();
        this.potionBrewing = PotionBrewing.bootstrap(this.enabledFeatures);
    }

    public ClientSuggestionProvider getSuggestionsProvider() {
        return this.suggestionsProvider;
    }

    public void close() {
        this.closed = true;
        this.clearLevel();
        this.telemetryManager.onDisconnect();
    }

    public void clearLevel() {
        this.level = null;
        this.levelLoadStatusManager = null;
    }

    public RecipeManager getRecipeManager() {
        return this.recipeManager;
    }

    /**
     * Registers some server properties (gametype, hardcore-mode, terraintype, difficulty, player limit), creates a new WorldClient and sets the player initial dimension.
     */
    @Override
    public void handleLogin(ClientboundLoginPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.gameMode = new MultiPlayerGameMode(this.minecraft, this);
        CommonPlayerSpawnInfo commonplayerspawninfo = pPacket.commonPlayerSpawnInfo();
        List<ResourceKey<Level>> list = Lists.newArrayList(pPacket.levels());
        Collections.shuffle(list);
        this.levels = Sets.newLinkedHashSet(list);
        ResourceKey<Level> resourcekey = commonplayerspawninfo.dimension();
        Holder<DimensionType> holder = commonplayerspawninfo.dimensionType();
        this.serverChunkRadius = pPacket.chunkRadius();
        this.serverSimulationDistance = pPacket.simulationDistance();
        boolean flag = commonplayerspawninfo.isDebug();
        boolean flag1 = commonplayerspawninfo.isFlat();
        ClientLevel.ClientLevelData clientlevel$clientleveldata = new ClientLevel.ClientLevelData(Difficulty.NORMAL, pPacket.hardcore(), flag1);
        this.levelData = clientlevel$clientleveldata;
        this.level = new ClientLevel(
            this,
            clientlevel$clientleveldata,
            resourcekey,
            holder,
            this.serverChunkRadius,
            this.serverSimulationDistance,
            this.minecraft::getProfiler,
            this.minecraft.levelRenderer,
            flag,
            commonplayerspawninfo.seed()
        );
        this.minecraft.setLevel(this.level, ReceivingLevelScreen.Reason.OTHER);
        if (this.minecraft.player == null) {
            this.minecraft.player = this.minecraft.gameMode.createPlayer(this.level, new StatsCounter(), new ClientRecipeBook());
            this.minecraft.player.setYRot(-180.0F);
            if (this.minecraft.getSingleplayerServer() != null) {
                this.minecraft.getSingleplayerServer().setUUID(this.minecraft.player.getUUID());
            }
        }

        this.minecraft.debugRenderer.clear();
        this.minecraft.player.resetPos();
        net.neoforged.neoforge.client.ClientHooks.firePlayerLogin(this.minecraft.gameMode, this.minecraft.player, this.minecraft.getConnection().connection);
        this.minecraft.player.setId(pPacket.playerId());
        this.level.addEntity(this.minecraft.player);
        this.minecraft.player.input = new KeyboardInput(this.minecraft.options);
        this.minecraft.gameMode.adjustPlayer(this.minecraft.player);
        this.minecraft.cameraEntity = this.minecraft.player;
        this.startWaitingForNewLevel(this.minecraft.player, this.level, ReceivingLevelScreen.Reason.OTHER);
        this.minecraft.player.setReducedDebugInfo(pPacket.reducedDebugInfo());
        this.minecraft.player.setShowDeathScreen(pPacket.showDeathScreen());
        this.minecraft.player.setDoLimitedCrafting(pPacket.doLimitedCrafting());
        this.minecraft.player.setLastDeathLocation(commonplayerspawninfo.lastDeathLocation());
        this.minecraft.player.setPortalCooldown(commonplayerspawninfo.portalCooldown());
        this.minecraft.gameMode.setLocalMode(commonplayerspawninfo.gameType(), commonplayerspawninfo.previousGameType());
        this.minecraft.options.setServerRenderDistance(pPacket.chunkRadius());
        this.chatSession = null;
        this.lastSeenMessages = new LastSeenMessagesTracker(20);
        this.messageSignatureCache = MessageSignatureCache.createDefault();
        if (this.connection.isEncrypted()) {
            this.minecraft.getProfileKeyPairManager().prepareKeyPair().thenAcceptAsync(p_253341_ -> p_253341_.ifPresent(this::setKeyPair), this.minecraft);
        }

        this.telemetryManager.onPlayerInfoReceived(commonplayerspawninfo.gameType(), pPacket.hardcore());
        this.minecraft.quickPlayLog().log(this.minecraft);
        this.serverEnforcesSecureChat = pPacket.enforcesSecureChat();
        if (this.serverData != null && !this.seenInsecureChatWarning && !this.enforcesSecureChat()) {
            SystemToast systemtoast = SystemToast.multiline(
                this.minecraft, SystemToast.SystemToastId.UNSECURE_SERVER_WARNING, UNSECURE_SERVER_TOAST_TITLE, UNSERURE_SERVER_TOAST
            );
            this.minecraft.getToasts().addToast(systemtoast);
            this.seenInsecureChatWarning = true;
        }
    }

    /**
     * Spawns an instance of the objecttype indicated by the packet and sets its position and momentum
     */
    @Override
    public void handleAddEntity(ClientboundAddEntityPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.createEntityFromPacket(pPacket);
        if (entity != null) {
            entity.recreateFromPacket(pPacket);
            this.level.addEntity(entity);
            this.postAddEntitySoundInstance(entity);
        } else {
            LOGGER.warn("Skipping Entity with id {}", pPacket.getType());
        }
    }

    @Nullable
    private Entity createEntityFromPacket(ClientboundAddEntityPacket pPacket) {
        EntityType<?> entitytype = pPacket.getType();
        if (entitytype == EntityType.PLAYER) {
            PlayerInfo playerinfo = this.getPlayerInfo(pPacket.getUUID());
            if (playerinfo == null) {
                LOGGER.warn("Server attempted to add player prior to sending player info (Player id {})", pPacket.getUUID());
                return null;
            } else {
                return new RemotePlayer(this.level, playerinfo.getProfile());
            }
        } else {
            return entitytype.create(this.level);
        }
    }

    private void postAddEntitySoundInstance(Entity pEntity) {
        if (pEntity instanceof AbstractMinecart abstractminecart) {
            this.minecraft.getSoundManager().play(new MinecartSoundInstance(abstractminecart));
        } else if (pEntity instanceof Bee bee) {
            boolean flag = bee.isAngry();
            BeeSoundInstance beesoundinstance;
            if (flag) {
                beesoundinstance = new BeeAggressiveSoundInstance(bee);
            } else {
                beesoundinstance = new BeeFlyingSoundInstance(bee);
            }

            this.minecraft.getSoundManager().queueTickingSound(beesoundinstance);
        }
    }

    /**
     * Spawns an experience orb and sets its value (amount of XP)
     */
    @Override
    public void handleAddExperienceOrb(ClientboundAddExperienceOrbPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        double d0 = pPacket.getX();
        double d1 = pPacket.getY();
        double d2 = pPacket.getZ();
        Entity entity = new ExperienceOrb(this.level, d0, d1, d2, pPacket.getValue());
        entity.syncPacketPositionCodec(d0, d1, d2);
        entity.setYRot(0.0F);
        entity.setXRot(0.0F);
        entity.setId(pPacket.getId());
        this.level.addEntity(entity);
    }

    /**
     * Sets the velocity of the specified entity to the specified value
     */
    @Override
    public void handleSetEntityMotion(ClientboundSetEntityMotionPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.getId());
        if (entity != null) {
            entity.lerpMotion(pPacket.getXa(), pPacket.getYa(), pPacket.getZa());
        }
    }

    /**
     * Invoked when the server registers new proximate objects in your watchlist or when objects in your watchlist have changed -> Registers any changes locally
     */
    @Override
    public void handleSetEntityData(ClientboundSetEntityDataPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.id());
        if (entity != null) {
            entity.getEntityData().assignValues(pPacket.packedItems());
        }
    }

    /**
     * Updates an entity's position and rotation as specified by the packet
     */
    @Override
    public void handleTeleportEntity(ClientboundTeleportEntityPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.getId());
        if (entity != null) {
            double d0 = pPacket.getX();
            double d1 = pPacket.getY();
            double d2 = pPacket.getZ();
            entity.syncPacketPositionCodec(d0, d1, d2);
            if (!entity.isControlledByLocalInstance()) {
                float f = (float)(pPacket.getyRot() * 360) / 256.0F;
                float f1 = (float)(pPacket.getxRot() * 360) / 256.0F;
                entity.lerpTo(d0, d1, d2, f, f1, 3);
                entity.setOnGround(pPacket.isOnGround());
            }
        }
    }

    @Override
    public void handleTickingState(ClientboundTickingStatePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (this.minecraft.level != null) {
            TickRateManager tickratemanager = this.minecraft.level.tickRateManager();
            tickratemanager.setTickRate(pPacket.tickRate());
            tickratemanager.setFrozen(pPacket.isFrozen());
        }
    }

    @Override
    public void handleTickingStep(ClientboundTickingStepPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (this.minecraft.level != null) {
            TickRateManager tickratemanager = this.minecraft.level.tickRateManager();
            tickratemanager.setFrozenTicksToRun(pPacket.tickSteps());
        }
    }

    /**
     * Updates which hotbar slot of the player is currently selected
     */
    @Override
    public void handleSetCarriedItem(ClientboundSetCarriedItemPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (Inventory.isHotbarSlot(pPacket.getSlot())) {
            this.minecraft.player.getInventory().selected = pPacket.getSlot();
        }
    }

    /**
     * Updates the specified entity's position by the specified relative momentum and absolute rotation. Note that subclassing of the packet allows for the specification of a subset of this data (e.g. only rel. position, abs. rotation or both).
     */
    @Override
    public void handleMoveEntity(ClientboundMoveEntityPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = pPacket.getEntity(this.level);
        if (entity != null) {
            if (!entity.isControlledByLocalInstance()) {
                if (pPacket.hasPosition()) {
                    VecDeltaCodec vecdeltacodec = entity.getPositionCodec();
                    Vec3 vec3 = vecdeltacodec.decode((long)pPacket.getXa(), (long)pPacket.getYa(), (long)pPacket.getZa());
                    vecdeltacodec.setBase(vec3);
                    float f = pPacket.hasRotation() ? (float)(pPacket.getyRot() * 360) / 256.0F : entity.lerpTargetYRot();
                    float f1 = pPacket.hasRotation() ? (float)(pPacket.getxRot() * 360) / 256.0F : entity.lerpTargetXRot();
                    entity.lerpTo(vec3.x(), vec3.y(), vec3.z(), f, f1, 3);
                } else if (pPacket.hasRotation()) {
                    float f2 = (float)(pPacket.getyRot() * 360) / 256.0F;
                    float f3 = (float)(pPacket.getxRot() * 360) / 256.0F;
                    entity.lerpTo(entity.lerpTargetX(), entity.lerpTargetY(), entity.lerpTargetZ(), f2, f3, 3);
                }

                entity.setOnGround(pPacket.isOnGround());
            }
        }
    }

    /**
     * Updates the direction in which the specified entity is looking, normally this head rotation is independent of the rotation of the entity itself
     */
    @Override
    public void handleRotateMob(ClientboundRotateHeadPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = pPacket.getEntity(this.level);
        if (entity != null) {
            float f = (float)(pPacket.getYHeadRot() * 360) / 256.0F;
            entity.lerpHeadTo(f, 3);
        }
    }

    @Override
    public void handleRemoveEntities(ClientboundRemoveEntitiesPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        pPacket.getEntityIds().forEach(p_205521_ -> this.level.removeEntity(p_205521_, Entity.RemovalReason.DISCARDED));
    }

    @Override
    public void handleMovePlayer(ClientboundPlayerPositionPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Player player = this.minecraft.player;
        Vec3 vec3 = player.getDeltaMovement();
        boolean flag = pPacket.getRelativeArguments().contains(RelativeMovement.X);
        boolean flag1 = pPacket.getRelativeArguments().contains(RelativeMovement.Y);
        boolean flag2 = pPacket.getRelativeArguments().contains(RelativeMovement.Z);
        double d0;
        double d1;
        if (flag) {
            d0 = vec3.x();
            d1 = player.getX() + pPacket.getX();
            player.xOld = player.xOld + pPacket.getX();
            player.xo = player.xo + pPacket.getX();
        } else {
            d0 = 0.0;
            d1 = pPacket.getX();
            player.xOld = d1;
            player.xo = d1;
        }

        double d2;
        double d3;
        if (flag1) {
            d2 = vec3.y();
            d3 = player.getY() + pPacket.getY();
            player.yOld = player.yOld + pPacket.getY();
            player.yo = player.yo + pPacket.getY();
        } else {
            d2 = 0.0;
            d3 = pPacket.getY();
            player.yOld = d3;
            player.yo = d3;
        }

        double d4;
        double d5;
        if (flag2) {
            d4 = vec3.z();
            d5 = player.getZ() + pPacket.getZ();
            player.zOld = player.zOld + pPacket.getZ();
            player.zo = player.zo + pPacket.getZ();
        } else {
            d4 = 0.0;
            d5 = pPacket.getZ();
            player.zOld = d5;
            player.zo = d5;
        }

        player.setPos(d1, d3, d5);
        player.setDeltaMovement(d0, d2, d4);
        float f = pPacket.getYRot();
        float f1 = pPacket.getXRot();
        if (pPacket.getRelativeArguments().contains(RelativeMovement.X_ROT)) {
            player.setXRot(player.getXRot() + f1);
            player.xRotO += f1;
        } else {
            player.setXRot(f1);
            player.xRotO = f1;
        }

        if (pPacket.getRelativeArguments().contains(RelativeMovement.Y_ROT)) {
            player.setYRot(player.getYRot() + f);
            player.yRotO += f;
        } else {
            player.setYRot(f);
            player.yRotO = f;
        }

        this.connection.send(new ServerboundAcceptTeleportationPacket(pPacket.getId()));
        this.connection.send(new ServerboundMovePlayerPacket.PosRot(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), false));
    }

    /**
     * Received from the servers PlayerManager if between 1 and 64 blocks in a chunk are changed. If only one block requires an update, the server sends S23PacketBlockChange and if 64 or more blocks are changed, the server sends S21PacketChunkData
     */
    @Override
    public void handleChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        pPacket.runUpdates((p_284633_, p_284634_) -> this.level.setServerVerifiedBlockState(p_284633_, p_284634_, 19));
    }

    @Override
    public void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        int i = pPacket.getX();
        int j = pPacket.getZ();
        this.updateLevelChunk(i, j, pPacket.getChunkData());
        ClientboundLightUpdatePacketData clientboundlightupdatepacketdata = pPacket.getLightData();
        this.level.queueLightUpdate(() -> {
            this.applyLightData(i, j, clientboundlightupdatepacketdata);
            LevelChunk levelchunk = this.level.getChunkSource().getChunk(i, j, false);
            if (levelchunk != null) {
                this.enableChunkLight(levelchunk, i, j);
            }
        });
    }

    @Override
    public void handleChunksBiomes(ClientboundChunksBiomesPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);

        for (ClientboundChunksBiomesPacket.ChunkBiomeData clientboundchunksbiomespacket$chunkbiomedata : pPacket.chunkBiomeData()) {
            this.level
                .getChunkSource()
                .replaceBiomes(
                    clientboundchunksbiomespacket$chunkbiomedata.pos().x,
                    clientboundchunksbiomespacket$chunkbiomedata.pos().z,
                    clientboundchunksbiomespacket$chunkbiomedata.getReadBuffer()
                );
        }

        for (ClientboundChunksBiomesPacket.ChunkBiomeData clientboundchunksbiomespacket$chunkbiomedata1 : pPacket.chunkBiomeData()) {
            this.level
                .onChunkLoaded(new ChunkPos(clientboundchunksbiomespacket$chunkbiomedata1.pos().x, clientboundchunksbiomespacket$chunkbiomedata1.pos().z));
        }

        for (ClientboundChunksBiomesPacket.ChunkBiomeData clientboundchunksbiomespacket$chunkbiomedata2 : pPacket.chunkBiomeData()) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    for (int k = this.level.getMinSection(); k < this.level.getMaxSection(); k++) {
                        this.minecraft
                            .levelRenderer
                            .setSectionDirty(
                                clientboundchunksbiomespacket$chunkbiomedata2.pos().x + i, k, clientboundchunksbiomespacket$chunkbiomedata2.pos().z + j
                            );
                    }
                }
            }
        }
    }

    private void updateLevelChunk(int pX, int pZ, ClientboundLevelChunkPacketData pData) {
        this.level
            .getChunkSource()
            .replaceWithPacketData(
                pX, pZ, pData.getReadBuffer(), pData.getHeightmaps(), pData.getBlockEntitiesTagsConsumer(pX, pZ)
            );
    }

    private void enableChunkLight(LevelChunk pChunk, int pX, int pZ) {
        LevelLightEngine levellightengine = this.level.getChunkSource().getLightEngine();
        LevelChunkSection[] alevelchunksection = pChunk.getSections();
        ChunkPos chunkpos = pChunk.getPos();

        for (int i = 0; i < alevelchunksection.length; i++) {
            LevelChunkSection levelchunksection = alevelchunksection[i];
            int j = this.level.getSectionYFromSectionIndex(i);
            levellightengine.updateSectionStatus(SectionPos.of(chunkpos, j), levelchunksection.hasOnlyAir());
            this.level.setSectionDirtyWithNeighbors(pX, j, pZ);
        }
    }

    @Override
    public void handleForgetLevelChunk(ClientboundForgetLevelChunkPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.level.getChunkSource().drop(pPacket.pos());
        this.queueLightRemoval(pPacket);
    }

    private void queueLightRemoval(ClientboundForgetLevelChunkPacket pPacket) {
        ChunkPos chunkpos = pPacket.pos();
        this.level.queueLightUpdate(() -> {
            LevelLightEngine levellightengine = this.level.getLightEngine();
            levellightengine.setLightEnabled(chunkpos, false);

            for (int i = levellightengine.getMinLightSection(); i < levellightengine.getMaxLightSection(); i++) {
                SectionPos sectionpos = SectionPos.of(chunkpos, i);
                levellightengine.queueSectionData(LightLayer.BLOCK, sectionpos, null);
                levellightengine.queueSectionData(LightLayer.SKY, sectionpos, null);
            }

            for (int j = this.level.getMinSection(); j < this.level.getMaxSection(); j++) {
                levellightengine.updateSectionStatus(SectionPos.of(chunkpos, j), true);
            }
        });
    }

    /**
     * Updates the block and metadata and generates a blockupdate (and notify the clients)
     */
    @Override
    public void handleBlockUpdate(ClientboundBlockUpdatePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.level.setServerVerifiedBlockState(pPacket.getPos(), pPacket.getBlockState(), 19);
    }

    @Override
    public void handleConfigurationStart(ClientboundStartConfigurationPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.getChatListener().clearQueue();
        this.sendChatAcknowledgement();
        ChatComponent.State chatcomponent$state = this.minecraft.gui.getChat().storeState();
        this.minecraft.clearClientLevel(new ServerReconfigScreen(RECONFIGURE_SCREEN_MESSAGE, this.connection));
        this.connection
            .setupInboundProtocol(
                ConfigurationProtocols.CLIENTBOUND,
                new ClientConfigurationPacketListenerImpl(
                    this.minecraft,
                    this.connection,
                    new CommonListenerCookie(
                        this.localGameProfile,
                        this.telemetryManager,
                        this.registryAccess,
                        this.enabledFeatures,
                        this.serverBrand,
                        this.serverData,
                        this.postDisconnectScreen,
                        this.serverCookies,
                        chatcomponent$state,
                        this.strictErrorHandling,
                        this.customReportDetails,
                        this.serverLinks,
                        this.connectionType
                    )
                )
            );
        this.send(ServerboundConfigurationAcknowledgedPacket.INSTANCE);
        this.connection.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
    }

    @Override
    public void handleTakeItemEntity(ClientboundTakeItemEntityPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.getItemId());
        LivingEntity livingentity = (LivingEntity)this.level.getEntity(pPacket.getPlayerId());
        if (livingentity == null) {
            livingentity = this.minecraft.player;
        }

        if (entity != null) {
            if (entity instanceof ExperienceOrb) {
                this.level
                    .playLocalSound(
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS,
                        0.1F,
                        (this.random.nextFloat() - this.random.nextFloat()) * 0.35F + 0.9F,
                        false
                    );
            } else {
                this.level
                    .playLocalSound(
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.ITEM_PICKUP,
                        SoundSource.PLAYERS,
                        0.2F,
                        (this.random.nextFloat() - this.random.nextFloat()) * 1.4F + 2.0F,
                        false
                    );
            }

            this.minecraft
                .particleEngine
                .add(new ItemPickupParticle(this.minecraft.getEntityRenderDispatcher(), this.minecraft.renderBuffers(), this.level, entity, livingentity));
            if (entity instanceof ItemEntity itementity) {
                ItemStack itemstack = itementity.getItem();
                if (!itemstack.isEmpty()) {
                    itemstack.shrink(pPacket.getAmount());
                }

                if (itemstack.isEmpty()) {
                    this.level.removeEntity(pPacket.getItemId(), Entity.RemovalReason.DISCARDED);
                }
            } else if (!(entity instanceof ExperienceOrb)) {
                this.level.removeEntity(pPacket.getItemId(), Entity.RemovalReason.DISCARDED);
            }
        }
    }

    @Override
    public void handleSystemChat(ClientboundSystemChatPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.getChatListener().handleSystemMessage(pPacket.content(), pPacket.overlay());
    }

    @Override
    public void handlePlayerChat(ClientboundPlayerChatPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Optional<SignedMessageBody> optional = pPacket.body().unpack(this.messageSignatureCache);
        if (optional.isEmpty()) {
            this.connection.disconnect(INVALID_PACKET);
        } else {
            this.messageSignatureCache.push(optional.get(), pPacket.signature());
            UUID uuid = pPacket.sender();
            PlayerInfo playerinfo = this.getPlayerInfo(uuid);
            if (playerinfo == null) {
                LOGGER.error("Received player chat packet for unknown player with ID: {}", uuid);
                this.minecraft.getChatListener().handleChatMessageError(uuid, pPacket.chatType());
            } else {
                RemoteChatSession remotechatsession = playerinfo.getChatSession();
                SignedMessageLink signedmessagelink;
                if (remotechatsession != null) {
                    signedmessagelink = new SignedMessageLink(pPacket.index(), uuid, remotechatsession.sessionId());
                } else {
                    signedmessagelink = SignedMessageLink.unsigned(uuid);
                }

                PlayerChatMessage playerchatmessage = new PlayerChatMessage(
                    signedmessagelink, pPacket.signature(), optional.get(), pPacket.unsignedContent(), pPacket.filterMask()
                );
                playerchatmessage = playerinfo.getMessageValidator().updateAndValidate(playerchatmessage);
                if (playerchatmessage != null) {
                    this.minecraft.getChatListener().handlePlayerChatMessage(playerchatmessage, playerinfo.getProfile(), pPacket.chatType());
                } else {
                    this.minecraft.getChatListener().handleChatMessageError(uuid, pPacket.chatType());
                }
            }
        }
    }

    @Override
    public void handleDisguisedChat(ClientboundDisguisedChatPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.getChatListener().handleDisguisedChatMessage(pPacket.message(), pPacket.chatType());
    }

    @Override
    public void handleDeleteChat(ClientboundDeleteChatPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Optional<MessageSignature> optional = pPacket.messageSignature().unpack(this.messageSignatureCache);
        if (optional.isEmpty()) {
            this.connection.disconnect(INVALID_PACKET);
        } else {
            this.lastSeenMessages.ignorePending(optional.get());
            if (!this.minecraft.getChatListener().removeFromDelayedMessageQueue(optional.get())) {
                this.minecraft.gui.getChat().deleteMessage(optional.get());
            }
        }
    }

    /**
     * Renders a specified animation: Waking up a player, a living entity swinging its currently held item, being hurt or receiving a critical hit by normal or magical means
     */
    @Override
    public void handleAnimate(ClientboundAnimatePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.getId());
        if (entity != null) {
            if (pPacket.getAction() == 0) {
                LivingEntity livingentity = (LivingEntity)entity;
                livingentity.swing(InteractionHand.MAIN_HAND);
            } else if (pPacket.getAction() == 3) {
                LivingEntity livingentity1 = (LivingEntity)entity;
                livingentity1.swing(InteractionHand.OFF_HAND);
            } else if (pPacket.getAction() == 2) {
                Player player = (Player)entity;
                player.stopSleepInBed(false, false);
            } else if (pPacket.getAction() == 4) {
                this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.CRIT);
            } else if (pPacket.getAction() == 5) {
                this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.ENCHANTED_HIT);
            }
        }
    }

    @Override
    public void handleHurtAnimation(ClientboundHurtAnimationPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.id());
        if (entity != null) {
            entity.animateHurt(pPacket.yaw());
        }
    }

    @Override
    public void handleSetTime(ClientboundSetTimePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.level.setGameTime(pPacket.getGameTime());
        this.minecraft.level.setDayTime(pPacket.getDayTime());
        this.telemetryManager.setTime(pPacket.getGameTime());
    }

    @Override
    public void handleSetSpawn(ClientboundSetDefaultSpawnPositionPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.level.setDefaultSpawnPos(pPacket.getPos(), pPacket.getAngle());
    }

    @Override
    public void handleSetEntityPassengersPacket(ClientboundSetPassengersPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.getVehicle());
        if (entity == null) {
            LOGGER.warn("Received passengers for unknown entity");
        } else {
            boolean flag = entity.hasIndirectPassenger(this.minecraft.player);
            entity.ejectPassengers();

            for (int i : pPacket.getPassengers()) {
                Entity entity1 = this.level.getEntity(i);
                if (entity1 != null) {
                    entity1.startRiding(entity, true);
                    if (entity1 == this.minecraft.player && !flag) {
                        if (entity instanceof Boat) {
                            this.minecraft.player.yRotO = entity.getYRot();
                            this.minecraft.player.setYRot(entity.getYRot());
                            this.minecraft.player.setYHeadRot(entity.getYRot());
                        }

                        Component component = Component.translatable("mount.onboard", this.minecraft.options.keyShift.getTranslatedKeyMessage());
                        this.minecraft.gui.setOverlayMessage(component, false);
                        this.minecraft.getNarrator().sayNow(component);
                    }
                }
            }
        }
    }

    @Override
    public void handleEntityLinkPacket(ClientboundSetEntityLinkPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (this.level.getEntity(pPacket.getSourceId()) instanceof Leashable leashable) {
            leashable.setDelayedLeashHolderId(pPacket.getDestId());
        }
    }

    private static ItemStack findTotem(Player pPlayer) {
        for (InteractionHand interactionhand : InteractionHand.values()) {
            ItemStack itemstack = pPlayer.getItemInHand(interactionhand);
            if (itemstack.is(Items.TOTEM_OF_UNDYING)) {
                return itemstack;
            }
        }

        return new ItemStack(Items.TOTEM_OF_UNDYING);
    }

    /**
     * Invokes the entities' handleUpdateHealth method which is implemented in LivingBase (hurt/death), MinecartMobSpawner (spawn delay), FireworkRocket & MinecartTNT (explosion), IronGolem (throwing, ...), Witch (spawn particles), Zombie (villager transformation), Animal (breeding mode particles), Horse (breeding/smoke particles), Sheep (...), Tameable (...), Villager (particles for breeding mode, angry and happy), Wolf (...)
     */
    @Override
    public void handleEntityEvent(ClientboundEntityEventPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = pPacket.getEntity(this.level);
        if (entity != null) {
            switch (pPacket.getEventId()) {
                case 21:
                    this.minecraft.getSoundManager().play(new GuardianAttackSoundInstance((Guardian)entity));
                    break;
                case 35:
                    int i = 40;
                    this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.TOTEM_OF_UNDYING, 30);
                    this.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TOTEM_USE, entity.getSoundSource(), 1.0F, 1.0F, false);
                    if (entity == this.minecraft.player) {
                        this.minecraft.gameRenderer.displayItemActivation(findTotem(this.minecraft.player));
                    }
                    break;
                case 63:
                    this.minecraft.getSoundManager().play(new SnifferSoundInstance((Sniffer)entity));
                    break;
                default:
                    entity.handleEntityEvent(pPacket.getEventId());
            }
        }
    }

    @Override
    public void handleDamageEvent(ClientboundDamageEventPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.entityId());
        if (entity != null) {
            entity.handleDamageEvent(pPacket.getSource(this.level));
        }
    }

    @Override
    public void handleSetHealth(ClientboundSetHealthPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.player.hurtTo(pPacket.getHealth());
        this.minecraft.player.getFoodData().setFoodLevel(pPacket.getFood());
        this.minecraft.player.getFoodData().setSaturation(pPacket.getSaturation());
    }

    @Override
    public void handleSetExperience(ClientboundSetExperiencePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.player.setExperienceValues(pPacket.getExperienceProgress(), pPacket.getTotalExperience(), pPacket.getExperienceLevel());
    }

    @Override
    public void handleRespawn(ClientboundRespawnPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        CommonPlayerSpawnInfo commonplayerspawninfo = pPacket.commonPlayerSpawnInfo();
        ResourceKey<Level> resourcekey = commonplayerspawninfo.dimension();
        Holder<DimensionType> holder = commonplayerspawninfo.dimensionType();
        LocalPlayer localplayer = this.minecraft.player;
        ResourceKey<Level> resourcekey1 = localplayer.level().dimension();
        boolean flag = resourcekey != resourcekey1;
        ReceivingLevelScreen.Reason receivinglevelscreen$reason = this.determineLevelLoadingReason(localplayer.isDeadOrDying(), resourcekey, resourcekey1);
        if (flag) {
            Map<MapId, MapItemSavedData> map = this.level.getAllMapData();
            boolean flag1 = commonplayerspawninfo.isDebug();
            boolean flag2 = commonplayerspawninfo.isFlat();
            ClientLevel.ClientLevelData clientlevel$clientleveldata = new ClientLevel.ClientLevelData(
                this.levelData.getDifficulty(), this.levelData.isHardcore(), flag2
            );
            this.levelData = clientlevel$clientleveldata;
            this.level = new ClientLevel(
                this,
                clientlevel$clientleveldata,
                resourcekey,
                holder,
                this.serverChunkRadius,
                this.serverSimulationDistance,
                this.minecraft::getProfiler,
                this.minecraft.levelRenderer,
                flag1,
                commonplayerspawninfo.seed()
            );
            this.level.addMapData(map);
            this.minecraft.setLevel(this.level, receivinglevelscreen$reason);
        }

        this.minecraft.cameraEntity = null;
        if (localplayer.hasContainerOpen()) {
            localplayer.closeContainer();
        }

        LocalPlayer localplayer1;
        if (pPacket.shouldKeep((byte)2)) {
            localplayer1 = this.minecraft
                .gameMode
                .createPlayer(this.level, localplayer.getStats(), localplayer.getRecipeBook(), localplayer.isShiftKeyDown(), localplayer.isSprinting());
        } else {
            localplayer1 = this.minecraft.gameMode.createPlayer(this.level, localplayer.getStats(), localplayer.getRecipeBook());
        }

        this.startWaitingForNewLevel(localplayer1, this.level, receivinglevelscreen$reason);
        localplayer1.setId(localplayer.getId());
        this.minecraft.player = localplayer1;
        if (flag) {
            this.minecraft.getMusicManager().stopPlaying();
        }

        this.minecraft.cameraEntity = localplayer1;
        if (pPacket.shouldKeep((byte)2)) {
            List<SynchedEntityData.DataValue<?>> list = localplayer.getEntityData().getNonDefaultValues();
            if (list != null) {
                localplayer1.getEntityData().assignValues(list);
            }
        }

        if (pPacket.shouldKeep((byte)1)) {
            localplayer1.getAttributes().assignAllValues(localplayer.getAttributes());
        } else {
            localplayer1.getAttributes().assignBaseValues(localplayer.getAttributes());
        }

        localplayer1.resetPos();
        net.neoforged.neoforge.client.ClientHooks.firePlayerRespawn(this.minecraft.gameMode, localplayer, localplayer1, localplayer1.connection.connection);
        this.level.addEntity(localplayer1);
        localplayer1.setYRot(-180.0F);
        localplayer1.input = new KeyboardInput(this.minecraft.options);
        this.minecraft.gameMode.adjustPlayer(localplayer1);
        localplayer1.setReducedDebugInfo(localplayer.isReducedDebugInfo());
        localplayer1.setShowDeathScreen(localplayer.shouldShowDeathScreen());
        localplayer1.setLastDeathLocation(commonplayerspawninfo.lastDeathLocation());
        localplayer1.setPortalCooldown(commonplayerspawninfo.portalCooldown());
        localplayer1.spinningEffectIntensity = localplayer.spinningEffectIntensity;
        localplayer1.oSpinningEffectIntensity = localplayer.oSpinningEffectIntensity;
        if (this.minecraft.screen instanceof DeathScreen || this.minecraft.screen instanceof DeathScreen.TitleConfirmScreen) {
            this.minecraft.setScreen(null);
        }

        this.minecraft.gameMode.setLocalMode(commonplayerspawninfo.gameType(), commonplayerspawninfo.previousGameType());
    }

    private ReceivingLevelScreen.Reason determineLevelLoadingReason(boolean pDying, ResourceKey<Level> pSpawnDimension, ResourceKey<Level> pCurrentDimension) {
        ReceivingLevelScreen.Reason receivinglevelscreen$reason = ReceivingLevelScreen.Reason.OTHER;
        if (!pDying) {
            if (pSpawnDimension == Level.NETHER || pCurrentDimension == Level.NETHER) {
                receivinglevelscreen$reason = ReceivingLevelScreen.Reason.NETHER_PORTAL;
            } else if (pSpawnDimension == Level.END || pCurrentDimension == Level.END) {
                receivinglevelscreen$reason = ReceivingLevelScreen.Reason.END_PORTAL;
            }
        }

        return receivinglevelscreen$reason;
    }

    /**
     * Initiates a new explosion (sound, particles, drop spawn) for the affected blocks indicated by the packet.
     */
    @Override
    public void handleExplosion(ClientboundExplodePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Explosion explosion = new Explosion(
            this.minecraft.level,
            null,
            pPacket.getX(),
            pPacket.getY(),
            pPacket.getZ(),
            pPacket.getPower(),
            pPacket.getToBlow(),
            pPacket.getBlockInteraction(),
            pPacket.getSmallExplosionParticles(),
            pPacket.getLargeExplosionParticles(),
            pPacket.getExplosionSound()
        );
        explosion.finalizeExplosion(true);
        this.minecraft
            .player
            .setDeltaMovement(
                this.minecraft
                    .player
                    .getDeltaMovement()
                    .add((double)pPacket.getKnockbackX(), (double)pPacket.getKnockbackY(), (double)pPacket.getKnockbackZ())
            );
    }

    @Override
    public void handleHorseScreenOpen(ClientboundHorseScreenOpenPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (this.level.getEntity(pPacket.getEntityId()) instanceof AbstractHorse abstracthorse) {
            LocalPlayer localplayer = this.minecraft.player;
            int i = pPacket.getInventoryColumns();
            SimpleContainer simplecontainer = new SimpleContainer(AbstractHorse.getInventorySize(i));
            HorseInventoryMenu horseinventorymenu = new HorseInventoryMenu(
                pPacket.getContainerId(), localplayer.getInventory(), simplecontainer, abstracthorse, i
            );
            localplayer.containerMenu = horseinventorymenu;
            this.minecraft.setScreen(new HorseInventoryScreen(horseinventorymenu, localplayer.getInventory(), abstracthorse, i));
        }
    }

    @Override
    public void handleOpenScreen(ClientboundOpenScreenPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        MenuScreens.create(pPacket.getType(), this.minecraft, pPacket.getContainerId(), pPacket.getTitle());
    }

    /**
     * Handles picking up an ItemStack or dropping one in your inventory or an open (non-creative) container
     */
    @Override
    public void handleContainerSetSlot(ClientboundContainerSetSlotPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Player player = this.minecraft.player;
        ItemStack itemstack = pPacket.getItem();
        int i = pPacket.getSlot();
        this.minecraft.getTutorial().onGetItem(itemstack);
        if (pPacket.getContainerId() == -1) {
            if (!(this.minecraft.screen instanceof CreativeModeInventoryScreen)) {
                player.containerMenu.setCarried(itemstack);
            }
        } else if (pPacket.getContainerId() == -2) {
            player.getInventory().setItem(i, itemstack);
        } else {
            boolean flag = false;
            if (this.minecraft.screen instanceof CreativeModeInventoryScreen creativemodeinventoryscreen) {
                flag = !creativemodeinventoryscreen.isInventoryOpen();
            }

            if (pPacket.getContainerId() == 0 && InventoryMenu.isHotbarSlot(i)) {
                if (!itemstack.isEmpty()) {
                    ItemStack itemstack1 = player.inventoryMenu.getSlot(i).getItem();
                    if (itemstack1.isEmpty() || itemstack1.getCount() < itemstack.getCount()) {
                        itemstack.setPopTime(5);
                    }
                }

                player.inventoryMenu.setItem(i, pPacket.getStateId(), itemstack);
            } else if (pPacket.getContainerId() == player.containerMenu.containerId && (pPacket.getContainerId() != 0 || !flag)) {
                player.containerMenu.setItem(i, pPacket.getStateId(), itemstack);
            }
        }
    }

    /**
     * Handles the placement of a specified ItemStack in a specified container/inventory slot
     */
    @Override
    public void handleContainerContent(ClientboundContainerSetContentPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Player player = this.minecraft.player;
        if (pPacket.getContainerId() == 0) {
            player.inventoryMenu.initializeContents(pPacket.getStateId(), pPacket.getItems(), pPacket.getCarriedItem());
        } else if (pPacket.getContainerId() == player.containerMenu.containerId) {
            player.containerMenu.initializeContents(pPacket.getStateId(), pPacket.getItems(), pPacket.getCarriedItem());
        }
    }

    /**
     * Creates a sign in the specified location if it didn't exist and opens the GUI to edit its text
     */
    @Override
    public void handleOpenSignEditor(ClientboundOpenSignEditorPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        BlockPos blockpos = pPacket.getPos();
        if (this.level.getBlockEntity(blockpos) instanceof SignBlockEntity signblockentity) {
            this.minecraft.player.openTextEdit(signblockentity, pPacket.isFrontText());
        } else {
            BlockState blockstate = this.level.getBlockState(blockpos);
            SignBlockEntity signblockentity1 = new SignBlockEntity(blockpos, blockstate);
            signblockentity1.setLevel(this.level);
            this.minecraft.player.openTextEdit(signblockentity1, pPacket.isFrontText());
        }
    }

    /**
     * Updates the NBTTagCompound metadata of instances of the following entitytypes: Mob spawners, command blocks, beacons, skulls, flowerpot
     */
    @Override
    public void handleBlockEntityData(ClientboundBlockEntityDataPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        BlockPos blockpos = pPacket.getPos();
        this.minecraft.level.getBlockEntity(blockpos, pPacket.getType()).ifPresent(p_337415_ -> {
            p_337415_.onDataPacket(connection, pPacket, this.registryAccess);

            if (p_337415_ instanceof CommandBlockEntity && this.minecraft.screen instanceof CommandBlockEditScreen) {
                ((CommandBlockEditScreen)this.minecraft.screen).updateGui();
            }
        });
    }

    /**
     * Sets the progressbar of the opened window to the specified value
     */
    @Override
    public void handleContainerSetData(ClientboundContainerSetDataPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Player player = this.minecraft.player;
        if (player.containerMenu != null && player.containerMenu.containerId == pPacket.getContainerId()) {
            player.containerMenu.setData(pPacket.getId(), pPacket.getValue());
        }
    }

    @Override
    public void handleSetEquipment(ClientboundSetEquipmentPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (this.level.getEntity(pPacket.getEntity()) instanceof LivingEntity livingentity) {
            pPacket.getSlots().forEach(p_323056_ -> livingentity.setItemSlot(p_323056_.getFirst(), p_323056_.getSecond()));
        }
    }

    /**
     * Resets the ItemStack held in hand and closes the window that is opened
     */
    @Override
    public void handleContainerClose(ClientboundContainerClosePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.player.clientSideCloseContainer();
    }

    /**
     * Triggers Block.onBlockEventReceived, which is implemented in BlockPistonBase for extension/retraction, BlockNote for setting the instrument (including audiovisual feedback) and in BlockContainer to set the number of players accessing a (Ender)Chest
     */
    @Override
    public void handleBlockEvent(ClientboundBlockEventPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.level.blockEvent(pPacket.getPos(), pPacket.getBlock(), pPacket.getB0(), pPacket.getB1());
    }

    /**
     * Updates all registered IWorldAccess instances with destroyBlockInWorldPartially
     */
    @Override
    public void handleBlockDestruction(ClientboundBlockDestructionPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.level.destroyBlockProgress(pPacket.getId(), pPacket.getPos(), pPacket.getProgress());
    }

    @Override
    public void handleGameEvent(ClientboundGameEventPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Player player = this.minecraft.player;
        ClientboundGameEventPacket.Type clientboundgameeventpacket$type = pPacket.getEvent();
        float f = pPacket.getParam();
        int i = Mth.floor(f + 0.5F);
        if (clientboundgameeventpacket$type == ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE) {
            player.displayClientMessage(Component.translatable("block.minecraft.spawn.not_valid"), false);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.START_RAINING) {
            this.level.getLevelData().setRaining(true);
            this.level.setRainLevel(0.0F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.STOP_RAINING) {
            this.level.getLevelData().setRaining(false);
            this.level.setRainLevel(1.0F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.CHANGE_GAME_MODE) {
            this.minecraft.gameMode.setLocalMode(GameType.byId(i));
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.WIN_GAME) {
            this.minecraft.setScreen(new WinScreen(true, () -> {
                this.minecraft.player.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                this.minecraft.setScreen(null);
            }));
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.DEMO_EVENT) {
            Options options = this.minecraft.options;
            if (f == 0.0F) {
                this.minecraft.setScreen(new DemoIntroScreen());
            } else if (f == 101.0F) {
                this.minecraft
                    .gui
                    .getChat()
                    .addMessage(
                        Component.translatable(
                            "demo.help.movement",
                            options.keyUp.getTranslatedKeyMessage(),
                            options.keyLeft.getTranslatedKeyMessage(),
                            options.keyDown.getTranslatedKeyMessage(),
                            options.keyRight.getTranslatedKeyMessage()
                        )
                    );
            } else if (f == 102.0F) {
                this.minecraft.gui.getChat().addMessage(Component.translatable("demo.help.jump", options.keyJump.getTranslatedKeyMessage()));
            } else if (f == 103.0F) {
                this.minecraft.gui.getChat().addMessage(Component.translatable("demo.help.inventory", options.keyInventory.getTranslatedKeyMessage()));
            } else if (f == 104.0F) {
                this.minecraft.gui.getChat().addMessage(Component.translatable("demo.day.6", options.keyScreenshot.getTranslatedKeyMessage()));
            }
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.ARROW_HIT_PLAYER) {
            this.level.playSound(player, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.18F, 0.45F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {
            this.level.setRainLevel(f);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
            this.level.setThunderLevel(f);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.PUFFER_FISH_STING) {
            this.level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.PUFFER_FISH_STING, SoundSource.NEUTRAL, 1.0F, 1.0F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT) {
            this.level.addParticle(ParticleTypes.ELDER_GUARDIAN, player.getX(), player.getY(), player.getZ(), 0.0, 0.0, 0.0);
            if (i == 1) {
                this.level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 1.0F);
            }
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.IMMEDIATE_RESPAWN) {
            this.minecraft.player.setShowDeathScreen(f == 0.0F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.LIMITED_CRAFTING) {
            this.minecraft.player.setDoLimitedCrafting(f == 1.0F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START && this.levelLoadStatusManager != null) {
            this.levelLoadStatusManager.loadingPacketsReceived();
        }
    }

    private void startWaitingForNewLevel(LocalPlayer pPlayer, ClientLevel pLevel, ReceivingLevelScreen.Reason pReason) {
        this.levelLoadStatusManager = new LevelLoadStatusManager(pPlayer, pLevel, this.minecraft.levelRenderer);
        this.minecraft.setScreen(new ReceivingLevelScreen(this.levelLoadStatusManager::levelReady, pReason));
    }

    /**
     * Updates the worlds MapStorage with the specified MapData for the specified map-identifier and invokes a MapItemRenderer for it
     */
    @Override
    public void handleMapItemData(ClientboundMapItemDataPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        MapRenderer maprenderer = this.minecraft.gameRenderer.getMapRenderer();
        MapId mapid = pPacket.mapId();
        MapItemSavedData mapitemsaveddata = this.minecraft.level.getMapData(mapid);
        if (mapitemsaveddata == null) {
            mapitemsaveddata = MapItemSavedData.createForClient(pPacket.scale(), pPacket.locked(), this.minecraft.level.dimension());
            this.minecraft.level.overrideMapData(mapid, mapitemsaveddata);
        }

        pPacket.applyToMap(mapitemsaveddata);
        maprenderer.update(mapid, mapitemsaveddata);
    }

    @Override
    public void handleLevelEvent(ClientboundLevelEventPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (pPacket.isGlobalEvent()) {
            this.minecraft.level.globalLevelEvent(pPacket.getType(), pPacket.getPos(), pPacket.getData());
        } else {
            this.minecraft.level.levelEvent(pPacket.getType(), pPacket.getPos(), pPacket.getData());
        }
    }

    @Override
    public void handleUpdateAdvancementsPacket(ClientboundUpdateAdvancementsPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.advancements.update(pPacket);
    }

    @Override
    public void handleSelectAdvancementsTab(ClientboundSelectAdvancementsTabPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        ResourceLocation resourcelocation = pPacket.getTab();
        if (resourcelocation == null) {
            this.advancements.setSelectedTab(null, false);
        } else {
            AdvancementHolder advancementholder = this.advancements.get(resourcelocation);
            this.advancements.setSelectedTab(advancementholder, false);
        }
    }

    @Override
    public void handleCommands(ClientboundCommandsPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        var context = CommandBuildContext.simple(this.registryAccess, this.enabledFeatures);
        this.commands = new CommandDispatcher<>(pPacket.getRoot(context));
        this.commands = net.neoforged.neoforge.client.ClientCommandHandler.mergeServerCommands(this.commands, context);
    }

    @Override
    public void handleStopSoundEvent(ClientboundStopSoundPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.getSoundManager().stop(pPacket.getName(), pPacket.getSource());
    }

    /**
     * This method is only called for manual tab-completion (the {@link net.minecraft.commands.synchronization.SuggestionProviders#ASK_SERVER minecraft:ask_server} suggestion provider).
     */
    @Override
    public void handleCommandSuggestions(ClientboundCommandSuggestionsPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.suggestionsProvider.completeCustomSuggestions(pPacket.id(), pPacket.toSuggestions());
    }

    @Override
    public void handleUpdateRecipes(ClientboundUpdateRecipesPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.recipeManager.replaceRecipes(pPacket.getRecipes());
        ClientRecipeBook clientrecipebook = this.minecraft.player.getRecipeBook();
        clientrecipebook.setupCollections(this.recipeManager.getOrderedRecipes(), this.minecraft.level.registryAccess());
        this.searchTrees.updateRecipes(clientrecipebook, this.registryAccess);
        net.neoforged.neoforge.client.ClientHooks.onRecipesUpdated(this.recipeManager);
    }

    @Override
    public void handleLookAt(ClientboundPlayerLookAtPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Vec3 vec3 = pPacket.getPosition(this.level);
        if (vec3 != null) {
            this.minecraft.player.lookAt(pPacket.getFromAnchor(), vec3);
        }
    }

    @Override
    public void handleTagQueryPacket(ClientboundTagQueryPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (!this.debugQueryHandler.handleResponse(pPacket.getTransactionId(), pPacket.getTag())) {
            LOGGER.debug("Got unhandled response to tag query {}", pPacket.getTransactionId());
        }
    }

    /**
     * Updates the players statistics or achievements
     */
    @Override
    public void handleAwardStats(ClientboundAwardStatsPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);

        for (Entry<Stat<?>> entry : pPacket.stats().object2IntEntrySet()) {
            Stat<?> stat = entry.getKey();
            int i = entry.getIntValue();
            this.minecraft.player.getStats().setValue(this.minecraft.player, stat, i);
        }

        if (this.minecraft.screen instanceof StatsScreen statsscreen) {
            statsscreen.onStatsUpdated();
        }
    }

    @Override
    public void handleAddOrRemoveRecipes(ClientboundRecipePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        ClientRecipeBook clientrecipebook = this.minecraft.player.getRecipeBook();
        clientrecipebook.setBookSettings(pPacket.getBookSettings());
        ClientboundRecipePacket.State clientboundrecipepacket$state = pPacket.getState();
        switch (clientboundrecipepacket$state) {
            case REMOVE:
                for (ResourceLocation resourcelocation3 : pPacket.getRecipes()) {
                    this.recipeManager.byKey(resourcelocation3).ifPresent(clientrecipebook::remove);
                }
                break;
            case INIT:
                for (ResourceLocation resourcelocation1 : pPacket.getRecipes()) {
                    this.recipeManager.byKey(resourcelocation1).ifPresent(clientrecipebook::add);
                }

                for (ResourceLocation resourcelocation2 : pPacket.getHighlights()) {
                    this.recipeManager.byKey(resourcelocation2).ifPresent(clientrecipebook::addHighlight);
                }
                break;
            case ADD:
                for (ResourceLocation resourcelocation : pPacket.getRecipes()) {
                    this.recipeManager.byKey(resourcelocation).ifPresent(p_300677_ -> {
                        clientrecipebook.add((RecipeHolder<?>)p_300677_);
                        clientrecipebook.addHighlight((RecipeHolder<?>)p_300677_);
                        if (p_300677_.value().showNotification()) {
                            RecipeToast.addOrUpdate(this.minecraft.getToasts(), (RecipeHolder<?>)p_300677_);
                        }
                    });
                }
        }

        clientrecipebook.getCollections().forEach(p_205540_ -> p_205540_.updateKnownRecipes(clientrecipebook));
        if (this.minecraft.screen instanceof RecipeUpdateListener) {
            ((RecipeUpdateListener)this.minecraft.screen).recipesUpdated();
        }
    }

    @Override
    public void handleUpdateMobEffect(ClientboundUpdateMobEffectPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.getEntityId());
        if (entity instanceof LivingEntity) {
            Holder<MobEffect> holder = pPacket.getEffect();
            MobEffectInstance mobeffectinstance = new MobEffectInstance(
                holder,
                pPacket.getEffectDurationTicks(),
                pPacket.getEffectAmplifier(),
                pPacket.isEffectAmbient(),
                pPacket.isEffectVisible(),
                pPacket.effectShowsIcon(),
                null
            );
            if (!pPacket.shouldBlend()) {
                mobeffectinstance.skipBlending();
            }

            ((LivingEntity)entity).forceAddEffect(mobeffectinstance, null);
        }
    }

    @Override
    public void handleUpdateTags(ClientboundUpdateTagsPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        TagCollector tagcollector = new TagCollector();
        pPacket.getTags().forEach(tagcollector::append);
        tagcollector.updateTags(this.registryAccess, this.connection.isMemoryConnection());
        CreativeModeTabs.allTabs().stream().filter(net.minecraft.world.item.CreativeModeTab::hasSearchBar).forEach(tab -> {
            List<ItemStack> list = List.copyOf(tab.getDisplayItems());
            this.searchTrees.updateCreativeTags(list, net.neoforged.neoforge.client.CreativeModeTabSearchRegistry.getTagSearchKey(tab));
        });
    }

    @Override
    public void handlePlayerCombatEnd(ClientboundPlayerCombatEndPacket pPacket) {
    }

    @Override
    public void handlePlayerCombatEnter(ClientboundPlayerCombatEnterPacket pPacket) {
    }

    @Override
    public void handlePlayerCombatKill(ClientboundPlayerCombatKillPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.playerId());
        if (entity == this.minecraft.player) {
            if (this.minecraft.player.shouldShowDeathScreen()) {
                this.minecraft.setScreen(new DeathScreen(pPacket.message(), this.level.getLevelData().isHardcore()));
            } else {
                this.minecraft.player.respawn();
            }
        }
    }

    @Override
    public void handleChangeDifficulty(ClientboundChangeDifficultyPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.levelData.setDifficulty(pPacket.getDifficulty());
        this.levelData.setDifficultyLocked(pPacket.isLocked());
    }

    @Override
    public void handleSetCamera(ClientboundSetCameraPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = pPacket.getEntity(this.level);
        if (entity != null) {
            this.minecraft.setCameraEntity(entity);
        }
    }

    @Override
    public void handleInitializeBorder(ClientboundInitializeBorderPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        WorldBorder worldborder = this.level.getWorldBorder();
        worldborder.setCenter(pPacket.getNewCenterX(), pPacket.getNewCenterZ());
        long i = pPacket.getLerpTime();
        if (i > 0L) {
            worldborder.lerpSizeBetween(pPacket.getOldSize(), pPacket.getNewSize(), i);
        } else {
            worldborder.setSize(pPacket.getNewSize());
        }

        worldborder.setAbsoluteMaxSize(pPacket.getNewAbsoluteMaxSize());
        worldborder.setWarningBlocks(pPacket.getWarningBlocks());
        worldborder.setWarningTime(pPacket.getWarningTime());
    }

    @Override
    public void handleSetBorderCenter(ClientboundSetBorderCenterPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.level.getWorldBorder().setCenter(pPacket.getNewCenterX(), pPacket.getNewCenterZ());
    }

    @Override
    public void handleSetBorderLerpSize(ClientboundSetBorderLerpSizePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.level.getWorldBorder().lerpSizeBetween(pPacket.getOldSize(), pPacket.getNewSize(), pPacket.getLerpTime());
    }

    @Override
    public void handleSetBorderSize(ClientboundSetBorderSizePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.level.getWorldBorder().setSize(pPacket.getSize());
    }

    @Override
    public void handleSetBorderWarningDistance(ClientboundSetBorderWarningDistancePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.level.getWorldBorder().setWarningBlocks(pPacket.getWarningBlocks());
    }

    @Override
    public void handleSetBorderWarningDelay(ClientboundSetBorderWarningDelayPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.level.getWorldBorder().setWarningTime(pPacket.getWarningDelay());
    }

    @Override
    public void handleTitlesClear(ClientboundClearTitlesPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.gui.clear();
        if (pPacket.shouldResetTimes()) {
            this.minecraft.gui.resetTitleTimes();
        }
    }

    @Override
    public void handleServerData(ClientboundServerDataPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (this.serverData != null) {
            this.serverData.motd = pPacket.motd();
            pPacket.iconBytes().map(ServerData::validateIcon).ifPresent(this.serverData::setIconBytes);
            ServerList.saveSingleServer(this.serverData);
        }
    }

    @Override
    public void handleCustomChatCompletions(ClientboundCustomChatCompletionsPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.suggestionsProvider.modifyCustomCompletions(pPacket.action(), pPacket.entries());
    }

    @Override
    public void setActionBarText(ClientboundSetActionBarTextPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.gui.setOverlayMessage(pPacket.text(), false);
    }

    @Override
    public void setTitleText(ClientboundSetTitleTextPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.gui.setTitle(pPacket.text());
    }

    @Override
    public void setSubtitleText(ClientboundSetSubtitleTextPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.gui.setSubtitle(pPacket.text());
    }

    @Override
    public void setTitlesAnimation(ClientboundSetTitlesAnimationPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.gui.setTimes(pPacket.getFadeIn(), pPacket.getStay(), pPacket.getFadeOut());
    }

    @Override
    public void handleTabListCustomisation(ClientboundTabListPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.gui.getTabList().setHeader(pPacket.header().getString().isEmpty() ? null : pPacket.header());
        this.minecraft.gui.getTabList().setFooter(pPacket.footer().getString().isEmpty() ? null : pPacket.footer());
    }

    @Override
    public void handleRemoveMobEffect(ClientboundRemoveMobEffectPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (pPacket.getEntity(this.level) instanceof LivingEntity livingentity) {
            livingentity.removeEffectNoUpdate(pPacket.effect());
        }
    }

    @Override
    public void handlePlayerInfoRemove(ClientboundPlayerInfoRemovePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);

        for (UUID uuid : pPacket.profileIds()) {
            this.minecraft.getPlayerSocialManager().removePlayer(uuid);
            PlayerInfo playerinfo = this.playerInfoMap.remove(uuid);
            if (playerinfo != null) {
                this.listedPlayers.remove(playerinfo);
            }
        }
    }

    @Override
    public void handlePlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);

        for (ClientboundPlayerInfoUpdatePacket.Entry clientboundplayerinfoupdatepacket$entry : pPacket.newEntries()) {
            PlayerInfo playerinfo = new PlayerInfo(Objects.requireNonNull(clientboundplayerinfoupdatepacket$entry.profile()), this.enforcesSecureChat());
            if (this.playerInfoMap.putIfAbsent(clientboundplayerinfoupdatepacket$entry.profileId(), playerinfo) == null) {
                this.minecraft.getPlayerSocialManager().addPlayer(playerinfo);
            }
        }

        for (ClientboundPlayerInfoUpdatePacket.Entry clientboundplayerinfoupdatepacket$entry1 : pPacket.entries()) {
            PlayerInfo playerinfo1 = this.playerInfoMap.get(clientboundplayerinfoupdatepacket$entry1.profileId());
            if (playerinfo1 == null) {
                LOGGER.warn("Ignoring player info update for unknown player {} ({})", clientboundplayerinfoupdatepacket$entry1.profileId(), pPacket.actions());
            } else {
                for (ClientboundPlayerInfoUpdatePacket.Action clientboundplayerinfoupdatepacket$action : pPacket.actions()) {
                    this.applyPlayerInfoUpdate(clientboundplayerinfoupdatepacket$action, clientboundplayerinfoupdatepacket$entry1, playerinfo1);
                }
            }
        }
    }

    private void applyPlayerInfoUpdate(
        ClientboundPlayerInfoUpdatePacket.Action pAction, ClientboundPlayerInfoUpdatePacket.Entry pEntry, PlayerInfo pPlayerInfo
    ) {
        switch (pAction) {
            case INITIALIZE_CHAT:
                this.initializeChatSession(pEntry, pPlayerInfo);
                break;
            case UPDATE_GAME_MODE:
                if (pPlayerInfo.getGameMode() != pEntry.gameMode()
                    && this.minecraft.player != null
                    && this.minecraft.player.getUUID().equals(pEntry.profileId())) {
                    this.minecraft.player.onGameModeChanged(pEntry.gameMode());
                }

                pPlayerInfo.setGameMode(pEntry.gameMode());
                break;
            case UPDATE_LISTED:
                if (pEntry.listed()) {
                    this.listedPlayers.add(pPlayerInfo);
                } else {
                    this.listedPlayers.remove(pPlayerInfo);
                }
                break;
            case UPDATE_LATENCY:
                pPlayerInfo.setLatency(pEntry.latency());
                break;
            case UPDATE_DISPLAY_NAME:
                pPlayerInfo.setTabListDisplayName(pEntry.displayName());
        }
    }

    private void initializeChatSession(ClientboundPlayerInfoUpdatePacket.Entry pEntry, PlayerInfo pPlayerInfo) {
        GameProfile gameprofile = pPlayerInfo.getProfile();
        SignatureValidator signaturevalidator = this.minecraft.getProfileKeySignatureValidator();
        if (signaturevalidator == null) {
            LOGGER.warn("Ignoring chat session from {} due to missing Services public key", gameprofile.getName());
            pPlayerInfo.clearChatSession(this.enforcesSecureChat());
        } else {
            RemoteChatSession.Data remotechatsession$data = pEntry.chatSession();
            if (remotechatsession$data != null) {
                try {
                    RemoteChatSession remotechatsession = remotechatsession$data.validate(gameprofile, signaturevalidator);
                    pPlayerInfo.setChatSession(remotechatsession);
                } catch (ProfilePublicKey.ValidationException profilepublickey$validationexception) {
                    LOGGER.error("Failed to validate profile key for player: '{}'", gameprofile.getName(), profilepublickey$validationexception);
                    pPlayerInfo.clearChatSession(this.enforcesSecureChat());
                }
            } else {
                pPlayerInfo.clearChatSession(this.enforcesSecureChat());
            }
        }
    }

    private boolean enforcesSecureChat() {
        return this.minecraft.canValidateProfileKeys() && this.serverEnforcesSecureChat;
    }

    @Override
    public void handlePlayerAbilities(ClientboundPlayerAbilitiesPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Player player = this.minecraft.player;
        player.getAbilities().flying = pPacket.isFlying();
        player.getAbilities().instabuild = pPacket.canInstabuild();
        player.getAbilities().invulnerable = pPacket.isInvulnerable();
        player.getAbilities().mayfly = pPacket.canFly();
        player.getAbilities().setFlyingSpeed(pPacket.getFlyingSpeed());
        player.getAbilities().setWalkingSpeed(pPacket.getWalkingSpeed());
    }

    @Override
    public void handleSoundEvent(ClientboundSoundPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft
            .level
            .playSeededSound(
                this.minecraft.player,
                pPacket.getX(),
                pPacket.getY(),
                pPacket.getZ(),
                pPacket.getSound(),
                pPacket.getSource(),
                pPacket.getVolume(),
                pPacket.getPitch(),
                pPacket.getSeed()
            );
    }

    @Override
    public void handleSoundEntityEvent(ClientboundSoundEntityPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.getId());
        if (entity != null) {
            this.minecraft
                .level
                .playSeededSound(
                    this.minecraft.player,
                    entity,
                    pPacket.getSound(),
                    pPacket.getSource(),
                    pPacket.getVolume(),
                    pPacket.getPitch(),
                    pPacket.getSeed()
                );
        }
    }

    @Override
    public void handleBossUpdate(ClientboundBossEventPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.gui.getBossOverlay().update(pPacket);
    }

    @Override
    public void handleItemCooldown(ClientboundCooldownPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (pPacket.duration() == 0) {
            this.minecraft.player.getCooldowns().removeCooldown(pPacket.item());
        } else {
            this.minecraft.player.getCooldowns().addCooldown(pPacket.item(), pPacket.duration());
        }
    }

    @Override
    public void handleMoveVehicle(ClientboundMoveVehiclePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.minecraft.player.getRootVehicle();
        if (entity != this.minecraft.player && entity.isControlledByLocalInstance()) {
            entity.absMoveTo(pPacket.getX(), pPacket.getY(), pPacket.getZ(), pPacket.getYRot(), pPacket.getXRot());
            this.connection.send(new ServerboundMoveVehiclePacket(entity));
        }
    }

    @Override
    public void handleOpenBook(ClientboundOpenBookPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        ItemStack itemstack = this.minecraft.player.getItemInHand(pPacket.getHand());
        BookViewScreen.BookAccess bookviewscreen$bookaccess = BookViewScreen.BookAccess.fromItem(itemstack);
        if (bookviewscreen$bookaccess != null) {
            this.minecraft.setScreen(new BookViewScreen(bookviewscreen$bookaccess));
        }
    }

    @Override
    public void handleCustomPayload(CustomPacketPayload pPayload) {
        if (pPayload instanceof PathfindingDebugPayload pathfindingdebugpayload) {
            this.minecraft
                .debugRenderer
                .pathfindingRenderer
                .addPath(pathfindingdebugpayload.entityId(), pathfindingdebugpayload.path(), pathfindingdebugpayload.maxNodeDistance());
        } else if (pPayload instanceof NeighborUpdatesDebugPayload neighborupdatesdebugpayload) {
            ((NeighborsUpdateRenderer)this.minecraft.debugRenderer.neighborsUpdateRenderer)
                .addUpdate(neighborupdatesdebugpayload.time(), neighborupdatesdebugpayload.pos());
        } else if (pPayload instanceof StructuresDebugPayload structuresdebugpayload) {
            this.minecraft
                .debugRenderer
                .structureRenderer
                .addBoundingBox(structuresdebugpayload.mainBB(), structuresdebugpayload.pieces(), structuresdebugpayload.dimension());
        } else if (pPayload instanceof WorldGenAttemptDebugPayload worldgenattemptdebugpayload) {
            ((WorldGenAttemptRenderer)this.minecraft.debugRenderer.worldGenAttemptRenderer)
                .addPos(
                    worldgenattemptdebugpayload.pos(),
                    worldgenattemptdebugpayload.scale(),
                    worldgenattemptdebugpayload.red(),
                    worldgenattemptdebugpayload.green(),
                    worldgenattemptdebugpayload.blue(),
                    worldgenattemptdebugpayload.alpha()
                );
        } else if (pPayload instanceof PoiTicketCountDebugPayload poiticketcountdebugpayload) {
            this.minecraft.debugRenderer.brainDebugRenderer.setFreeTicketCount(poiticketcountdebugpayload.pos(), poiticketcountdebugpayload.freeTicketCount());
        } else if (pPayload instanceof PoiAddedDebugPayload poiaddeddebugpayload) {
            BrainDebugRenderer.PoiInfo braindebugrenderer$poiinfo = new BrainDebugRenderer.PoiInfo(
                poiaddeddebugpayload.pos(), poiaddeddebugpayload.poiType(), poiaddeddebugpayload.freeTicketCount()
            );
            this.minecraft.debugRenderer.brainDebugRenderer.addPoi(braindebugrenderer$poiinfo);
        } else if (pPayload instanceof PoiRemovedDebugPayload poiremoveddebugpayload) {
            this.minecraft.debugRenderer.brainDebugRenderer.removePoi(poiremoveddebugpayload.pos());
        } else if (pPayload instanceof VillageSectionsDebugPayload villagesectionsdebugpayload) {
            VillageSectionsDebugRenderer villagesectionsdebugrenderer = this.minecraft.debugRenderer.villageSectionsDebugRenderer;
            villagesectionsdebugpayload.villageChunks().forEach(villagesectionsdebugrenderer::setVillageSection);
            villagesectionsdebugpayload.notVillageChunks().forEach(villagesectionsdebugrenderer::setNotVillageSection);
        } else if (pPayload instanceof GoalDebugPayload goaldebugpayload) {
            this.minecraft.debugRenderer.goalSelectorRenderer.addGoalSelector(goaldebugpayload.entityId(), goaldebugpayload.pos(), goaldebugpayload.goals());
        } else if (pPayload instanceof BrainDebugPayload braindebugpayload) {
            this.minecraft.debugRenderer.brainDebugRenderer.addOrUpdateBrainDump(braindebugpayload.brainDump());
        } else if (pPayload instanceof BeeDebugPayload beedebugpayload) {
            this.minecraft.debugRenderer.beeDebugRenderer.addOrUpdateBeeInfo(beedebugpayload.beeInfo());
        } else if (pPayload instanceof HiveDebugPayload hivedebugpayload) {
            this.minecraft.debugRenderer.beeDebugRenderer.addOrUpdateHiveInfo(hivedebugpayload.hiveInfo(), this.level.getGameTime());
        } else if (pPayload instanceof GameTestAddMarkerDebugPayload gametestaddmarkerdebugpayload) {
            this.minecraft
                .debugRenderer
                .gameTestDebugRenderer
                .addMarker(
                    gametestaddmarkerdebugpayload.pos(),
                    gametestaddmarkerdebugpayload.color(),
                    gametestaddmarkerdebugpayload.text(),
                    gametestaddmarkerdebugpayload.durationMs()
                );
        } else if (pPayload instanceof GameTestClearMarkersDebugPayload) {
            this.minecraft.debugRenderer.gameTestDebugRenderer.clear();
        } else if (pPayload instanceof RaidsDebugPayload raidsdebugpayload) {
            this.minecraft.debugRenderer.raidDebugRenderer.setRaidCenters(raidsdebugpayload.raidCenters());
        } else if (pPayload instanceof GameEventDebugPayload gameeventdebugpayload) {
            this.minecraft.debugRenderer.gameEventListenerRenderer.trackGameEvent(gameeventdebugpayload.gameEventType(), gameeventdebugpayload.pos());
        } else if (pPayload instanceof GameEventListenerDebugPayload gameeventlistenerdebugpayload) {
            this.minecraft
                .debugRenderer
                .gameEventListenerRenderer
                .trackListener(gameeventlistenerdebugpayload.listenerPos(), gameeventlistenerdebugpayload.listenerRange());
        } else if (pPayload instanceof BreezeDebugPayload breezedebugpayload) {
            this.minecraft.debugRenderer.breezeDebugRenderer.add(breezedebugpayload.breezeInfo());
        } else {
            this.handleUnknownCustomPayload(pPayload);
        }
    }

    private void handleUnknownCustomPayload(CustomPacketPayload pPacket) {
        LOGGER.warn("Unknown custom packet payload: {}", pPacket.type().id());
    }

    /**
     * May create a scoreboard objective, remove an objective from the scoreboard or update an objectives' displayname
     */
    @Override
    public void handleAddObjective(ClientboundSetObjectivePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        String s = pPacket.getObjectiveName();
        if (pPacket.getMethod() == 0) {
            this.scoreboard
                .addObjective(
                    s, ObjectiveCriteria.DUMMY, pPacket.getDisplayName(), pPacket.getRenderType(), false, pPacket.getNumberFormat().orElse(null)
                );
        } else {
            Objective objective = this.scoreboard.getObjective(s);
            if (objective != null) {
                if (pPacket.getMethod() == 1) {
                    this.scoreboard.removeObjective(objective);
                } else if (pPacket.getMethod() == 2) {
                    objective.setRenderType(pPacket.getRenderType());
                    objective.setDisplayName(pPacket.getDisplayName());
                    objective.setNumberFormat(pPacket.getNumberFormat().orElse(null));
                }
            }
        }
    }

    /**
     * Either updates the score with a specified value or removes the score for an objective
     */
    @Override
    public void handleSetScore(ClientboundSetScorePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        String s = pPacket.objectiveName();
        ScoreHolder scoreholder = ScoreHolder.forNameOnly(pPacket.owner());
        Objective objective = this.scoreboard.getObjective(s);
        if (objective != null) {
            ScoreAccess scoreaccess = this.scoreboard.getOrCreatePlayerScore(scoreholder, objective, true);
            scoreaccess.set(pPacket.score());
            scoreaccess.display(pPacket.display().orElse(null));
            scoreaccess.numberFormatOverride(pPacket.numberFormat().orElse(null));
        } else {
            LOGGER.warn("Received packet for unknown scoreboard objective: {}", s);
        }
    }

    @Override
    public void handleResetScore(ClientboundResetScorePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        String s = pPacket.objectiveName();
        ScoreHolder scoreholder = ScoreHolder.forNameOnly(pPacket.owner());
        if (s == null) {
            this.scoreboard.resetAllPlayerScores(scoreholder);
        } else {
            Objective objective = this.scoreboard.getObjective(s);
            if (objective != null) {
                this.scoreboard.resetSinglePlayerScore(scoreholder, objective);
            } else {
                LOGGER.warn("Received packet for unknown scoreboard objective: {}", s);
            }
        }
    }

    /**
     * Removes or sets the ScoreObjective to be displayed at a particular scoreboard position (list, sidebar, below name)
     */
    @Override
    public void handleSetDisplayObjective(ClientboundSetDisplayObjectivePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        String s = pPacket.getObjectiveName();
        Objective objective = s == null ? null : this.scoreboard.getObjective(s);
        this.scoreboard.setDisplayObjective(pPacket.getSlot(), objective);
    }

    /**
     * Updates a team managed by the scoreboard: Create/Remove the team registration, Register/Remove the player-team-memberships, Set team displayname/prefix/suffix and/or whether friendly fire is enabled
     */
    @Override
    public void handleSetPlayerTeamPacket(ClientboundSetPlayerTeamPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        ClientboundSetPlayerTeamPacket.Action clientboundsetplayerteampacket$action = pPacket.getTeamAction();
        PlayerTeam playerteam;
        if (clientboundsetplayerteampacket$action == ClientboundSetPlayerTeamPacket.Action.ADD) {
            playerteam = this.scoreboard.addPlayerTeam(pPacket.getName());
        } else {
            playerteam = this.scoreboard.getPlayerTeam(pPacket.getName());
            if (playerteam == null) {
                LOGGER.warn(
                    "Received packet for unknown team {}: team action: {}, player action: {}",
                    pPacket.getName(),
                    pPacket.getTeamAction(),
                    pPacket.getPlayerAction()
                );
                return;
            }
        }

        Optional<ClientboundSetPlayerTeamPacket.Parameters> optional = pPacket.getParameters();
        optional.ifPresent(p_233670_ -> {
            playerteam.setDisplayName(p_233670_.getDisplayName());
            playerteam.setColor(p_233670_.getColor());
            playerteam.unpackOptions(p_233670_.getOptions());
            Team.Visibility team$visibility = Team.Visibility.byName(p_233670_.getNametagVisibility());
            if (team$visibility != null) {
                playerteam.setNameTagVisibility(team$visibility);
            }

            Team.CollisionRule team$collisionrule = Team.CollisionRule.byName(p_233670_.getCollisionRule());
            if (team$collisionrule != null) {
                playerteam.setCollisionRule(team$collisionrule);
            }

            playerteam.setPlayerPrefix(p_233670_.getPlayerPrefix());
            playerteam.setPlayerSuffix(p_233670_.getPlayerSuffix());
        });
        ClientboundSetPlayerTeamPacket.Action clientboundsetplayerteampacket$action1 = pPacket.getPlayerAction();
        if (clientboundsetplayerteampacket$action1 == ClientboundSetPlayerTeamPacket.Action.ADD) {
            for (String s : pPacket.getPlayers()) {
                this.scoreboard.addPlayerToTeam(s, playerteam);
            }
        } else if (clientboundsetplayerteampacket$action1 == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
            for (String s1 : pPacket.getPlayers()) {
                this.scoreboard.removePlayerFromTeam(s1, playerteam);
            }
        }

        if (clientboundsetplayerteampacket$action == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
            this.scoreboard.removePlayerTeam(playerteam);
        }
    }

    /**
     * Spawns a specified number of particles at the specified location with a randomized displacement according to specified bounds
     */
    @Override
    public void handleParticleEvent(ClientboundLevelParticlesPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (pPacket.getCount() == 0) {
            double d0 = (double)(pPacket.getMaxSpeed() * pPacket.getXDist());
            double d2 = (double)(pPacket.getMaxSpeed() * pPacket.getYDist());
            double d4 = (double)(pPacket.getMaxSpeed() * pPacket.getZDist());

            try {
                this.level
                    .addParticle(pPacket.getParticle(), pPacket.isOverrideLimiter(), pPacket.getX(), pPacket.getY(), pPacket.getZ(), d0, d2, d4);
            } catch (Throwable throwable1) {
                LOGGER.warn("Could not spawn particle effect {}", pPacket.getParticle());
            }
        } else {
            for (int i = 0; i < pPacket.getCount(); i++) {
                double d1 = this.random.nextGaussian() * (double)pPacket.getXDist();
                double d3 = this.random.nextGaussian() * (double)pPacket.getYDist();
                double d5 = this.random.nextGaussian() * (double)pPacket.getZDist();
                double d6 = this.random.nextGaussian() * (double)pPacket.getMaxSpeed();
                double d7 = this.random.nextGaussian() * (double)pPacket.getMaxSpeed();
                double d8 = this.random.nextGaussian() * (double)pPacket.getMaxSpeed();

                try {
                    this.level
                        .addParticle(
                            pPacket.getParticle(),
                            pPacket.isOverrideLimiter(),
                            pPacket.getX() + d1,
                            pPacket.getY() + d3,
                            pPacket.getZ() + d5,
                            d6,
                            d7,
                            d8
                        );
                } catch (Throwable throwable) {
                    LOGGER.warn("Could not spawn particle effect {}", pPacket.getParticle());
                    return;
                }
            }
        }
    }

    /**
     * Updates en entity's attributes and their respective modifiers, which are used for speed bonuses (player sprinting, animals fleeing, baby speed), weapon/tool attackDamage, hostiles followRange randomization, zombie maxHealth and knockback resistance as well as reinforcement spawning chance.
     */
    @Override
    public void handleUpdateAttributes(ClientboundUpdateAttributesPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.getEntityId());
        if (entity != null) {
            if (!(entity instanceof LivingEntity)) {
                throw new IllegalStateException("Server tried to update attributes of a non-living entity (actually: " + entity + ")");
            } else {
                AttributeMap attributemap = ((LivingEntity)entity).getAttributes();

                for (ClientboundUpdateAttributesPacket.AttributeSnapshot clientboundupdateattributespacket$attributesnapshot : pPacket.getValues()) {
                    AttributeInstance attributeinstance = attributemap.getInstance(clientboundupdateattributespacket$attributesnapshot.attribute());
                    if (attributeinstance == null) {
                        LOGGER.warn(
                            "Entity {} does not have attribute {}", entity, clientboundupdateattributespacket$attributesnapshot.attribute().getRegisteredName()
                        );
                    } else {
                        attributeinstance.setBaseValue(clientboundupdateattributespacket$attributesnapshot.base());
                        attributeinstance.removeModifiers();

                        for (AttributeModifier attributemodifier : clientboundupdateattributespacket$attributesnapshot.modifiers()) {
                            attributeinstance.addTransientModifier(attributemodifier);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handlePlaceRecipe(ClientboundPlaceGhostRecipePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        AbstractContainerMenu abstractcontainermenu = this.minecraft.player.containerMenu;
        if (abstractcontainermenu.containerId == pPacket.getContainerId()) {
            this.recipeManager.byKey(pPacket.getRecipe()).ifPresent(p_300679_ -> {
                if (this.minecraft.screen instanceof RecipeUpdateListener) {
                    RecipeBookComponent recipebookcomponent = ((RecipeUpdateListener)this.minecraft.screen).getRecipeBookComponent();
                    recipebookcomponent.setupGhostRecipe((RecipeHolder<?>)p_300679_, abstractcontainermenu.slots);
                }
            });
        }
    }

    @Override
    public void handleLightUpdatePacket(ClientboundLightUpdatePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        int i = pPacket.getX();
        int j = pPacket.getZ();
        ClientboundLightUpdatePacketData clientboundlightupdatepacketdata = pPacket.getLightData();
        this.level.queueLightUpdate(() -> this.applyLightData(i, j, clientboundlightupdatepacketdata));
    }

    private void applyLightData(int pX, int pZ, ClientboundLightUpdatePacketData pData) {
        LevelLightEngine levellightengine = this.level.getChunkSource().getLightEngine();
        BitSet bitset = pData.getSkyYMask();
        BitSet bitset1 = pData.getEmptySkyYMask();
        Iterator<byte[]> iterator = pData.getSkyUpdates().iterator();
        this.readSectionList(pX, pZ, levellightengine, LightLayer.SKY, bitset, bitset1, iterator);
        BitSet bitset2 = pData.getBlockYMask();
        BitSet bitset3 = pData.getEmptyBlockYMask();
        Iterator<byte[]> iterator1 = pData.getBlockUpdates().iterator();
        this.readSectionList(pX, pZ, levellightengine, LightLayer.BLOCK, bitset2, bitset3, iterator1);
        levellightengine.setLightEnabled(new ChunkPos(pX, pZ), true);
    }

    @Override
    public void handleMerchantOffers(ClientboundMerchantOffersPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        AbstractContainerMenu abstractcontainermenu = this.minecraft.player.containerMenu;
        if (pPacket.getContainerId() == abstractcontainermenu.containerId && abstractcontainermenu instanceof MerchantMenu merchantmenu) {
            merchantmenu.setOffers(pPacket.getOffers());
            merchantmenu.setXp(pPacket.getVillagerXp());
            merchantmenu.setMerchantLevel(pPacket.getVillagerLevel());
            merchantmenu.setShowProgressBar(pPacket.showProgress());
            merchantmenu.setCanRestock(pPacket.canRestock());
        }
    }

    @Override
    public void handleSetChunkCacheRadius(ClientboundSetChunkCacheRadiusPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.serverChunkRadius = pPacket.getRadius();
        this.minecraft.options.setServerRenderDistance(this.serverChunkRadius);
        this.level.getChunkSource().updateViewRadius(pPacket.getRadius());
    }

    @Override
    public void handleSetSimulationDistance(ClientboundSetSimulationDistancePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.serverSimulationDistance = pPacket.simulationDistance();
        this.level.setServerSimulationDistance(this.serverSimulationDistance);
    }

    @Override
    public void handleSetChunkCacheCenter(ClientboundSetChunkCacheCenterPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.level.getChunkSource().updateViewCenter(pPacket.getX(), pPacket.getZ());
    }

    @Override
    public void handleBlockChangedAck(ClientboundBlockChangedAckPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.level.handleBlockChangedAck(pPacket.sequence());
    }

    @Override
    public void handleBundlePacket(ClientboundBundlePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);

        for (Packet<? super ClientGamePacketListener> packet : pPacket.subPackets()) {
            packet.handle(this);
        }
    }

    @Override
    public void handleProjectilePowerPacket(ClientboundProjectilePowerPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (this.level.getEntity(pPacket.getId()) instanceof AbstractHurtingProjectile abstracthurtingprojectile) {
            abstracthurtingprojectile.accelerationPower = pPacket.getAccelerationPower();
        }
    }

    @Override
    public void handleChunkBatchStart(ClientboundChunkBatchStartPacket pPacket) {
        this.chunkBatchSizeCalculator.onBatchStart();
    }

    @Override
    public void handleChunkBatchFinished(ClientboundChunkBatchFinishedPacket pPacket) {
        this.chunkBatchSizeCalculator.onBatchFinished(pPacket.batchSize());
        this.send(new ServerboundChunkBatchReceivedPacket(this.chunkBatchSizeCalculator.getDesiredChunksPerTick()));
    }

    @Override
    public void handleDebugSample(ClientboundDebugSamplePacket pPacket) {
        this.minecraft.getDebugOverlay().logRemoteSample(pPacket.sample(), pPacket.debugSampleType());
    }

    @Override
    public void handlePongResponse(ClientboundPongResponsePacket pPacket) {
        this.pingDebugMonitor.onPongReceived(pPacket);
    }

    private void readSectionList(
        int pX, int pZ, LevelLightEngine pLightEngine, LightLayer pLightLayer, BitSet pSkyYMask, BitSet pEmptySkyYMask, Iterator<byte[]> pSkyUpdates
    ) {
        for (int i = 0; i < pLightEngine.getLightSectionCount(); i++) {
            int j = pLightEngine.getMinLightSection() + i;
            boolean flag = pSkyYMask.get(i);
            boolean flag1 = pEmptySkyYMask.get(i);
            if (flag || flag1) {
                pLightEngine.queueSectionData(
                    pLightLayer, SectionPos.of(pX, j, pZ), flag ? new DataLayer((byte[])pSkyUpdates.next().clone()) : new DataLayer()
                );
                this.level.setSectionDirtyWithNeighbors(pX, j, pZ);
            }
        }
    }

    public Connection getConnection() {
        return this.connection;
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected() && !this.closed;
    }

    public Collection<PlayerInfo> getListedOnlinePlayers() {
        return this.listedPlayers;
    }

    public Collection<PlayerInfo> getOnlinePlayers() {
        return this.playerInfoMap.values();
    }

    public Collection<UUID> getOnlinePlayerIds() {
        return this.playerInfoMap.keySet();
    }

    @Nullable
    public PlayerInfo getPlayerInfo(UUID pUniqueId) {
        return this.playerInfoMap.get(pUniqueId);
    }

    /**
     * Gets the client's description information about another player on the server.
     */
    @Nullable
    public PlayerInfo getPlayerInfo(String pName) {
        for (PlayerInfo playerinfo : this.playerInfoMap.values()) {
            if (playerinfo.getProfile().getName().equals(pName)) {
                return playerinfo;
            }
        }

        return null;
    }

    public GameProfile getLocalGameProfile() {
        return this.localGameProfile;
    }

    public ClientAdvancements getAdvancements() {
        return this.advancements;
    }

    public CommandDispatcher<SharedSuggestionProvider> getCommands() {
        return this.commands;
    }

    public ClientLevel getLevel() {
        return this.level;
    }

    public DebugQueryHandler getDebugQueryHandler() {
        return this.debugQueryHandler;
    }

    public UUID getId() {
        return this.id;
    }

    public Set<ResourceKey<Level>> levels() {
        return this.levels;
    }

    public RegistryAccess.Frozen registryAccess() {
        return this.registryAccess;
    }

    public void markMessageAsProcessed(PlayerChatMessage pChatMessage, boolean pAcknowledged) {
        MessageSignature messagesignature = pChatMessage.signature();
        if (messagesignature != null && this.lastSeenMessages.addPending(messagesignature, pAcknowledged) && this.lastSeenMessages.offset() > 64) {
            this.sendChatAcknowledgement();
        }
    }

    private void sendChatAcknowledgement() {
        int i = this.lastSeenMessages.getAndClearOffset();
        if (i > 0) {
            this.send(new ServerboundChatAckPacket(i));
        }
    }

    public void sendChat(String pMessage) {
        pMessage = net.neoforged.neoforge.client.ClientHooks.onClientSendMessage(pMessage);
        if (pMessage.isEmpty()) return;
        Instant instant = Instant.now();
        long i = Crypt.SaltSupplier.getLong();
        LastSeenMessagesTracker.Update lastseenmessagestracker$update = this.lastSeenMessages.generateAndApplyUpdate();
        MessageSignature messagesignature = this.signedMessageEncoder
            .pack(new SignedMessageBody(pMessage, instant, i, lastseenmessagestracker$update.lastSeen()));
        this.send(new ServerboundChatPacket(pMessage, instant, i, messagesignature, lastseenmessagestracker$update.update()));
    }

    public void sendCommand(String pCommand) {
        if (net.neoforged.neoforge.client.ClientCommandHandler.runCommand(pCommand)) return;
        SignableCommand<SharedSuggestionProvider> signablecommand = SignableCommand.of(this.parseCommand(pCommand));
        if (signablecommand.arguments().isEmpty()) {
            this.send(new ServerboundChatCommandPacket(pCommand));
        } else {
            Instant instant = Instant.now();
            long i = Crypt.SaltSupplier.getLong();
            LastSeenMessagesTracker.Update lastseenmessagestracker$update = this.lastSeenMessages.generateAndApplyUpdate();
            ArgumentSignatures argumentsignatures = ArgumentSignatures.signCommand(signablecommand, p_247875_ -> {
                SignedMessageBody signedmessagebody = new SignedMessageBody(p_247875_, instant, i, lastseenmessagestracker$update.lastSeen());
                return this.signedMessageEncoder.pack(signedmessagebody);
            });
            this.send(new ServerboundChatCommandSignedPacket(pCommand, instant, i, argumentsignatures, lastseenmessagestracker$update.update()));
        }
    }

    public boolean sendUnsignedCommand(String pCommand) {
        if (!SignableCommand.hasSignableArguments(this.parseCommand(pCommand))) {
            this.send(new ServerboundChatCommandPacket(pCommand));
            return true;
        } else {
            return false;
        }
    }

    private ParseResults<SharedSuggestionProvider> parseCommand(String pCommand) {
        return this.commands.parse(pCommand, this.suggestionsProvider);
    }

    @Override
    public void tick() {
        if (this.connection.isEncrypted()) {
            ProfileKeyPairManager profilekeypairmanager = this.minecraft.getProfileKeyPairManager();
            if (profilekeypairmanager.shouldRefreshKeyPair()) {
                profilekeypairmanager.prepareKeyPair().thenAcceptAsync(p_253339_ -> p_253339_.ifPresent(this::setKeyPair), this.minecraft);
            }
        }

        this.sendDeferredPackets();
        if (this.minecraft.getDebugOverlay().showNetworkCharts()) {
            this.pingDebugMonitor.tick();
        }

        this.debugSampleSubscriber.tick();
        this.telemetryManager.tick();
        if (this.levelLoadStatusManager != null) {
            this.levelLoadStatusManager.tick();
        }
    }

    public void setKeyPair(ProfileKeyPair p_261475_) {
        if (this.minecraft.isLocalPlayer(this.localGameProfile.getId())) {
            if (this.chatSession == null || !this.chatSession.keyPair().equals(p_261475_)) {
                this.chatSession = LocalChatSession.create(p_261475_);
                this.signedMessageEncoder = this.chatSession.createMessageEncoder(this.localGameProfile.getId());
                this.send(new ServerboundChatSessionUpdatePacket(this.chatSession.asRemote().asData()));
            }
        }
    }

    @Nullable
    public ServerData getServerData() {
        return this.serverData;
    }

    public FeatureFlagSet enabledFeatures() {
        return this.enabledFeatures;
    }

    public boolean isFeatureEnabled(FeatureFlagSet pEnabledFeatures) {
        return pEnabledFeatures.isSubsetOf(this.enabledFeatures());
    }

    public Scoreboard scoreboard() {
        return this.scoreboard;
    }

    public net.neoforged.neoforge.network.connection.ConnectionType getConnectionType() {
        return this.connectionType;
    }

    public PotionBrewing potionBrewing() {
        return this.potionBrewing;
    }

    public void updateSearchTrees() {
        this.searchTrees.rebuildAfterLanguageChange();
    }

    public SessionSearchTrees searchTrees() {
        return this.searchTrees;
    }

    public ServerLinks serverLinks() {
        return this.serverLinks;
    }
}
