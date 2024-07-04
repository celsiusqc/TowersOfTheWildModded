package net.minecraft.world.level.storage;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class PlayerDataStorage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final File playerDir;
    protected final DataFixer fixerUpper;
    private static final DateTimeFormatter FORMATTER = FileNameDateFormatter.create();

    public PlayerDataStorage(LevelStorageSource.LevelStorageAccess pLevelStorageAccess, DataFixer pFixerUpper) {
        this.fixerUpper = pFixerUpper;
        this.playerDir = pLevelStorageAccess.getLevelPath(LevelResource.PLAYER_DATA_DIR).toFile();
        this.playerDir.mkdirs();
    }

    public void save(Player pPlayer) {
        try {
            CompoundTag compoundtag = pPlayer.saveWithoutId(new CompoundTag());
            Path path = this.playerDir.toPath();
            Path path1 = Files.createTempFile(path, pPlayer.getStringUUID() + "-", ".dat");
            NbtIo.writeCompressed(compoundtag, path1);
            Path path2 = path.resolve(pPlayer.getStringUUID() + ".dat");
            Path path3 = path.resolve(pPlayer.getStringUUID() + ".dat_old");
            Util.safeReplaceFile(path2, path1, path3);
            net.neoforged.neoforge.event.EventHooks.firePlayerSavingEvent(pPlayer, playerDir, pPlayer.getStringUUID());
        } catch (Exception exception) {
            LOGGER.warn("Failed to save player data for {}", pPlayer.getName().getString());
        }
    }

    private void backup(Player pPlayer, String pSuffix) {
        Path path = this.playerDir.toPath();
        Path path1 = path.resolve(pPlayer.getStringUUID() + pSuffix);
        Path path2 = path.resolve(pPlayer.getStringUUID() + "_corrupted_" + LocalDateTime.now().format(FORMATTER) + pSuffix);
        if (Files.isRegularFile(path1)) {
            try {
                Files.copy(path1, path2, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            } catch (Exception exception) {
                LOGGER.warn("Failed to copy the player.dat file for {}", pPlayer.getName().getString(), exception);
            }
        }
    }

    private Optional<CompoundTag> load(Player pPlayer, String pSuffix) {
        File file1 = new File(this.playerDir, pPlayer.getStringUUID() + pSuffix);
        if (file1.exists() && file1.isFile()) {
            try {
                return Optional.of(NbtIo.readCompressed(file1.toPath(), NbtAccounter.unlimitedHeap()));
            } catch (Exception exception) {
                LOGGER.warn("Failed to load player data for {}", pPlayer.getName().getString());
            }
        }

        return Optional.empty();
    }

    public Optional<CompoundTag> load(Player pPlayer) {
        Optional<CompoundTag> optional = this.load(pPlayer, ".dat");
        if (optional.isEmpty()) {
            this.backup(pPlayer, ".dat");
        }

        return optional.or(() -> this.load(pPlayer, ".dat_old")).map(p_316252_ -> {
            int i = NbtUtils.getDataVersion(p_316252_, -1);
            p_316252_ = DataFixTypes.PLAYER.updateToCurrentVersion(this.fixerUpper, p_316252_, i);
            pPlayer.load(p_316252_);
            net.neoforged.neoforge.event.EventHooks.firePlayerLoadingEvent(pPlayer, playerDir, pPlayer.getStringUUID());
            return p_316252_;
        });
    }

    public File getPlayerDir() {
        return playerDir;
    }
}
