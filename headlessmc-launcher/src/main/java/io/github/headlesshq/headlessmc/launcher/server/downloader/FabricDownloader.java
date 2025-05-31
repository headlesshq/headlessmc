package io.github.headlesshq.headlessmc.launcher.server.downloader;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.command.FabricCommand;
import io.github.headlesshq.headlessmc.launcher.server.ServerTypeDownloader;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class FabricDownloader implements ServerTypeDownloader {
    private static final URI URL = URI.create("https://meta.fabricmc.net/v2/versions/loader");

    private final ServerTypeDownloader vanillaDownloader;

    @Override
    public DownloadHandler download(Launcher launcher, String version, @Nullable String typeVersion, String... args) throws IOException {
        Version parsedVersion = VersionUtil.getVersion(launcher, version);
        String build;
        if (typeVersion == null) {
            String json = launcher.getDownloadService().download(URL).getContentAsString();
            Gson gson = new Gson();
            Type listType = new TypeToken<List<BuildData>>() {}.getType();
            List<BuildData> builds = gson.fromJson(json, listType);
            Collections.sort(builds);
            if (builds.isEmpty()) {
                throw new IOException("No Fabric builds found!");
            }

            build = builds.get(builds.size() - 1).getVersion();
        } else {
            build = typeVersion;
        }

        return typeVersionToDownloadPathResolver -> {
            Path directory = typeVersionToDownloadPathResolver.resolve(build);

            FabricCommand fabricCommand = new FabricCommand(launcher);
            List<String> commandArgs = new ArrayList<>();
            commandArgs.add("fabric");
            commandArgs.add(parsedVersion.getName());
            commandArgs.add("-server");
            commandArgs.add("--dir");
            commandArgs.add(directory.toAbsolutePath().toString());
            if (typeVersion != null) {
                commandArgs.add("--uid");
                commandArgs.add(typeVersion);
            }

            commandArgs.addAll(Arrays.stream(args)
                    .filter(arg -> !"-list".equalsIgnoreCase(arg))
                    .collect(Collectors.toList()));
            try {
                Files.createDirectories(directory);
                fabricCommand.execute(parsedVersion, commandArgs.toArray(new String[0]));
            } catch (CommandException e) {
                throw new IOException(e);
            }

            // we also need the server.jar for the fabric server
            vanillaDownloader.download(launcher, version, null).download(ignored -> directory);
            return directory;
        };
    }

    @Getter
    private static class BuildData implements Comparable<BuildData> {
        @SerializedName("separator")
        String separator;
        @SerializedName("build")
        int build;
        @SerializedName("maven")
        String maven;
        @SerializedName("version")
        String version;
        @SerializedName("stable")
        boolean stable;

        @Override
        public int compareTo(BuildData other) {
            return Integer.compare(this.build, other.build);
        }
    }

}
