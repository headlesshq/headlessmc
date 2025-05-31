package io.github.headlesshq.headlessmc.java.download;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.github.headlesshq.headlessmc.os.OS;
import lombok.CustomLog;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

@CustomLog
public class TemurinDownloader implements JavaDownloader {
    @Override
    public void download(Path javaVersionsDir, JavaDownloadRequest request) throws IOException {
        TemurinPackage temurinPackage = getPackage(request);
        Files.createDirectories(javaVersionsDir);
        Path downloadPath = javaVersionsDir.resolve(getFileNameFromPackage(temurinPackage.getLink()));
        try {
            request.getClient().downloadBigFile(temurinPackage.getLink(), downloadPath, request.getProgressBarTitle(), request.getProgressBarProvider());
            downloadPath.toFile().deleteOnExit();
            new ArchiveExtractor().extract(downloadPath, true/* zip contains one jre folder so we unzip to HeadlessMC/java directly*/);
        } finally {
            Files.delete(downloadPath);
        }
    }

    TemurinPackage getPackage(JavaDownloadRequest request) throws IOException {
        String imageType = request.isJdk() ? "jdk" : "jre";
        String url = String.format("https://api.adoptium.net/v3/assets/latest/%d/hotspot?os=%s&architecture=%s",
                request.getVersion(), getPlatform(request.getOs()), getArchitecture(request.getOs()));

        String text = request.getClient().httpGetText(url);
        Gson gson = new Gson();
        JsonArray jsonArray = gson.fromJson(text, JsonArray.class);
        for (JsonElement element : jsonArray) {
            TemurinPackage temurinPackage = TemurinPackage.parse(element.getAsJsonObject());
            if (temurinPackage == null) {
                log.warn("Failed to parse temurin package " + element);
            } else if (imageType.equals(temurinPackage.getImageType())) {
                return temurinPackage;
            }
        }

        throw new IOException("Failed to find Temuring package for " + request);
    }

    private static String getFileNameFromPackage(String url) throws IOException {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private String getArchitecture(OS os) {
        if ("amd64".equalsIgnoreCase(os.getArchitecture())) {
            return "x64";
        }

        if (os.isArm()) {
            return "aarch64";
        }

        return os.getArchitecture();
    }

    private String getPlatform(OS os) {
        OS.Type type = os.getType();
        if (type == OS.Type.UNKNOWN || type == OS.Type.OSX) {
            return "mac";
        }

        return type.getName();
    }

    @Data
    public static class TemurinPackage {
        private final String link;
        private final String imageType;
        // could also check the checksum? private final String checksum_link;

        // TODO: at some point use gson reflection to get this
        //  but for graalvm this is simpler for now
        public static @Nullable TemurinPackage parse(JsonElement element) {
            if (element.isJsonObject()) {
                JsonElement bin = element.getAsJsonObject().get("binary");
                if (bin != null && bin.isJsonObject()) {
                    JsonElement imageType = bin.getAsJsonObject().get("image_type");
                    if (imageType != null && imageType.isJsonPrimitive()) {
                        JsonElement pkg = bin.getAsJsonObject().get("package");
                        if (pkg != null && pkg.isJsonObject()) {
                            JsonElement link = pkg.getAsJsonObject().get("link");
                            if (link != null && link.isJsonPrimitive()) {
                                return new TemurinPackage(link.getAsString(), imageType.getAsString());
                            }
                        }
                    }
                }
            }

            return null;
        }
    }

}
