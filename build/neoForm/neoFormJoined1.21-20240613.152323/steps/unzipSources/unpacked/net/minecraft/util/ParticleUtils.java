package net.minecraft.util;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ParticleUtils {
    public static void spawnParticlesOnBlockFaces(Level pLevel, BlockPos pPos, ParticleOptions pParticle, IntProvider pCount) {
        for (Direction direction : Direction.values()) {
            spawnParticlesOnBlockFace(pLevel, pPos, pParticle, pCount, direction, () -> getRandomSpeedRanges(pLevel.random), 0.55);
        }
    }

    public static void spawnParticlesOnBlockFace(
        Level pLevel, BlockPos pPos, ParticleOptions pParticle, IntProvider pCount, Direction pDirection, Supplier<Vec3> pSpeedSupplier, double p_216325_
    ) {
        int i = pCount.sample(pLevel.random);

        for (int j = 0; j < i; j++) {
            spawnParticleOnFace(pLevel, pPos, pDirection, pParticle, pSpeedSupplier.get(), p_216325_);
        }
    }

    private static Vec3 getRandomSpeedRanges(RandomSource pRandom) {
        return new Vec3(Mth.nextDouble(pRandom, -0.5, 0.5), Mth.nextDouble(pRandom, -0.5, 0.5), Mth.nextDouble(pRandom, -0.5, 0.5));
    }

    public static void spawnParticlesAlongAxis(
        Direction.Axis pAxis, Level pLevel, BlockPos pPos, double p_144971_, ParticleOptions pParticle, UniformInt pCount
    ) {
        Vec3 vec3 = Vec3.atCenterOf(pPos);
        boolean flag = pAxis == Direction.Axis.X;
        boolean flag1 = pAxis == Direction.Axis.Y;
        boolean flag2 = pAxis == Direction.Axis.Z;
        int i = pCount.sample(pLevel.random);

        for (int j = 0; j < i; j++) {
            double d0 = vec3.x + Mth.nextDouble(pLevel.random, -1.0, 1.0) * (flag ? 0.5 : p_144971_);
            double d1 = vec3.y + Mth.nextDouble(pLevel.random, -1.0, 1.0) * (flag1 ? 0.5 : p_144971_);
            double d2 = vec3.z + Mth.nextDouble(pLevel.random, -1.0, 1.0) * (flag2 ? 0.5 : p_144971_);
            double d3 = flag ? Mth.nextDouble(pLevel.random, -1.0, 1.0) : 0.0;
            double d4 = flag1 ? Mth.nextDouble(pLevel.random, -1.0, 1.0) : 0.0;
            double d5 = flag2 ? Mth.nextDouble(pLevel.random, -1.0, 1.0) : 0.0;
            pLevel.addParticle(pParticle, d0, d1, d2, d3, d4, d5);
        }
    }

    public static void spawnParticleOnFace(
        Level pLevel, BlockPos pPos, Direction pDirection, ParticleOptions pParticle, Vec3 pSpeed, double p_216312_
    ) {
        Vec3 vec3 = Vec3.atCenterOf(pPos);
        int i = pDirection.getStepX();
        int j = pDirection.getStepY();
        int k = pDirection.getStepZ();
        double d0 = vec3.x + (i == 0 ? Mth.nextDouble(pLevel.random, -0.5, 0.5) : (double)i * p_216312_);
        double d1 = vec3.y + (j == 0 ? Mth.nextDouble(pLevel.random, -0.5, 0.5) : (double)j * p_216312_);
        double d2 = vec3.z + (k == 0 ? Mth.nextDouble(pLevel.random, -0.5, 0.5) : (double)k * p_216312_);
        double d3 = i == 0 ? pSpeed.x() : 0.0;
        double d4 = j == 0 ? pSpeed.y() : 0.0;
        double d5 = k == 0 ? pSpeed.z() : 0.0;
        pLevel.addParticle(pParticle, d0, d1, d2, d3, d4, d5);
    }

    public static void spawnParticleBelow(Level pLevel, BlockPos pPos, RandomSource pRandom, ParticleOptions pParticle) {
        double d0 = (double)pPos.getX() + pRandom.nextDouble();
        double d1 = (double)pPos.getY() - 0.05;
        double d2 = (double)pPos.getZ() + pRandom.nextDouble();
        pLevel.addParticle(pParticle, d0, d1, d2, 0.0, 0.0, 0.0);
    }

    public static void spawnParticleInBlock(LevelAccessor pLevel, BlockPos pPos, int pCount, ParticleOptions pParticle) {
        double d0 = 0.5;
        BlockState blockstate = pLevel.getBlockState(pPos);
        double d1 = blockstate.isAir() ? 1.0 : blockstate.getShape(pLevel, pPos).max(Direction.Axis.Y);
        spawnParticles(pLevel, pPos, pCount, 0.5, d1, true, pParticle);
    }

    public static void spawnParticles(
        LevelAccessor pLevel, BlockPos pPos, int pCount, double pXzSpread, double pYSpread, boolean pAllowInAir, ParticleOptions pParticle
    ) {
        RandomSource randomsource = pLevel.getRandom();

        for (int i = 0; i < pCount; i++) {
            double d0 = randomsource.nextGaussian() * 0.02;
            double d1 = randomsource.nextGaussian() * 0.02;
            double d2 = randomsource.nextGaussian() * 0.02;
            double d3 = 0.5 - pXzSpread;
            double d4 = (double)pPos.getX() + d3 + randomsource.nextDouble() * pXzSpread * 2.0;
            double d5 = (double)pPos.getY() + randomsource.nextDouble() * pYSpread;
            double d6 = (double)pPos.getZ() + d3 + randomsource.nextDouble() * pXzSpread * 2.0;
            if (pAllowInAir || !pLevel.getBlockState(BlockPos.containing(d4, d5, d6).below()).isAir()) {
                pLevel.addParticle(pParticle, d4, d5, d6, d0, d1, d2);
            }
        }
    }

    public static void spawnSmashAttackParticles(LevelAccessor pLevel, BlockPos pPos, int pPower) {
        Vec3 vec3 = pPos.getCenter().add(0.0, 0.5, 0.0);
        BlockParticleOption blockparticleoption = new BlockParticleOption(ParticleTypes.DUST_PILLAR, pLevel.getBlockState(pPos));

        for (int i = 0; (float)i < (float)pPower / 3.0F; i++) {
            double d0 = vec3.x + pLevel.getRandom().nextGaussian() / 2.0;
            double d1 = vec3.y;
            double d2 = vec3.z + pLevel.getRandom().nextGaussian() / 2.0;
            double d3 = pLevel.getRandom().nextGaussian() * 0.2F;
            double d4 = pLevel.getRandom().nextGaussian() * 0.2F;
            double d5 = pLevel.getRandom().nextGaussian() * 0.2F;
            pLevel.addParticle(blockparticleoption, d0, d1, d2, d3, d4, d5);
        }

        for (int j = 0; (float)j < (float)pPower / 1.5F; j++) {
            double d6 = vec3.x + 3.5 * Math.cos((double)j) + pLevel.getRandom().nextGaussian() / 2.0;
            double d7 = vec3.y;
            double d8 = vec3.z + 3.5 * Math.sin((double)j) + pLevel.getRandom().nextGaussian() / 2.0;
            double d9 = pLevel.getRandom().nextGaussian() * 0.05F;
            double d10 = pLevel.getRandom().nextGaussian() * 0.05F;
            double d11 = pLevel.getRandom().nextGaussian() * 0.05F;
            pLevel.addParticle(blockparticleoption, d6, d7, d8, d9, d10, d11);
        }
    }
}
