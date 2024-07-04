package net.minecraft.util;

import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalLong;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class HttpUtil {
    private static final Logger LOGGER = LogUtils.getLogger();

    private HttpUtil() {
    }

    public static Path downloadFile(
        Path pSaveFile,
        URL pUrl,
        Map<String, String> pRequestProperties,
        HashFunction pHashFunction,
        @Nullable HashCode pHash,
        int pMaxSize,
        Proxy pProxy,
        HttpUtil.DownloadProgressListener pProgressListener
    ) {
        HttpURLConnection httpurlconnection = null;
        InputStream inputstream = null;
        pProgressListener.requestStart();
        Path path;
        if (pHash != null) {
            path = cachedFilePath(pSaveFile, pHash);

            try {
                if (checkExistingFile(path, pHashFunction, pHash)) {
                    LOGGER.info("Returning cached file since actual hash matches requested");
                    pProgressListener.requestFinished(true);
                    updateModificationTime(path);
                    return path;
                }
            } catch (IOException ioexception1) {
                LOGGER.warn("Failed to check cached file {}", path, ioexception1);
            }

            try {
                LOGGER.warn("Existing file {} not found or had mismatched hash", path);
                Files.deleteIfExists(path);
            } catch (IOException ioexception) {
                pProgressListener.requestFinished(false);
                throw new UncheckedIOException("Failed to remove existing file " + path, ioexception);
            }
        } else {
            path = null;
        }

        Path $$18;
        try {
            httpurlconnection = (HttpURLConnection)pUrl.openConnection(pProxy);
            httpurlconnection.setInstanceFollowRedirects(true);
            pRequestProperties.forEach(httpurlconnection::setRequestProperty);
            inputstream = httpurlconnection.getInputStream();
            long i = httpurlconnection.getContentLengthLong();
            OptionalLong optionallong = i != -1L ? OptionalLong.of(i) : OptionalLong.empty();
            FileUtil.createDirectoriesSafe(pSaveFile);
            pProgressListener.downloadStart(optionallong);
            if (optionallong.isPresent() && optionallong.getAsLong() > (long)pMaxSize) {
                throw new IOException("Filesize is bigger than maximum allowed (file is " + optionallong + ", limit is " + pMaxSize + ")");
            }

            if (path == null) {
                Path path3 = Files.createTempFile(pSaveFile, "download", ".tmp");

                try {
                    HashCode hashcode1 = downloadAndHash(pHashFunction, pMaxSize, pProgressListener, inputstream, path3);
                    Path path2 = cachedFilePath(pSaveFile, hashcode1);
                    if (!checkExistingFile(path2, pHashFunction, hashcode1)) {
                        Files.move(path3, path2, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        updateModificationTime(path2);
                    }

                    pProgressListener.requestFinished(true);
                    return path2;
                } finally {
                    Files.deleteIfExists(path3);
                }
            }

            HashCode hashcode = downloadAndHash(pHashFunction, pMaxSize, pProgressListener, inputstream, path);
            if (!hashcode.equals(pHash)) {
                throw new IOException("Hash of downloaded file (" + hashcode + ") did not match requested (" + pHash + ")");
            }

            pProgressListener.requestFinished(true);
            $$18 = path;
        } catch (Throwable throwable) {
            if (httpurlconnection != null) {
                InputStream inputstream1 = httpurlconnection.getErrorStream();
                if (inputstream1 != null) {
                    try {
                        LOGGER.error("HTTP response error: {}", IOUtils.toString(inputstream1, StandardCharsets.UTF_8));
                    } catch (Exception exception) {
                        LOGGER.error("Failed to read response from server");
                    }
                }
            }

            pProgressListener.requestFinished(false);
            throw new IllegalStateException("Failed to download file " + pUrl, throwable);
        } finally {
            IOUtils.closeQuietly(inputstream);
        }

        return $$18;
    }

    private static void updateModificationTime(Path pPath) {
        try {
            Files.setLastModifiedTime(pPath, FileTime.from(Instant.now()));
        } catch (IOException ioexception) {
            LOGGER.warn("Failed to update modification time of {}", pPath, ioexception);
        }
    }

    private static HashCode hashFile(Path pPath, HashFunction pHashFunction) throws IOException {
        Hasher hasher = pHashFunction.newHasher();

        try (
            OutputStream outputstream = Funnels.asOutputStream(hasher);
            InputStream inputstream = Files.newInputStream(pPath);
        ) {
            inputstream.transferTo(outputstream);
        }

        return hasher.hash();
    }

    private static boolean checkExistingFile(Path pPath, HashFunction pHashFunction, HashCode pExpectedHash) throws IOException {
        if (Files.exists(pPath)) {
            HashCode hashcode = hashFile(pPath, pHashFunction);
            if (hashcode.equals(pExpectedHash)) {
                return true;
            }

            LOGGER.warn("Mismatched hash of file {}, expected {} but found {}", pPath, pExpectedHash, hashcode);
        }

        return false;
    }

    private static Path cachedFilePath(Path pPath, HashCode pHash) {
        return pPath.resolve(pHash.toString());
    }

    private static HashCode downloadAndHash(
        HashFunction pHashFuntion, int pMaxSize, HttpUtil.DownloadProgressListener pProgressListener, InputStream pStream, Path pOutputPath
    ) throws IOException {
        HashCode hashcode;
        try (OutputStream outputstream = Files.newOutputStream(pOutputPath, StandardOpenOption.CREATE)) {
            Hasher hasher = pHashFuntion.newHasher();
            byte[] abyte = new byte[8196];
            long j = 0L;

            int i;
            while ((i = pStream.read(abyte)) >= 0) {
                j += (long)i;
                pProgressListener.downloadedBytes(j);
                if (j > (long)pMaxSize) {
                    throw new IOException("Filesize was bigger than maximum allowed (got >= " + j + ", limit was " + pMaxSize + ")");
                }

                if (Thread.interrupted()) {
                    LOGGER.error("INTERRUPTED");
                    throw new IOException("Download interrupted");
                }

                outputstream.write(abyte, 0, i);
                hasher.putBytes(abyte, 0, i);
            }

            hashcode = hasher.hash();
        }

        return hashcode;
    }

    public static int getAvailablePort() {
        try {
            int i;
            try (ServerSocket serversocket = new ServerSocket(0)) {
                i = serversocket.getLocalPort();
            }

            return i;
        } catch (IOException ioexception) {
            return 25564;
        }
    }

    public static boolean isPortAvailable(int pPort) {
        if (pPort >= 0 && pPort <= 65535) {
            try {
                boolean flag;
                try (ServerSocket serversocket = new ServerSocket(pPort)) {
                    flag = serversocket.getLocalPort() == pPort;
                }

                return flag;
            } catch (IOException ioexception) {
                return false;
            }
        } else {
            return false;
        }
    }

    public interface DownloadProgressListener {
        void requestStart();

        void downloadStart(OptionalLong pTotalSize);

        void downloadedBytes(long pProgress);

        void requestFinished(boolean pSuccess);
    }
}
