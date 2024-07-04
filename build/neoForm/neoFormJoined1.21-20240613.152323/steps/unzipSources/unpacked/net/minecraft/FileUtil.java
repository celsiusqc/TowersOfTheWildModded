package net.minecraft;

import com.mojang.serialization.DataResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

public class FileUtil {
    private static final Pattern COPY_COUNTER_PATTERN = Pattern.compile("(<name>.*) \\((<count>\\d*)\\)", 66);
    private static final int MAX_FILE_NAME = 255;
    private static final Pattern RESERVED_WINDOWS_FILENAMES = Pattern.compile(".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?", 2);
    private static final Pattern RESERVED_WINDOWS_FILENAMES_NEOFORGE = Pattern.compile(".*\\.|(?:CON|PRN|AUX|NUL|CLOCK\\$|CONIN\\$|CONOUT\\$|(?:COM|LPT)[¹²³0-9])(?:\\..*)?", 2); // Neo: Fix MC-268617 - See PR #767
    private static final Pattern STRICT_PATH_SEGMENT_CHECK = Pattern.compile("[-._a-z0-9]+");

    public static String sanitizeName(String p_352262_) {
        for (char c0 : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
            p_352262_ = p_352262_.replace(c0, '_');
        }

        return p_352262_.replaceAll("[./\"]", "_");
    }

    public static String findAvailableName(Path pDirPath, String pFileName, String pFileFormat) throws IOException {
        pFileName = sanitizeName(pFileName);
        if ((net.neoforged.neoforge.common.NeoForgeMod.getProperFilenameValidation() ? RESERVED_WINDOWS_FILENAMES_NEOFORGE : RESERVED_WINDOWS_FILENAMES).matcher(pFileName).matches()) {
            pFileName = "_" + pFileName + "_";
        }

        Matcher matcher = COPY_COUNTER_PATTERN.matcher(pFileName);
        int i = 0;
        if (matcher.matches()) {
            pFileName = matcher.group("name");
            i = Integer.parseInt(matcher.group("count"));
        }

        if (pFileName.length() > 255 - pFileFormat.length()) {
            pFileName = pFileName.substring(0, 255 - pFileFormat.length());
        }

        while (true) {
            String s = pFileName;
            if (i != 0) {
                String s1 = " (" + i + ")";
                int j = 255 - s1.length();
                if (pFileName.length() > j) {
                    s = pFileName.substring(0, j);
                }

                s = s + s1;
            }

            s = s + pFileFormat;
            Path path = pDirPath.resolve(s);

            try {
                Path path1 = Files.createDirectory(path);
                Files.deleteIfExists(path1);
                return pDirPath.relativize(path1).toString();
            } catch (FileAlreadyExistsException filealreadyexistsexception) {
                i++;
            }
        }
    }

    public static boolean isPathNormalized(Path pPath) {
        Path path = pPath.normalize();
        return path.equals(pPath);
    }

    public static boolean isPathPortable(Path pPath) {
        for (Path path : pPath) {
            if ((net.neoforged.neoforge.common.NeoForgeMod.getProperFilenameValidation() ? RESERVED_WINDOWS_FILENAMES_NEOFORGE : RESERVED_WINDOWS_FILENAMES).matcher(path.toString()).matches()) {
                return false;
            }
        }

        return true;
    }

    public static Path createPathToResource(Path pDirPath, String pLocationPath, String pFileFormat) {
        String s = pLocationPath + pFileFormat;
        Path path = Paths.get(s);
        if (path.endsWith(pFileFormat)) {
            throw new InvalidPathException(s, "empty resource name");
        } else {
            return pDirPath.resolve(path);
        }
    }

    public static String getFullResourcePath(String pPath) {
        return FilenameUtils.getFullPath(pPath).replace(File.separator, "/");
    }

    public static String normalizeResourcePath(String pPath) {
        return FilenameUtils.normalize(pPath).replace(File.separator, "/");
    }

    public static DataResult<List<String>> decomposePath(String pPath) {
        int i = pPath.indexOf(47);
        if (i == -1) {
            return switch (pPath) {
                case "", ".", ".." -> DataResult.error(() -> "Invalid path '" + pPath + "'");
                default -> !isValidStrictPathSegment(pPath)
                ? DataResult.error(() -> "Invalid path '" + pPath + "'")
                : DataResult.success(List.of(pPath));
            };
        } else {
            List<String> list = new ArrayList<>();
            int j = 0;
            boolean flag = false;

            while (true) {
                String s = pPath.substring(j, i);
                switch (s) {
                    case "":
                    case ".":
                    case "..":
                        return DataResult.error(() -> "Invalid segment '" + s + "' in path '" + pPath + "'");
                }

                if (!isValidStrictPathSegment(s)) {
                    return DataResult.error(() -> "Invalid segment '" + s + "' in path '" + pPath + "'");
                }

                list.add(s);
                if (flag) {
                    return DataResult.success(list);
                }

                j = i + 1;
                i = pPath.indexOf(47, j);
                if (i == -1) {
                    i = pPath.length();
                    flag = true;
                }
            }
        }
    }

    public static Path resolvePath(Path pPath, List<String> p_251495_) {
        int i = p_251495_.size();

        return switch (i) {
            case 0 -> pPath;
            case 1 -> pPath.resolve(p_251495_.get(0));
            default -> {
                String[] astring = new String[i - 1];

                for (int j = 1; j < i; j++) {
                    astring[j - 1] = p_251495_.get(j);
                }

                yield pPath.resolve(pPath.getFileSystem().getPath(p_251495_.get(0), astring));
            }
        };
    }

    public static boolean isValidStrictPathSegment(String p_249814_) {
        return STRICT_PATH_SEGMENT_CHECK.matcher(p_249814_).matches();
    }

    public static void validatePath(String... pElements) {
        if (pElements.length == 0) {
            throw new IllegalArgumentException("Path must have at least one element");
        } else {
            for (String s : pElements) {
                if (s.equals("..") || s.equals(".") || !isValidStrictPathSegment(s)) {
                    throw new IllegalArgumentException("Illegal segment " + s + " in path " + Arrays.toString((Object[])pElements));
                }
            }
        }
    }

    public static void createDirectoriesSafe(Path pPath) throws IOException {
        Files.createDirectories(Files.exists(pPath) ? pPath.toRealPath() : pPath);
    }
}
