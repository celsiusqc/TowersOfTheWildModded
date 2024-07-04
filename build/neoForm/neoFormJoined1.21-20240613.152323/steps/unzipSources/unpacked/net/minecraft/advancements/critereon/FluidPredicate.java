package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public record FluidPredicate(Optional<HolderSet<Fluid>> fluids, Optional<StatePropertiesPredicate> properties) {
    public static final Codec<FluidPredicate> CODEC = RecordCodecBuilder.create(
        p_337366_ -> p_337366_.group(
                    RegistryCodecs.homogeneousList(Registries.FLUID).optionalFieldOf("fluids").forGetter(FluidPredicate::fluids),
                    StatePropertiesPredicate.CODEC.optionalFieldOf("state").forGetter(FluidPredicate::properties)
                )
                .apply(p_337366_, FluidPredicate::new)
    );

    public boolean matches(ServerLevel pLevel, BlockPos pPos) {
        if (!pLevel.isLoaded(pPos)) {
            return false;
        } else {
            FluidState fluidstate = pLevel.getFluidState(pPos);
            return this.fluids.isPresent() && !fluidstate.is(this.fluids.get())
                ? false
                : !this.properties.isPresent() || this.properties.get().matches(fluidstate);
        }
    }

    public static class Builder {
        private Optional<HolderSet<Fluid>> fluids = Optional.empty();
        private Optional<StatePropertiesPredicate> properties = Optional.empty();

        private Builder() {
        }

        public static FluidPredicate.Builder fluid() {
            return new FluidPredicate.Builder();
        }

        public FluidPredicate.Builder of(Fluid pFluid) {
            this.fluids = Optional.of(HolderSet.direct(pFluid.builtInRegistryHolder()));
            return this;
        }

        public FluidPredicate.Builder of(HolderSet<Fluid> p_330492_) {
            this.fluids = Optional.of(p_330492_);
            return this;
        }

        public FluidPredicate.Builder setProperties(StatePropertiesPredicate pProperties) {
            this.properties = Optional.of(pProperties);
            return this;
        }

        public FluidPredicate build() {
            return new FluidPredicate(this.fluids, this.properties);
        }
    }
}
