package net.minecraft.world.level.levelgen.feature.rootplacers;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class RootPlacerType<P extends RootPlacer> {
    public static final RootPlacerType<MangroveRootPlacer> MANGROVE_ROOT_PLACER = register("mangrove_root_placer", MangroveRootPlacer.CODEC);
    private final MapCodec<P> codec;

    private static <P extends RootPlacer> RootPlacerType<P> register(String pName, MapCodec<P> pCodec) {
        return Registry.register(BuiltInRegistries.ROOT_PLACER_TYPE, pName, new RootPlacerType<>(pCodec));
    }

    private RootPlacerType(MapCodec<P> pCodec) {
        this.codec = pCodec;
    }

    public MapCodec<P> codec() {
        return this.codec;
    }
}
