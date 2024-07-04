package net.minecraft.server.packs.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;

public abstract class PackDetector<T> {
    private final DirectoryValidator validator;

    protected PackDetector(DirectoryValidator pValidator) {
        this.validator = pValidator;
    }

    @Nullable
    public T detectPackResources(Path pPath, List<ForbiddenSymlinkInfo> pForbiddenSymlinkInfos) throws IOException {
        Path path = pPath;

        BasicFileAttributes basicfileattributes;
        try {
            basicfileattributes = Files.readAttributes(pPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException nosuchfileexception) {
            return null;
        }

        if (basicfileattributes.isSymbolicLink()) {
            this.validator.validateSymlink(pPath, pForbiddenSymlinkInfos);
            if (!pForbiddenSymlinkInfos.isEmpty()) {
                return null;
            }

            path = Files.readSymbolicLink(pPath);
            basicfileattributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        }

        if (basicfileattributes.isDirectory()) {
            this.validator.validateKnownDirectory(path, pForbiddenSymlinkInfos);
            if (!pForbiddenSymlinkInfos.isEmpty()) {
                return null;
            } else {
                return !Files.isRegularFile(path.resolve("pack.mcmeta")) ? null : this.createDirectoryPack(path);
            }
        } else {
            return basicfileattributes.isRegularFile() && path.getFileName().toString().endsWith(".zip") ? this.createZipPack(path) : null;
        }
    }

    @Nullable
    protected abstract T createZipPack(Path pPath) throws IOException;

    @Nullable
    protected abstract T createDirectoryPack(Path pPath) throws IOException;
}
