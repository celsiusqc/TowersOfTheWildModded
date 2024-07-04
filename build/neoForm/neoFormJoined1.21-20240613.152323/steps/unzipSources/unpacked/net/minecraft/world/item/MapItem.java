package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class MapItem extends ComplexItem {
    public static final int IMAGE_WIDTH = 128;
    public static final int IMAGE_HEIGHT = 128;

    public MapItem(Item.Properties pProperties) {
        super(pProperties);
    }

    public static ItemStack create(Level pLevel, int pLevelX, int pLevelZ, byte pScale, boolean pTrackingPosition, boolean pUnlimitedTracking) {
        ItemStack itemstack = new ItemStack(Items.FILLED_MAP);
        MapId mapid = createNewSavedData(pLevel, pLevelX, pLevelZ, pScale, pTrackingPosition, pUnlimitedTracking, pLevel.dimension());
        itemstack.set(DataComponents.MAP_ID, mapid);
        return itemstack;
    }

    @Nullable
    public static MapItemSavedData getSavedData(@Nullable MapId pMapId, Level pLevel) {
        return pMapId == null ? null : pLevel.getMapData(pMapId);
    }

    @Nullable
    public static MapItemSavedData getSavedData(ItemStack pStack, Level pLevel) {
        // Neo: Add instance method so that mods can override
        Item map = pStack.getItem();
        if(map instanceof MapItem) {
            return ((MapItem)map).getCustomMapData(pStack, pLevel);
        }
        return null;
    }

    @Nullable
    protected MapItemSavedData getCustomMapData(ItemStack p_42854_, Level p_42855_) {
        MapId mapid = p_42854_.get(DataComponents.MAP_ID);
        return getSavedData(mapid, p_42855_);
    }

    private static MapId createNewSavedData(
        Level pLevel, int pX, int pZ, int pScale, boolean pTrackingPosition, boolean pUnlimitedTracking, ResourceKey<Level> pDimension
    ) {
        MapItemSavedData mapitemsaveddata = MapItemSavedData.createFresh((double)pX, (double)pZ, (byte)pScale, pTrackingPosition, pUnlimitedTracking, pDimension);
        MapId mapid = pLevel.getFreeMapId();
        pLevel.setMapData(mapid, mapitemsaveddata);
        return mapid;
    }

    public void update(Level pLevel, Entity pViewer, MapItemSavedData pData) {
        if (pLevel.dimension() == pData.dimension && pViewer instanceof Player) {
            int i = 1 << pData.scale;
            int j = pData.centerX;
            int k = pData.centerZ;
            int l = Mth.floor(pViewer.getX() - (double)j) / i + 64;
            int i1 = Mth.floor(pViewer.getZ() - (double)k) / i + 64;
            int j1 = 128 / i;
            if (pLevel.dimensionType().hasCeiling()) {
                j1 /= 2;
            }

            MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer = pData.getHoldingPlayer((Player)pViewer);
            mapitemsaveddata$holdingplayer.step++;
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos blockpos$mutableblockpos1 = new BlockPos.MutableBlockPos();
            boolean flag = false;

            for (int k1 = l - j1 + 1; k1 < l + j1; k1++) {
                if ((k1 & 15) == (mapitemsaveddata$holdingplayer.step & 15) || flag) {
                    flag = false;
                    double d0 = 0.0;

                    for (int l1 = i1 - j1 - 1; l1 < i1 + j1; l1++) {
                        if (k1 >= 0 && l1 >= -1 && k1 < 128 && l1 < 128) {
                            int i2 = Mth.square(k1 - l) + Mth.square(l1 - i1);
                            boolean flag1 = i2 > (j1 - 2) * (j1 - 2);
                            int j2 = (j / i + k1 - 64) * i;
                            int k2 = (k / i + l1 - 64) * i;
                            Multiset<MapColor> multiset = LinkedHashMultiset.create();
                            LevelChunk levelchunk = pLevel.getChunk(SectionPos.blockToSectionCoord(j2), SectionPos.blockToSectionCoord(k2));
                            if (!levelchunk.isEmpty()) {
                                int l2 = 0;
                                double d1 = 0.0;
                                if (pLevel.dimensionType().hasCeiling()) {
                                    int i3 = j2 + k2 * 231871;
                                    i3 = i3 * i3 * 31287121 + i3 * 11;
                                    if ((i3 >> 20 & 1) == 0) {
                                        multiset.add(Blocks.DIRT.defaultBlockState().getMapColor(pLevel, BlockPos.ZERO), 10);
                                    } else {
                                        multiset.add(Blocks.STONE.defaultBlockState().getMapColor(pLevel, BlockPos.ZERO), 100);
                                    }

                                    d1 = 100.0;
                                } else {
                                    for (int i4 = 0; i4 < i; i4++) {
                                        for (int j3 = 0; j3 < i; j3++) {
                                            blockpos$mutableblockpos.set(j2 + i4, 0, k2 + j3);
                                            int k3 = levelchunk.getHeight(
                                                    Heightmap.Types.WORLD_SURFACE, blockpos$mutableblockpos.getX(), blockpos$mutableblockpos.getZ()
                                                )
                                                + 1;
                                            BlockState blockstate;
                                            if (k3 <= pLevel.getMinBuildHeight() + 1) {
                                                blockstate = Blocks.BEDROCK.defaultBlockState();
                                            } else {
                                                do {
                                                    blockpos$mutableblockpos.setY(--k3);
                                                    blockstate = levelchunk.getBlockState(blockpos$mutableblockpos);
                                                } while (
                                                    blockstate.getMapColor(pLevel, blockpos$mutableblockpos) == MapColor.NONE
                                                        && k3 > pLevel.getMinBuildHeight()
                                                );

                                                if (k3 > pLevel.getMinBuildHeight() && !blockstate.getFluidState().isEmpty()) {
                                                    int l3 = k3 - 1;
                                                    blockpos$mutableblockpos1.set(blockpos$mutableblockpos);

                                                    BlockState blockstate1;
                                                    do {
                                                        blockpos$mutableblockpos1.setY(l3--);
                                                        blockstate1 = levelchunk.getBlockState(blockpos$mutableblockpos1);
                                                        l2++;
                                                    } while (l3 > pLevel.getMinBuildHeight() && !blockstate1.getFluidState().isEmpty());

                                                    blockstate = this.getCorrectStateForFluidBlock(pLevel, blockstate, blockpos$mutableblockpos);
                                                }
                                            }

                                            pData.checkBanners(pLevel, blockpos$mutableblockpos.getX(), blockpos$mutableblockpos.getZ());
                                            d1 += (double)k3 / (double)(i * i);
                                            multiset.add(blockstate.getMapColor(pLevel, blockpos$mutableblockpos));
                                        }
                                    }
                                }

                                l2 /= i * i;
                                MapColor mapcolor = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.NONE);
                                MapColor.Brightness mapcolor$brightness;
                                if (mapcolor == MapColor.WATER) {
                                    double d2 = (double)l2 * 0.1 + (double)(k1 + l1 & 1) * 0.2;
                                    if (d2 < 0.5) {
                                        mapcolor$brightness = MapColor.Brightness.HIGH;
                                    } else if (d2 > 0.9) {
                                        mapcolor$brightness = MapColor.Brightness.LOW;
                                    } else {
                                        mapcolor$brightness = MapColor.Brightness.NORMAL;
                                    }
                                } else {
                                    double d3 = (d1 - d0) * 4.0 / (double)(i + 4) + ((double)(k1 + l1 & 1) - 0.5) * 0.4;
                                    if (d3 > 0.6) {
                                        mapcolor$brightness = MapColor.Brightness.HIGH;
                                    } else if (d3 < -0.6) {
                                        mapcolor$brightness = MapColor.Brightness.LOW;
                                    } else {
                                        mapcolor$brightness = MapColor.Brightness.NORMAL;
                                    }
                                }

                                d0 = d1;
                                if (l1 >= 0 && i2 < j1 * j1 && (!flag1 || (k1 + l1 & 1) != 0)) {
                                    flag |= pData.updateColor(k1, l1, mapcolor.getPackedId(mapcolor$brightness));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private BlockState getCorrectStateForFluidBlock(Level pLevel, BlockState pState, BlockPos pPos) {
        FluidState fluidstate = pState.getFluidState();
        return !fluidstate.isEmpty() && !pState.isFaceSturdy(pLevel, pPos, Direction.UP) ? fluidstate.createLegacyBlock() : pState;
    }

    private static boolean isBiomeWatery(boolean[] pWateryMap, int pXSample, int pZSample) {
        return pWateryMap[pZSample * 128 + pXSample];
    }

    public static void renderBiomePreviewMap(ServerLevel pServerLevel, ItemStack pStack) {
        MapItemSavedData mapitemsaveddata = getSavedData(pStack, pServerLevel);
        if (mapitemsaveddata != null) {
            if (pServerLevel.dimension() == mapitemsaveddata.dimension) {
                int i = 1 << mapitemsaveddata.scale;
                int j = mapitemsaveddata.centerX;
                int k = mapitemsaveddata.centerZ;
                boolean[] aboolean = new boolean[16384];
                int l = j / i - 64;
                int i1 = k / i - 64;
                BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

                for (int j1 = 0; j1 < 128; j1++) {
                    for (int k1 = 0; k1 < 128; k1++) {
                        Holder<Biome> holder = pServerLevel.getBiome(blockpos$mutableblockpos.set((l + k1) * i, 0, (i1 + j1) * i));
                        aboolean[j1 * 128 + k1] = holder.is(BiomeTags.WATER_ON_MAP_OUTLINES);
                    }
                }

                for (int j2 = 1; j2 < 127; j2++) {
                    for (int k2 = 1; k2 < 127; k2++) {
                        int l2 = 0;

                        for (int l1 = -1; l1 < 2; l1++) {
                            for (int i2 = -1; i2 < 2; i2++) {
                                if ((l1 != 0 || i2 != 0) && isBiomeWatery(aboolean, j2 + l1, k2 + i2)) {
                                    l2++;
                                }
                            }
                        }

                        MapColor.Brightness mapcolor$brightness = MapColor.Brightness.LOWEST;
                        MapColor mapcolor = MapColor.NONE;
                        if (isBiomeWatery(aboolean, j2, k2)) {
                            mapcolor = MapColor.COLOR_ORANGE;
                            if (l2 > 7 && k2 % 2 == 0) {
                                switch ((j2 + (int)(Mth.sin((float)k2 + 0.0F) * 7.0F)) / 8 % 5) {
                                    case 0:
                                    case 4:
                                        mapcolor$brightness = MapColor.Brightness.LOW;
                                        break;
                                    case 1:
                                    case 3:
                                        mapcolor$brightness = MapColor.Brightness.NORMAL;
                                        break;
                                    case 2:
                                        mapcolor$brightness = MapColor.Brightness.HIGH;
                                }
                            } else if (l2 > 7) {
                                mapcolor = MapColor.NONE;
                            } else if (l2 > 5) {
                                mapcolor$brightness = MapColor.Brightness.NORMAL;
                            } else if (l2 > 3) {
                                mapcolor$brightness = MapColor.Brightness.LOW;
                            } else if (l2 > 1) {
                                mapcolor$brightness = MapColor.Brightness.LOW;
                            }
                        } else if (l2 > 0) {
                            mapcolor = MapColor.COLOR_BROWN;
                            if (l2 > 3) {
                                mapcolor$brightness = MapColor.Brightness.NORMAL;
                            } else {
                                mapcolor$brightness = MapColor.Brightness.LOWEST;
                            }
                        }

                        if (mapcolor != MapColor.NONE) {
                            mapitemsaveddata.setColor(j2, k2, mapcolor.getPackedId(mapcolor$brightness));
                        }
                    }
                }
            }
        }
    }

    /**
     * Called each tick as long the item is in a player's inventory. Used by maps to check if it's in a player's hand and update its contents.
     */
    @Override
    public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pItemSlot, boolean pIsSelected) {
        if (!pLevel.isClientSide) {
            MapItemSavedData mapitemsaveddata = getSavedData(pStack, pLevel);
            if (mapitemsaveddata != null) {
                if (pEntity instanceof Player player) {
                    mapitemsaveddata.tickCarriedBy(player, pStack);
                }

                if (!mapitemsaveddata.locked && (pIsSelected || pEntity instanceof Player && ((Player)pEntity).getOffhandItem() == pStack)) {
                    this.update(pLevel, pEntity, mapitemsaveddata);
                }
            }
        }
    }

    @Nullable
    @Override
    public Packet<?> getUpdatePacket(ItemStack pStack, Level pLevel, Player pPlayer) {
        MapId mapid = pStack.get(DataComponents.MAP_ID);
        MapItemSavedData mapitemsaveddata = getSavedData(mapid, pLevel);
        return mapitemsaveddata != null ? mapitemsaveddata.getUpdatePacket(mapid, pPlayer) : null;
    }

    @Override
    public void onCraftedPostProcess(ItemStack pStack, Level pLevel) {
        MapPostProcessing mappostprocessing = pStack.remove(DataComponents.MAP_POST_PROCESSING);
        if (mappostprocessing != null) {
            switch (mappostprocessing) {
                case LOCK:
                    lockMap(pLevel, pStack);
                    break;
                case SCALE:
                    scaleMap(pStack, pLevel);
            }
        }
    }

    private static void scaleMap(ItemStack pStack, Level pLevel) {
        MapItemSavedData mapitemsaveddata = getSavedData(pStack, pLevel);
        if (mapitemsaveddata != null) {
            MapId mapid = pLevel.getFreeMapId();
            pLevel.setMapData(mapid, mapitemsaveddata.scaled());
            pStack.set(DataComponents.MAP_ID, mapid);
        }
    }

    public static void lockMap(Level pLevel, ItemStack pStack) {
        MapItemSavedData mapitemsaveddata = getSavedData(pStack, pLevel);
        if (mapitemsaveddata != null) {
            MapId mapid = pLevel.getFreeMapId();
            MapItemSavedData mapitemsaveddata1 = mapitemsaveddata.locked();
            pLevel.setMapData(mapid, mapitemsaveddata1);
            pStack.set(DataComponents.MAP_ID, mapid);
        }
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        MapId mapid = pStack.get(DataComponents.MAP_ID);
        MapItemSavedData mapitemsaveddata = mapid != null ? pContext.mapData(mapid) : null;
        MapPostProcessing mappostprocessing = pStack.get(DataComponents.MAP_POST_PROCESSING);
        if (mapitemsaveddata != null && (mapitemsaveddata.locked || mappostprocessing == MapPostProcessing.LOCK)) {
            pTooltipComponents.add(Component.translatable("filled_map.locked", mapid.id()).withStyle(ChatFormatting.GRAY));
        }

        if (pTooltipFlag.isAdvanced()) {
            if (mapitemsaveddata != null) {
                if (mappostprocessing == null) {
                    pTooltipComponents.add(getTooltipForId(mapid));
                }

                int i = mappostprocessing == MapPostProcessing.SCALE ? 1 : 0;
                int j = Math.min(mapitemsaveddata.scale + i, 4);
                pTooltipComponents.add(Component.translatable("filled_map.scale", 1 << j).withStyle(ChatFormatting.GRAY));
                pTooltipComponents.add(Component.translatable("filled_map.level", j, 4).withStyle(ChatFormatting.GRAY));
            } else {
                pTooltipComponents.add(Component.translatable("filled_map.unknown").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    public static Component getTooltipForId(MapId pMapId) {
        return Component.translatable("filled_map.id", pMapId.id()).withStyle(ChatFormatting.GRAY);
    }

    /**
     * Called when this item is used when targeting a Block
     */
    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        BlockState blockstate = pContext.getLevel().getBlockState(pContext.getClickedPos());
        if (blockstate.is(BlockTags.BANNERS)) {
            if (!pContext.getLevel().isClientSide) {
                MapItemSavedData mapitemsaveddata = getSavedData(pContext.getItemInHand(), pContext.getLevel());
                if (mapitemsaveddata != null && !mapitemsaveddata.toggleBanner(pContext.getLevel(), pContext.getClickedPos())) {
                    return InteractionResult.FAIL;
                }
            }

            return InteractionResult.sidedSuccess(pContext.getLevel().isClientSide);
        } else {
            return super.useOn(pContext);
        }
    }
}
