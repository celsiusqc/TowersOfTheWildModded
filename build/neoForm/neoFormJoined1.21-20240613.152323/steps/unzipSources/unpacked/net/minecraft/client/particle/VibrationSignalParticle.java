package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class VibrationSignalParticle extends TextureSheetParticle {
    private final PositionSource target;
    private float rot;
    private float rotO;
    private float pitch;
    private float pitchO;

    VibrationSignalParticle(ClientLevel pLevel, double pX, double pY, double pZ, PositionSource pTarget, int pLifetime) {
        super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);
        this.quadSize = 0.3F;
        this.target = pTarget;
        this.lifetime = pLifetime;
        Optional<Vec3> optional = pTarget.getPosition(pLevel);
        if (optional.isPresent()) {
            Vec3 vec3 = optional.get();
            double d0 = pX - vec3.x();
            double d1 = pY - vec3.y();
            double d2 = pZ - vec3.z();
            this.rotO = this.rot = (float)Mth.atan2(d0, d2);
            this.pitchO = this.pitch = (float)Mth.atan2(d1, Math.sqrt(d0 * d0 + d2 * d2));
        }
    }

    @Override
    public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
        float f = Mth.sin(((float)this.age + pPartialTicks - (float) (Math.PI * 2)) * 0.05F) * 2.0F;
        float f1 = Mth.lerp(pPartialTicks, this.rotO, this.rot);
        float f2 = Mth.lerp(pPartialTicks, this.pitchO, this.pitch) + (float) (Math.PI / 2);
        Quaternionf quaternionf = new Quaternionf();
        quaternionf.rotationY(f1).rotateX(-f2).rotateY(f);
        this.renderRotatedQuad(pBuffer, pRenderInfo, quaternionf, pPartialTicks);
        quaternionf.rotationY((float) -Math.PI + f1).rotateX(f2).rotateY(f);
        this.renderRotatedQuad(pBuffer, pRenderInfo, quaternionf, pPartialTicks);
    }

    @Override
    public int getLightColor(float pPartialTick) {
        return 240;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            Optional<Vec3> optional = this.target.getPosition(this.level);
            if (optional.isEmpty()) {
                this.remove();
            } else {
                int i = this.lifetime - this.age;
                double d0 = 1.0 / (double)i;
                Vec3 vec3 = optional.get();
                this.x = Mth.lerp(d0, this.x, vec3.x());
                this.y = Mth.lerp(d0, this.y, vec3.y());
                this.z = Mth.lerp(d0, this.z, vec3.z());
                this.setPos(this.x, this.y, this.z); // FORGE: Update the particle's bounding box
                double d1 = this.x - vec3.x();
                double d2 = this.y - vec3.y();
                double d3 = this.z - vec3.z();
                this.rotO = this.rot;
                this.rot = (float)Mth.atan2(d1, d3);
                this.pitchO = this.pitch;
                this.pitch = (float)Mth.atan2(d2, Math.sqrt(d1 * d1 + d3 * d3));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<VibrationParticleOption> {
        private final SpriteSet sprite;

        public Provider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        public Particle createParticle(
            VibrationParticleOption pType,
            ClientLevel pLevel,
            double pX,
            double pY,
            double pZ,
            double pXSpeed,
            double pYSpeed,
            double pZSpeed
        ) {
            VibrationSignalParticle vibrationsignalparticle = new VibrationSignalParticle(
                pLevel, pX, pY, pZ, pType.getDestination(), pType.getArrivalInTicks()
            );
            vibrationsignalparticle.pickSprite(this.sprite);
            vibrationsignalparticle.setAlpha(1.0F);
            return vibrationsignalparticle;
        }
    }
}
