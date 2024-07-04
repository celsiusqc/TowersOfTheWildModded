package net.minecraft.server.packs.repository;

import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.BuiltInMetadata;
import net.minecraft.server.packs.FeatureFlagsMetadataSection;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.VanillaPackResourcesBuilder;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.DirectoryValidator;

public class ServerPacksSource extends BuiltInPackSource {
    private static final PackMetadataSection VERSION_METADATA_SECTION = new PackMetadataSection(
        Component.translatable("dataPack.vanilla.description"), SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA), Optional.empty()
    );
    private static final FeatureFlagsMetadataSection FEATURE_FLAGS_METADATA_SECTION = new FeatureFlagsMetadataSection(FeatureFlags.DEFAULT_FLAGS);
    private static final BuiltInMetadata BUILT_IN_METADATA = BuiltInMetadata.of(
        PackMetadataSection.TYPE, VERSION_METADATA_SECTION, FeatureFlagsMetadataSection.TYPE, FEATURE_FLAGS_METADATA_SECTION
    );
    private static final PackLocationInfo VANILLA_PACK_INFO = new PackLocationInfo(
        "vanilla", Component.translatable("dataPack.vanilla.name"), PackSource.BUILT_IN, Optional.of(CORE_PACK_INFO)
    );
    private static final PackSelectionConfig VANILLA_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.BOTTOM, false);
    private static final PackSelectionConfig FEATURE_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);
    private static final ResourceLocation PACKS_DIR = ResourceLocation.withDefaultNamespace("datapacks");

    public ServerPacksSource(DirectoryValidator pValidator) {
        super(PackType.SERVER_DATA, createVanillaPackSource(), PACKS_DIR, pValidator);
    }

    private static PackLocationInfo createBuiltInPackLocation(String pId, Component pTitle) {
        return new PackLocationInfo(pId, pTitle, PackSource.FEATURE, Optional.of(KnownPack.vanilla(pId)));
    }

    @VisibleForTesting
    public static VanillaPackResources createVanillaPackSource() {
        return new VanillaPackResourcesBuilder()
            .setMetadata(BUILT_IN_METADATA)
            .exposeNamespace("minecraft")
            .applyDevelopmentConfig()
            .pushJarResources()
            .build(VANILLA_PACK_INFO);
    }

    @Override
    protected Component getPackTitle(String pId) {
        return Component.literal(pId);
    }

    @Nullable
    @Override
    protected Pack createVanillaPack(PackResources pResources) {
        return Pack.readMetaAndCreate(VANILLA_PACK_INFO, fixedResources(pResources), PackType.SERVER_DATA, VANILLA_SELECTION_CONFIG);
    }

    @Nullable
    @Override
    protected Pack createBuiltinPack(String pId, Pack.ResourcesSupplier pResources, Component pTitle) {
        return Pack.readMetaAndCreate(createBuiltInPackLocation(pId, pTitle), pResources, PackType.SERVER_DATA, FEATURE_SELECTION_CONFIG);
    }

    public static PackRepository createPackRepository(Path pFolder, DirectoryValidator pValidator) {
        final PackRepository packRepository = new PackRepository(new ServerPacksSource(pValidator), new FolderRepositorySource(pFolder, PackType.SERVER_DATA, PackSource.WORLD, pValidator));
        net.neoforged.neoforge.resource.ResourcePackLoader.populatePackRepository(packRepository, PackType.SERVER_DATA, false);
        return packRepository;
    }

    public static PackRepository createVanillaTrustedRepository() {
        final PackRepository packRepository = new PackRepository(new ServerPacksSource(new DirectoryValidator(p_293813_ -> true)));
        net.neoforged.neoforge.resource.ResourcePackLoader.populatePackRepository(packRepository, PackType.SERVER_DATA, true);
        return packRepository;
    }

    public static PackRepository createPackRepository(LevelStorageSource.LevelStorageAccess pLevel) {
        return createPackRepository(pLevel.getLevelPath(LevelResource.DATAPACK_DIR), pLevel.parent().getWorldDirValidator());
    }
}
