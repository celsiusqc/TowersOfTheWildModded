package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WaterDropParticle extends TextureSheetParticle {
    protected WaterDropParticle(ClientLevel pLevel, double pX, double pY, double pZ) {
        super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);
        this.xd *= 0.3F;
        this.yd = Math.random() * 0.2F + 0.1F;
        this.zd *= 0.3F;
        this.setSize(0.01F, 0.01F);
        this.gravity = 0.06F;
        this.lifetime = (int)(8.0 / (Math.random() * 0.8 + 0.2));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.lifetime-- <= 0) {
            this.remove();
        } else {
            this.yd = this.yd - (double)this.gravity;
            this.move(this.xd, this.yd, this.zd);
            this.xd *= 0.98F;
            this.yd *= 0.98F;
            this.zd *= 0.98F;
            if (this.onGround) {
                if (Math.random() < 0.5) {
                    this.remove();
                }

                this.xd *= 0.7F;
                this.zd *= 0.7F;
            }

            BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
            double d0 = Math.max(
                this.level
                    .getBlockState(blockpos)
                    .getCollisionShape(this.level, blockpos)
                    .max(Direction.Axis.Y, this.x - (double)blockpos.getX(), this.z - (double)blockpos.getZ()),
                (double)this.level.getFluidState(blockpos).getHeight(this.level, blockpos)
            );
            if (d0 > 0.0 && this.y < (double)blockpos.getY() + d0) {
                this.remove();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        public Particle createParticle(
            SimpleParticleType pType,
            ClientLevel pLevel,
            double pX,
            double pY,
            double pZ,
            double pXSpeed,
            double pYSpeed,
            double pZSpeed
        ) {
            WaterDropParticle waterdropparticle = new WaterDropParticle(pLevel, pX, pY, pZ);
            waterdropparticle.pickSprite(this.sprite);
            return waterdropparticle;
        }
    }
}
