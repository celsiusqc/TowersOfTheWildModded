package net.minecraft;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;

public class SystemReport {
    public static final long BYTES_PER_MEBIBYTE = 1048576L;
    private static final long ONE_GIGA = 1000000000L;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String OPERATING_SYSTEM = System.getProperty("os.name")
        + " ("
        + System.getProperty("os.arch")
        + ") version "
        + System.getProperty("os.version");
    private static final String JAVA_VERSION = System.getProperty("java.version") + ", " + System.getProperty("java.vendor");
    private static final String JAVA_VM_VERSION = System.getProperty("java.vm.name")
        + " ("
        + System.getProperty("java.vm.info")
        + "), "
        + System.getProperty("java.vm.vendor");
    private final Map<String, String> entries = Maps.newLinkedHashMap();

    public SystemReport() {
        this.setDetail("Minecraft Version", SharedConstants.getCurrentVersion().getName());
        this.setDetail("Minecraft Version ID", SharedConstants.getCurrentVersion().getId());
        this.setDetail("Operating System", OPERATING_SYSTEM);
        this.setDetail("Java Version", JAVA_VERSION);
        this.setDetail("Java VM Version", JAVA_VM_VERSION);
        this.setDetail("Memory", () -> {
            Runtime runtime = Runtime.getRuntime();
            long i = runtime.maxMemory();
            long j = runtime.totalMemory();
            long k = runtime.freeMemory();
            long l = i / 1048576L;
            long i1 = j / 1048576L;
            long j1 = k / 1048576L;
            return k + " bytes (" + j1 + " MiB) / " + j + " bytes (" + i1 + " MiB) up to " + i + " bytes (" + l + " MiB)";
        });
        this.setDetail("CPUs", () -> String.valueOf(Runtime.getRuntime().availableProcessors()));
        this.ignoreErrors("hardware", () -> this.putHardware(new SystemInfo()));
        this.setDetail("JVM Flags", () -> {
            List<String> list = Util.getVmArguments().collect(Collectors.toList());
            return String.format(Locale.ROOT, "%d total; %s", list.size(), String.join(" ", list));
        });
    }

    public void setDetail(String pIdentifier, String pValue) {
        this.entries.put(pIdentifier, pValue);
    }

    public void setDetail(String pIdentifier, Supplier<String> pValueSupplier) {
        try {
            this.setDetail(pIdentifier, pValueSupplier.get());
        } catch (Exception exception) {
            LOGGER.warn("Failed to get system info for {}", pIdentifier, exception);
            this.setDetail(pIdentifier, "ERR");
        }
    }

    private void putHardware(SystemInfo pInfo) {
        HardwareAbstractionLayer hardwareabstractionlayer = pInfo.getHardware();
        this.ignoreErrors("processor", () -> this.putProcessor(hardwareabstractionlayer.getProcessor()));
        this.ignoreErrors("graphics", () -> this.putGraphics(hardwareabstractionlayer.getGraphicsCards()));
        this.ignoreErrors("memory", () -> this.putMemory(hardwareabstractionlayer.getMemory()));
        this.ignoreErrors("storage", this::putStorage);
    }

    private void ignoreErrors(String pGroupIdentifier, Runnable pExecutor) {
        try {
            pExecutor.run();
        } catch (Throwable throwable) {
            LOGGER.warn("Failed retrieving info for group {}", pGroupIdentifier, throwable);
        }
    }

    public static float sizeInMiB(long p_345366_) {
        return (float)p_345366_ / 1048576.0F;
    }

    private void putPhysicalMemory(List<PhysicalMemory> pMemorySlots) {
        int i = 0;

        for (PhysicalMemory physicalmemory : pMemorySlots) {
            String s = String.format(Locale.ROOT, "Memory slot #%d ", i++);
            this.setDetail(s + "capacity (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(physicalmemory.getCapacity())));
            this.setDetail(s + "clockSpeed (GHz)", () -> String.format(Locale.ROOT, "%.2f", (float)physicalmemory.getClockSpeed() / 1.0E9F));
            this.setDetail(s + "type", physicalmemory::getMemoryType);
        }
    }

    private void putVirtualMemory(VirtualMemory pMemory) {
        this.setDetail("Virtual memory max (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(pMemory.getVirtualMax())));
        this.setDetail("Virtual memory used (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(pMemory.getVirtualInUse())));
        this.setDetail("Swap memory total (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(pMemory.getSwapTotal())));
        this.setDetail("Swap memory used (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(pMemory.getSwapUsed())));
    }

    private void putMemory(GlobalMemory pMemory) {
        this.ignoreErrors("physical memory", () -> this.putPhysicalMemory(pMemory.getPhysicalMemory()));
        this.ignoreErrors("virtual memory", () -> this.putVirtualMemory(pMemory.getVirtualMemory()));
    }

    private void putGraphics(List<GraphicsCard> pGpus) {
        int i = 0;

        for (GraphicsCard graphicscard : pGpus) {
            String s = String.format(Locale.ROOT, "Graphics card #%d ", i++);
            this.setDetail(s + "name", graphicscard::getName);
            this.setDetail(s + "vendor", graphicscard::getVendor);
            this.setDetail(s + "VRAM (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(graphicscard.getVRam())));
            this.setDetail(s + "deviceId", graphicscard::getDeviceId);
            this.setDetail(s + "versionInfo", graphicscard::getVersionInfo);
        }
    }

    private void putProcessor(CentralProcessor pCpu) {
        ProcessorIdentifier processoridentifier = pCpu.getProcessorIdentifier();
        this.setDetail("Processor Vendor", processoridentifier::getVendor);
        this.setDetail("Processor Name", processoridentifier::getName);
        this.setDetail("Identifier", processoridentifier::getIdentifier);
        this.setDetail("Microarchitecture", processoridentifier::getMicroarchitecture);
        this.setDetail("Frequency (GHz)", () -> String.format(Locale.ROOT, "%.2f", (float)processoridentifier.getVendorFreq() / 1.0E9F));
        this.setDetail("Number of physical packages", () -> String.valueOf(pCpu.getPhysicalPackageCount()));
        this.setDetail("Number of physical CPUs", () -> String.valueOf(pCpu.getPhysicalProcessorCount()));
        this.setDetail("Number of logical CPUs", () -> String.valueOf(pCpu.getLogicalProcessorCount()));
    }

    private void putStorage() {
        this.putSpaceForProperty("jna.tmpdir");
        this.putSpaceForProperty("org.lwjgl.system.SharedLibraryExtractPath");
        this.putSpaceForProperty("io.netty.native.workdir");
        this.putSpaceForProperty("java.io.tmpdir");
        this.putSpaceForPath("workdir", () -> "");
    }

    private void putSpaceForProperty(String p_345533_) {
        this.putSpaceForPath(p_345533_, () -> System.getProperty(p_345533_));
    }

    private void putSpaceForPath(String p_344883_, Supplier<String> p_345715_) {
        String s = "Space in storage for " + p_344883_ + " (MiB)";

        try {
            String s1 = p_345715_.get();
            if (s1 == null) {
                this.setDetail(s, "<path not set>");
                return;
            }

            FileStore filestore = Files.getFileStore(Path.of(s1));
            this.setDetail(
                s, String.format(Locale.ROOT, "available: %.2f, total: %.2f", sizeInMiB(filestore.getUsableSpace()), sizeInMiB(filestore.getTotalSpace()))
            );
        } catch (InvalidPathException invalidpathexception) {
            LOGGER.warn("{} is not a path", p_344883_, invalidpathexception);
            this.setDetail(s, "<invalid path>");
        } catch (Exception exception) {
            LOGGER.warn("Failed retrieving storage space for {}", p_344883_, exception);
            this.setDetail(s, "ERR");
        }
    }

    public void appendToCrashReportString(StringBuilder pReportAppender) {
        pReportAppender.append("-- ").append("System Details").append(" --\n");
        pReportAppender.append("Details:");
        this.entries.forEach((p_143529_, p_143530_) -> {
            pReportAppender.append("\n\t");
            pReportAppender.append(p_143529_);
            pReportAppender.append(": ");
            pReportAppender.append(p_143530_);
        });
    }

    public String toLineSeparatedString() {
        return this.entries
            .entrySet()
            .stream()
            .map(p_143534_ -> p_143534_.getKey() + ": " + p_143534_.getValue())
            .collect(Collectors.joining(System.lineSeparator()));
    }
}
