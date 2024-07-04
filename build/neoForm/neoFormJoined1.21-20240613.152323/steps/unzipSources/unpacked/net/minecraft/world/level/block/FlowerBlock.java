package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FlowerBlock extends BushBlock implements SuspiciousEffectHolder {
    protected static final MapCodec<SuspiciousStewEffects> EFFECTS_FIELD = SuspiciousStewEffects.CODEC.fieldOf("suspicious_stew_effects");
    public static final MapCodec<FlowerBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_308824_ -> p_308824_.group(EFFECTS_FIELD.forGetter(FlowerBlock::getSuspiciousEffects), propertiesCodec()).apply(p_308824_, FlowerBlock::new)
    );
    protected static final float AABB_OFFSET = 3.0F;
    protected static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 10.0, 11.0);
    private final SuspiciousStewEffects suspiciousStewEffects;

    @Override
    public MapCodec<? extends FlowerBlock> codec() {
        return CODEC;
    }

    public FlowerBlock(Holder<MobEffect> pEffect, float pSeconds, BlockBehaviour.Properties pProperties) {
        this(makeEffectList(pEffect, pSeconds), pProperties);
    }

    public FlowerBlock(SuspiciousStewEffects p_330645_, BlockBehaviour.Properties p_304822_) {
        super(p_304822_);
        this.suspiciousStewEffects = p_330645_;
    }

    protected static SuspiciousStewEffects makeEffectList(Holder<MobEffect> pEffect, float pSeconds) {
        return new SuspiciousStewEffects(List.of(new SuspiciousStewEffects.Entry(pEffect, Mth.floor(pSeconds * 20.0F))));
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        Vec3 vec3 = pState.getOffset(pLevel, pPos);
        return SHAPE.move(vec3.x, vec3.y, vec3.z);
    }

    @Override
    public SuspiciousStewEffects getSuspiciousEffects() {
        return this.suspiciousStewEffects;
    }
}
