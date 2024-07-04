package net.minecraft.client.particle;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TerrainParticle extends TextureSheetParticle {
    private final BlockPos pos;
    private final float uo;
    private final float vo;

    public TerrainParticle(
        ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed, BlockState pState
    ) {
        this(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, pState, BlockPos.containing(pX, pY, pZ));
    }

    public TerrainParticle(
        ClientLevel pLevel,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed,
        BlockState pState,
        BlockPos pPos
    ) {
        super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
        this.pos = pPos;
        this.setSprite(Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(pState));
        this.gravity = 1.0F;
        this.rCol = 0.6F;
        this.gCol = 0.6F;
        this.bCol = 0.6F;
        if (net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions.of(pState).areBreakingParticlesTinted(pState, pLevel, pPos)) {
            int i = Minecraft.getInstance().getBlockColors().getColor(pState, pLevel, pPos, 0);
            this.rCol *= (float)(i >> 16 & 0xFF) / 255.0F;
            this.gCol *= (float)(i >> 8 & 0xFF) / 255.0F;
            this.bCol *= (float)(i & 0xFF) / 255.0F;
        }

        this.quadSize /= 2.0F;
        this.uo = this.random.nextFloat() * 3.0F;
        this.vo = this.random.nextFloat() * 3.0F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    @Override
    protected float getU0() {
        return this.sprite.getU((this.uo + 1.0F) / 4.0F);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU(this.uo / 4.0F);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(this.vo / 4.0F);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV((this.vo + 1.0F) / 4.0F);
    }

    @Override
    public int getLightColor(float pPartialTick) {
        int i = super.getLightColor(pPartialTick);
        return i == 0 && this.level.hasChunkAt(this.pos) ? LevelRenderer.getLightColor(this.level, this.pos) : i;
    }

    @Nullable
    static TerrainParticle createTerrainParticle(
        BlockParticleOption pType,
        ClientLevel pLevel,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed
    ) {
        BlockState blockstate = pType.getState();
        return !blockstate.isAir() && !blockstate.is(Blocks.MOVING_PISTON) && blockstate.shouldSpawnTerrainParticles()
            ? new TerrainParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, blockstate).updateSprite(blockstate, pType.getPos())
            : null;
    }

    public TerrainParticle updateSprite(BlockState state, BlockPos pos) { //FORGE: we cannot assume that the x y z of the particles match the block pos of the block.
        if (pos != null) // There are cases where we are not able to obtain the correct source pos, and need to fallback to the non-model data version
            this.setSprite(Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getTexture(state, level, pos));
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    public static class DustPillarProvider implements ParticleProvider<BlockParticleOption> {
        @Nullable
        public Particle createParticle(
            BlockParticleOption p_338199_,
            ClientLevel p_338462_,
            double p_338552_,
            double p_338714_,
            double p_338211_,
            double p_338881_,
            double p_338238_,
            double p_338376_
        ) {
            Particle particle = TerrainParticle.createTerrainParticle(p_338199_, p_338462_, p_338552_, p_338714_, p_338211_, p_338881_, p_338238_, p_338376_);
            if (particle != null) {
                particle.setParticleSpeed(
                    p_338462_.random.nextGaussian() / 30.0, p_338238_ + p_338462_.random.nextGaussian() / 2.0, p_338462_.random.nextGaussian() / 30.0
                );
                particle.setLifetime(p_338462_.random.nextInt(20) + 20);
            }

            return particle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<BlockParticleOption> {
        @Nullable
        public Particle createParticle(
            BlockParticleOption pType,
            ClientLevel pLevel,
            double pX,
            double pY,
            double pZ,
            double pXSpeed,
            double pYSpeed,
            double pZSpeed
        ) {
            return TerrainParticle.createTerrainParticle(pType, pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
        }
    }
}
