package net.minecraft.nbt;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.Util;
import net.minecraft.util.DelegateDataOutput;
import net.minecraft.util.FastBufferedInputStream;

public class NbtIo {
    private static final OpenOption[] SYNC_OUTPUT_OPTIONS = new OpenOption[]{
        StandardOpenOption.SYNC, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
    };

    public static CompoundTag readCompressed(Path pPath, NbtAccounter pAccounter) throws IOException {
        CompoundTag compoundtag;
        try (
            InputStream inputstream = Files.newInputStream(pPath);
            InputStream inputstream1 = new FastBufferedInputStream(inputstream);
        ) {
            compoundtag = readCompressed(inputstream1, pAccounter);
        }

        return compoundtag;
    }

    private static DataInputStream createDecompressorStream(InputStream pZippedStream) throws IOException {
        return new DataInputStream(new FastBufferedInputStream(new GZIPInputStream(pZippedStream)));
    }

    private static DataOutputStream createCompressorStream(OutputStream pOutputSteam) throws IOException {
        return new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(pOutputSteam)));
    }

    public static CompoundTag readCompressed(InputStream pZippedStream, NbtAccounter pAccounter) throws IOException {
        CompoundTag compoundtag;
        try (DataInputStream datainputstream = createDecompressorStream(pZippedStream)) {
            compoundtag = read(datainputstream, pAccounter);
        }

        return compoundtag;
    }

    public static void parseCompressed(Path pPath, StreamTagVisitor pVisitor, NbtAccounter pAccounter) throws IOException {
        try (
            InputStream inputstream = Files.newInputStream(pPath);
            InputStream inputstream1 = new FastBufferedInputStream(inputstream);
        ) {
            parseCompressed(inputstream1, pVisitor, pAccounter);
        }
    }

    public static void parseCompressed(InputStream pZippedStream, StreamTagVisitor pVisitor, NbtAccounter pAccounter) throws IOException {
        try (DataInputStream datainputstream = createDecompressorStream(pZippedStream)) {
            parse(datainputstream, pVisitor, pAccounter);
        }
    }

    public static void writeCompressed(CompoundTag pCompoundTag, Path pPath) throws IOException {
        try (
            OutputStream outputstream = Files.newOutputStream(pPath, SYNC_OUTPUT_OPTIONS);
            OutputStream outputstream1 = new BufferedOutputStream(outputstream);
        ) {
            writeCompressed(pCompoundTag, outputstream1);
        }
    }

    /**
     * Writes and compresses a compound tag to a GNU zipped file.
     * @see #writeCompressed(CompoundTag, File)
     */
    public static void writeCompressed(CompoundTag pCompoundTag, OutputStream pOutputStream) throws IOException {
        try (DataOutputStream dataoutputstream = createCompressorStream(pOutputStream)) {
            write(pCompoundTag, dataoutputstream);
        }
    }

    public static void write(CompoundTag pCompoundTag, Path pPath) throws IOException {
        try (
            OutputStream outputstream = Files.newOutputStream(pPath, SYNC_OUTPUT_OPTIONS);
            OutputStream outputstream1 = new BufferedOutputStream(outputstream);
            DataOutputStream dataoutputstream = new DataOutputStream(outputstream1);
        ) {
            write(pCompoundTag, dataoutputstream);
        }
    }

    @Nullable
    public static CompoundTag read(Path pPath) throws IOException {
        if (!Files.exists(pPath)) {
            return null;
        } else {
            CompoundTag compoundtag;
            try (
                InputStream inputstream = Files.newInputStream(pPath);
                DataInputStream datainputstream = new DataInputStream(inputstream);
            ) {
                compoundtag = read(datainputstream, NbtAccounter.unlimitedHeap());
            }

            return compoundtag;
        }
    }

    /**
     * Reads a compound tag from a file. The size of the file can be infinite.
     */
    public static CompoundTag read(DataInput pInput) throws IOException {
        return read(pInput, NbtAccounter.unlimitedHeap());
    }

    /**
     * Reads a compound tag from a file. The size of the file is limited by the {@code accounter}.
     * @throws RuntimeException if the size of the file is larger than the maximum amount of bytes specified by the {@code accounter}
     */
    public static CompoundTag read(DataInput pInput, NbtAccounter pAccounter) throws IOException {
        Tag tag = readUnnamedTag(pInput, pAccounter);
        if (tag instanceof CompoundTag) {
            return (CompoundTag)tag;
        } else {
            throw new IOException("Root tag must be a named compound tag");
        }
    }

    public static void write(CompoundTag pCompoundTag, DataOutput pOutput) throws IOException {
        writeUnnamedTagWithFallback(pCompoundTag, pOutput);
    }

    public static void parse(DataInput pInput, StreamTagVisitor pVisitor, NbtAccounter pAccounter) throws IOException {
        TagType<?> tagtype = TagTypes.getType(pInput.readByte());
        if (tagtype == EndTag.TYPE) {
            if (pVisitor.visitRootEntry(EndTag.TYPE) == StreamTagVisitor.ValueResult.CONTINUE) {
                pVisitor.visitEnd();
            }
        } else {
            switch (pVisitor.visitRootEntry(tagtype)) {
                case HALT:
                default:
                    break;
                case BREAK:
                    StringTag.skipString(pInput);
                    tagtype.skip(pInput, pAccounter);
                    break;
                case CONTINUE:
                    StringTag.skipString(pInput);
                    tagtype.parse(pInput, pVisitor, pAccounter);
            }
        }
    }

    public static Tag readAnyTag(DataInput pInput, NbtAccounter pAccounter) throws IOException {
        byte b0 = pInput.readByte();
        return (Tag)(b0 == 0 ? EndTag.INSTANCE : readTagSafe(pInput, pAccounter, b0));
    }

    public static void writeAnyTag(Tag pTag, DataOutput pOutput) throws IOException {
        pOutput.writeByte(pTag.getId());
        if (pTag.getId() != 0) {
            pTag.write(pOutput);
        }
    }

    public static void writeUnnamedTag(Tag pTag, DataOutput pOutput) throws IOException {
        pOutput.writeByte(pTag.getId());
        if (pTag.getId() != 0) {
            pOutput.writeUTF("");
            pTag.write(pOutput);
        }
    }

    public static void writeUnnamedTagWithFallback(Tag pTag, DataOutput pOutput) throws IOException {
        writeUnnamedTag(pTag, new NbtIo.StringFallbackDataOutput(pOutput));
    }

    private static Tag readUnnamedTag(DataInput pInput, NbtAccounter pAccounter) throws IOException {
        byte b0 = pInput.readByte();
        pAccounter.accountBytes(1); // Forge: Count everything!
        if (b0 == 0) {
            return EndTag.INSTANCE;
        } else {
            pAccounter.readUTF(pInput.readUTF()); //Forge: Count this string.
            pAccounter.accountBytes(4); //Forge: 4 extra bytes for the object allocation.
            return readTagSafe(pInput, pAccounter, b0);
        }
    }

    private static Tag readTagSafe(DataInput pInput, NbtAccounter pAccounter, byte pType) {
        try {
            return TagTypes.getType(pType).load(pInput, pAccounter);
        } catch (IOException ioexception) {
            CrashReport crashreport = CrashReport.forThrowable(ioexception, "Loading NBT data");
            CrashReportCategory crashreportcategory = crashreport.addCategory("NBT Tag");
            crashreportcategory.setDetail("Tag type", pType);
            throw new ReportedNbtException(crashreport);
        }
    }

    public static class StringFallbackDataOutput extends DelegateDataOutput {
        public StringFallbackDataOutput(DataOutput p_312308_) {
            super(p_312308_);
        }

        @Override
        public void writeUTF(String p_312136_) throws IOException {
            try {
                super.writeUTF(p_312136_);
            } catch (UTFDataFormatException utfdataformatexception) {
                Util.logAndPauseIfInIde("Failed to write NBT String", utfdataformatexception);
                super.writeUTF("");
            }
        }
    }
}
