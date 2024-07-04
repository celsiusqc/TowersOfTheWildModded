package net.minecraft.client.renderer.entity;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.MatrixUtil;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemRenderer implements ResourceManagerReloadListener {
    public static final ResourceLocation ENCHANTED_GLINT_ENTITY = ResourceLocation.withDefaultNamespace("textures/misc/enchanted_glint_entity.png");
    public static final ResourceLocation ENCHANTED_GLINT_ITEM = ResourceLocation.withDefaultNamespace("textures/misc/enchanted_glint_item.png");
    private static final Set<Item> IGNORED = Sets.newHashSet(Items.AIR);
    public static final int GUI_SLOT_CENTER_X = 8;
    public static final int GUI_SLOT_CENTER_Y = 8;
    public static final int ITEM_COUNT_BLIT_OFFSET = 200;
    public static final float COMPASS_FOIL_UI_SCALE = 0.5F;
    public static final float COMPASS_FOIL_FIRST_PERSON_SCALE = 0.75F;
    public static final float COMPASS_FOIL_TEXTURE_SCALE = 0.0078125F;
    private static final ModelResourceLocation TRIDENT_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("trident"));
    public static final ModelResourceLocation TRIDENT_IN_HAND_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("trident_in_hand"));
    private static final ModelResourceLocation SPYGLASS_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("spyglass"));
    public static final ModelResourceLocation SPYGLASS_IN_HAND_MODEL = ModelResourceLocation.inventory(
        ResourceLocation.withDefaultNamespace("spyglass_in_hand")
    );
    private final Minecraft minecraft;
    private final ItemModelShaper itemModelShaper;
    private final TextureManager textureManager;
    private final ItemColors itemColors;
    private final BlockEntityWithoutLevelRenderer blockEntityRenderer;

    public ItemRenderer(Minecraft pMinecraft, TextureManager pTextureManager, ModelManager pModelManager, ItemColors pItemColors, BlockEntityWithoutLevelRenderer pBlockEntityRenderer) {
        this.minecraft = pMinecraft;
        this.textureManager = pTextureManager;
        this.itemModelShaper = new net.neoforged.neoforge.client.model.RegistryAwareItemModelShaper(pModelManager);
        this.blockEntityRenderer = pBlockEntityRenderer;

        for (Item item : BuiltInRegistries.ITEM) {
            if (!IGNORED.contains(item)) {
                this.itemModelShaper.register(item, ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item)));
            }
        }

        this.itemColors = pItemColors;
    }

    public ItemModelShaper getItemModelShaper() {
        return this.itemModelShaper;
    }

    public void renderModelLists(BakedModel pModel, ItemStack pStack, int pCombinedLight, int pCombinedOverlay, PoseStack pPoseStack, VertexConsumer pBuffer) {
        RandomSource randomsource = RandomSource.create();
        long i = 42L;

        for (Direction direction : Direction.values()) {
            randomsource.setSeed(42L);
            this.renderQuadList(pPoseStack, pBuffer, pModel.getQuads(null, direction, randomsource), pStack, pCombinedLight, pCombinedOverlay);
        }

        randomsource.setSeed(42L);
        this.renderQuadList(pPoseStack, pBuffer, pModel.getQuads(null, null, randomsource), pStack, pCombinedLight, pCombinedOverlay);
    }

    public void render(
        ItemStack pItemStack,
        ItemDisplayContext pDisplayContext,
        boolean pLeftHand,
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pCombinedLight,
        int pCombinedOverlay,
        BakedModel pModel
    ) {
        if (!pItemStack.isEmpty()) {
            pPoseStack.pushPose();
            boolean flag = pDisplayContext == ItemDisplayContext.GUI || pDisplayContext == ItemDisplayContext.GROUND || pDisplayContext == ItemDisplayContext.FIXED;
            if (flag) {
                if (pItemStack.is(Items.TRIDENT)) {
                    pModel = this.itemModelShaper.getModelManager().getModel(TRIDENT_MODEL);
                } else if (pItemStack.is(Items.SPYGLASS)) {
                    pModel = this.itemModelShaper.getModelManager().getModel(SPYGLASS_MODEL);
                }
            }

            pModel = net.neoforged.neoforge.client.ClientHooks.handleCameraTransforms(pPoseStack, pModel, pDisplayContext, pLeftHand);
            pPoseStack.translate(-0.5F, -0.5F, -0.5F);
            if (!pModel.isCustomRenderer() && (!pItemStack.is(Items.TRIDENT) || flag)) {
                boolean flag1;
                if (pDisplayContext != ItemDisplayContext.GUI && !pDisplayContext.firstPerson() && pItemStack.getItem() instanceof BlockItem blockitem) {
                    Block block = blockitem.getBlock();
                    flag1 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
                } else {
                    flag1 = true;
                }

                for (var model : pModel.getRenderPasses(pItemStack, flag1)) {
                for (var rendertype : model.getRenderTypes(pItemStack, flag1)) {
                VertexConsumer vertexconsumer;
                if (hasAnimatedTexture(pItemStack) && pItemStack.hasFoil()) {
                    PoseStack.Pose posestack$pose = pPoseStack.last().copy();
                    if (pDisplayContext == ItemDisplayContext.GUI) {
                        MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.5F);
                    } else if (pDisplayContext.firstPerson()) {
                        MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.75F);
                    }

                    vertexconsumer = getCompassFoilBuffer(pBuffer, rendertype, posestack$pose);
                } else if (flag1) {
                    vertexconsumer = getFoilBufferDirect(pBuffer, rendertype, true, pItemStack.hasFoil());
                } else {
                    vertexconsumer = getFoilBuffer(pBuffer, rendertype, true, pItemStack.hasFoil());
                }

                this.renderModelLists(model, pItemStack, pCombinedLight, pCombinedOverlay, pPoseStack, vertexconsumer);
                }
                }
            } else {
                net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(pItemStack).getCustomRenderer().renderByItem(pItemStack, pDisplayContext, pPoseStack, pBuffer, pCombinedLight, pCombinedOverlay);
            }

            pPoseStack.popPose();
        }
    }

    private static boolean hasAnimatedTexture(ItemStack pStack) {
        return pStack.is(ItemTags.COMPASSES) || pStack.is(Items.CLOCK);
    }

    public static VertexConsumer getArmorFoilBuffer(MultiBufferSource p_115185_, RenderType p_115186_, boolean p_115187_) {
        return p_115187_
            ? VertexMultiConsumer.create(p_115185_.getBuffer(RenderType.armorEntityGlint()), p_115185_.getBuffer(p_115186_))
            : p_115185_.getBuffer(p_115186_);
    }

    public static VertexConsumer getCompassFoilBuffer(MultiBufferSource pBuffer, RenderType pRenderType, PoseStack.Pose pMatrixEntry) {
        return VertexMultiConsumer.create(
            new SheetedDecalTextureGenerator(pBuffer.getBuffer(RenderType.glint()), pMatrixEntry, 0.0078125F), pBuffer.getBuffer(pRenderType)
        );
    }

    public static VertexConsumer getFoilBuffer(MultiBufferSource pBuffer, RenderType pRenderType, boolean pIsItem, boolean pGlint) {
        if (pGlint) {
            return Minecraft.useShaderTransparency() && pRenderType == Sheets.translucentItemSheet()
                ? VertexMultiConsumer.create(pBuffer.getBuffer(RenderType.glintTranslucent()), pBuffer.getBuffer(pRenderType))
                : VertexMultiConsumer.create(pBuffer.getBuffer(pIsItem ? RenderType.glint() : RenderType.entityGlint()), pBuffer.getBuffer(pRenderType));
        } else {
            return pBuffer.getBuffer(pRenderType);
        }
    }

    public static VertexConsumer getFoilBufferDirect(MultiBufferSource pBuffer, RenderType pRenderType, boolean pNoEntity, boolean pWithGlint) {
        return pWithGlint
            ? VertexMultiConsumer.create(pBuffer.getBuffer(pNoEntity ? RenderType.glint() : RenderType.entityGlintDirect()), pBuffer.getBuffer(pRenderType))
            : pBuffer.getBuffer(pRenderType);
    }

    public void renderQuadList(PoseStack pPoseStack, VertexConsumer pBuffer, List<BakedQuad> pQuads, ItemStack pItemStack, int pCombinedLight, int pCombinedOverlay) {
        boolean flag = !pItemStack.isEmpty();
        PoseStack.Pose posestack$pose = pPoseStack.last();

        for (BakedQuad bakedquad : pQuads) {
            int i = -1;
            if (flag && bakedquad.isTinted()) {
                i = this.itemColors.getColor(pItemStack, bakedquad.getTintIndex());
            }

            float f = (float)FastColor.ARGB32.alpha(i) / 255.0F;
            float f1 = (float)FastColor.ARGB32.red(i) / 255.0F;
            float f2 = (float)FastColor.ARGB32.green(i) / 255.0F;
            float f3 = (float)FastColor.ARGB32.blue(i) / 255.0F;
            pBuffer.putBulkData(posestack$pose, bakedquad, f1, f2, f3, f, pCombinedLight, pCombinedOverlay, true); // Neo: pass readExistingColor=true
        }
    }

    public BakedModel getModel(ItemStack pStack, @Nullable Level pLevel, @Nullable LivingEntity pEntity, int pSeed) {
        BakedModel bakedmodel;
        if (pStack.is(Items.TRIDENT)) {
            bakedmodel = this.itemModelShaper.getModelManager().getModel(TRIDENT_IN_HAND_MODEL);
        } else if (pStack.is(Items.SPYGLASS)) {
            bakedmodel = this.itemModelShaper.getModelManager().getModel(SPYGLASS_IN_HAND_MODEL);
        } else {
            bakedmodel = this.itemModelShaper.getItemModel(pStack);
        }

        ClientLevel clientlevel = pLevel instanceof ClientLevel ? (ClientLevel)pLevel : null;
        BakedModel bakedmodel1 = bakedmodel.getOverrides().resolve(bakedmodel, pStack, clientlevel, pEntity, pSeed);
        return bakedmodel1 == null ? this.itemModelShaper.getModelManager().getMissingModel() : bakedmodel1;
    }

    public void renderStatic(
        ItemStack pStack,
        ItemDisplayContext pDisplayContext,
        int pCombinedLight,
        int pCombinedOverlay,
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        @Nullable Level pLevel,
        int pSeed
    ) {
        this.renderStatic(null, pStack, pDisplayContext, false, pPoseStack, pBuffer, pLevel, pCombinedLight, pCombinedOverlay, pSeed);
    }

    public void renderStatic(
        @Nullable LivingEntity pEntity,
        ItemStack pItemStack,
        ItemDisplayContext pDiplayContext,
        boolean pLeftHand,
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        @Nullable Level pLevel,
        int pCombinedLight,
        int pCombinedOverlay,
        int pSeed
    ) {
        if (!pItemStack.isEmpty()) {
            BakedModel bakedmodel = this.getModel(pItemStack, pLevel, pEntity, pSeed);
            this.render(pItemStack, pDiplayContext, pLeftHand, pPoseStack, pBuffer, pCombinedLight, pCombinedOverlay, bakedmodel);
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager pResourceManager) {
        this.itemModelShaper.rebuildCache();
    }

    public BlockEntityWithoutLevelRenderer getBlockEntityRenderer() {
         return blockEntityRenderer;
    }
}
