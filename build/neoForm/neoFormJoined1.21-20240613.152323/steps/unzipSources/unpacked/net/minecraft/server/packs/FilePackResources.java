package net.minecraft.server.packs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class FilePackResources extends AbstractPackResources {
    static final Logger LOGGER = LogUtils.getLogger();
    private final FilePackResources.SharedZipFileAccess zipFileAccess;
    private final String prefix;

    public FilePackResources(PackLocationInfo pLocation, FilePackResources.SharedZipFileAccess pZipFileAccess, String pPrefix) {
        super(pLocation);
        this.zipFileAccess = pZipFileAccess;
        this.prefix = pPrefix;
    }

    private static String getPathFromLocation(PackType pPackType, ResourceLocation pLocation) {
        return String.format(Locale.ROOT, "%s/%s/%s", pPackType.getDirectory(), pLocation.getNamespace(), pLocation.getPath());
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... pElements) {
        return this.getResource(String.join("/", pElements));
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType pPackType, ResourceLocation pLocation) {
        return this.getResource(getPathFromLocation(pPackType, pLocation));
    }

    private String addPrefix(String pResourcePath) {
        return this.prefix.isEmpty() ? pResourcePath : this.prefix + "/" + pResourcePath;
    }

    @Nullable
    private IoSupplier<InputStream> getResource(String pResourcePath) {
        ZipFile zipfile = this.zipFileAccess.getOrCreateZipFile();
        if (zipfile == null) {
            return null;
        } else {
            ZipEntry zipentry = zipfile.getEntry(this.addPrefix(pResourcePath));
            return zipentry == null ? null : IoSupplier.create(zipfile, zipentry);
        }
    }

    @Override
    public Set<String> getNamespaces(PackType pType) {
        ZipFile zipfile = this.zipFileAccess.getOrCreateZipFile();
        if (zipfile == null) {
            return Set.of();
        } else {
            Enumeration<? extends ZipEntry> enumeration = zipfile.entries();
            Set<String> set = Sets.newHashSet();
            String s = this.addPrefix(pType.getDirectory() + "/");

            while (enumeration.hasMoreElements()) {
                ZipEntry zipentry = enumeration.nextElement();
                String s1 = zipentry.getName();
                String s2 = extractNamespace(s, s1);
                if (!s2.isEmpty()) {
                    if (ResourceLocation.isValidNamespace(s2)) {
                        set.add(s2);
                    } else {
                        LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring", s2, this.zipFileAccess.file);
                    }
                }
            }

            return set;
        }
    }

    @VisibleForTesting
    public static String extractNamespace(String pDirectory, String pName) {
        if (!pName.startsWith(pDirectory)) {
            return "";
        } else {
            int i = pDirectory.length();
            int j = pName.indexOf(47, i);
            return j == -1 ? pName.substring(i) : pName.substring(i, j);
        }
    }

    @Override
    public void close() {
        this.zipFileAccess.close();
    }

    @Override
    public void listResources(PackType pPackType, String pNamespace, String pPath, PackResources.ResourceOutput pResourceOutput) {
        ZipFile zipfile = this.zipFileAccess.getOrCreateZipFile();
        if (zipfile != null) {
            Enumeration<? extends ZipEntry> enumeration = zipfile.entries();
            String s = this.addPrefix(pPackType.getDirectory() + "/" + pNamespace + "/");
            String s1 = s + pPath + "/";

            while (enumeration.hasMoreElements()) {
                ZipEntry zipentry = enumeration.nextElement();
                if (!zipentry.isDirectory()) {
                    String s2 = zipentry.getName();
                    if (s2.startsWith(s1)) {
                        String s3 = s2.substring(s.length());
                        ResourceLocation resourcelocation = ResourceLocation.tryBuild(pNamespace, s3);
                        if (resourcelocation != null) {
                            pResourceOutput.accept(resourcelocation, IoSupplier.create(zipfile, zipentry));
                        } else {
                            LOGGER.warn("Invalid path in datapack: {}:{}, ignoring", pNamespace, s3);
                        }
                    }
                }
            }
        }
    }

    public static class FileResourcesSupplier implements Pack.ResourcesSupplier {
        private final File content;

        public FileResourcesSupplier(Path pContent) {
            this(pContent.toFile());
        }

        public FileResourcesSupplier(File pContent) {
            this.content = pContent;
        }

        @Override
        public PackResources openPrimary(PackLocationInfo pLocation) {
            FilePackResources.SharedZipFileAccess filepackresources$sharedzipfileaccess = new FilePackResources.SharedZipFileAccess(this.content);
            return new FilePackResources(pLocation, filepackresources$sharedzipfileaccess, "");
        }

        @Override
        public PackResources openFull(PackLocationInfo pLocation, Pack.Metadata pMetadata) {
            FilePackResources.SharedZipFileAccess filepackresources$sharedzipfileaccess = new FilePackResources.SharedZipFileAccess(this.content);
            PackResources packresources = new FilePackResources(pLocation, filepackresources$sharedzipfileaccess, "");
            List<String> list = pMetadata.overlays();
            if (list.isEmpty()) {
                return packresources;
            } else {
                List<PackResources> list1 = new ArrayList<>(list.size());

                for (String s : list) {
                    list1.add(new FilePackResources(pLocation, filepackresources$sharedzipfileaccess, s));
                }

                return new CompositePackResources(packresources, list1);
            }
        }
    }

    public static class SharedZipFileAccess implements AutoCloseable {
        final File file;
        @Nullable
        private ZipFile zipFile;
        private boolean failedToLoad;

        public SharedZipFileAccess(File pFile) {
            this.file = pFile;
        }

        @Nullable
        ZipFile getOrCreateZipFile() {
            if (this.failedToLoad) {
                return null;
            } else {
                if (this.zipFile == null) {
                    try {
                        this.zipFile = new ZipFile(this.file);
                    } catch (IOException ioexception) {
                        FilePackResources.LOGGER.error("Failed to open pack {}", this.file, ioexception);
                        this.failedToLoad = true;
                        return null;
                    }
                }

                return this.zipFile;
            }
        }

        @Override
        public void close() {
            if (this.zipFile != null) {
                IOUtils.closeQuietly(this.zipFile);
                this.zipFile = null;
            }
        }

        @Override
        protected void finalize() throws Throwable {
            this.close();
            super.finalize();
        }
    }
}
