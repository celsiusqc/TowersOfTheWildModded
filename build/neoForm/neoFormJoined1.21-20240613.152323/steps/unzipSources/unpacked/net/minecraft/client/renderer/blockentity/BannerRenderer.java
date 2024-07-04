package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BannerRenderer implements BlockEntityRenderer<BannerBlockEntity> {
    private static final int BANNER_WIDTH = 20;
    private static final int BANNER_HEIGHT = 40;
    private static final int MAX_PATTERNS = 16;
    public static final String FLAG = "flag";
    private static final String POLE = "pole";
    private static final String BAR = "bar";
    private final ModelPart flag;
    private final ModelPart pole;
    private final ModelPart bar;

    public BannerRenderer(BlockEntityRendererProvider.Context pContext) {
        ModelPart modelpart = pContext.bakeLayer(ModelLayers.BANNER);
        this.flag = modelpart.getChild("flag");
        this.pole = modelpart.getChild("pole");
        this.bar = modelpart.getChild("bar");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("flag", CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, 0.0F, -2.0F, 20.0F, 40.0F, 1.0F), PartPose.ZERO);
        partdefinition.addOrReplaceChild("pole", CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, -30.0F, -1.0F, 2.0F, 42.0F, 2.0F), PartPose.ZERO);
        partdefinition.addOrReplaceChild("bar", CubeListBuilder.create().texOffs(0, 42).addBox(-10.0F, -32.0F, -1.0F, 20.0F, 2.0F, 2.0F), PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public void render(BannerBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        float f = 0.6666667F;
        boolean flag = pBlockEntity.getLevel() == null;
        pPoseStack.pushPose();
        long i;
        if (flag) {
            i = 0L;
            pPoseStack.translate(0.5F, 0.5F, 0.5F);
            this.pole.visible = true;
        } else {
            i = pBlockEntity.getLevel().getGameTime();
            BlockState blockstate = pBlockEntity.getBlockState();
            if (blockstate.getBlock() instanceof BannerBlock) {
                pPoseStack.translate(0.5F, 0.5F, 0.5F);
                float f1 = -RotationSegment.convertToDegrees(blockstate.getValue(BannerBlock.ROTATION));
                pPoseStack.mulPose(Axis.YP.rotationDegrees(f1));
                this.pole.visible = true;
            } else {
                pPoseStack.translate(0.5F, -0.16666667F, 0.5F);
                float f3 = -blockstate.getValue(WallBannerBlock.FACING).toYRot();
                pPoseStack.mulPose(Axis.YP.rotationDegrees(f3));
                pPoseStack.translate(0.0F, -0.3125F, -0.4375F);
                this.pole.visible = false;
            }
        }

        pPoseStack.pushPose();
        pPoseStack.scale(0.6666667F, -0.6666667F, -0.6666667F);
        VertexConsumer vertexconsumer = ModelBakery.BANNER_BASE.buffer(pBuffer, RenderType::entitySolid);
        this.pole.render(pPoseStack, vertexconsumer, pPackedLight, pPackedOverlay);
        this.bar.render(pPoseStack, vertexconsumer, pPackedLight, pPackedOverlay);
        BlockPos blockpos = pBlockEntity.getBlockPos();
        float f2 = ((float)Math.floorMod((long)(blockpos.getX() * 7 + blockpos.getY() * 9 + blockpos.getZ() * 13) + i, 100L) + pPartialTick) / 100.0F;
        this.flag.xRot = (-0.0125F + 0.01F * Mth.cos((float) (Math.PI * 2) * f2)) * (float) Math.PI;
        this.flag.y = -32.0F;
        renderPatterns(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, this.flag, ModelBakery.BANNER_BASE, true, pBlockEntity.getBaseColor(), pBlockEntity.getPatterns());
        pPoseStack.popPose();
        pPoseStack.popPose();
    }

    /**
     * @param pBanner if {@code true}, uses banner material; otherwise if {@code false
     *                } uses shield material
     */
    public static void renderPatterns(
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight,
        int pPackedOverlay,
        ModelPart pFlagPart,
        Material pFlagMaterial,
        boolean pBanner,
        DyeColor pBaseColor,
        BannerPatternLayers pPatterns
    ) {
        renderPatterns(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pFlagPart, pFlagMaterial, pBanner, pBaseColor, pPatterns, false);
    }

    /**
     * @param pBanner if {@code true}, uses banner material; otherwise if {@code false
     *                } uses shield material
     */
    public static void renderPatterns(
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight,
        int pPackedOverlay,
        ModelPart pFlagPart,
        Material pFlagMaterial,
        boolean pBanner,
        DyeColor pBaseColor,
        BannerPatternLayers pPatterns,
        boolean pGlint
    ) {
        pFlagPart.render(pPoseStack, pFlagMaterial.buffer(pBuffer, RenderType::entitySolid, pGlint), pPackedLight, pPackedOverlay);
        renderPatternLayer(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pFlagPart, pBanner ? Sheets.BANNER_BASE : Sheets.SHIELD_BASE, pBaseColor);

        for (int i = 0; i < 16 && i < pPatterns.layers().size(); i++) {
            BannerPatternLayers.Layer bannerpatternlayers$layer = pPatterns.layers().get(i);
            Material material = pBanner
                ? Sheets.getBannerMaterial(bannerpatternlayers$layer.pattern())
                : Sheets.getShieldMaterial(bannerpatternlayers$layer.pattern());
            renderPatternLayer(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pFlagPart, material, bannerpatternlayers$layer.color());
        }
    }

    private static void renderPatternLayer(
        PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay, ModelPart pFlagPart, Material pMaterial, DyeColor pColor
    ) {
        int i = pColor.getTextureDiffuseColor();
        pFlagPart.render(pPoseStack, pMaterial.buffer(pBuffer, RenderType::entityNoOutline), pPackedLight, pPackedOverlay, i);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(BannerBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        boolean standing = blockEntity.getBlockState().getBlock() instanceof BannerBlock;
        return net.minecraft.world.phys.AABB.encapsulatingFullBlocks(pos, standing ? pos.above() : pos.below());
    }
}
