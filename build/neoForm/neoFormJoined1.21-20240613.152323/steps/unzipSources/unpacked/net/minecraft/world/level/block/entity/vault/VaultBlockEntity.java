package net.minecraft.world.level.block.entity.vault;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class VaultBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final VaultServerData serverData = new VaultServerData();
    private final VaultSharedData sharedData = new VaultSharedData();
    private final VaultClientData clientData = new VaultClientData();
    private VaultConfig config = VaultConfig.DEFAULT;

    public VaultBlockEntity(BlockPos pPos, BlockState pState) {
        super(BlockEntityType.VAULT, pPos, pState);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return Util.make(new CompoundTag(), p_330145_ -> p_330145_.put("shared_data", encode(VaultSharedData.CODEC, this.sharedData, pRegistries)));
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.put("config", encode(VaultConfig.CODEC, this.config, pRegistries));
        pTag.put("shared_data", encode(VaultSharedData.CODEC, this.sharedData, pRegistries));
        pTag.put("server_data", encode(VaultServerData.CODEC, this.serverData, pRegistries));
    }

    private static <T> Tag encode(Codec<T> pCodec, T pValue, HolderLookup.Provider pLevelRegistry) {
        return pCodec.encodeStart(pLevelRegistry.createSerializationContext(NbtOps.INSTANCE), pValue).getOrThrow();
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        DynamicOps<Tag> dynamicops = pRegistries.createSerializationContext(NbtOps.INSTANCE);
        if (pTag.contains("server_data")) {
            VaultServerData.CODEC.parse(dynamicops, pTag.get("server_data")).resultOrPartial(LOGGER::error).ifPresent(this.serverData::set);
        }

        if (pTag.contains("config")) {
            VaultConfig.CODEC.parse(dynamicops, pTag.get("config")).resultOrPartial(LOGGER::error).ifPresent(p_324546_ -> this.config = p_324546_);
        }

        if (pTag.contains("shared_data")) {
            VaultSharedData.CODEC.parse(dynamicops, pTag.get("shared_data")).resultOrPartial(LOGGER::error).ifPresent(this.sharedData::set);
        }
    }

    @Nullable
    public VaultServerData getServerData() {
        return this.level != null && !this.level.isClientSide ? this.serverData : null;
    }

    public VaultSharedData getSharedData() {
        return this.sharedData;
    }

    public VaultClientData getClientData() {
        return this.clientData;
    }

    public VaultConfig getConfig() {
        return this.config;
    }

    @VisibleForTesting
    public void setConfig(VaultConfig pConfig) {
        this.config = pConfig;
    }

    public static final class Client {
        private static final int PARTICLE_TICK_RATE = 20;
        private static final float IDLE_PARTICLE_CHANCE = 0.5F;
        private static final float AMBIENT_SOUND_CHANCE = 0.02F;
        private static final int ACTIVATION_PARTICLE_COUNT = 20;
        private static final int DEACTIVATION_PARTICLE_COUNT = 20;

        public static void tick(Level pLevel, BlockPos pPos, BlockState pState, VaultClientData pClientData, VaultSharedData pSharedData) {
            pClientData.updateDisplayItemSpin();
            if (pLevel.getGameTime() % 20L == 0L) {
                emitConnectionParticlesForNearbyPlayers(pLevel, pPos, pState, pSharedData);
            }

            emitIdleParticles(
                pLevel, pPos, pSharedData, pState.getValue(VaultBlock.OMINOUS) ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMALL_FLAME
            );
            playIdleSounds(pLevel, pPos, pSharedData);
        }

        public static void emitActivationParticles(
            Level pLevel, BlockPos pPos, BlockState pState, VaultSharedData pSharedData, ParticleOptions pParticle
        ) {
            emitConnectionParticlesForNearbyPlayers(pLevel, pPos, pState, pSharedData);
            RandomSource randomsource = pLevel.random;

            for (int i = 0; i < 20; i++) {
                Vec3 vec3 = randomPosInsideCage(pPos, randomsource);
                pLevel.addParticle(ParticleTypes.SMOKE, vec3.x(), vec3.y(), vec3.z(), 0.0, 0.0, 0.0);
                pLevel.addParticle(pParticle, vec3.x(), vec3.y(), vec3.z(), 0.0, 0.0, 0.0);
            }
        }

        public static void emitDeactivationParticles(Level pLevel, BlockPos pPos, ParticleOptions pParticle) {
            RandomSource randomsource = pLevel.random;

            for (int i = 0; i < 20; i++) {
                Vec3 vec3 = randomPosCenterOfCage(pPos, randomsource);
                Vec3 vec31 = new Vec3(randomsource.nextGaussian() * 0.02, randomsource.nextGaussian() * 0.02, randomsource.nextGaussian() * 0.02);
                pLevel.addParticle(pParticle, vec3.x(), vec3.y(), vec3.z(), vec31.x(), vec31.y(), vec31.z());
            }
        }

        private static void emitIdleParticles(Level pLevel, BlockPos pPos, VaultSharedData pSharedData, ParticleOptions pParticle) {
            RandomSource randomsource = pLevel.getRandom();
            if (randomsource.nextFloat() <= 0.5F) {
                Vec3 vec3 = randomPosInsideCage(pPos, randomsource);
                pLevel.addParticle(ParticleTypes.SMOKE, vec3.x(), vec3.y(), vec3.z(), 0.0, 0.0, 0.0);
                if (shouldDisplayActiveEffects(pSharedData)) {
                    pLevel.addParticle(pParticle, vec3.x(), vec3.y(), vec3.z(), 0.0, 0.0, 0.0);
                }
            }
        }

        private static void emitConnectionParticlesForPlayer(Level pLevel, Vec3 pPos, Player pPlayer) {
            RandomSource randomsource = pLevel.random;
            Vec3 vec3 = pPos.vectorTo(pPlayer.position().add(0.0, (double)(pPlayer.getBbHeight() / 2.0F), 0.0));
            int i = Mth.nextInt(randomsource, 2, 5);

            for (int j = 0; j < i; j++) {
                Vec3 vec31 = vec3.offsetRandom(randomsource, 1.0F);
                pLevel.addParticle(ParticleTypes.VAULT_CONNECTION, pPos.x(), pPos.y(), pPos.z(), vec31.x(), vec31.y(), vec31.z());
            }
        }

        private static void emitConnectionParticlesForNearbyPlayers(Level pLevel, BlockPos pPos, BlockState pState, VaultSharedData pSharedData) {
            Set<UUID> set = pSharedData.getConnectedPlayers();
            if (!set.isEmpty()) {
                Vec3 vec3 = keyholePos(pPos, pState.getValue(VaultBlock.FACING));

                for (UUID uuid : set) {
                    Player player = pLevel.getPlayerByUUID(uuid);
                    if (player != null && isWithinConnectionRange(pPos, pSharedData, player)) {
                        emitConnectionParticlesForPlayer(pLevel, vec3, player);
                    }
                }
            }
        }

        private static boolean isWithinConnectionRange(BlockPos pPos, VaultSharedData pSharedData, Player pPlayer) {
            return pPlayer.blockPosition().distSqr(pPos) <= Mth.square(pSharedData.connectedParticlesRange());
        }

        private static void playIdleSounds(Level pLevel, BlockPos pPos, VaultSharedData pSharedData) {
            if (shouldDisplayActiveEffects(pSharedData)) {
                RandomSource randomsource = pLevel.getRandom();
                if (randomsource.nextFloat() <= 0.02F) {
                    pLevel.playLocalSound(
                        pPos,
                        SoundEvents.VAULT_AMBIENT,
                        SoundSource.BLOCKS,
                        randomsource.nextFloat() * 0.25F + 0.75F,
                        randomsource.nextFloat() + 0.5F,
                        false
                    );
                }
            }
        }

        public static boolean shouldDisplayActiveEffects(VaultSharedData pSharedData) {
            return pSharedData.hasDisplayItem();
        }

        private static Vec3 randomPosCenterOfCage(BlockPos pPos, RandomSource pRandom) {
            return Vec3.atLowerCornerOf(pPos)
                .add(Mth.nextDouble(pRandom, 0.4, 0.6), Mth.nextDouble(pRandom, 0.4, 0.6), Mth.nextDouble(pRandom, 0.4, 0.6));
        }

        private static Vec3 randomPosInsideCage(BlockPos pPos, RandomSource pRandom) {
            return Vec3.atLowerCornerOf(pPos)
                .add(Mth.nextDouble(pRandom, 0.1, 0.9), Mth.nextDouble(pRandom, 0.25, 0.75), Mth.nextDouble(pRandom, 0.1, 0.9));
        }

        private static Vec3 keyholePos(BlockPos pPos, Direction pFacing) {
            return Vec3.atBottomCenterOf(pPos).add((double)pFacing.getStepX() * 0.5, 1.75, (double)pFacing.getStepZ() * 0.5);
        }
    }

    public static final class Server {
        private static final int UNLOCKING_DELAY_TICKS = 14;
        private static final int DISPLAY_CYCLE_TICK_RATE = 20;
        private static final int INSERT_FAIL_SOUND_BUFFER_TICKS = 15;

        public static void tick(
            ServerLevel pLevel, BlockPos pPos, BlockState pState, VaultConfig pConfig, VaultServerData pServerData, VaultSharedData pSharedData
        ) {
            VaultState vaultstate = pState.getValue(VaultBlock.STATE);
            if (shouldCycleDisplayItem(pLevel.getGameTime(), vaultstate)) {
                cycleDisplayItemFromLootTable(pLevel, vaultstate, pConfig, pSharedData, pPos);
            }

            BlockState blockstate = pState;
            if (pLevel.getGameTime() >= pServerData.stateUpdatingResumesAt()) {
                blockstate = pState.setValue(VaultBlock.STATE, vaultstate.tickAndGetNext(pLevel, pPos, pConfig, pServerData, pSharedData));
                if (!pState.equals(blockstate)) {
                    setVaultState(pLevel, pPos, pState, blockstate, pConfig, pSharedData);
                }
            }

            if (pServerData.isDirty || pSharedData.isDirty) {
                VaultBlockEntity.setChanged(pLevel, pPos, pState);
                if (pSharedData.isDirty) {
                    pLevel.sendBlockUpdated(pPos, pState, blockstate, 2);
                }

                pServerData.isDirty = false;
                pSharedData.isDirty = false;
            }
        }

        public static void tryInsertKey(
            ServerLevel pLevel,
            BlockPos pPos,
            BlockState pState,
            VaultConfig pConfig,
            VaultServerData pServerData,
            VaultSharedData pSharedData,
            Player pPlayer,
            ItemStack pStack
        ) {
            VaultState vaultstate = pState.getValue(VaultBlock.STATE);
            if (canEjectReward(pConfig, vaultstate)) {
                if (!isValidToInsert(pConfig, pStack)) {
                    playInsertFailSound(pLevel, pServerData, pPos, SoundEvents.VAULT_INSERT_ITEM_FAIL);
                } else if (pServerData.hasRewardedPlayer(pPlayer)) {
                    playInsertFailSound(pLevel, pServerData, pPos, SoundEvents.VAULT_REJECT_REWARDED_PLAYER);
                } else {
                    List<ItemStack> list = resolveItemsToEject(pLevel, pConfig, pPos, pPlayer);
                    if (!list.isEmpty()) {
                        pPlayer.awardStat(Stats.ITEM_USED.get(pStack.getItem()));
                        pStack.consume(pConfig.keyItem().getCount(), pPlayer);
                        unlock(pLevel, pState, pPos, pConfig, pServerData, pSharedData, list);
                        pServerData.addToRewardedPlayers(pPlayer);
                        pSharedData.updateConnectedPlayersWithinRange(pLevel, pPos, pServerData, pConfig, pConfig.deactivationRange());
                    }
                }
            }
        }

        static void setVaultState(
            ServerLevel pLevel, BlockPos pPos, BlockState pOldState, BlockState pNewState, VaultConfig pConfig, VaultSharedData pSharedData
        ) {
            VaultState vaultstate = pOldState.getValue(VaultBlock.STATE);
            VaultState vaultstate1 = pNewState.getValue(VaultBlock.STATE);
            pLevel.setBlock(pPos, pNewState, 3);
            vaultstate.onTransition(pLevel, pPos, vaultstate1, pConfig, pSharedData, pNewState.getValue(VaultBlock.OMINOUS));
        }

        static void cycleDisplayItemFromLootTable(
            ServerLevel pLevel, VaultState pState, VaultConfig pConfig, VaultSharedData pSharedData, BlockPos pPos
        ) {
            if (!canEjectReward(pConfig, pState)) {
                pSharedData.setDisplayItem(ItemStack.EMPTY);
            } else {
                ItemStack itemstack = getRandomDisplayItemFromLootTable(
                    pLevel, pPos, pConfig.overrideLootTableToDisplay().orElse(pConfig.lootTable())
                );
                pSharedData.setDisplayItem(itemstack);
            }
        }

        private static ItemStack getRandomDisplayItemFromLootTable(ServerLevel pLevel, BlockPos pPos, ResourceKey<LootTable> pLootTable) {
            LootTable loottable = pLevel.getServer().reloadableRegistries().getLootTable(pLootTable);
            LootParams lootparams = new LootParams.Builder(pLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pPos))
                .create(LootContextParamSets.VAULT);
            List<ItemStack> list = loottable.getRandomItems(lootparams, pLevel.getRandom());
            return list.isEmpty() ? ItemStack.EMPTY : Util.getRandom(list, pLevel.getRandom());
        }

        private static void unlock(
            ServerLevel pLevel,
            BlockState pState,
            BlockPos pPos,
            VaultConfig pConfig,
            VaultServerData pServerData,
            VaultSharedData pSharedData,
            List<ItemStack> pItemsToEject
        ) {
            pServerData.setItemsToEject(pItemsToEject);
            pSharedData.setDisplayItem(pServerData.getNextItemToEject());
            pServerData.pauseStateUpdatingUntil(pLevel.getGameTime() + 14L);
            setVaultState(pLevel, pPos, pState, pState.setValue(VaultBlock.STATE, VaultState.UNLOCKING), pConfig, pSharedData);
        }

        private static List<ItemStack> resolveItemsToEject(ServerLevel pLevel, VaultConfig pConfig, BlockPos pPos, Player pPlayer) {
            LootTable loottable = pLevel.getServer().reloadableRegistries().getLootTable(pConfig.lootTable());
            LootParams lootparams = new LootParams.Builder(pLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pPos))
                .withLuck(pPlayer.getLuck())
                .withParameter(LootContextParams.THIS_ENTITY, pPlayer)
                .create(LootContextParamSets.VAULT);
            return loottable.getRandomItems(lootparams);
        }

        private static boolean canEjectReward(VaultConfig pConfig, VaultState pState) {
            return pConfig.lootTable() != BuiltInLootTables.EMPTY && !pConfig.keyItem().isEmpty() && pState != VaultState.INACTIVE;
        }

        private static boolean isValidToInsert(VaultConfig pConfig, ItemStack pStack) {
            return ItemStack.isSameItemSameComponents(pStack, pConfig.keyItem()) && pStack.getCount() >= pConfig.keyItem().getCount();
        }

        private static boolean shouldCycleDisplayItem(long pGameTime, VaultState pState) {
            return pGameTime % 20L == 0L && pState == VaultState.ACTIVE;
        }

        private static void playInsertFailSound(ServerLevel pLevel, VaultServerData pServerData, BlockPos pPos, SoundEvent p_350407_) {
            if (pLevel.getGameTime() >= pServerData.getLastInsertFailTimestamp() + 15L) {
                pLevel.playSound(null, pPos, p_350407_, SoundSource.BLOCKS);
                pServerData.setLastInsertFailTimestamp(pLevel.getGameTime());
            }
        }
    }
}
