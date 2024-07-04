package com.mojang.blaze3d.vertex;

import com.google.common.collect.Queues;
import com.mojang.math.MatrixUtil;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class PoseStack implements net.neoforged.neoforge.client.extensions.IPoseStackExtension {
    private final Deque<PoseStack.Pose> poseStack = Util.make(Queues.newArrayDeque(), p_85848_ -> {
        Matrix4f matrix4f = new Matrix4f();
        Matrix3f matrix3f = new Matrix3f();
        p_85848_.add(new PoseStack.Pose(matrix4f, matrix3f));
    });

    public void translate(double pX, double pY, double pZ) {
        this.translate((float)pX, (float)pY, (float)pZ);
    }

    public void translate(float pX, float pY, float pZ) {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.translate(pX, pY, pZ);
    }

    public void scale(float pX, float pY, float pZ) {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.scale(pX, pY, pZ);
        if (Math.abs(pX) == Math.abs(pY) && Math.abs(pY) == Math.abs(pZ)) {
            if (pX < 0.0F || pY < 0.0F || pZ < 0.0F) {
                posestack$pose.normal.scale(Math.signum(pX), Math.signum(pY), Math.signum(pZ));
            }
        } else {
            posestack$pose.normal.scale(1.0F / pX, 1.0F / pY, 1.0F / pZ);
            posestack$pose.trustedNormals = false;
        }
    }

    public void mulPose(Quaternionf pQuaternion) {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.rotate(pQuaternion);
        posestack$pose.normal.rotate(pQuaternion);
    }

    public void rotateAround(Quaternionf pQuaternion, float pX, float pY, float pZ) {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.rotateAround(pQuaternion, pX, pY, pZ);
        posestack$pose.normal.rotate(pQuaternion);
    }

    public void pushPose() {
        this.poseStack.addLast(new PoseStack.Pose(this.poseStack.getLast()));
    }

    public void popPose() {
        this.poseStack.removeLast();
    }

    public PoseStack.Pose last() {
        return this.poseStack.getLast();
    }

    public boolean clear() {
        return this.poseStack.size() == 1;
    }

    public void setIdentity() {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.identity();
        posestack$pose.normal.identity();
        posestack$pose.trustedNormals = true;
    }

    public void mulPose(Matrix4f pPose) {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.mul(pPose);
        if (!MatrixUtil.isPureTranslation(pPose)) {
            if (MatrixUtil.isOrthonormal(pPose)) {
                posestack$pose.normal.mul(new Matrix3f(pPose));
            } else {
                posestack$pose.computeNormalMatrix();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class Pose {
        final Matrix4f pose;
        final Matrix3f normal;
        boolean trustedNormals = true;

        Pose(Matrix4f pPose, Matrix3f pNormal) {
            this.pose = pPose;
            this.normal = pNormal;
        }

        Pose(PoseStack.Pose pPose) {
            this.pose = new Matrix4f(pPose.pose);
            this.normal = new Matrix3f(pPose.normal);
            this.trustedNormals = pPose.trustedNormals;
        }

        void computeNormalMatrix() {
            this.normal.set(this.pose).invert().transpose();
            this.trustedNormals = false;
        }

        public Matrix4f pose() {
            return this.pose;
        }

        public Matrix3f normal() {
            return this.normal;
        }

        public Vector3f transformNormal(Vector3f pVector, Vector3f pDestination) {
            return this.transformNormal(pVector.x, pVector.y, pVector.z, pDestination);
        }

        public Vector3f transformNormal(float pX, float pY, float pZ, Vector3f pDestination) {
            Vector3f vector3f = this.normal.transform(pX, pY, pZ, pDestination);
            return this.trustedNormals ? vector3f : vector3f.normalize();
        }

        public PoseStack.Pose copy() {
            return new PoseStack.Pose(this);
        }
    }
}
