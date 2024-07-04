package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public abstract class SingleQuadParticle extends Particle {
    protected float quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;

    protected SingleQuadParticle(ClientLevel pLevel, double pX, double pY, double pZ) {
        super(pLevel, pX, pY, pZ);
    }

    protected SingleQuadParticle(
        ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
    ) {
        super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
        return SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ;
    }

    @Override
    public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
        Quaternionf quaternionf = new Quaternionf();
        this.getFacingCameraMode().setRotation(quaternionf, pRenderInfo, pPartialTicks);
        if (this.roll != 0.0F) {
            quaternionf.rotateZ(Mth.lerp(pPartialTicks, this.oRoll, this.roll));
        }

        this.renderRotatedQuad(pBuffer, pRenderInfo, quaternionf, pPartialTicks);
    }

    protected void renderRotatedQuad(VertexConsumer p_345690_, Camera p_344809_, Quaternionf p_344798_, float p_345099_) {
        Vec3 vec3 = p_344809_.getPosition();
        float f = (float)(Mth.lerp((double)p_345099_, this.xo, this.x) - vec3.x());
        float f1 = (float)(Mth.lerp((double)p_345099_, this.yo, this.y) - vec3.y());
        float f2 = (float)(Mth.lerp((double)p_345099_, this.zo, this.z) - vec3.z());
        this.renderRotatedQuad(p_345690_, p_344798_, f, f1, f2, p_345099_);
    }

    protected void renderRotatedQuad(VertexConsumer p_346432_, Quaternionf p_345557_, float p_345634_, float p_345953_, float p_345531_, float p_346426_) {
        float f = this.getQuadSize(p_346426_);
        float f1 = this.getU0();
        float f2 = this.getU1();
        float f3 = this.getV0();
        float f4 = this.getV1();
        int i = this.getLightColor(p_346426_);
        this.renderVertex(p_346432_, p_345557_, p_345634_, p_345953_, p_345531_, 1.0F, -1.0F, f, f2, f4, i);
        this.renderVertex(p_346432_, p_345557_, p_345634_, p_345953_, p_345531_, 1.0F, 1.0F, f, f2, f3, i);
        this.renderVertex(p_346432_, p_345557_, p_345634_, p_345953_, p_345531_, -1.0F, 1.0F, f, f1, f3, i);
        this.renderVertex(p_346432_, p_345557_, p_345634_, p_345953_, p_345531_, -1.0F, -1.0F, f, f1, f4, i);
    }

    private void renderVertex(
        VertexConsumer p_345983_,
        Quaternionf p_345441_,
        float p_345563_,
        float p_345839_,
        float p_346305_,
        float p_345242_,
        float p_346207_,
        float p_344893_,
        float p_345909_,
        float p_345984_,
        int p_345037_
    ) {
        Vector3f vector3f = new Vector3f(p_345242_, p_346207_, 0.0F).rotate(p_345441_).mul(p_344893_).add(p_345563_, p_345839_, p_346305_);
        p_345983_.addVertex(vector3f.x(), vector3f.y(), vector3f.z())
            .setUv(p_345909_, p_345984_)
            .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
            .setLight(p_345037_);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(float partialTicks) {
        float size = getQuadSize(partialTicks);
        return new net.minecraft.world.phys.AABB(this.x - size, this.y - size, this.z - size, this.x + size, this.y + size, this.z + size);
    }

    public float getQuadSize(float pScaleFactor) {
        return this.quadSize;
    }

    @Override
    public Particle scale(float pScale) {
        this.quadSize *= pScale;
        return super.scale(pScale);
    }

    protected abstract float getU0();

    protected abstract float getU1();

    protected abstract float getV0();

    protected abstract float getV1();

    @OnlyIn(Dist.CLIENT)
    public interface FacingCameraMode {
        SingleQuadParticle.FacingCameraMode LOOKAT_XYZ = (p_312316_, p_311843_, p_312119_) -> p_312316_.set(p_311843_.rotation());
        SingleQuadParticle.FacingCameraMode LOOKAT_Y = (p_312695_, p_312346_, p_312064_) -> p_312695_.set(
                0.0F, p_312346_.rotation().y, 0.0F, p_312346_.rotation().w
            );

        void setRotation(Quaternionf pQuaternion, Camera pCamera, float pPartialTick);
    }
}
