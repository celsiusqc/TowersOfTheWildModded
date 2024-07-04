package net.minecraft.server.packs;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult.Error;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;

public class PathPackResources extends AbstractPackResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Joiner PATH_JOINER = Joiner.on("/");
    private final Path root;

    public PathPackResources(PackLocationInfo pLocation, Path pRoot) {
        super(pLocation);
        this.root = pRoot;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... pElements) {
        FileUtil.validatePath(pElements);
        Path path = FileUtil.resolvePath(this.root, List.of(pElements));
        return Files.exists(path) ? IoSupplier.create(path) : null;
    }

    public static boolean validatePath(Path pPath) {
        return true;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType pPackType, ResourceLocation pLocation) {
        Path path = this.root.resolve(pPackType.getDirectory()).resolve(pLocation.getNamespace());
        return getResource(pLocation, path);
    }

    @Nullable
    public static IoSupplier<InputStream> getResource(ResourceLocation pLocation, Path pPath) {
        return FileUtil.decomposePath(pLocation.getPath()).mapOrElse(p_251647_ -> {
            Path path = FileUtil.resolvePath(pPath, (List<String>)p_251647_);
            return returnFileIfExists(path);
        }, p_337558_ -> {
            LOGGER.error("Invalid path {}: {}", pLocation, p_337558_.message());
            return null;
        });
    }

    @Nullable
    private static IoSupplier<InputStream> returnFileIfExists(Path pPath) {
        return Files.exists(pPath) && validatePath(pPath) ? IoSupplier.create(pPath) : null;
    }

    @Override
    public void listResources(PackType pPackType, String pNamespace, String pPath, PackResources.ResourceOutput pResourceOutput) {
        FileUtil.decomposePath(pPath).ifSuccess(p_250225_ -> {
            Path path = this.root.resolve(pPackType.getDirectory()).resolve(pNamespace);
            listPath(pNamespace, path, (List<String>)p_250225_, pResourceOutput);
        }).ifError(p_337560_ -> LOGGER.error("Invalid path {}: {}", pPath, p_337560_.message()));
    }

    public static void listPath(String pNamespace, Path pNamespacePath, List<String> pDecomposedPath, PackResources.ResourceOutput pResourceOutput) {
        Path path = FileUtil.resolvePath(pNamespacePath, pDecomposedPath);

        try (Stream<Path> stream = Files.find(path, Integer.MAX_VALUE, (p_250060_, p_250796_) -> p_250796_.isRegularFile())) {
            stream.forEach(p_249092_ -> {
                String s = PATH_JOINER.join(pNamespacePath.relativize(p_249092_));
                ResourceLocation resourcelocation = ResourceLocation.tryBuild(pNamespace, s);
                if (resourcelocation == null) {
                    Util.logAndPauseIfInIde(String.format(Locale.ROOT, "Invalid path in pack: %s:%s, ignoring", pNamespace, s));
                } else {
                    pResourceOutput.accept(resourcelocation, IoSupplier.create(p_249092_));
                }
            });
        } catch (NotDirectoryException | NoSuchFileException nosuchfileexception) {
        } catch (IOException ioexception) {
            LOGGER.error("Failed to list path {}", path, ioexception);
        }
    }

    @Override
    public Set<String> getNamespaces(PackType pType) {
        Set<String> set = Sets.newHashSet();
        Path path = this.root.resolve(pType.getDirectory());

        try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(path)) {
            for (Path path1 : directorystream) {
                String s = path1.getFileName().toString();
                if (ResourceLocation.isValidNamespace(s)) {
                    set.add(s);
                } else {
                    LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring", s, this.root);
                }
            }
        } catch (NotDirectoryException | NoSuchFileException nosuchfileexception) {
        } catch (IOException ioexception) {
            LOGGER.error("Failed to list path {}", path, ioexception);
        }

        return set;
    }

    @Override
    public void close() {
    }

    public static class PathResourcesSupplier implements Pack.ResourcesSupplier {
        private final Path content;

        public PathResourcesSupplier(Path pContent) {
            this.content = pContent;
        }

        @Override
        public PackResources openPrimary(PackLocationInfo pLocation) {
            return new PathPackResources(pLocation, this.content);
        }

        @Override
        public PackResources openFull(PackLocationInfo pLocation, Pack.Metadata pMetadata) {
            PackResources packresources = this.openPrimary(pLocation);
            List<String> list = pMetadata.overlays();
            if (list.isEmpty()) {
                return packresources;
            } else {
                List<PackResources> list1 = new ArrayList<>(list.size());

                for (String s : list) {
                    Path path = this.content.resolve(s);
                    list1.add(new PathPackResources(pLocation, path));
                }

                return new CompositePackResources(packresources, list1);
            }
        }
    }
}
