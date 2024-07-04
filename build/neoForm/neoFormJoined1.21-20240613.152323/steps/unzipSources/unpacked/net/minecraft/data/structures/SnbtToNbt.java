package net.minecraft.data.structures;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class SnbtToNbt implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackOutput output;
    private final Iterable<Path> inputFolders;
    private final List<SnbtToNbt.Filter> filters = Lists.newArrayList();

    public SnbtToNbt(PackOutput pOutput, Iterable<Path> pInputFolders) {
        this.output = pOutput;
        this.inputFolders = pInputFolders;
    }

    public SnbtToNbt addFilter(SnbtToNbt.Filter pFilter) {
        this.filters.add(pFilter);
        return this;
    }

    private CompoundTag applyFilters(String pFileName, CompoundTag pTag) {
        CompoundTag compoundtag = pTag;

        for (SnbtToNbt.Filter snbttonbt$filter : this.filters) {
            compoundtag = snbttonbt$filter.apply(pFileName, compoundtag);
        }

        return compoundtag;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput) {
        Path path = this.output.getOutputFolder();
        List<CompletableFuture<?>> list = Lists.newArrayList();

        for (Path path1 : this.inputFolders) {
            list.add(
                CompletableFuture.<CompletableFuture>supplyAsync(
                        () -> {
                            try {
                                CompletableFuture completablefuture;
                                try (Stream<Path> stream = Files.walk(path1)) {
                                    completablefuture = CompletableFuture.allOf(
                                        stream.filter(p_126464_ -> p_126464_.toString().endsWith(".snbt")).map(p_253432_ -> CompletableFuture.runAsync(() -> {
                                                SnbtToNbt.TaskResult snbttonbt$taskresult = this.readStructure(p_253432_, this.getName(path1, p_253432_));
                                                this.storeStructureIfChanged(pOutput, snbttonbt$taskresult, path);
                                            }, Util.backgroundExecutor())).toArray(CompletableFuture[]::new)
                                    );
                                }

                                return completablefuture;
                            } catch (Exception exception) {
                                throw new RuntimeException("Failed to read structure input directory, aborting", exception);
                            }
                        },
                        Util.backgroundExecutor()
                    )
                    .thenCompose(p_253441_ -> p_253441_)
            );
        }

        return Util.sequenceFailFast(list);
    }

    @Override
    public final String getName() {
        return "SNBT -> NBT";
    }

    /**
     * Gets the name of the given SNBT file, based on its path and the input directory. The result does not have the ".snbt" extension.
     */
    private String getName(Path pInputFolder, Path pFile) {
        String s = pInputFolder.relativize(pFile).toString().replaceAll("\\\\", "/");
        return s.substring(0, s.length() - ".snbt".length());
    }

    private SnbtToNbt.TaskResult readStructure(Path pFilePath, String pFileName) {
        try {
            SnbtToNbt.TaskResult snbttonbt$taskresult;
            try (BufferedReader bufferedreader = Files.newBufferedReader(pFilePath)) {
                String s = IOUtils.toString(bufferedreader);
                CompoundTag compoundtag = this.applyFilters(pFileName, NbtUtils.snbtToStructure(s));
                ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
                HashingOutputStream hashingoutputstream = new HashingOutputStream(Hashing.sha1(), bytearrayoutputstream);
                NbtIo.writeCompressed(compoundtag, hashingoutputstream);
                byte[] abyte = bytearrayoutputstream.toByteArray();
                HashCode hashcode = hashingoutputstream.hash();
                snbttonbt$taskresult = new SnbtToNbt.TaskResult(pFileName, abyte, hashcode);
            }

            return snbttonbt$taskresult;
        } catch (Throwable throwable1) {
            throw new SnbtToNbt.StructureConversionException(pFilePath, throwable1);
        }
    }

    private void storeStructureIfChanged(CachedOutput pOutput, SnbtToNbt.TaskResult pTaskResult, Path pDirectoryPath) {
        Path path = pDirectoryPath.resolve(pTaskResult.name + ".nbt");

        try {
            pOutput.writeIfNeeded(path, pTaskResult.payload, pTaskResult.hash);
        } catch (IOException ioexception) {
            LOGGER.error("Couldn't write structure {} at {}", pTaskResult.name, path, ioexception);
        }
    }

    @FunctionalInterface
    public interface Filter {
        CompoundTag apply(String pStructureLocationPath, CompoundTag pTag);
    }

    /**
     * Wraps exceptions thrown while reading structures to include the path of the structure in the exception message.
     */
    static class StructureConversionException extends RuntimeException {
        public StructureConversionException(Path pPath, Throwable pCause) {
            super(pPath.toAbsolutePath().toString(), pCause);
        }
    }

    static record TaskResult(String name, byte[] payload, HashCode hash) {
    }
}
