package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public abstract class BlockEntity extends net.neoforged.neoforge.attachment.AttachmentHolder implements net.neoforged.neoforge.common.extensions.IBlockEntityExtension {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final BlockEntityType<?> type;
    @Nullable
    protected Level level;
    protected final BlockPos worldPosition;
    protected boolean remove;
    private BlockState blockState;
    private DataComponentMap components = DataComponentMap.EMPTY;
    @Nullable
    private CompoundTag customPersistentData;

    public BlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        this.type = pType;
        this.worldPosition = pPos.immutable();
        this.blockState = pBlockState;
    }

    public static BlockPos getPosFromTag(CompoundTag pTag) {
        return new BlockPos(pTag.getInt("x"), pTag.getInt("y"), pTag.getInt("z"));
    }

    @Nullable
    public Level getLevel() {
        return this.level;
    }

    public void setLevel(Level pLevel) {
        this.level = pLevel;
    }

    public boolean hasLevel() {
        return this.level != null;
    }

    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        if (pTag.contains("NeoForgeData", net.minecraft.nbt.Tag.TAG_COMPOUND)) this.customPersistentData = pTag.getCompound("NeoForgeData");
        if (pTag.contains(ATTACHMENTS_NBT_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) deserializeAttachments(pRegistries, pTag.getCompound(ATTACHMENTS_NBT_KEY));
    }

    public final void loadWithComponents(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        this.loadAdditional(pTag, pRegistries);
        BlockEntity.ComponentHelper.COMPONENTS_CODEC
            .parse(pRegistries.createSerializationContext(NbtOps.INSTANCE), pTag)
            .resultOrPartial(p_337987_ -> LOGGER.warn("Failed to load components: {}", p_337987_))
            .ifPresent(p_337995_ -> this.components = p_337995_);
    }

    public final void loadCustomOnly(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        this.loadAdditional(pTag, pRegistries);
    }

    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        if (this.customPersistentData != null) pTag.put("NeoForgeData", this.customPersistentData.copy());
        var attachmentsTag = serializeAttachments(pRegistries);
        if (attachmentsTag != null) pTag.put(ATTACHMENTS_NBT_KEY, attachmentsTag);
    }

    public final CompoundTag saveWithFullMetadata(HolderLookup.Provider pRegistries) {
        CompoundTag compoundtag = this.saveWithoutMetadata(pRegistries);
        this.saveMetadata(compoundtag);
        return compoundtag;
    }

    public final CompoundTag saveWithId(HolderLookup.Provider pRegistries) {
        CompoundTag compoundtag = this.saveWithoutMetadata(pRegistries);
        this.saveId(compoundtag);
        return compoundtag;
    }

    public final CompoundTag saveWithoutMetadata(HolderLookup.Provider pRegistries) {
        CompoundTag compoundtag = new CompoundTag();
        this.saveAdditional(compoundtag, pRegistries);
        BlockEntity.ComponentHelper.COMPONENTS_CODEC
            .encodeStart(pRegistries.createSerializationContext(NbtOps.INSTANCE), this.components)
            .resultOrPartial(p_337988_ -> LOGGER.warn("Failed to save components: {}", p_337988_))
            .ifPresent(p_337994_ -> compoundtag.merge((CompoundTag)p_337994_));
        return compoundtag;
    }

    public final CompoundTag saveCustomOnly(HolderLookup.Provider pRegistries) {
        CompoundTag compoundtag = new CompoundTag();
        this.saveAdditional(compoundtag, pRegistries);
        return compoundtag;
    }

    public final CompoundTag saveCustomAndMetadata(HolderLookup.Provider pRegistries) {
        CompoundTag compoundtag = this.saveCustomOnly(pRegistries);
        this.saveMetadata(compoundtag);
        return compoundtag;
    }

    private void saveId(CompoundTag pTag) {
        ResourceLocation resourcelocation = BlockEntityType.getKey(this.getType());
        if (resourcelocation == null) {
            throw new RuntimeException(this.getClass() + " is missing a mapping! This is a bug!");
        } else {
            pTag.putString("id", resourcelocation.toString());
        }
    }

    public static void addEntityType(CompoundTag pTag, BlockEntityType<?> pEntityType) {
        pTag.putString("id", BlockEntityType.getKey(pEntityType).toString());
    }

    public void saveToItem(ItemStack pStack, HolderLookup.Provider pRegistries) {
        CompoundTag compoundtag = this.saveCustomOnly(pRegistries);
        this.removeComponentsFromTag(compoundtag);
        BlockItem.setBlockEntityData(pStack, this.getType(), compoundtag);
        pStack.applyComponents(this.collectComponents());
    }

    private void saveMetadata(CompoundTag pTag) {
        this.saveId(pTag);
        pTag.putInt("x", this.worldPosition.getX());
        pTag.putInt("y", this.worldPosition.getY());
        pTag.putInt("z", this.worldPosition.getZ());
    }

    @Nullable
    public static BlockEntity loadStatic(BlockPos pPos, BlockState pState, CompoundTag pTag, HolderLookup.Provider pRegistries) {
        String s = pTag.getString("id");
        ResourceLocation resourcelocation = ResourceLocation.tryParse(s);
        if (resourcelocation == null) {
            LOGGER.error("Block entity has invalid type: {}", s);
            return null;
        } else {
            return BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(resourcelocation).map(p_155240_ -> {
                try {
                    return p_155240_.create(pPos, pState);
                } catch (Throwable throwable) {
                    LOGGER.error("Failed to create block entity {}", s, throwable);
                    return null;
                }
            }).map(p_337992_ -> {
                try {
                    p_337992_.loadWithComponents(pTag, pRegistries);
                    return (BlockEntity)p_337992_;
                } catch (Throwable throwable) {
                    LOGGER.error("Failed to load data for block entity {}", s, throwable);
                    return null;
                }
            }).orElseGet(() -> {
                LOGGER.warn("Skipping BlockEntity with id {}", s);
                return null;
            });
        }
    }

    public void setChanged() {
        if (this.level != null) {
            setChanged(this.level, this.worldPosition, this.blockState);
        }
    }

    protected static void setChanged(Level pLevel, BlockPos pPos, BlockState pState) {
        pLevel.blockEntityChanged(pPos);
        if (!pState.isAir()) {
            pLevel.updateNeighbourForOutputSignal(pPos, pState.getBlock());
        }
    }

    public BlockPos getBlockPos() {
        return this.worldPosition;
    }

    public BlockState getBlockState() {
        return this.blockState;
    }

    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return null;
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return new CompoundTag();
    }

    public boolean isRemoved() {
        return this.remove;
    }

    public void setRemoved() {
        this.remove = true;
        this.invalidateCapabilities();
        requestModelDataUpdate();
    }

    public void clearRemoved() {
        this.remove = false;
        // Neo: invalidate capabilities on block entity placement
        invalidateCapabilities();
    }

    public boolean triggerEvent(int pId, int pType) {
        return false;
    }

    public void fillCrashReportCategory(CrashReportCategory pReportCategory) {
        pReportCategory.setDetail("Name", () -> BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(this.getType()) + " // " + this.getClass().getCanonicalName());
        if (this.level != null) {
            CrashReportCategory.populateBlockDetails(pReportCategory, this.level, this.worldPosition, this.getBlockState());
            CrashReportCategory.populateBlockDetails(pReportCategory, this.level, this.worldPosition, this.level.getBlockState(this.worldPosition));
        }
    }

    public boolean onlyOpCanSetNbt() {
        return false;
    }

    public BlockEntityType<?> getType() {
        return this.type;
    }

    @Override
    public CompoundTag getPersistentData() {
        if (this.customPersistentData == null)
            this.customPersistentData = new CompoundTag();
        return this.customPersistentData;
    }

    @Override
    @Nullable
    public final <T> T setData(net.neoforged.neoforge.attachment.AttachmentType<T> type, T data) {
        setChanged();
        return super.setData(type, data);
    }

    @Override
    @Nullable
    public final <T> T removeData(net.neoforged.neoforge.attachment.AttachmentType<T> type) {
        setChanged();
        return super.removeData(type);
    }

    @Deprecated
    public void setBlockState(BlockState pBlockState) {
        this.blockState = pBlockState;
    }

    protected void applyImplicitComponents(BlockEntity.DataComponentInput pComponentInput) {
    }

    public final void applyComponentsFromItemStack(ItemStack pStack) {
        this.applyComponents(pStack.getPrototype(), pStack.getComponentsPatch());
    }

    public final void applyComponents(DataComponentMap pComponents, DataComponentPatch pPatch) {
        final Set<DataComponentType<?>> set = new HashSet<>();
        set.add(DataComponents.BLOCK_ENTITY_DATA);
        final DataComponentMap datacomponentmap = PatchedDataComponentMap.fromPatch(pComponents, pPatch);
        this.applyImplicitComponents(new BlockEntity.DataComponentInput() {
            @Nullable
            @Override
            public <T> T get(DataComponentType<T> p_338266_) {
                set.add(p_338266_);
                return datacomponentmap.get(p_338266_);
            }

            @Override
            public <T> T getOrDefault(DataComponentType<? extends T> p_338358_, T p_338352_) {
                set.add(p_338358_);
                return datacomponentmap.getOrDefault(p_338358_, p_338352_);
            }
        });
        DataComponentPatch datacomponentpatch = pPatch.forget(set::contains);
        this.components = datacomponentpatch.split().added();
    }

    protected void collectImplicitComponents(DataComponentMap.Builder pComponents) {
    }

    @Deprecated
    public void removeComponentsFromTag(CompoundTag pTag) {
    }

    public final DataComponentMap collectComponents() {
        DataComponentMap.Builder datacomponentmap$builder = DataComponentMap.builder();
        datacomponentmap$builder.addAll(this.components);
        this.collectImplicitComponents(datacomponentmap$builder);
        return datacomponentmap$builder.build();
    }

    public DataComponentMap components() {
        return this.components;
    }

    public void setComponents(DataComponentMap pComponents) {
        this.components = pComponents;
    }

    @Nullable
    public static Component parseCustomNameSafe(String pCustomName, HolderLookup.Provider pRegistries) {
        try {
            return Component.Serializer.fromJson(pCustomName, pRegistries);
        } catch (Exception exception) {
            LOGGER.warn("Failed to parse custom name from string '{}', discarding", pCustomName, exception);
            return null;
        }
    }

    static class ComponentHelper {
        public static final Codec<DataComponentMap> COMPONENTS_CODEC = DataComponentMap.CODEC.optionalFieldOf("components", DataComponentMap.EMPTY).codec();

        private ComponentHelper() {
        }
    }

    protected interface DataComponentInput {
        @Nullable
        <T> T get(DataComponentType<T> pComponent);

        <T> T getOrDefault(DataComponentType<? extends T> pComponent, T pDefaultValue);

        // Neo: Utility for modded component types, to remove the need to invoke '.value()'
        @Nullable
        default <T> T get(java.util.function.Supplier<? extends DataComponentType<T>> componentType) {
            return get(componentType.get());
        }

        default <T> T getOrDefault(java.util.function.Supplier<? extends DataComponentType<T>> componentType, T value) {
            return getOrDefault(componentType.get(), value);
        }
    }
}
