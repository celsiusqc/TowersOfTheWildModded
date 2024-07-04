package net.minecraft.server.packs;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;

public class DownloadCacheCleaner {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void vacuumCacheDir(Path pPath, int pMaxEntries) {
        try {
            List<DownloadCacheCleaner.PathAndTime> list = listFilesWithModificationTimes(pPath);
            int i = list.size() - pMaxEntries;
            if (i <= 0) {
                return;
            }

            list.sort(DownloadCacheCleaner.PathAndTime.NEWEST_FIRST);
            List<DownloadCacheCleaner.PathAndPriority> list1 = prioritizeFilesInDirs(list);
            Collections.reverse(list1);
            list1.sort(DownloadCacheCleaner.PathAndPriority.HIGHEST_PRIORITY_FIRST);
            Set<Path> set = new HashSet<>();

            for (int j = 0; j < i; j++) {
                DownloadCacheCleaner.PathAndPriority downloadcachecleaner$pathandpriority = list1.get(j);
                Path path = downloadcachecleaner$pathandpriority.path;

                try {
                    Files.delete(path);
                    if (downloadcachecleaner$pathandpriority.removalPriority == 0) {
                        set.add(path.getParent());
                    }
                } catch (IOException ioexception1) {
                    LOGGER.warn("Failed to delete cache file {}", path, ioexception1);
                }
            }

            set.remove(pPath);

            for (Path path1 : set) {
                try {
                    Files.delete(path1);
                } catch (DirectoryNotEmptyException directorynotemptyexception) {
                } catch (IOException ioexception) {
                    LOGGER.warn("Failed to delete empty(?) cache directory {}", path1, ioexception);
                }
            }
        } catch (UncheckedIOException | IOException ioexception2) {
            LOGGER.error("Failed to vacuum cache dir {}", pPath, ioexception2);
        }
    }

    private static List<DownloadCacheCleaner.PathAndTime> listFilesWithModificationTimes(final Path pPath) throws IOException {
        try {
            final List<DownloadCacheCleaner.PathAndTime> list = new ArrayList<>();
            Files.walkFileTree(pPath, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path p_314922_, BasicFileAttributes p_315004_) {
                    if (p_315004_.isRegularFile() && !p_314922_.getParent().equals(pPath)) {
                        FileTime filetime = p_315004_.lastModifiedTime();
                        list.add(new DownloadCacheCleaner.PathAndTime(p_314922_, filetime));
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
            return list;
        } catch (NoSuchFileException nosuchfileexception) {
            return List.of();
        }
    }

    private static List<DownloadCacheCleaner.PathAndPriority> prioritizeFilesInDirs(List<DownloadCacheCleaner.PathAndTime> pPaths) {
        List<DownloadCacheCleaner.PathAndPriority> list = new ArrayList<>();
        Object2IntOpenHashMap<Path> object2intopenhashmap = new Object2IntOpenHashMap<>();

        for (DownloadCacheCleaner.PathAndTime downloadcachecleaner$pathandtime : pPaths) {
            int i = object2intopenhashmap.addTo(downloadcachecleaner$pathandtime.path.getParent(), 1);
            list.add(new DownloadCacheCleaner.PathAndPriority(downloadcachecleaner$pathandtime.path, i));
        }

        return list;
    }

    static record PathAndPriority(Path path, int removalPriority) {
        public static final Comparator<DownloadCacheCleaner.PathAndPriority> HIGHEST_PRIORITY_FIRST = Comparator.comparing(
                DownloadCacheCleaner.PathAndPriority::removalPriority
            )
            .reversed();
    }

    static record PathAndTime(Path path, FileTime modifiedTime) {
        public static final Comparator<DownloadCacheCleaner.PathAndTime> NEWEST_FIRST = Comparator.comparing(DownloadCacheCleaner.PathAndTime::modifiedTime)
            .reversed();
    }
}
