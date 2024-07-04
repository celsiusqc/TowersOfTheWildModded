package net.minecraft.client.renderer.culling;

import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class Frustum {
    public static final int OFFSET_STEP = 4;
    private final FrustumIntersection intersection = new FrustumIntersection();
    private final Matrix4f matrix = new Matrix4f();
    private Vector4f viewVector;
    private double camX;
    private double camY;
    private double camZ;

    public Frustum(Matrix4f pProjection, Matrix4f pFrustum) {
        this.calculateFrustum(pProjection, pFrustum);
    }

    public Frustum(Frustum pOther) {
        this.intersection.set(pOther.matrix);
        this.matrix.set(pOther.matrix);
        this.camX = pOther.camX;
        this.camY = pOther.camY;
        this.camZ = pOther.camZ;
        this.viewVector = pOther.viewVector;
    }

    public Frustum offsetToFullyIncludeCameraCube(int pOffset) {
        double d0 = Math.floor(this.camX / (double)pOffset) * (double)pOffset;
        double d1 = Math.floor(this.camY / (double)pOffset) * (double)pOffset;
        double d2 = Math.floor(this.camZ / (double)pOffset) * (double)pOffset;
        double d3 = Math.ceil(this.camX / (double)pOffset) * (double)pOffset;
        double d4 = Math.ceil(this.camY / (double)pOffset) * (double)pOffset;

        for (double d5 = Math.ceil(this.camZ / (double)pOffset) * (double)pOffset;
            this.intersection
                    .intersectAab(
                        (float)(d0 - this.camX),
                        (float)(d1 - this.camY),
                        (float)(d2 - this.camZ),
                        (float)(d3 - this.camX),
                        (float)(d4 - this.camY),
                        (float)(d5 - this.camZ)
                    )
                != -2;
            this.camZ = this.camZ - (double)(this.viewVector.z() * 4.0F)
        ) {
            this.camX = this.camX - (double)(this.viewVector.x() * 4.0F);
            this.camY = this.camY - (double)(this.viewVector.y() * 4.0F);
        }

        return this;
    }

    public void prepare(double pCamX, double pCamY, double pCamZ) {
        this.camX = pCamX;
        this.camY = pCamY;
        this.camZ = pCamZ;
    }

    private void calculateFrustum(Matrix4f pProjection, Matrix4f pFrustum) {
        pFrustum.mul(pProjection, this.matrix);
        this.intersection.set(this.matrix);
        this.viewVector = this.matrix.transformTranspose(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
    }

    public boolean isVisible(AABB pAabb) {
        // FORGE: exit early for infinite bounds, these would otherwise fail in the intersection test at certain camera angles (GH-9321)
        if (pAabb.isInfinite()) return true;
        return this.cubeInFrustum(pAabb.minX, pAabb.minY, pAabb.minZ, pAabb.maxX, pAabb.maxY, pAabb.maxZ);
    }

    private boolean cubeInFrustum(double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ) {
        float f = (float)(pMinX - this.camX);
        float f1 = (float)(pMinY - this.camY);
        float f2 = (float)(pMinZ - this.camZ);
        float f3 = (float)(pMaxX - this.camX);
        float f4 = (float)(pMaxY - this.camY);
        float f5 = (float)(pMaxZ - this.camZ);
        return this.intersection.testAab(f, f1, f2, f3, f4, f5);
    }
}
