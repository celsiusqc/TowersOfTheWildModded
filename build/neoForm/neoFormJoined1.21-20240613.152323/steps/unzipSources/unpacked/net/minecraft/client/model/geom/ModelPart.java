package net.minecraft.client.model.geom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class ModelPart {
    public static final float DEFAULT_SCALE = 1.0F;
    public float x;
    public float y;
    public float z;
    public float xRot;
    public float yRot;
    public float zRot;
    public float xScale = 1.0F;
    public float yScale = 1.0F;
    public float zScale = 1.0F;
    public boolean visible = true;
    public boolean skipDraw;
    private final List<ModelPart.Cube> cubes;
    private final Map<String, ModelPart> children;
    private PartPose initialPose = PartPose.ZERO;

    public ModelPart(List<ModelPart.Cube> pCubes, Map<String, ModelPart> pChildren) {
        this.cubes = pCubes;
        this.children = pChildren;
    }

    public PartPose storePose() {
        return PartPose.offsetAndRotation(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot);
    }

    public PartPose getInitialPose() {
        return this.initialPose;
    }

    public void setInitialPose(PartPose pInitialPose) {
        this.initialPose = pInitialPose;
    }

    public void resetPose() {
        this.loadPose(this.initialPose);
    }

    public void loadPose(PartPose pPartPose) {
        this.x = pPartPose.x;
        this.y = pPartPose.y;
        this.z = pPartPose.z;
        this.xRot = pPartPose.xRot;
        this.yRot = pPartPose.yRot;
        this.zRot = pPartPose.zRot;
        this.xScale = 1.0F;
        this.yScale = 1.0F;
        this.zScale = 1.0F;
    }

    public void copyFrom(ModelPart pModelPart) {
        this.xScale = pModelPart.xScale;
        this.yScale = pModelPart.yScale;
        this.zScale = pModelPart.zScale;
        this.xRot = pModelPart.xRot;
        this.yRot = pModelPart.yRot;
        this.zRot = pModelPart.zRot;
        this.x = pModelPart.x;
        this.y = pModelPart.y;
        this.z = pModelPart.z;
    }

    public boolean hasChild(String pName) {
        return this.children.containsKey(pName);
    }

    public ModelPart getChild(String pName) {
        ModelPart modelpart = this.children.get(pName);
        if (modelpart == null) {
            throw new NoSuchElementException("Can't find part " + pName);
        } else {
            return modelpart;
        }
    }

    public void setPos(float pX, float pY, float pZ) {
        this.x = pX;
        this.y = pY;
        this.z = pZ;
    }

    public void setRotation(float pXRot, float pYRot, float pZRot) {
        this.xRot = pXRot;
        this.yRot = pYRot;
        this.zRot = pZRot;
    }

    public void render(PoseStack pPoseStack, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay) {
        this.render(pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, -1);
    }

    public void render(PoseStack pPoseStack, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, int p_350978_) {
        if (this.visible) {
            if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
                pPoseStack.pushPose();
                this.translateAndRotate(pPoseStack);
                if (!this.skipDraw) {
                    this.compile(pPoseStack.last(), pVertexConsumer, pPackedLight, pPackedOverlay, p_350978_);
                }

                for (ModelPart modelpart : this.children.values()) {
                    modelpart.render(pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, p_350978_);
                }

                pPoseStack.popPose();
            }
        }
    }

    public void visit(PoseStack pPoseStack, ModelPart.Visitor pVisitor) {
        this.visit(pPoseStack, pVisitor, "");
    }

    private void visit(PoseStack pPoseStack, ModelPart.Visitor pVisitor, String pPath) {
        if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
            pPoseStack.pushPose();
            this.translateAndRotate(pPoseStack);
            PoseStack.Pose posestack$pose = pPoseStack.last();

            for (int i = 0; i < this.cubes.size(); i++) {
                pVisitor.visit(posestack$pose, pPath, i, this.cubes.get(i));
            }

            String s = pPath + "/";
            this.children.forEach((p_171320_, p_171321_) -> p_171321_.visit(pPoseStack, pVisitor, s + p_171320_));
            pPoseStack.popPose();
        }
    }

    public void translateAndRotate(PoseStack pPoseStack) {
        pPoseStack.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);
        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
            pPoseStack.mulPose(new Quaternionf().rotationZYX(this.zRot, this.yRot, this.xRot));
        }

        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            pPoseStack.scale(this.xScale, this.yScale, this.zScale);
        }
    }

    private void compile(PoseStack.Pose p_104291_, VertexConsumer p_104292_, int p_104293_, int p_104294_, int p_350388_) {
        for (ModelPart.Cube modelpart$cube : this.cubes) {
            modelpart$cube.compile(p_104291_, p_104292_, p_104293_, p_104294_, p_350388_);
        }
    }

    public ModelPart.Cube getRandomCube(RandomSource pRandom) {
        return this.cubes.get(pRandom.nextInt(this.cubes.size()));
    }

    public boolean isEmpty() {
        return this.cubes.isEmpty();
    }

    public void offsetPos(Vector3f pOffset) {
        this.x = this.x + pOffset.x();
        this.y = this.y + pOffset.y();
        this.z = this.z + pOffset.z();
    }

    public void offsetRotation(Vector3f pOffset) {
        this.xRot = this.xRot + pOffset.x();
        this.yRot = this.yRot + pOffset.y();
        this.zRot = this.zRot + pOffset.z();
    }

    public void offsetScale(Vector3f pOffset) {
        this.xScale = this.xScale + pOffset.x();
        this.yScale = this.yScale + pOffset.y();
        this.zScale = this.zScale + pOffset.z();
    }

    public Stream<ModelPart> getAllParts() {
        return Stream.concat(Stream.of(this), this.children.values().stream().flatMap(ModelPart::getAllParts));
    }

    @OnlyIn(Dist.CLIENT)
    public static class Cube {
        private final ModelPart.Polygon[] polygons;
        public final float minX;
        public final float minY;
        public final float minZ;
        public final float maxX;
        public final float maxY;
        public final float maxZ;

        public Cube(
            int pTexCoordU,
            int pTexCoordV,
            float pOriginX,
            float pOriginY,
            float pOriginZ,
            float pDimensionX,
            float pDimensionY,
            float pDimensionZ,
            float pGtowX,
            float pGrowY,
            float pGrowZ,
            boolean pMirror,
            float pTexScaleU,
            float pTexScaleV,
            Set<Direction> pVisibleFaces
        ) {
            this.minX = pOriginX;
            this.minY = pOriginY;
            this.minZ = pOriginZ;
            this.maxX = pOriginX + pDimensionX;
            this.maxY = pOriginY + pDimensionY;
            this.maxZ = pOriginZ + pDimensionZ;
            this.polygons = new ModelPart.Polygon[pVisibleFaces.size()];
            float f = pOriginX + pDimensionX;
            float f1 = pOriginY + pDimensionY;
            float f2 = pOriginZ + pDimensionZ;
            pOriginX -= pGtowX;
            pOriginY -= pGrowY;
            pOriginZ -= pGrowZ;
            f += pGtowX;
            f1 += pGrowY;
            f2 += pGrowZ;
            if (pMirror) {
                float f3 = f;
                f = pOriginX;
                pOriginX = f3;
            }

            ModelPart.Vertex modelpart$vertex7 = new ModelPart.Vertex(pOriginX, pOriginY, pOriginZ, 0.0F, 0.0F);
            ModelPart.Vertex modelpart$vertex = new ModelPart.Vertex(f, pOriginY, pOriginZ, 0.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex1 = new ModelPart.Vertex(f, f1, pOriginZ, 8.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex2 = new ModelPart.Vertex(pOriginX, f1, pOriginZ, 8.0F, 0.0F);
            ModelPart.Vertex modelpart$vertex3 = new ModelPart.Vertex(pOriginX, pOriginY, f2, 0.0F, 0.0F);
            ModelPart.Vertex modelpart$vertex4 = new ModelPart.Vertex(f, pOriginY, f2, 0.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex5 = new ModelPart.Vertex(f, f1, f2, 8.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex6 = new ModelPart.Vertex(pOriginX, f1, f2, 8.0F, 0.0F);
            float f4 = (float)pTexCoordU;
            float f5 = (float)pTexCoordU + pDimensionZ;
            float f6 = (float)pTexCoordU + pDimensionZ + pDimensionX;
            float f7 = (float)pTexCoordU + pDimensionZ + pDimensionX + pDimensionX;
            float f8 = (float)pTexCoordU + pDimensionZ + pDimensionX + pDimensionZ;
            float f9 = (float)pTexCoordU + pDimensionZ + pDimensionX + pDimensionZ + pDimensionX;
            float f10 = (float)pTexCoordV;
            float f11 = (float)pTexCoordV + pDimensionZ;
            float f12 = (float)pTexCoordV + pDimensionZ + pDimensionY;
            int i = 0;
            if (pVisibleFaces.contains(Direction.DOWN)) {
                this.polygons[i++] = new ModelPart.Polygon(
                    new ModelPart.Vertex[]{modelpart$vertex4, modelpart$vertex3, modelpart$vertex7, modelpart$vertex},
                    f5,
                    f10,
                    f6,
                    f11,
                    pTexScaleU,
                    pTexScaleV,
                    pMirror,
                    Direction.DOWN
                );
            }

            if (pVisibleFaces.contains(Direction.UP)) {
                this.polygons[i++] = new ModelPart.Polygon(
                    new ModelPart.Vertex[]{modelpart$vertex1, modelpart$vertex2, modelpart$vertex6, modelpart$vertex5},
                    f6,
                    f11,
                    f7,
                    f10,
                    pTexScaleU,
                    pTexScaleV,
                    pMirror,
                    Direction.UP
                );
            }

            if (pVisibleFaces.contains(Direction.WEST)) {
                this.polygons[i++] = new ModelPart.Polygon(
                    new ModelPart.Vertex[]{modelpart$vertex7, modelpart$vertex3, modelpart$vertex6, modelpart$vertex2},
                    f4,
                    f11,
                    f5,
                    f12,
                    pTexScaleU,
                    pTexScaleV,
                    pMirror,
                    Direction.WEST
                );
            }

            if (pVisibleFaces.contains(Direction.NORTH)) {
                this.polygons[i++] = new ModelPart.Polygon(
                    new ModelPart.Vertex[]{modelpart$vertex, modelpart$vertex7, modelpart$vertex2, modelpart$vertex1},
                    f5,
                    f11,
                    f6,
                    f12,
                    pTexScaleU,
                    pTexScaleV,
                    pMirror,
                    Direction.NORTH
                );
            }

            if (pVisibleFaces.contains(Direction.EAST)) {
                this.polygons[i++] = new ModelPart.Polygon(
                    new ModelPart.Vertex[]{modelpart$vertex4, modelpart$vertex, modelpart$vertex1, modelpart$vertex5},
                    f6,
                    f11,
                    f8,
                    f12,
                    pTexScaleU,
                    pTexScaleV,
                    pMirror,
                    Direction.EAST
                );
            }

            if (pVisibleFaces.contains(Direction.SOUTH)) {
                this.polygons[i] = new ModelPart.Polygon(
                    new ModelPart.Vertex[]{modelpart$vertex3, modelpart$vertex4, modelpart$vertex5, modelpart$vertex6},
                    f8,
                    f11,
                    f9,
                    f12,
                    pTexScaleU,
                    pTexScaleV,
                    pMirror,
                    Direction.SOUTH
                );
            }
        }

        public void compile(PoseStack.Pose p_171333_, VertexConsumer p_171334_, int p_171335_, int p_171336_, int p_350744_) {
            Matrix4f matrix4f = p_171333_.pose();
            Vector3f vector3f = new Vector3f();

            for (ModelPart.Polygon modelpart$polygon : this.polygons) {
                Vector3f vector3f1 = p_171333_.transformNormal(modelpart$polygon.normal, vector3f);
                float f = vector3f1.x();
                float f1 = vector3f1.y();
                float f2 = vector3f1.z();

                for (ModelPart.Vertex modelpart$vertex : modelpart$polygon.vertices) {
                    float f3 = modelpart$vertex.pos.x() / 16.0F;
                    float f4 = modelpart$vertex.pos.y() / 16.0F;
                    float f5 = modelpart$vertex.pos.z() / 16.0F;
                    Vector3f vector3f2 = matrix4f.transformPosition(f3, f4, f5, vector3f);
                    p_171334_.addVertex(
                        vector3f2.x(), vector3f2.y(), vector3f2.z(), p_350744_, modelpart$vertex.u, modelpart$vertex.v, p_171336_, p_171335_, f, f1, f2
                    );
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Polygon {
        public final ModelPart.Vertex[] vertices;
        public final Vector3f normal;

        public Polygon(
            ModelPart.Vertex[] pVertices,
            float pU1,
            float pV1,
            float pU2,
            float pV2,
            float pTextureWidth,
            float pTextureHeight,
            boolean pMirror,
            Direction pDirection
        ) {
            this.vertices = pVertices;
            float f = 0.0F / pTextureWidth;
            float f1 = 0.0F / pTextureHeight;
            pVertices[0] = pVertices[0].remap(pU2 / pTextureWidth - f, pV1 / pTextureHeight + f1);
            pVertices[1] = pVertices[1].remap(pU1 / pTextureWidth + f, pV1 / pTextureHeight + f1);
            pVertices[2] = pVertices[2].remap(pU1 / pTextureWidth + f, pV2 / pTextureHeight - f1);
            pVertices[3] = pVertices[3].remap(pU2 / pTextureWidth - f, pV2 / pTextureHeight - f1);
            if (pMirror) {
                int i = pVertices.length;

                for (int j = 0; j < i / 2; j++) {
                    ModelPart.Vertex modelpart$vertex = pVertices[j];
                    pVertices[j] = pVertices[i - 1 - j];
                    pVertices[i - 1 - j] = modelpart$vertex;
                }
            }

            this.normal = pDirection.step();
            if (pMirror) {
                this.normal.mul(-1.0F, 1.0F, 1.0F);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Vertex {
        public final Vector3f pos;
        public final float u;
        public final float v;

        public Vertex(float pX, float pY, float pZ, float pU, float pV) {
            this(new Vector3f(pX, pY, pZ), pU, pV);
        }

        public ModelPart.Vertex remap(float pU, float pV) {
            return new ModelPart.Vertex(this.pos, pU, pV);
        }

        public Vertex(Vector3f pPos, float pU, float pV) {
            this.pos = pPos;
            this.u = pU;
            this.v = pV;
        }
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface Visitor {
        void visit(PoseStack.Pose pPose, String pPath, int pIndex, ModelPart.Cube pCube);
    }
}
