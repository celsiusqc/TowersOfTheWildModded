package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class BannerBlockEntity extends BlockEntity implements Nameable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int MAX_PATTERNS = 6;
    private static final String TAG_PATTERNS = "patterns";
    @Nullable
    private Component name;
    private DyeColor baseColor;
    /**
     * A list of all patterns stored on this banner.
     */
    private BannerPatternLayers patterns = BannerPatternLayers.EMPTY;

    public BannerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.BANNER, pPos, pBlockState);
        this.baseColor = ((AbstractBannerBlock)pBlockState.getBlock()).getColor();
    }

    public BannerBlockEntity(BlockPos pPos, BlockState pBlockState, DyeColor pBaseColor) {
        this(pPos, pBlockState);
        this.baseColor = pBaseColor;
    }

    public void fromItem(ItemStack pStack, DyeColor pColor) {
        this.baseColor = pColor;
        this.applyComponentsFromItemStack(pStack);
    }

    @Override
    public Component getName() {
        return (Component)(this.name != null ? this.name : Component.translatable("block.minecraft.banner"));
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return this.name;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        if (!this.patterns.equals(BannerPatternLayers.EMPTY)) {
            pTag.put("patterns", BannerPatternLayers.CODEC.encodeStart(pRegistries.createSerializationContext(NbtOps.INSTANCE), this.patterns).getOrThrow());
        }

        if (this.name != null) {
            pTag.putString("CustomName", Component.Serializer.toJson(this.name, pRegistries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        if (pTag.contains("CustomName", 8)) {
            this.name = parseCustomNameSafe(pTag.getString("CustomName"), pRegistries);
        }

        if (pTag.contains("patterns")) {
            BannerPatternLayers.CODEC
                .parse(pRegistries.createSerializationContext(NbtOps.INSTANCE), pTag.get("patterns"))
                .resultOrPartial(p_331289_ -> LOGGER.error("Failed to parse banner patterns: '{}'", p_331289_))
                .ifPresent(p_332632_ -> this.patterns = p_332632_);
        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return this.saveWithoutMetadata(pRegistries);
    }

    public BannerPatternLayers getPatterns() {
        return this.patterns;
    }

    public ItemStack getItem() {
        ItemStack itemstack = new ItemStack(BannerBlock.byColor(this.baseColor));
        itemstack.applyComponents(this.collectComponents());
        return itemstack;
    }

    public DyeColor getBaseColor() {
        return this.baseColor;
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput pComponentInput) {
        super.applyImplicitComponents(pComponentInput);
        this.patterns = pComponentInput.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        this.name = pComponentInput.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder pComponents) {
        super.collectImplicitComponents(pComponents);
        pComponents.set(DataComponents.BANNER_PATTERNS, this.patterns);
        pComponents.set(DataComponents.CUSTOM_NAME, this.name);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag pTag) {
        pTag.remove("patterns");
        pTag.remove("CustomName");
    }
}
