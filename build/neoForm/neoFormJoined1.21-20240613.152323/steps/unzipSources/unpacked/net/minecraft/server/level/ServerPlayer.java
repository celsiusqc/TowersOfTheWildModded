package net.minecraft.server.level;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ComplexItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ServerItemCooldowns;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;

public class ServerPlayer extends Player {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_XZ = 32;
    private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_Y = 10;
    private static final int FLY_STAT_RECORDING_SPEED = 25;
    public static final double INTERACTION_DISTANCE_VERIFICATION_BUFFER = 1.0;
    private static final AttributeModifier CREATIVE_BLOCK_INTERACTION_RANGE_MODIFIER = new AttributeModifier(
        ResourceLocation.withDefaultNamespace("creative_mode_block_range"), 0.5, AttributeModifier.Operation.ADD_VALUE
    );
    private static final AttributeModifier CREATIVE_ENTITY_INTERACTION_RANGE_MODIFIER = new AttributeModifier(
        ResourceLocation.withDefaultNamespace("creative_mode_entity_range"), 2.0, AttributeModifier.Operation.ADD_VALUE
    );
    public ServerGamePacketListenerImpl connection;
    public final MinecraftServer server;
    public final ServerPlayerGameMode gameMode;
    private final PlayerAdvancements advancements;
    private final ServerStatsCounter stats;
    /**
     * the total health of the player, includes actual health and absorption health. Updated every tick.
     */
    private float lastRecordedHealthAndAbsorption = Float.MIN_VALUE;
    private int lastRecordedFoodLevel = Integer.MIN_VALUE;
    private int lastRecordedAirLevel = Integer.MIN_VALUE;
    private int lastRecordedArmor = Integer.MIN_VALUE;
    private int lastRecordedLevel = Integer.MIN_VALUE;
    private int lastRecordedExperience = Integer.MIN_VALUE;
    private float lastSentHealth = -1.0E8F;
    private int lastSentFood = -99999999;
    private boolean lastFoodSaturationZero = true;
    private int lastSentExp = -99999999;
    private int spawnInvulnerableTime = 60;
    private ChatVisiblity chatVisibility = ChatVisiblity.FULL;
    private boolean canChatColor = true;
    private long lastActionTime = Util.getMillis();
    /**
     * The entity the player is currently spectating through.
     */
    @Nullable
    private Entity camera;
    private boolean isChangingDimension;
    public boolean seenCredits;
    private final ServerRecipeBook recipeBook = new ServerRecipeBook();
    @Nullable
    private Vec3 levitationStartPos;
    private int levitationStartTime;
    private boolean disconnected;
    private int requestedViewDistance = 2;
    private String language = "en_us";
    @Nullable
    private Vec3 startingToFallPosition;
    @Nullable
    private Vec3 enteredNetherPosition;
    @Nullable
    private Vec3 enteredLavaOnVehiclePosition;
    /**
     * Player section position as last updated by TicketManager, used by ChunkManager
     */
    private SectionPos lastSectionPos = SectionPos.of(0, 0, 0);
    private ChunkTrackingView chunkTrackingView = ChunkTrackingView.EMPTY;
    private ResourceKey<Level> respawnDimension = Level.OVERWORLD;
    @Nullable
    private BlockPos respawnPosition;
    private boolean respawnForced;
    private float respawnAngle;
    private final TextFilter textFilter;
    private boolean textFilteringEnabled;
    private boolean allowsListing;
    private boolean spawnExtraParticlesOnFall;
    private WardenSpawnTracker wardenSpawnTracker = new WardenSpawnTracker(0, 0, 0);
    @Nullable
    private BlockPos raidOmenPosition;
    private Vec3 lastKnownClientMovement = Vec3.ZERO;
    private final ContainerSynchronizer containerSynchronizer = new ContainerSynchronizer() {
        @Override
        public void sendInitialData(AbstractContainerMenu p_143448_, NonNullList<ItemStack> p_143449_, ItemStack p_143450_, int[] p_143451_) {
            ServerPlayer.this.connection
                .send(new ClientboundContainerSetContentPacket(p_143448_.containerId, p_143448_.incrementStateId(), p_143449_, p_143450_));

            for (int i = 0; i < p_143451_.length; i++) {
                this.broadcastDataValue(p_143448_, i, p_143451_[i]);
            }
        }

        @Override
        public void sendSlotChange(AbstractContainerMenu p_143441_, int p_143442_, ItemStack p_143443_) {
            ServerPlayer.this.connection.send(new ClientboundContainerSetSlotPacket(p_143441_.containerId, p_143441_.incrementStateId(), p_143442_, p_143443_));
        }

        @Override
        public void sendCarriedChange(AbstractContainerMenu p_143445_, ItemStack p_143446_) {
            ServerPlayer.this.connection.send(new ClientboundContainerSetSlotPacket(-1, p_143445_.incrementStateId(), -1, p_143446_));
        }

        @Override
        public void sendDataChange(AbstractContainerMenu p_143437_, int p_143438_, int p_143439_) {
            this.broadcastDataValue(p_143437_, p_143438_, p_143439_);
        }

        private void broadcastDataValue(AbstractContainerMenu p_143455_, int p_143456_, int p_143457_) {
            if (ServerPlayer.this.connection.hasChannel(net.neoforged.neoforge.network.payload.AdvancedContainerSetDataPayload.TYPE)) {
                ServerPlayer.this.connection.send(new net.neoforged.neoforge.network.payload.AdvancedContainerSetDataPayload((byte) p_143455_.containerId, (short) p_143456_, p_143457_));
                return;
            }
            ServerPlayer.this.connection.send(new ClientboundContainerSetDataPacket(p_143455_.containerId, p_143456_, p_143457_));
        }
    };
    private final ContainerListener containerListener = new ContainerListener() {
        @Override
        public void slotChanged(AbstractContainerMenu p_143466_, int p_143467_, ItemStack p_143468_) {
            Slot slot = p_143466_.getSlot(p_143467_);
            if (!(slot instanceof ResultSlot)) {
                if (slot.container == ServerPlayer.this.getInventory()) {
                    CriteriaTriggers.INVENTORY_CHANGED.trigger(ServerPlayer.this, ServerPlayer.this.getInventory(), p_143468_);
                }
            }
        }

        @Override
        public void dataChanged(AbstractContainerMenu p_143462_, int p_143463_, int p_143464_) {
        }
    };
    @Nullable
    private RemoteChatSession chatSession;
    @Nullable
    public final Object object;
    public int containerCounter;
    public boolean wonGame;

    public ServerPlayer(MinecraftServer pServer, ServerLevel pLevel, GameProfile pGameProfile, ClientInformation pClientInformation) {
        super(pLevel, pLevel.getSharedSpawnPos(), pLevel.getSharedSpawnAngle(), pGameProfile);
        this.textFilter = pServer.createTextFilterForPlayer(this);
        this.gameMode = pServer.createGameModeForPlayer(this);
        this.server = pServer;
        this.stats = pServer.getPlayerList().getPlayerStats(this);
        this.advancements = pServer.getPlayerList().getPlayerAdvancements(this);
        this.moveTo(this.adjustSpawnLocation(pLevel, pLevel.getSharedSpawnPos()).getBottomCenter(), 0.0F, 0.0F);
        this.updateOptions(pClientInformation);
        this.object = null;
    }

    @Override
    public BlockPos adjustSpawnLocation(ServerLevel p_352206_, BlockPos p_352202_) {
        AABB aabb = this.getDimensions(Pose.STANDING).makeBoundingBox(Vec3.ZERO);
        BlockPos blockpos = p_352202_;
        if (p_352206_.dimensionType().hasSkyLight() && p_352206_.getServer().getWorldData().getGameType() != GameType.ADVENTURE) {
            int i = Math.max(0, this.server.getSpawnRadius(p_352206_));
            int j = Mth.floor(p_352206_.getWorldBorder().getDistanceToBorder((double)p_352202_.getX(), (double)p_352202_.getZ()));
            if (j < i) {
                i = j;
            }

            if (j <= 1) {
                i = 1;
            }

            long k = (long)(i * 2 + 1);
            long l = k * k;
            int i1 = l > 2147483647L ? Integer.MAX_VALUE : (int)l;
            int j1 = this.getCoprime(i1);
            int k1 = RandomSource.create().nextInt(i1);

            for (int l1 = 0; l1 < i1; l1++) {
                int i2 = (k1 + j1 * l1) % i1;
                int j2 = i2 % (i * 2 + 1);
                int k2 = i2 / (i * 2 + 1);
                blockpos = PlayerRespawnLogic.getOverworldRespawnPos(p_352206_, p_352202_.getX() + j2 - i, p_352202_.getZ() + k2 - i);
                if (blockpos != null && p_352206_.noCollision(this, aabb.move(blockpos.getBottomCenter()))) {
                    return blockpos;
                }
            }

            blockpos = p_352202_;
        }

        while (!p_352206_.noCollision(this, aabb.move(blockpos.getBottomCenter())) && blockpos.getY() < p_352206_.getMaxBuildHeight() - 1) {
            blockpos = blockpos.above();
        }

        while (p_352206_.noCollision(this, aabb.move(blockpos.below().getBottomCenter())) && blockpos.getY() > p_352206_.getMinBuildHeight() + 1) {
            blockpos = blockpos.below();
        }

        return blockpos;
    }

    private int getCoprime(int pSpawnArea) {
        return pSpawnArea <= 16 ? pSpawnArea - 1 : 17;
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("warden_spawn_tracker", 10)) {
            WardenSpawnTracker.CODEC
                .parse(new Dynamic<>(NbtOps.INSTANCE, pCompound.get("warden_spawn_tracker")))
                .resultOrPartial(LOGGER::error)
                .ifPresent(p_248205_ -> this.wardenSpawnTracker = p_248205_);
        }

        if (pCompound.contains("enteredNetherPosition", 10)) {
            CompoundTag compoundtag = pCompound.getCompound("enteredNetherPosition");
            this.enteredNetherPosition = new Vec3(compoundtag.getDouble("x"), compoundtag.getDouble("y"), compoundtag.getDouble("z"));
        }

        this.seenCredits = pCompound.getBoolean("seenCredits");
        if (pCompound.contains("recipeBook", 10)) {
            this.recipeBook.fromNbt(pCompound.getCompound("recipeBook"), this.server.getRecipeManager());
        }

        if (this.isSleeping()) {
            this.stopSleeping();
        }

        if (pCompound.contains("SpawnX", 99) && pCompound.contains("SpawnY", 99) && pCompound.contains("SpawnZ", 99)) {
            this.respawnPosition = new BlockPos(pCompound.getInt("SpawnX"), pCompound.getInt("SpawnY"), pCompound.getInt("SpawnZ"));
            this.respawnForced = pCompound.getBoolean("SpawnForced");
            this.respawnAngle = pCompound.getFloat("SpawnAngle");
            if (pCompound.contains("SpawnDimension")) {
                this.respawnDimension = Level.RESOURCE_KEY_CODEC
                    .parse(NbtOps.INSTANCE, pCompound.get("SpawnDimension"))
                    .resultOrPartial(LOGGER::error)
                    .orElse(Level.OVERWORLD);
            }
        }

        this.spawnExtraParticlesOnFall = pCompound.getBoolean("spawn_extra_particles_on_fall");
        Tag tag = pCompound.get("raid_omen_position");
        if (tag != null) {
            BlockPos.CODEC.parse(NbtOps.INSTANCE, tag).resultOrPartial(LOGGER::error).ifPresent(p_337547_ -> this.raidOmenPosition = p_337547_);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        WardenSpawnTracker.CODEC
            .encodeStart(NbtOps.INSTANCE, this.wardenSpawnTracker)
            .resultOrPartial(LOGGER::error)
            .ifPresent(p_9134_ -> pCompound.put("warden_spawn_tracker", p_9134_));
        this.storeGameTypes(pCompound);
        pCompound.putBoolean("seenCredits", this.seenCredits);
        if (this.enteredNetherPosition != null) {
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.putDouble("x", this.enteredNetherPosition.x);
            compoundtag.putDouble("y", this.enteredNetherPosition.y);
            compoundtag.putDouble("z", this.enteredNetherPosition.z);
            pCompound.put("enteredNetherPosition", compoundtag);
        }

        Entity entity1 = this.getRootVehicle();
        Entity entity = this.getVehicle();
        if (entity != null && entity1 != this && entity1.hasExactlyOnePlayerPassenger()) {
            CompoundTag compoundtag1 = new CompoundTag();
            CompoundTag compoundtag2 = new CompoundTag();
            entity1.save(compoundtag2);
            compoundtag1.putUUID("Attach", entity.getUUID());
            compoundtag1.put("Entity", compoundtag2);
            pCompound.put("RootVehicle", compoundtag1);
        }

        pCompound.put("recipeBook", this.recipeBook.toNbt());
        pCompound.putString("Dimension", this.level().dimension().location().toString());
        if (this.respawnPosition != null) {
            pCompound.putInt("SpawnX", this.respawnPosition.getX());
            pCompound.putInt("SpawnY", this.respawnPosition.getY());
            pCompound.putInt("SpawnZ", this.respawnPosition.getZ());
            pCompound.putBoolean("SpawnForced", this.respawnForced);
            pCompound.putFloat("SpawnAngle", this.respawnAngle);
            ResourceLocation.CODEC
                .encodeStart(NbtOps.INSTANCE, this.respawnDimension.location())
                .resultOrPartial(LOGGER::error)
                .ifPresent(p_248207_ -> pCompound.put("SpawnDimension", p_248207_));
        }

        pCompound.putBoolean("spawn_extra_particles_on_fall", this.spawnExtraParticlesOnFall);
        if (this.raidOmenPosition != null) {
            BlockPos.CODEC
                .encodeStart(NbtOps.INSTANCE, this.raidOmenPosition)
                .resultOrPartial(LOGGER::error)
                .ifPresent(p_337549_ -> pCompound.put("raid_omen_position", p_337549_));
        }
    }

    public void setExperiencePoints(int pExperiencePoints) {
        float f = (float)this.getXpNeededForNextLevel();
        float f1 = (f - 1.0F) / f;
        this.experienceProgress = Mth.clamp((float)pExperiencePoints / f, 0.0F, f1);
        this.lastSentExp = -1;
    }

    public void setExperienceLevels(int pLevel) {
        this.experienceLevel = pLevel;
        this.lastSentExp = -1;
    }

    /**
     * Add experience levels to this player.
     */
    @Override
    public void giveExperienceLevels(int pLevels) {
        super.giveExperienceLevels(pLevels);
        this.lastSentExp = -1;
    }

    @Override
    public void onEnchantmentPerformed(ItemStack pEnchantedItem, int pCost) {
        super.onEnchantmentPerformed(pEnchantedItem, pCost);
        this.lastSentExp = -1;
    }

    public void initMenu(AbstractContainerMenu pMenu) {
        pMenu.addSlotListener(this.containerListener);
        pMenu.setSynchronizer(this.containerSynchronizer);
    }

    public void initInventoryMenu() {
        this.initMenu(this.inventoryMenu);
    }

    @Override
    public void onEnterCombat() {
        super.onEnterCombat();
        this.connection.send(ClientboundPlayerCombatEnterPacket.INSTANCE);
    }

    @Override
    public void onLeaveCombat() {
        super.onLeaveCombat();
        this.connection.send(new ClientboundPlayerCombatEndPacket(this.getCombatTracker()));
    }

    @Override
    public void onInsideBlock(BlockState pState) {
        CriteriaTriggers.ENTER_BLOCK.trigger(this, pState);
    }

    @Override
    protected ItemCooldowns createItemCooldowns() {
        return new ServerItemCooldowns(this);
    }

    @Override
    public void tick() {
        this.gameMode.tick();
        this.wardenSpawnTracker.tick();
        this.spawnInvulnerableTime--;
        if (this.invulnerableTime > 0) {
            this.invulnerableTime--;
        }

        this.containerMenu.broadcastChanges();
        if (!this.level().isClientSide && !this.containerMenu.stillValid(this)) {
            this.closeContainer();
            this.containerMenu = this.inventoryMenu;
        }

        Entity entity = this.getCamera();
        if (entity != this) {
            if (entity.isAlive()) {
                this.absMoveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
                this.serverLevel().getChunkSource().move(this);
                if (this.wantsToStopRiding()) {
                    this.setCamera(this);
                }
            } else {
                this.setCamera(this);
            }
        }

        CriteriaTriggers.TICK.trigger(this);
        if (this.levitationStartPos != null) {
            CriteriaTriggers.LEVITATION.trigger(this, this.levitationStartPos, this.tickCount - this.levitationStartTime);
        }

        this.trackStartFallingPosition();
        this.trackEnteredOrExitedLavaOnVehicle();
        this.updatePlayerAttributes();
        this.advancements.flushDirty(this);
    }

    private void updatePlayerAttributes() {
        AttributeInstance attributeinstance = this.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (attributeinstance != null) {
            if (this.isCreative()) {
                attributeinstance.addOrUpdateTransientModifier(CREATIVE_BLOCK_INTERACTION_RANGE_MODIFIER);
            } else {
                attributeinstance.removeModifier(CREATIVE_BLOCK_INTERACTION_RANGE_MODIFIER);
            }
        }

        AttributeInstance attributeinstance1 = this.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (attributeinstance1 != null) {
            if (this.isCreative()) {
                attributeinstance1.addOrUpdateTransientModifier(CREATIVE_ENTITY_INTERACTION_RANGE_MODIFIER);
            } else {
                attributeinstance1.removeModifier(CREATIVE_ENTITY_INTERACTION_RANGE_MODIFIER);
            }
        }
    }

    public void doTick() {
        try {
            if (!this.isSpectator() || !this.touchingUnloadedChunk()) {
                super.tick();
            }

            for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
                ItemStack itemstack = this.getInventory().getItem(i);
                if (itemstack.getItem().isComplex()) {
                    Packet<?> packet = ((ComplexItem)itemstack.getItem()).getUpdatePacket(itemstack, this.level(), this);
                    if (packet != null) {
                        this.connection.send(packet);
                    }
                }
            }

            if (this.getHealth() != this.lastSentHealth
                || this.lastSentFood != this.foodData.getFoodLevel()
                || this.foodData.getSaturationLevel() == 0.0F != this.lastFoodSaturationZero) {
                this.connection.send(new ClientboundSetHealthPacket(this.getHealth(), this.foodData.getFoodLevel(), this.foodData.getSaturationLevel()));
                this.lastSentHealth = this.getHealth();
                this.lastSentFood = this.foodData.getFoodLevel();
                this.lastFoodSaturationZero = this.foodData.getSaturationLevel() == 0.0F;
            }

            if (this.getHealth() + this.getAbsorptionAmount() != this.lastRecordedHealthAndAbsorption) {
                this.lastRecordedHealthAndAbsorption = this.getHealth() + this.getAbsorptionAmount();
                this.updateScoreForCriteria(ObjectiveCriteria.HEALTH, Mth.ceil(this.lastRecordedHealthAndAbsorption));
            }

            if (this.foodData.getFoodLevel() != this.lastRecordedFoodLevel) {
                this.lastRecordedFoodLevel = this.foodData.getFoodLevel();
                this.updateScoreForCriteria(ObjectiveCriteria.FOOD, Mth.ceil((float)this.lastRecordedFoodLevel));
            }

            if (this.getAirSupply() != this.lastRecordedAirLevel) {
                this.lastRecordedAirLevel = this.getAirSupply();
                this.updateScoreForCriteria(ObjectiveCriteria.AIR, Mth.ceil((float)this.lastRecordedAirLevel));
            }

            if (this.getArmorValue() != this.lastRecordedArmor) {
                this.lastRecordedArmor = this.getArmorValue();
                this.updateScoreForCriteria(ObjectiveCriteria.ARMOR, Mth.ceil((float)this.lastRecordedArmor));
            }

            if (this.totalExperience != this.lastRecordedExperience) {
                this.lastRecordedExperience = this.totalExperience;
                this.updateScoreForCriteria(ObjectiveCriteria.EXPERIENCE, Mth.ceil((float)this.lastRecordedExperience));
            }

            if (this.experienceLevel != this.lastRecordedLevel) {
                this.lastRecordedLevel = this.experienceLevel;
                this.updateScoreForCriteria(ObjectiveCriteria.LEVEL, Mth.ceil((float)this.lastRecordedLevel));
            }

            if (this.totalExperience != this.lastSentExp) {
                this.lastSentExp = this.totalExperience;
                this.connection.send(new ClientboundSetExperiencePacket(this.experienceProgress, this.totalExperience, this.experienceLevel));
            }

            if (this.getAbilities().flying && !this.mayFly()) {
                this.getAbilities().flying = false;
                this.onUpdateAbilities();
            }

            if (this.tickCount % 20 == 0) {
                CriteriaTriggers.LOCATION.trigger(this);
            }
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking player");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Player being ticked");
            this.fillCrashReportCategory(crashreportcategory);
            throw new ReportedException(crashreport);
        }
    }

    @Override
    public void resetFallDistance() {
        if (this.getHealth() > 0.0F && this.startingToFallPosition != null) {
            CriteriaTriggers.FALL_FROM_HEIGHT.trigger(this, this.startingToFallPosition);
        }

        this.startingToFallPosition = null;
        super.resetFallDistance();
    }

    public void trackStartFallingPosition() {
        if (this.fallDistance > 0.0F && this.startingToFallPosition == null) {
            this.startingToFallPosition = this.position();
            if (this.currentImpulseImpactPos != null && this.currentImpulseImpactPos.y <= this.startingToFallPosition.y) {
                CriteriaTriggers.FALL_AFTER_EXPLOSION.trigger(this, this.currentImpulseImpactPos, this.currentExplosionCause);
            }
        }
    }

    public void trackEnteredOrExitedLavaOnVehicle() {
        if (this.getVehicle() != null && this.getVehicle().isInLava()) {
            if (this.enteredLavaOnVehiclePosition == null) {
                this.enteredLavaOnVehiclePosition = this.position();
            } else {
                CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER.trigger(this, this.enteredLavaOnVehiclePosition);
            }
        }

        if (this.enteredLavaOnVehiclePosition != null && (this.getVehicle() == null || !this.getVehicle().isInLava())) {
            this.enteredLavaOnVehiclePosition = null;
        }
    }

    private void updateScoreForCriteria(ObjectiveCriteria pCriteria, int pPoints) {
        this.getScoreboard().forAllObjectives(pCriteria, this, p_313589_ -> p_313589_.set(pPoints));
    }

    /**
     * Called when the mob's health reaches 0.
     */
    @Override
    public void die(DamageSource pCause) {
        this.gameEvent(GameEvent.ENTITY_DIE);
        if (net.neoforged.neoforge.common.CommonHooks.onLivingDeath(this, pCause)) return;
        boolean flag = this.level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
        if (flag) {
            Component component = this.getCombatTracker().getDeathMessage();
            this.connection
                .send(
                    new ClientboundPlayerCombatKillPacket(this.getId(), component),
                    PacketSendListener.exceptionallySend(
                        () -> {
                            int i = 256;
                            String s = component.getString(256);
                            Component component1 = Component.translatable(
                                "death.attack.message_too_long", Component.literal(s).withStyle(ChatFormatting.YELLOW)
                            );
                            Component component2 = Component.translatable("death.attack.even_more_magic", this.getDisplayName())
                                .withStyle(p_143420_ -> p_143420_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, component1)));
                            return new ClientboundPlayerCombatKillPacket(this.getId(), component2);
                        }
                    )
                );
            Team team = this.getTeam();
            if (team == null || team.getDeathMessageVisibility() == Team.Visibility.ALWAYS) {
                this.server.getPlayerList().broadcastSystemMessage(component, false);
            } else if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OTHER_TEAMS) {
                this.server.getPlayerList().broadcastSystemToTeam(this, component);
            } else if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OWN_TEAM) {
                this.server.getPlayerList().broadcastSystemToAllExceptTeam(this, component);
            }
        } else {
            this.connection.send(new ClientboundPlayerCombatKillPacket(this.getId(), CommonComponents.EMPTY));
        }

        this.removeEntitiesOnShoulder();
        if (this.level().getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            this.tellNeutralMobsThatIDied();
        }

        if (!this.isSpectator()) {
            this.dropAllDeathLoot(this.serverLevel(), pCause);
        }

        this.getScoreboard().forAllObjectives(ObjectiveCriteria.DEATH_COUNT, this, ScoreAccess::increment);
        LivingEntity livingentity = this.getKillCredit();
        if (livingentity != null) {
            this.awardStat(Stats.ENTITY_KILLED_BY.get(livingentity.getType()));
            livingentity.awardKillScore(this, this.deathScore, pCause);
            this.createWitherRose(livingentity);
        }

        this.level().broadcastEntityEvent(this, (byte)3);
        this.awardStat(Stats.DEATHS);
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        this.clearFire();
        this.setTicksFrozen(0);
        this.setSharedFlagOnFire(false);
        this.getCombatTracker().recheckStatus();
        this.setLastDeathLocation(Optional.of(GlobalPos.of(this.level().dimension(), this.blockPosition())));
    }

    private void tellNeutralMobsThatIDied() {
        AABB aabb = new AABB(this.blockPosition()).inflate(32.0, 10.0, 32.0);
        this.level()
            .getEntitiesOfClass(Mob.class, aabb, EntitySelector.NO_SPECTATORS)
            .stream()
            .filter(p_9188_ -> p_9188_ instanceof NeutralMob)
            .forEach(p_9057_ -> ((NeutralMob)p_9057_).playerDied(this));
    }

    @Override
    public void awardKillScore(Entity pKilled, int pScoreValue, DamageSource pDamageSource) {
        if (pKilled != this) {
            super.awardKillScore(pKilled, pScoreValue, pDamageSource);
            this.increaseScore(pScoreValue);
            this.getScoreboard().forAllObjectives(ObjectiveCriteria.KILL_COUNT_ALL, this, ScoreAccess::increment);
            if (pKilled instanceof Player) {
                this.awardStat(Stats.PLAYER_KILLS);
                this.getScoreboard().forAllObjectives(ObjectiveCriteria.KILL_COUNT_PLAYERS, this, ScoreAccess::increment);
            } else {
                this.awardStat(Stats.MOB_KILLS);
            }

            this.handleTeamKill(this, pKilled, ObjectiveCriteria.TEAM_KILL);
            this.handleTeamKill(pKilled, this, ObjectiveCriteria.KILLED_BY_TEAM);
            CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(this, pKilled, pDamageSource);
        }
    }

    private void handleTeamKill(ScoreHolder pScoreHolder, ScoreHolder pTeamMember, ObjectiveCriteria[] pCrtieria) {
        PlayerTeam playerteam = this.getScoreboard().getPlayersTeam(pTeamMember.getScoreboardName());
        if (playerteam != null) {
            int i = playerteam.getColor().getId();
            if (i >= 0 && i < pCrtieria.length) {
                this.getScoreboard().forAllObjectives(pCrtieria[i], pScoreHolder, ScoreAccess::increment);
            }
        }
    }

    /**
     * Called when the entity is attacked.
     */
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isInvulnerableTo(pSource)) {
            return false;
        } else {
            boolean flag = this.server.isDedicatedServer() && this.isPvpAllowed() && pSource.is(DamageTypeTags.IS_FALL);
            if (!flag && this.spawnInvulnerableTime > 0 && !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return false;
            } else {
                Entity entity = pSource.getEntity();
                if (entity instanceof Player player && !this.canHarmPlayer(player)) {
                    return false;
                }

                if (entity instanceof AbstractArrow abstractarrow && abstractarrow.getOwner() instanceof Player player1 && !this.canHarmPlayer(player1)) {
                    return false;
                }

                return super.hurt(pSource, pAmount);
            }
        }
    }

    @Override
    public boolean canHarmPlayer(Player pOther) {
        return !this.isPvpAllowed() ? false : super.canHarmPlayer(pOther);
    }

    private boolean isPvpAllowed() {
        return this.server.isPvpAllowed();
    }

    public DimensionTransition findRespawnPositionAndUseSpawnBlock(boolean p_348590_, DimensionTransition.PostDimensionTransition p_352261_) {
        BlockPos blockpos = this.getRespawnPosition();
        float f = this.getRespawnAngle();
        boolean flag = this.isRespawnForced();
        ServerLevel serverlevel = this.server.getLevel(this.getRespawnDimension());
        if (serverlevel != null && blockpos != null) {
            Optional<ServerPlayer.RespawnPosAngle> optional = findRespawnAndUseSpawnBlock(serverlevel, blockpos, f, flag, p_348590_);
            if (optional.isPresent()) {
                ServerPlayer.RespawnPosAngle serverplayer$respawnposangle = optional.get();
                return new DimensionTransition(
                    serverlevel, serverplayer$respawnposangle.position(), Vec3.ZERO, serverplayer$respawnposangle.yaw(), 0.0F, p_352261_
                );
            } else {
                return DimensionTransition.missingRespawnBlock(this.server.overworld(), this, p_352261_);
            }
        } else {
            return new DimensionTransition(this.server.overworld(), this, p_352261_);
        }
    }

    private static Optional<ServerPlayer.RespawnPosAngle> findRespawnAndUseSpawnBlock(
        ServerLevel p_348505_, BlockPos p_348607_, float p_348481_, boolean p_348513_, boolean p_348600_
    ) {
        BlockState blockstate = p_348505_.getBlockState(p_348607_);
        Block block = blockstate.getBlock();
        if (block instanceof RespawnAnchorBlock
            && (p_348513_ || blockstate.getValue(RespawnAnchorBlock.CHARGE) > 0)
            && RespawnAnchorBlock.canSetSpawn(p_348505_)) {
            Optional<Vec3> optional = RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, p_348505_, p_348607_);
            if (!p_348513_ && !p_348600_ && optional.isPresent()) {
                p_348505_.setBlock(
                    p_348607_, blockstate.setValue(RespawnAnchorBlock.CHARGE, Integer.valueOf(blockstate.getValue(RespawnAnchorBlock.CHARGE) - 1)), 3
                );
            }

            return optional.map(p_348139_ -> ServerPlayer.RespawnPosAngle.of(p_348139_, p_348607_));
        } else if (block instanceof BedBlock && BedBlock.canSetSpawn(p_348505_)) {
            return BedBlock.findStandUpPosition(EntityType.PLAYER, p_348505_, p_348607_, blockstate.getValue(BedBlock.FACING), p_348481_)
                .map(p_348148_ -> ServerPlayer.RespawnPosAngle.of(p_348148_, p_348607_));
        } else if (!p_348513_) {
            return blockstate.getRespawnPosition(EntityType.PLAYER, p_348505_, p_348607_, p_348481_);
        } else {
            boolean flag = block.isPossibleToRespawnInThis(blockstate);
            BlockState blockstate1 = p_348505_.getBlockState(p_348607_.above());
            boolean flag1 = blockstate1.getBlock().isPossibleToRespawnInThis(blockstate1);
            return flag && flag1
                ? Optional.of(
                    new ServerPlayer.RespawnPosAngle(
                        new Vec3((double)p_348607_.getX() + 0.5, (double)p_348607_.getY() + 0.1, (double)p_348607_.getZ() + 0.5), p_348481_
                    )
                )
                : Optional.empty();
        }
    }

    public void showEndCredits() {
        this.unRide();
        this.serverLevel().removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
        if (!this.wonGame) {
            this.wonGame = true;
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0.0F));
            this.seenCredits = true;
        }
    }

    @Nullable
    @Override
    public Entity changeDimension(DimensionTransition p_350472_) {
        if (this.isRemoved()) {
            return null;
        } else {
            if (p_350472_.missingRespawnBlock()) {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
            }

            ServerLevel serverlevel = p_350472_.newLevel();
            ServerLevel serverlevel1 = this.serverLevel();
            ResourceKey<Level> resourcekey = serverlevel1.dimension();
            if (serverlevel.dimension() == resourcekey) {
                this.connection.teleport(p_350472_.pos().x, p_350472_.pos().y, p_350472_.pos().z, p_350472_.yRot(), p_350472_.xRot());
                this.connection.resetPosition();
                p_350472_.postDimensionTransition().onTransition(this);
                return this;
            } else {
                if (!net.neoforged.neoforge.common.CommonHooks.onTravelToDimension(this, p_350472_.newLevel().dimension())) return null;
                this.isChangingDimension = true;
                LevelData leveldata = serverlevel.getLevelData();
                this.connection.send(new ClientboundRespawnPacket(this.createCommonSpawnInfo(serverlevel), (byte)3));
                this.connection.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
                PlayerList playerlist = this.server.getPlayerList();
                playerlist.sendPlayerPermissionLevel(this);
                serverlevel1.removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
                this.revive();
                serverlevel1.getProfiler().push("moving");
                if (resourcekey == Level.OVERWORLD && serverlevel.dimension() == Level.NETHER) {
                    this.enteredNetherPosition = this.position();
                }

                serverlevel1.getProfiler().pop();
                serverlevel1.getProfiler().push("placing");
                this.setServerLevel(serverlevel);
                this.connection.teleport(p_350472_.pos().x, p_350472_.pos().y, p_350472_.pos().z, p_350472_.yRot(), p_350472_.xRot());
                this.connection.resetPosition();
                serverlevel.addDuringTeleport(this);
                serverlevel1.getProfiler().pop();
                this.triggerDimensionChangeTriggers(serverlevel1);
                this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
                playerlist.sendLevelInfo(this, serverlevel);
                playerlist.sendAllPlayerInfo(this);
                playerlist.sendActivePlayerEffects(this);
                // TODO 1.21: Play custom teleport sound
                p_350472_.postDimensionTransition().onTransition(this);
                this.lastSentExp = -1;
                this.lastSentHealth = -1.0F;
                this.lastSentFood = -1;
                net.neoforged.neoforge.event.EventHooks.firePlayerChangedDimensionEvent(this, resourcekey, p_350472_.newLevel().dimension());
                return this;
            }
        }
    }

    private void triggerDimensionChangeTriggers(ServerLevel pLevel) {
        ResourceKey<Level> resourcekey = pLevel.dimension();
        ResourceKey<Level> resourcekey1 = this.level().dimension();
        CriteriaTriggers.CHANGED_DIMENSION.trigger(this, resourcekey, resourcekey1);
        if (resourcekey == Level.NETHER && resourcekey1 == Level.OVERWORLD && this.enteredNetherPosition != null) {
            CriteriaTriggers.NETHER_TRAVEL.trigger(this, this.enteredNetherPosition);
        }

        if (resourcekey1 != Level.NETHER) {
            this.enteredNetherPosition = null;
        }
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer pPlayer) {
        if (pPlayer.isSpectator()) {
            return this.getCamera() == this;
        } else {
            return this.isSpectator() ? false : super.broadcastToPlayer(pPlayer);
        }
    }

    /**
     * Called when the entity picks up an item.
     */
    @Override
    public void take(Entity pEntity, int pQuantity) {
        super.take(pEntity, pQuantity);
        this.containerMenu.broadcastChanges();
    }

    @Override
    public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos pAt) {
        // Neo: Encapsulate the vanilla check logic to supply to the CanPlayerSleepEvent
        var vanillaResult = ((java.util.function.Supplier<Either<BedSleepingProblem, Unit>>) () -> {
        // Guard against modded beds that may not have the FACING property.
        // We just return success (Unit) here. Modders will need to implement conditions in the CanPlayerSleepEvent
        if (!this.level().getBlockState(pAt).hasProperty(HorizontalDirectionalBlock.FACING)) {
            return Either.right(Unit.INSTANCE);
        }

        // Start vanilla code
        Direction direction = this.level().getBlockState(pAt).getValue(HorizontalDirectionalBlock.FACING);
        if (this.isSleeping() || !this.isAlive()) {
            return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM);
        } else if (!this.level().dimensionType().natural()) {
            return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
        } else if (!this.bedInRange(pAt, direction)) {
            return Either.left(Player.BedSleepingProblem.TOO_FAR_AWAY);
        } else if (this.bedBlocked(pAt, direction)) {
            return Either.left(Player.BedSleepingProblem.OBSTRUCTED);
        } else {
            this.setRespawnPosition(this.level().dimension(), pAt, this.getYRot(), false, true);
            if (this.level().isDay()) {
                return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            } else {
                if (!this.isCreative()) {
                    double d0 = 8.0;
                    double d1 = 5.0;
                    Vec3 vec3 = Vec3.atBottomCenterOf(pAt);
                    List<Monster> list = this.level()
                        .getEntitiesOfClass(
                            Monster.class,
                            new AABB(vec3.x() - 8.0, vec3.y() - 5.0, vec3.z() - 8.0, vec3.x() + 8.0, vec3.y() + 5.0, vec3.z() + 8.0),
                            p_9062_ -> p_9062_.isPreventingPlayerRest(this)
                        );
                    if (!list.isEmpty()) {
                        return Either.left(Player.BedSleepingProblem.NOT_SAFE);
                    }
                }
        // End vanilla code
            }
        }
        return Either.right(Unit.INSTANCE);
        }).get();

        // Fire the event. Return the error if one exists after the event, otherwise use the vanilla logic to start sleeping.
        vanillaResult = net.neoforged.neoforge.event.EventHooks.canPlayerStartSleeping(this, pAt, vanillaResult);
        if (vanillaResult.left().isPresent()) {
            return vanillaResult;
        }

        {
            {
                // Start vanilla code
                Either<Player.BedSleepingProblem, Unit> either = super.startSleepInBed(pAt).ifRight(p_9029_ -> {
                    this.awardStat(Stats.SLEEP_IN_BED);
                    CriteriaTriggers.SLEPT_IN_BED.trigger(this);
                });
                if (!this.serverLevel().canSleepThroughNights()) {
                    this.displayClientMessage(Component.translatable("sleep.not_possible"), true);
                }

                ((ServerLevel)this.level()).updateSleepingPlayerList();
                return either;
            }
        }
    }

    @Override
    public void startSleeping(BlockPos pPos) {
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        super.startSleeping(pPos);
    }

    private boolean bedInRange(BlockPos pPos, Direction pDirection) {
        if (pDirection == null) return false;
        return this.isReachableBedBlock(pPos) || this.isReachableBedBlock(pPos.relative(pDirection.getOpposite()));
    }

    private boolean isReachableBedBlock(BlockPos pPos) {
        Vec3 vec3 = Vec3.atBottomCenterOf(pPos);
        return Math.abs(this.getX() - vec3.x()) <= 3.0 && Math.abs(this.getY() - vec3.y()) <= 2.0 && Math.abs(this.getZ() - vec3.z()) <= 3.0;
    }

    private boolean bedBlocked(BlockPos pPos, Direction pDirection) {
        BlockPos blockpos = pPos.above();
        return !this.freeAt(blockpos) || !this.freeAt(blockpos.relative(pDirection.getOpposite()));
    }

    @Override
    public void stopSleepInBed(boolean pWakeImmediately, boolean pUpdateLevelForSleepingPlayers) {
        if (this.isSleeping()) {
            this.serverLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(this, 2));
        }

        super.stopSleepInBed(pWakeImmediately, pUpdateLevelForSleepingPlayers);
        if (this.connection != null) {
            this.connection.teleport(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        }
    }

    @Override
    public void dismountTo(double pX, double pY, double pZ) {
        this.removeVehicle();
        this.setPos(pX, pY, pZ);
    }

    /**
     * Returns whether this Entity is invulnerable to the given DamageSource.
     */
    @Override
    public boolean isInvulnerableTo(DamageSource pSource) {
        return super.isInvulnerableTo(pSource) || this.isChangingDimension();
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {
    }

    @Override
    protected void onChangedBlock(ServerLevel p_346052_, BlockPos p_9206_) {
        if (!this.isSpectator()) {
            super.onChangedBlock(p_346052_, p_9206_);
        }
    }

    public void doCheckFallDamage(double pMovementX, double pMovementY, double pMovementZ, boolean pOnGround) {
        if (!this.touchingUnloadedChunk()) {
            this.checkSupportingBlock(pOnGround, new Vec3(pMovementX, pMovementY, pMovementZ));
            BlockPos blockpos = this.getOnPosLegacy();
            BlockState blockstate = this.level().getBlockState(blockpos);
            if (this.spawnExtraParticlesOnFall && pOnGround && this.fallDistance > 0.0F) {
                Vec3 vec3 = blockpos.getCenter().add(0.0, 0.5, 0.0);
                int i = (int)Mth.clamp(50.0F * this.fallDistance, 0.0F, 200.0F);
                this.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockstate), vec3.x, vec3.y, vec3.z, i, 0.3F, 0.3F, 0.3F, 0.15F);
                this.spawnExtraParticlesOnFall = false;
            }

            super.checkFallDamage(pMovementY, pOnGround, blockstate, blockpos);
        }
    }

    @Override
    public void onExplosionHit(@Nullable Entity pEntity) {
        super.onExplosionHit(pEntity);
        this.currentImpulseImpactPos = this.position();
        this.currentExplosionCause = pEntity;
        this.setIgnoreFallDamageFromCurrentImpulse(pEntity != null && pEntity.getType() == EntityType.WIND_CHARGE);
    }

    @Override
    protected void pushEntities() {
        if (this.level().tickRateManager().runsNormally()) {
            super.pushEntities();
        }
    }

    @Override
    public void openTextEdit(SignBlockEntity pSignEntity, boolean pIsFrontText) {
        this.connection.send(new ClientboundBlockUpdatePacket(this.level(), pSignEntity.getBlockPos()));
        this.connection.send(new ClientboundOpenSignEditorPacket(pSignEntity.getBlockPos(), pIsFrontText));
    }

    public void nextContainerCounter() {
        this.containerCounter = this.containerCounter % 100 + 1;
    }

    @Override
    public OptionalInt openMenu(@Nullable MenuProvider pMenu) {
        return openMenu(pMenu, (java.util.function.Consumer<net.minecraft.network.RegistryFriendlyByteBuf>) null);
    }

    @Override
    public OptionalInt openMenu(@Nullable MenuProvider pMenu, @Nullable java.util.function.Consumer<net.minecraft.network.RegistryFriendlyByteBuf> extraDataWriter) {
        if (pMenu == null) {
            return OptionalInt.empty();
        } else {
            if (this.containerMenu != this.inventoryMenu) {
                if (pMenu.shouldTriggerClientSideContainerClosingOnOpen())
                this.closeContainer();
                else
                    this.doCloseContainer();
            }

            this.nextContainerCounter();
            AbstractContainerMenu abstractcontainermenu = pMenu.createMenu(this.containerCounter, this.getInventory(), this);
            if (abstractcontainermenu == null) {
                if (this.isSpectator()) {
                    this.displayClientMessage(Component.translatable("container.spectatorCantOpen").withStyle(ChatFormatting.RED), true);
                }

                return OptionalInt.empty();
            } else {
                if (extraDataWriter == null) {
                this.connection
                    .send(new ClientboundOpenScreenPacket(abstractcontainermenu.containerId, abstractcontainermenu.getType(), pMenu.getDisplayName()));
                } else {
                    this.connection
                        .send(new net.neoforged.neoforge.network.payload.AdvancedOpenScreenPayload(abstractcontainermenu.containerId, abstractcontainermenu.getType(), pMenu.getDisplayName(), net.neoforged.neoforge.common.util.FriendlyByteBufUtil.writeCustomData(extraDataWriter, registryAccess())));
                }
                this.initMenu(abstractcontainermenu);
                this.containerMenu = abstractcontainermenu;
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open(this, this.containerMenu));
                return OptionalInt.of(this.containerCounter);
            }
        }
    }

    @Override
    public void sendMerchantOffers(int pContainerId, MerchantOffers pOffers, int pLevel, int pXp, boolean pShowProgress, boolean pCanRestock) {
        this.connection.send(new ClientboundMerchantOffersPacket(pContainerId, pOffers, pLevel, pXp, pShowProgress, pCanRestock));
    }

    @Override
    public void openHorseInventory(AbstractHorse pHorse, Container pInventory) {
        if (this.containerMenu != this.inventoryMenu) {
            this.closeContainer();
        }

        this.nextContainerCounter();
        int i = pHorse.getInventoryColumns();
        this.connection.send(new ClientboundHorseScreenOpenPacket(this.containerCounter, i, pHorse.getId()));
        this.containerMenu = new HorseInventoryMenu(this.containerCounter, this.getInventory(), pInventory, pHorse, i);
        this.initMenu(this.containerMenu);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open(this, this.containerMenu));
    }

    @Override
    public void openItemGui(ItemStack pStack, InteractionHand pHand) {
        if (pStack.is(Items.WRITTEN_BOOK)) {
            if (WrittenBookItem.resolveBookComponents(pStack, this.createCommandSourceStack(), this)) {
                this.containerMenu.broadcastChanges();
            }

            this.connection.send(new ClientboundOpenBookPacket(pHand));
        }
    }

    @Override
    public void openCommandBlock(CommandBlockEntity pCommandBlock) {
        this.connection.send(ClientboundBlockEntityDataPacket.create(pCommandBlock, BlockEntity::saveCustomOnly));
    }

    @Override
    public void closeContainer() {
        this.connection.send(new ClientboundContainerClosePacket(this.containerMenu.containerId));
        this.doCloseContainer();
    }

    @Override
    public void doCloseContainer() {
        this.containerMenu.removed(this);
        this.inventoryMenu.transferState(this.containerMenu);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close(this, this.containerMenu));
        this.containerMenu = this.inventoryMenu;
    }

    public void setPlayerInput(float pStrafe, float pForward, boolean pJumping, boolean pSneaking) {
        if (this.isPassenger()) {
            if (pStrafe >= -1.0F && pStrafe <= 1.0F) {
                this.xxa = pStrafe;
            }

            if (pForward >= -1.0F && pForward <= 1.0F) {
                this.zza = pForward;
            }

            this.jumping = pJumping;
            this.setShiftKeyDown(pSneaking);
        }
    }

    @Override
    public void travel(Vec3 pTravelVector) {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.travel(pTravelVector);
        this.checkMovementStatistics(this.getX() - d0, this.getY() - d1, this.getZ() - d2);
    }

    @Override
    public void rideTick() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.rideTick();
        this.checkRidingStatistics(this.getX() - d0, this.getY() - d1, this.getZ() - d2);
    }

    public void checkMovementStatistics(double pDx, double pDy, double pDz) {
        if (!this.isPassenger() && !didNotMove(pDx, pDy, pDz)) {
            if (this.isSwimming()) {
                int i = Math.round((float)Math.sqrt(pDx * pDx + pDy * pDy + pDz * pDz) * 100.0F);
                if (i > 0) {
                    this.awardStat(Stats.SWIM_ONE_CM, i);
                    this.causeFoodExhaustion(0.01F * (float)i * 0.01F);
                }
            } else if (this.isEyeInFluid(FluidTags.WATER)) {
                int j = Math.round((float)Math.sqrt(pDx * pDx + pDy * pDy + pDz * pDz) * 100.0F);
                if (j > 0) {
                    this.awardStat(Stats.WALK_UNDER_WATER_ONE_CM, j);
                    this.causeFoodExhaustion(0.01F * (float)j * 0.01F);
                }
            } else if (this.isInWater()) {
                int k = Math.round((float)Math.sqrt(pDx * pDx + pDz * pDz) * 100.0F);
                if (k > 0) {
                    this.awardStat(Stats.WALK_ON_WATER_ONE_CM, k);
                    this.causeFoodExhaustion(0.01F * (float)k * 0.01F);
                }
            } else if (this.onClimbable()) {
                if (pDy > 0.0) {
                    this.awardStat(Stats.CLIMB_ONE_CM, (int)Math.round(pDy * 100.0));
                }
            } else if (this.onGround()) {
                int l = Math.round((float)Math.sqrt(pDx * pDx + pDz * pDz) * 100.0F);
                if (l > 0) {
                    if (this.isSprinting()) {
                        this.awardStat(Stats.SPRINT_ONE_CM, l);
                        this.causeFoodExhaustion(0.1F * (float)l * 0.01F);
                    } else if (this.isCrouching()) {
                        this.awardStat(Stats.CROUCH_ONE_CM, l);
                        this.causeFoodExhaustion(0.0F * (float)l * 0.01F);
                    } else {
                        this.awardStat(Stats.WALK_ONE_CM, l);
                        this.causeFoodExhaustion(0.0F * (float)l * 0.01F);
                    }
                }
            } else if (this.isFallFlying()) {
                int i1 = Math.round((float)Math.sqrt(pDx * pDx + pDy * pDy + pDz * pDz) * 100.0F);
                this.awardStat(Stats.AVIATE_ONE_CM, i1);
            } else {
                int j1 = Math.round((float)Math.sqrt(pDx * pDx + pDz * pDz) * 100.0F);
                if (j1 > 25) {
                    this.awardStat(Stats.FLY_ONE_CM, j1);
                }
            }
        }
    }

    private void checkRidingStatistics(double pDx, double pDy, double pDz) {
        if (this.isPassenger() && !didNotMove(pDx, pDy, pDz)) {
            int i = Math.round((float)Math.sqrt(pDx * pDx + pDy * pDy + pDz * pDz) * 100.0F);
            Entity entity = this.getVehicle();
            if (entity instanceof AbstractMinecart) {
                this.awardStat(Stats.MINECART_ONE_CM, i);
            } else if (entity instanceof Boat) {
                this.awardStat(Stats.BOAT_ONE_CM, i);
            } else if (entity instanceof Pig) {
                this.awardStat(Stats.PIG_ONE_CM, i);
            } else if (entity instanceof AbstractHorse) {
                this.awardStat(Stats.HORSE_ONE_CM, i);
            } else if (entity instanceof Strider) {
                this.awardStat(Stats.STRIDER_ONE_CM, i);
            }
        }
    }

    private static boolean didNotMove(double pDx, double pDy, double pDz) {
        return pDx == 0.0 && pDy == 0.0 && pDz == 0.0;
    }

    /**
     * Adds a value to a statistic field.
     */
    @Override
    public void awardStat(Stat<?> pStat, int pAmount) {
        this.stats.increment(this, pStat, pAmount);
        this.getScoreboard().forAllObjectives(pStat, this, p_313587_ -> p_313587_.add(pAmount));
    }

    @Override
    public void resetStat(Stat<?> pStat) {
        this.stats.setValue(this, pStat, 0);
        this.getScoreboard().forAllObjectives(pStat, this, ScoreAccess::reset);
    }

    @Override
    public int awardRecipes(Collection<RecipeHolder<?>> pRecipes) {
        return this.recipeBook.addRecipes(pRecipes, this);
    }

    @Override
    public void triggerRecipeCrafted(RecipeHolder<?> pRecipe, List<ItemStack> pItems) {
        CriteriaTriggers.RECIPE_CRAFTED.trigger(this, pRecipe.id(), pItems);
    }

    @Override
    public void awardRecipesByKey(List<ResourceLocation> pRecipes) {
        List<RecipeHolder<?>> list = pRecipes.stream()
            .flatMap(p_311549_ -> this.server.getRecipeManager().byKey(p_311549_).stream())
            .collect(Collectors.toList());
        this.awardRecipes(list);
    }

    @Override
    public int resetRecipes(Collection<RecipeHolder<?>> pRecipes) {
        return this.recipeBook.removeRecipes(pRecipes, this);
    }

    @Override
    public void giveExperiencePoints(int pXpPoints) {
        super.giveExperiencePoints(pXpPoints);
        this.lastSentExp = -1;
    }

    public void disconnect() {
        this.disconnected = true;
        this.ejectPassengers();
        if (this.isSleeping()) {
            this.stopSleepInBed(true, false);
        }
    }

    public boolean hasDisconnected() {
        return this.disconnected;
    }

    public void resetSentInfo() {
        this.lastSentHealth = -1.0E8F;
    }

    @Override
    public void displayClientMessage(Component pChatComponent, boolean pActionBar) {
        this.sendSystemMessage(pChatComponent, pActionBar);
    }

    @Override
    protected void completeUsingItem() {
        if (!this.useItem.isEmpty() && this.isUsingItem()) {
            this.connection.send(new ClientboundEntityEventPacket(this, (byte)9));
            super.completeUsingItem();
        }
    }

    @Override
    public void lookAt(EntityAnchorArgument.Anchor pAnchor, Vec3 pTarget) {
        super.lookAt(pAnchor, pTarget);
        this.connection.send(new ClientboundPlayerLookAtPacket(pAnchor, pTarget.x, pTarget.y, pTarget.z));
    }

    public void lookAt(EntityAnchorArgument.Anchor pFromAnchor, Entity pEntity, EntityAnchorArgument.Anchor pToAnchor) {
        Vec3 vec3 = pToAnchor.apply(pEntity);
        super.lookAt(pFromAnchor, vec3);
        this.connection.send(new ClientboundPlayerLookAtPacket(pFromAnchor, pEntity, pToAnchor));
    }

    public void restoreFrom(ServerPlayer pThat, boolean pKeepEverything) {
        this.wardenSpawnTracker = pThat.wardenSpawnTracker;
        this.chatSession = pThat.chatSession;
        this.gameMode.setGameModeForPlayer(pThat.gameMode.getGameModeForPlayer(), pThat.gameMode.getPreviousGameModeForPlayer());
        this.onUpdateAbilities();
        this.getAttributes().assignBaseValues(pThat.getAttributes());
        this.setHealth(this.getMaxHealth());
        if (pKeepEverything) {
            this.getInventory().replaceWith(pThat.getInventory());
            this.setHealth(pThat.getHealth());
            this.foodData = pThat.foodData;

            for (MobEffectInstance mobeffectinstance : pThat.getActiveEffects()) {
                this.addEffect(new MobEffectInstance(mobeffectinstance));
            }

            this.experienceLevel = pThat.experienceLevel;
            this.totalExperience = pThat.totalExperience;
            this.experienceProgress = pThat.experienceProgress;
            this.setScore(pThat.getScore());
            this.portalProcess = pThat.portalProcess;
        } else if (this.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || pThat.isSpectator()) {
            this.getInventory().replaceWith(pThat.getInventory());
            this.experienceLevel = pThat.experienceLevel;
            this.totalExperience = pThat.totalExperience;
            this.experienceProgress = pThat.experienceProgress;
            this.setScore(pThat.getScore());
        }

        this.enchantmentSeed = pThat.enchantmentSeed;
        this.enderChestInventory = pThat.enderChestInventory;
        this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, pThat.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
        this.lastSentExp = -1;
        this.lastSentHealth = -1.0F;
        this.lastSentFood = -1;
        this.recipeBook.copyOverData(pThat.recipeBook);
        this.seenCredits = pThat.seenCredits;
        this.enteredNetherPosition = pThat.enteredNetherPosition;
        this.chunkTrackingView = pThat.chunkTrackingView;
        this.setShoulderEntityLeft(pThat.getShoulderEntityLeft());
        this.setShoulderEntityRight(pThat.getShoulderEntityRight());
        this.setLastDeathLocation(pThat.getLastDeathLocation());

        //Copy over a section of the Entity Data from the old player.
        //Allows mods to specify data that persists after players respawn.
        CompoundTag old = pThat.getPersistentData();
        if (old.contains(PERSISTED_NBT_TAG))
             getPersistentData().put(PERSISTED_NBT_TAG, old.get(PERSISTED_NBT_TAG));
        net.neoforged.neoforge.event.EventHooks.onPlayerClone(this, pThat, !pKeepEverything);
        this.tabListHeader = pThat.tabListHeader;
        this.tabListFooter = pThat.tabListFooter;
    }

    @Override
    protected void onEffectAdded(MobEffectInstance pEffectInstance, @Nullable Entity pEntity) {
        super.onEffectAdded(pEffectInstance, pEntity);
        this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), pEffectInstance, true));
        if (pEffectInstance.is(MobEffects.LEVITATION)) {
            this.levitationStartTime = this.tickCount;
            this.levitationStartPos = this.position();
        }

        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, pEntity);
    }

    @Override
    protected void onEffectUpdated(MobEffectInstance pEffectInstance, boolean pForced, @Nullable Entity pEntity) {
        super.onEffectUpdated(pEffectInstance, pForced, pEntity);
        this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), pEffectInstance, false));
        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, pEntity);
    }

    @Override
    protected void onEffectRemoved(MobEffectInstance pEffect) {
        super.onEffectRemoved(pEffect);
        this.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), pEffect.getEffect()));
        if (pEffect.is(MobEffects.LEVITATION)) {
            this.levitationStartPos = null;
        }

        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, null);
    }

    /**
     * Sets the position of the entity and updates the 'last' variables
     */
    @Override
    public void teleportTo(double pX, double pY, double pZ) {
        this.connection.teleport(pX, pY, pZ, this.getYRot(), this.getXRot(), RelativeMovement.ROTATION);
    }

    @Override
    public void teleportRelative(double pDx, double pDy, double pDz) {
        this.connection
            .teleport(this.getX() + pDx, this.getY() + pDy, this.getZ() + pDz, this.getYRot(), this.getXRot(), RelativeMovement.ALL);
    }

    @Override
    public boolean teleportTo(
        ServerLevel pLevel, double pX, double pY, double pZ, Set<RelativeMovement> pRelativeMovements, float pYRot, float pXRot
    ) {
        ChunkPos chunkpos = new ChunkPos(BlockPos.containing(pX, pY, pZ));
        pLevel.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkpos, 1, this.getId());
        this.stopRiding();
        if (this.isSleeping()) {
            this.stopSleepInBed(true, true);
        }

        if (pLevel == this.level()) {
            this.connection.teleport(pX, pY, pZ, pYRot, pXRot, pRelativeMovements);
        } else {
            this.teleportTo(pLevel, pX, pY, pZ, pYRot, pXRot);
        }

        this.setYHeadRot(pYRot);
        return true;
    }

    @Override
    public void moveTo(double pX, double pY, double pZ) {
        super.moveTo(pX, pY, pZ);
        this.connection.resetPosition();
    }

    /**
     * Called when the entity is dealt a critical hit.
     */
    @Override
    public void crit(Entity pEntityHit) {
        this.serverLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(pEntityHit, 4));
    }

    @Override
    public void magicCrit(Entity pEntityHit) {
        this.serverLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(pEntityHit, 5));
    }

    @Override
    public void onUpdateAbilities() {
        if (this.connection != null) {
            this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
            this.updateInvisibilityStatus();
        }
    }

    public ServerLevel serverLevel() {
        return (ServerLevel)this.level();
    }

    public boolean setGameMode(GameType pGameMode) {
        boolean flag = this.isSpectator();
        pGameMode = net.neoforged.neoforge.common.CommonHooks.onChangeGameType(this, this.gameMode.getGameModeForPlayer(), pGameMode);
        if (pGameMode == null) return false;
        if (!this.gameMode.changeGameModeForPlayer(pGameMode)) {
            return false;
        } else {
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, (float)pGameMode.getId()));
            if (pGameMode == GameType.SPECTATOR) {
                this.removeEntitiesOnShoulder();
                this.stopRiding();
                EnchantmentHelper.stopLocationBasedEffects(this);
            } else {
                this.setCamera(this);
                if (flag) {
                    EnchantmentHelper.runLocationChangedEffects(this.serverLevel(), this);
                }
            }

            this.onUpdateAbilities();
            this.updateEffectVisibility();
            return true;
        }
    }

    @Override
    public boolean isSpectator() {
        return this.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
    }

    @Override
    public boolean isCreative() {
        return this.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
    }

    @Override
    public void sendSystemMessage(Component pComponent) {
        this.sendSystemMessage(pComponent, false);
    }

    public void sendSystemMessage(Component pComponent, boolean pBypassHiddenChat) {
        if (this.acceptsSystemMessages(pBypassHiddenChat)) {
            this.connection
                .send(
                    new ClientboundSystemChatPacket(pComponent, pBypassHiddenChat),
                    PacketSendListener.exceptionallySend(
                        () -> {
                            if (this.acceptsSystemMessages(false)) {
                                int i = 256;
                                String s = pComponent.getString(256);
                                Component component = Component.literal(s).withStyle(ChatFormatting.YELLOW);
                                return new ClientboundSystemChatPacket(
                                    Component.translatable("multiplayer.message_not_delivered", component).withStyle(ChatFormatting.RED), false
                                );
                            } else {
                                return null;
                            }
                        }
                    )
                );
        }
    }

    public void sendChatMessage(OutgoingChatMessage pMessage, boolean pFiltered, ChatType.Bound pBoundType) {
        if (this.acceptsChatMessages()) {
            pMessage.sendToPlayer(this, pFiltered, pBoundType);
        }
    }

    public String getIpAddress() {
        return this.connection.getRemoteAddress() instanceof InetSocketAddress inetsocketaddress
            ? InetAddresses.toAddrString(inetsocketaddress.getAddress())
            : "<unknown>";
    }

    public void updateOptions(ClientInformation pClientInformation) {
        this.language = pClientInformation.language();
        this.requestedViewDistance = pClientInformation.viewDistance();
        this.chatVisibility = pClientInformation.chatVisibility();
        this.canChatColor = pClientInformation.chatColors();
        this.textFilteringEnabled = pClientInformation.textFilteringEnabled();
        this.allowsListing = pClientInformation.allowsListing();
        this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte)pClientInformation.modelCustomisation());
        this.getEntityData().set(DATA_PLAYER_MAIN_HAND, (byte)pClientInformation.mainHand().getId());
    }

    public ClientInformation clientInformation() {
        int i = this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION);
        HumanoidArm humanoidarm = HumanoidArm.BY_ID.apply(this.getEntityData().get(DATA_PLAYER_MAIN_HAND));
        return new ClientInformation(
            this.language, this.requestedViewDistance, this.chatVisibility, this.canChatColor, i, humanoidarm, this.textFilteringEnabled, this.allowsListing
        );
    }

    public boolean canChatInColor() {
        return this.canChatColor;
    }

    public ChatVisiblity getChatVisibility() {
        return this.chatVisibility;
    }

    private boolean acceptsSystemMessages(boolean pBypassHiddenChat) {
        return this.chatVisibility == ChatVisiblity.HIDDEN ? pBypassHiddenChat : true;
    }

    private boolean acceptsChatMessages() {
        return this.chatVisibility == ChatVisiblity.FULL;
    }

    public int requestedViewDistance() {
        return this.requestedViewDistance;
    }

    public void sendServerStatus(ServerStatus pServerStatus) {
        this.connection.send(new ClientboundServerDataPacket(pServerStatus.description(), pServerStatus.favicon().map(ServerStatus.Favicon::iconBytes)));
    }

    @Override
    protected int getPermissionLevel() {
        return this.server.getProfilePermissions(this.getGameProfile());
    }

    public void resetLastActionTime() {
        this.lastActionTime = Util.getMillis();
    }

    public ServerStatsCounter getStats() {
        return this.stats;
    }

    public ServerRecipeBook getRecipeBook() {
        return this.recipeBook;
    }

    @Override
    protected void updateInvisibilityStatus() {
        if (this.isSpectator()) {
            this.removeEffectParticles();
            this.setInvisible(true);
        } else {
            super.updateInvisibilityStatus();
        }
    }

    public Entity getCamera() {
        return (Entity)(this.camera == null ? this : this.camera);
    }

    public void setCamera(@Nullable Entity pEntityToSpectate) {
        Entity entity = this.getCamera();
        this.camera = (Entity)(pEntityToSpectate == null ? this : pEntityToSpectate);
        while (this.camera instanceof net.neoforged.neoforge.entity.PartEntity<?> partEntity) this.camera = partEntity.getParent(); // Neo: fix MC-46486
        if (entity != this.camera) {
            if (this.camera.level() instanceof ServerLevel serverlevel) {
                this.teleportTo(serverlevel, this.camera.getX(), this.camera.getY(), this.camera.getZ(), Set.of(), this.getYRot(), this.getXRot());
            }

            if (pEntityToSpectate != null) {
                this.serverLevel().getChunkSource().move(this);
            }

            this.connection.send(new ClientboundSetCameraPacket(this.camera));
            this.connection.resetPosition();
        }
    }

    @Override
    protected void processPortalCooldown() {
        if (!this.isChangingDimension) {
            super.processPortalCooldown();
        }
    }

    /**
     * Attacks for the player the targeted entity with the currently equipped item.  The equipped item has hitEntity called on it. Args: targetEntity
     */
    @Override
    public void attack(Entity pTargetEntity) {
        if (this.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            this.setCamera(pTargetEntity);
        } else {
            super.attack(pTargetEntity);
        }
    }

    public long getLastActionTime() {
        return this.lastActionTime;
    }

    @Nullable
    public Component getTabListDisplayName() {
        if (!this.hasTabListName) {
            this.tabListDisplayName = net.neoforged.neoforge.event.EventHooks.getPlayerTabListDisplayName(this);
            this.hasTabListName = true;
        }
        return this.tabListDisplayName;
    }

    @Override
    public void swing(InteractionHand pHand) {
        super.swing(pHand);
        this.resetAttackStrengthTicker();
    }

    public boolean isChangingDimension() {
        return this.isChangingDimension;
    }

    public void hasChangedDimension() {
        this.isChangingDimension = false;
    }

    public PlayerAdvancements getAdvancements() {
        return this.advancements;
    }

    public void teleportTo(ServerLevel pNewLevel, double pX, double pY, double pZ, float pYaw, float pPitch) {
        this.setCamera(this);
        this.stopRiding();
        if (pNewLevel == this.level()) {
            this.connection.teleport(pX, pY, pZ, pYaw, pPitch);
        } else {
            this.changeDimension(
                new DimensionTransition(pNewLevel, new Vec3(pX, pY, pZ), Vec3.ZERO, pYaw, pPitch, DimensionTransition.DO_NOTHING)
            );
        }
    }

    @Nullable
    public BlockPos getRespawnPosition() {
        return this.respawnPosition;
    }

    public float getRespawnAngle() {
        return this.respawnAngle;
    }

    public ResourceKey<Level> getRespawnDimension() {
        return this.respawnDimension;
    }

    public boolean isRespawnForced() {
        return this.respawnForced;
    }

    public void copyRespawnPosition(ServerPlayer p_348642_) {
        this.setRespawnPosition(
            p_348642_.getRespawnDimension(), p_348642_.getRespawnPosition(), p_348642_.getRespawnAngle(), p_348642_.isRespawnForced(), false
        );
    }

    public void setRespawnPosition(ResourceKey<Level> pDimension, @Nullable BlockPos pPosition, float pAngle, boolean pForced, boolean pSendMessage) {
        if (net.neoforged.neoforge.event.EventHooks.onPlayerSpawnSet(this, pPosition == null ? Level.OVERWORLD : pDimension, pPosition, pForced)) return;
        if (pPosition != null) {
            boolean flag = pPosition.equals(this.respawnPosition) && pDimension.equals(this.respawnDimension);
            if (pSendMessage && !flag) {
                this.sendSystemMessage(Component.translatable("block.minecraft.set_spawn"));
            }

            this.respawnPosition = pPosition;
            this.respawnDimension = pDimension;
            this.respawnAngle = pAngle;
            this.respawnForced = pForced;
        } else {
            this.respawnPosition = null;
            this.respawnDimension = Level.OVERWORLD;
            this.respawnAngle = 0.0F;
            this.respawnForced = false;
        }
    }

    public SectionPos getLastSectionPos() {
        return this.lastSectionPos;
    }

    public void setLastSectionPos(SectionPos pSectionPos) {
        this.lastSectionPos = pSectionPos;
    }

    public ChunkTrackingView getChunkTrackingView() {
        return this.chunkTrackingView;
    }

    public void setChunkTrackingView(ChunkTrackingView pChunkTrackingView) {
        this.chunkTrackingView = pChunkTrackingView;
    }

    @Override
    public void playNotifySound(SoundEvent pSound, SoundSource pSource, float pVolume, float pPitch) {
        this.connection
            .send(
                new ClientboundSoundPacket(
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(pSound),
                    pSource,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    pVolume,
                    pPitch,
                    this.random.nextLong()
                )
            );
    }

    /**
     * Creates and drops the provided item. Depending on the dropAround, it will drop the item around the player, instead of dropping the item from where the player is pointing at. Likewise, if includeThrowerName is true, the dropped item entity will have the thrower set as the player.
     */
    @Override
    public ItemEntity drop(ItemStack pDroppedItem, boolean pDropAround, boolean pTraceItem) {
        ItemEntity itementity = super.drop(pDroppedItem, pDropAround, pTraceItem);
        if (itementity == null) {
            return null;
        } else {
            if (captureDrops() != null) captureDrops().add(itementity);
            else
            this.level().addFreshEntity(itementity);
            ItemStack itemstack = itementity.getItem();
            if (pTraceItem) {
                if (!itemstack.isEmpty()) {
                    this.awardStat(Stats.ITEM_DROPPED.get(itemstack.getItem()), pDroppedItem.getCount());
                }

                this.awardStat(Stats.DROP);
            }

            return itementity;
        }
    }

    /**
     * Returns the language last reported by the player as their local language.
     * Defaults to en_us if the value is unknown.
     */
    public String getLanguage() {
        return this.language;
    }

    private Component tabListHeader = Component.empty();
    private Component tabListFooter = Component.empty();

    public Component getTabListHeader() {
         return this.tabListHeader;
    }

    /**
     * Set the tab list header while preserving the footer.
     *
     * @param header the new header, or {@link Component#empty()} to clear
     */
    public void setTabListHeader(final Component header) {
         this.setTabListHeaderFooter(header, this.tabListFooter);
    }

    public Component getTabListFooter() {
         return this.tabListFooter;
    }

    /**
     * Set the tab list footer while preserving the header.
     *
     * @param footer the new footer, or {@link Component#empty()} to clear
     */
    public void setTabListFooter(final Component footer) {
         this.setTabListHeaderFooter(this.tabListHeader, footer);
    }

    /**
     * Set the tab list header and footer at once.
     *
     * @param header the new header, or {@link Component#empty()} to clear
     * @param footer the new footer, or {@link Component#empty()} to clear
     */
    public void setTabListHeaderFooter(final Component header, final Component footer) {
         if (java.util.Objects.equals(header, this.tabListHeader)
              && java.util.Objects.equals(footer, this.tabListFooter)) {
              return;
         }

         this.tabListHeader = java.util.Objects.requireNonNull(header, "header");
         this.tabListFooter = java.util.Objects.requireNonNull(footer, "footer");

         this.connection.send(new net.minecraft.network.protocol.game.ClientboundTabListPacket(header, footer));
    }

    // We need this as tablistDisplayname may be null even if the event was fired.
    private boolean hasTabListName = false;
    private Component tabListDisplayName = null;
    /**
     * Force the name displayed in the tab list to refresh, by firing {@link net.neoforged.neoforge.event.entity.player.PlayerEvent.TabListNameFormat}.
     */
    public void refreshTabListName() {
        Component oldName = this.tabListDisplayName;
        this.tabListDisplayName = net.neoforged.neoforge.event.EventHooks.getPlayerTabListDisplayName(this);
        if (!java.util.Objects.equals(oldName, this.tabListDisplayName)) {
            this.getServer().getPlayerList().broadcastAll(new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, this));
        }
    }

    public TextFilter getTextFilter() {
        return this.textFilter;
    }

    public void setServerLevel(ServerLevel pLevel) {
        this.setLevel(pLevel);
        this.gameMode.setLevel(pLevel);
    }

    @Nullable
    private static GameType readPlayerMode(@Nullable CompoundTag pTag, String pKey) {
        return pTag != null && pTag.contains(pKey, 99) ? GameType.byId(pTag.getInt(pKey)) : null;
    }

    private GameType calculateGameModeForNewPlayer(@Nullable GameType pGameType) {
        GameType gametype = this.server.getForcedGameType();
        if (gametype != null) {
            return gametype;
        } else {
            return pGameType != null ? pGameType : this.server.getDefaultGameType();
        }
    }

    public void loadGameTypes(@Nullable CompoundTag pTag) {
        this.gameMode
            .setGameModeForPlayer(
                this.calculateGameModeForNewPlayer(readPlayerMode(pTag, "playerGameType")), readPlayerMode(pTag, "previousPlayerGameType")
            );
    }

    private void storeGameTypes(CompoundTag pTag) {
        pTag.putInt("playerGameType", this.gameMode.getGameModeForPlayer().getId());
        GameType gametype = this.gameMode.getPreviousGameModeForPlayer();
        if (gametype != null) {
            pTag.putInt("previousPlayerGameType", gametype.getId());
        }
    }

    @Override
    public boolean isTextFilteringEnabled() {
        return this.textFilteringEnabled;
    }

    public boolean shouldFilterMessageTo(ServerPlayer pPlayer) {
        return pPlayer == this ? false : this.textFilteringEnabled || pPlayer.textFilteringEnabled;
    }

    @Override
    public boolean mayInteract(Level pLevel, BlockPos pPos) {
        return super.mayInteract(pLevel, pPos) && pLevel.mayInteract(this, pPos);
    }

    @Override
    protected void updateUsingItem(ItemStack pUsingItem) {
        CriteriaTriggers.USING_ITEM.trigger(this, pUsingItem);
        super.updateUsingItem(pUsingItem);
    }

    /**
     * @param pDropStack Whether to drop the entire stack of items. If {@code false},
     *                   drops a single item.
     */
    public boolean drop(boolean pDropStack) {
        Inventory inventory = this.getInventory();
        ItemStack selected = inventory.getSelected();
        if (selected.isEmpty() || !selected.onDroppedByPlayer(this)) return false;
        if (isUsingItem() && getUsedItemHand() == InteractionHand.MAIN_HAND && (pDropStack || selected.getCount() == 1)) stopUsingItem(); // Forge: fix MC-231097 on the serverside
        ItemStack itemstack = inventory.removeFromSelected(pDropStack);
        this.containerMenu.findSlot(inventory, inventory.selected).ifPresent(p_287377_ -> this.containerMenu.setRemoteSlot(p_287377_, inventory.getSelected()));
        return net.neoforged.neoforge.common.CommonHooks.onPlayerTossEvent(this, itemstack, true) != null;
    }

    public boolean allowsListing() {
        return this.allowsListing;
    }

    @Override
    public Optional<WardenSpawnTracker> getWardenSpawnTracker() {
        return Optional.of(this.wardenSpawnTracker);
    }

    public void setSpawnExtraParticlesOnFall(boolean pSpawnExtraParticlesOnFall) {
        this.spawnExtraParticlesOnFall = pSpawnExtraParticlesOnFall;
    }

    @Override
    public void onItemPickup(ItemEntity pItemEntity) {
        super.onItemPickup(pItemEntity);
        Entity entity = pItemEntity.getOwner();
        if (entity != null) {
            CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_PLAYER.trigger(this, pItemEntity.getItem(), entity);
        }
    }

    public void setChatSession(RemoteChatSession pChatSession) {
        this.chatSession = pChatSession;
    }

    @Nullable
    public RemoteChatSession getChatSession() {
        return this.chatSession != null && this.chatSession.hasExpired() ? null : this.chatSession;
    }

    @Override
    public void indicateDamage(double pXDistance, double pZDistance) {
        this.hurtDir = (float)(Mth.atan2(pZDistance, pXDistance) * 180.0F / (float)Math.PI - (double)this.getYRot());
        this.connection.send(new ClientboundHurtAnimationPacket(this));
    }

    @Override
    public boolean startRiding(Entity pVehicle, boolean pForce) {
        if (super.startRiding(pVehicle, pForce)) {
            this.setKnownMovement(Vec3.ZERO);
            pVehicle.positionRider(this);
            this.connection.teleport(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            if (pVehicle instanceof LivingEntity livingentity) {
                this.server.getPlayerList().sendActiveEffects(livingentity, this.connection);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void stopRiding() {
        Entity entity = this.getVehicle();
        super.stopRiding();
        if (entity instanceof LivingEntity livingentity) {
            for (MobEffectInstance mobeffectinstance : livingentity.getActiveEffects()) {
                this.connection.send(new ClientboundRemoveMobEffectPacket(entity.getId(), mobeffectinstance.getEffect()));
            }
        }
    }

    public CommonPlayerSpawnInfo createCommonSpawnInfo(ServerLevel pLevel) {
        return new CommonPlayerSpawnInfo(
            pLevel.dimensionTypeRegistration(),
            pLevel.dimension(),
            BiomeManager.obfuscateSeed(pLevel.getSeed()),
            this.gameMode.getGameModeForPlayer(),
            this.gameMode.getPreviousGameModeForPlayer(),
            pLevel.isDebug(),
            pLevel.isFlat(),
            this.getLastDeathLocation(),
            this.getPortalCooldown()
        );
    }

    public void setRaidOmenPosition(BlockPos pRaidOmenPosition) {
        this.raidOmenPosition = pRaidOmenPosition;
    }

    public void clearRaidOmenPosition() {
        this.raidOmenPosition = null;
    }

    @Nullable
    public BlockPos getRaidOmenPosition() {
        return this.raidOmenPosition;
    }

    @Override
    public Vec3 getKnownMovement() {
        Entity entity = this.getVehicle();
        return entity != null && entity.getControllingPassenger() != this ? entity.getKnownMovement() : this.lastKnownClientMovement;
    }

    public void setKnownMovement(Vec3 p_348509_) {
        this.lastKnownClientMovement = p_348509_;
    }

    @Override
    protected float getEnchantedDamage(Entity p_346252_, float p_346142_, DamageSource p_345841_) {
        return EnchantmentHelper.modifyDamage(this.serverLevel(), this.getWeaponItem(), p_346252_, p_345841_, p_346142_);
    }

    @Override
    public void onEquippedItemBroken(Item p_348565_, EquipmentSlot p_348623_) {
        super.onEquippedItemBroken(p_348565_, p_348623_);
        this.awardStat(Stats.ITEM_BROKEN.get(p_348565_));
    }

    public static record RespawnPosAngle(Vec3 position, float yaw) {
        public static ServerPlayer.RespawnPosAngle of(Vec3 p_348670_, BlockPos p_348504_) {
            return new ServerPlayer.RespawnPosAngle(p_348670_, calculateLookAtYaw(p_348670_, p_348504_));
        }

        private static float calculateLookAtYaw(Vec3 p_348686_, BlockPos p_348467_) {
            Vec3 vec3 = Vec3.atBottomCenterOf(p_348467_).subtract(p_348686_).normalize();
            return (float)Mth.wrapDegrees(Mth.atan2(vec3.z, vec3.x) * 180.0F / (float)Math.PI - 90.0);
        }
    }
}
