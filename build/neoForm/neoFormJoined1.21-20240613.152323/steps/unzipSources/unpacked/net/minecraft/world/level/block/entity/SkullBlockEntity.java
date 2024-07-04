package net.minecraft.world.level.block.entity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Services;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class SkullBlockEntity extends BlockEntity {
    private static final String TAG_PROFILE = "profile";
    private static final String TAG_NOTE_BLOCK_SOUND = "note_block_sound";
    private static final String TAG_CUSTOM_NAME = "custom_name";
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private static Executor mainThreadExecutor;
    @Nullable
    private static LoadingCache<String, CompletableFuture<Optional<GameProfile>>> profileCacheByName;
    @Nullable
    private static LoadingCache<UUID, CompletableFuture<Optional<GameProfile>>> profileCacheById;
    public static final Executor CHECKED_MAIN_THREAD_EXECUTOR = p_294078_ -> {
        Executor executor = mainThreadExecutor;
        if (executor != null) {
            executor.execute(p_294078_);
        }
    };
    @Nullable
    private ResolvableProfile owner;
    @Nullable
    private ResourceLocation noteBlockSound;
    private int animationTickCount;
    private boolean isAnimating;
    @Nullable
    private Component customName;

    public SkullBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.SKULL, pPos, pBlockState);
    }

    public static void setup(final Services pServices, Executor pMainThreadExecutor) {
        mainThreadExecutor = pMainThreadExecutor;
        final BooleanSupplier booleansupplier = () -> profileCacheById == null;
        profileCacheByName = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10L))
            .maximumSize(256L)
            .build(new CacheLoader<String, CompletableFuture<Optional<GameProfile>>>() {
                public CompletableFuture<Optional<GameProfile>> load(String p_304652_) {
                    return SkullBlockEntity.fetchProfileByName(p_304652_, pServices);
                }
            });
        profileCacheById = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10L))
            .maximumSize(256L)
            .build(new CacheLoader<UUID, CompletableFuture<Optional<GameProfile>>>() {
                public CompletableFuture<Optional<GameProfile>> load(UUID p_339657_) {
                    return SkullBlockEntity.fetchProfileById(p_339657_, pServices, booleansupplier);
                }
            });
    }

    static CompletableFuture<Optional<GameProfile>> fetchProfileByName(String pName, Services pServices) {
        return pServices.profileCache()
            .getAsync(pName)
            .thenCompose(
                p_339545_ -> {
                    LoadingCache<UUID, CompletableFuture<Optional<GameProfile>>> loadingcache = profileCacheById;
                    return loadingcache != null && !p_339545_.isEmpty()
                        ? loadingcache.getUnchecked(p_339545_.get().getId()).thenApply(p_339543_ -> p_339543_.or(() -> p_339545_))
                        : CompletableFuture.completedFuture(Optional.empty());
                }
            );
    }

    static CompletableFuture<Optional<GameProfile>> fetchProfileById(UUID pId, Services pServices, BooleanSupplier pCacheUninitialized) {
        return CompletableFuture.supplyAsync(() -> {
            if (pCacheUninitialized.getAsBoolean()) {
                return Optional.empty();
            } else {
                ProfileResult profileresult = pServices.sessionService().fetchProfile(pId, true);
                return Optional.ofNullable(profileresult).map(ProfileResult::profile);
            }
        }, Util.backgroundExecutor());
    }

    public static void clear() {
        mainThreadExecutor = null;
        profileCacheByName = null;
        profileCacheById = null;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        if (this.owner != null) {
            pTag.put("profile", ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, this.owner).getOrThrow());
        }

        if (this.noteBlockSound != null) {
            pTag.putString("note_block_sound", this.noteBlockSound.toString());
        }

        if (this.customName != null) {
            pTag.putString("custom_name", Component.Serializer.toJson(this.customName, pRegistries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        if (pTag.contains("profile")) {
            ResolvableProfile.CODEC
                .parse(NbtOps.INSTANCE, pTag.get("profile"))
                .resultOrPartial(p_332637_ -> LOGGER.error("Failed to load profile from player head: {}", p_332637_))
                .ifPresent(this::setOwner);
        }

        if (pTag.contains("note_block_sound", 8)) {
            this.noteBlockSound = ResourceLocation.tryParse(pTag.getString("note_block_sound"));
        }

        if (pTag.contains("custom_name", 8)) {
            this.customName = parseCustomNameSafe(pTag.getString("custom_name"), pRegistries);
        } else {
            this.customName = null;
        }
    }

    public static void animation(Level pLevel, BlockPos pPos, BlockState pState, SkullBlockEntity pBlockEntity) {
        if (pState.hasProperty(SkullBlock.POWERED) && pState.getValue(SkullBlock.POWERED)) {
            pBlockEntity.isAnimating = true;
            pBlockEntity.animationTickCount++;
        } else {
            pBlockEntity.isAnimating = false;
        }
    }

    public float getAnimation(float pPartialTick) {
        return this.isAnimating ? (float)this.animationTickCount + pPartialTick : (float)this.animationTickCount;
    }

    @Nullable
    public ResolvableProfile getOwnerProfile() {
        return this.owner;
    }

    @Nullable
    public ResourceLocation getNoteBlockSound() {
        return this.noteBlockSound;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return this.saveCustomOnly(pRegistries);
    }

    public void setOwner(@Nullable ResolvableProfile p_332738_) {
        synchronized (this) {
            this.owner = p_332738_;
        }

        this.updateOwnerProfile();
    }

    private void updateOwnerProfile() {
        if (this.owner != null && !this.owner.isResolved()) {
            this.owner.resolve().thenAcceptAsync(p_332638_ -> {
                this.owner = p_332638_;
                this.setChanged();
            }, CHECKED_MAIN_THREAD_EXECUTOR);
        } else {
            this.setChanged();
        }
    }

    public static CompletableFuture<Optional<GameProfile>> fetchGameProfile(String pProfileName) {
        LoadingCache<String, CompletableFuture<Optional<GameProfile>>> loadingcache = profileCacheByName;
        return loadingcache != null && StringUtil.isValidPlayerName(pProfileName)
            ? loadingcache.getUnchecked(pProfileName)
            : CompletableFuture.completedFuture(Optional.empty());
    }

    public static CompletableFuture<Optional<GameProfile>> fetchGameProfile(UUID pProfileUuid) {
        LoadingCache<UUID, CompletableFuture<Optional<GameProfile>>> loadingcache = profileCacheById;
        return loadingcache != null ? loadingcache.getUnchecked(pProfileUuid) : CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput pComponentInput) {
        super.applyImplicitComponents(pComponentInput);
        this.setOwner(pComponentInput.get(DataComponents.PROFILE));
        this.noteBlockSound = pComponentInput.get(DataComponents.NOTE_BLOCK_SOUND);
        this.customName = pComponentInput.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder pComponents) {
        super.collectImplicitComponents(pComponents);
        pComponents.set(DataComponents.PROFILE, this.owner);
        pComponents.set(DataComponents.NOTE_BLOCK_SOUND, this.noteBlockSound);
        pComponents.set(DataComponents.CUSTOM_NAME, this.customName);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag pTag) {
        super.removeComponentsFromTag(pTag);
        pTag.remove("profile");
        pTag.remove("note_block_sound");
        pTag.remove("custom_name");
    }
}
