package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.github.headlesshq.headlessmc.plugins.version.Version;
import io.github.headlesshq.headlessmc.plugins.version.VersionRange;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Loads the {@link PluginIndex} of officially supported plugins from {@link PluginConfig#indexUrl()}
 * and installs plugins from it into the plugins directory.
 */
@Slf4j
@ApplicationScoped
public class PluginIndexService {
    /**
     * The default {@link PluginConfig#indexUrl()}.
     */
    public static final String DEFAULT_INDEX_URL = "https://raw.githubusercontent.com/headlesshq/headlessmc-plugins/main/index.json";
    /**
     * Plugin ids become part of file names, like in the {@link PluginManifest}.
     */
    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9_-]*");
    /**
     * Versions become part of file names, so they must not contain separators.
     */
    private static final Pattern VERSION = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._+-]*");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-fA-F]{64}");
    private static final long PROGRESS_BAR_SIZE = 1_000_000;

    private final Holder<PluginConfig> config;
    private final DownloadService downloadService;
    private final JsonService jsonService;
    private final PluginScanner scanner;
    private final Version headlessMcVersion;

    @Inject
    public PluginIndexService(Holder<PluginConfig> config, DownloadService downloadService,
                              JsonService jsonService, PluginScanner scanner) {
        this(config, downloadService, jsonService, scanner, HeadlessMc.VERSION);
    }

    public PluginIndexService(Holder<PluginConfig> config, DownloadService downloadService,
                              JsonService jsonService, PluginScanner scanner, String headlessMcVersion) {
        this.config = config;
        this.downloadService = downloadService;
        this.jsonService = jsonService;
        this.scanner = scanner;
        this.headlessMcVersion = Version.parse(headlessMcVersion);
    }

    /**
     * @return the version of HeadlessMc plugins need to be compatible with.
     */
    public Version getHeadlessMcVersion() {
        return headlessMcVersion;
    }

    /**
     * @return the configured {@link PluginConfig#indexUrl()}.
     */
    public String getIndexUrl() {
        return config.get().indexUrl();
    }

    /**
     * Downloads the index.
     *
     * @return the index, without plugins that have an invalid id.
     * @throws HeadlessMcException if the index could not be downloaded or has an unsupported schema version.
     */
    public PluginIndex load() throws HeadlessMcException {
        String url = getIndexUrl();
        PluginIndex index;
        try {
            index = downloadService.download(URI.create(url)).map(download -> {
                try (InputStream inputStream = download.getInputStream()) {
                    return jsonService.parse(inputStream, PluginIndex.class);
                }
            });
        } catch (IllegalArgumentException e) {
            throw new HeadlessMcException("Invalid plugin index URL " + url + ": " + e.getMessage(), e);
        } catch (HeadlessMcException e) {
            throw new HeadlessMcException("Failed to load the plugin index from " + url + ": " + e.getMessage(), e);
        }

        if (index == null) {
            throw new HeadlessMcException("The plugin index " + url + " is empty");
        }

        if (index.schemaVersion() != PluginIndex.SCHEMA_VERSION) {
            throw new HeadlessMcException("The plugin index " + url + " has the unsupported schemaVersion "
                + index.schemaVersion() + ", expected " + PluginIndex.SCHEMA_VERSION + ", try updating HeadlessMc");
        }

        List<PluginIndex.Entry> plugins = new ArrayList<>();
        for (PluginIndex.Entry entry : index.pluginList()) {
            if (isValidId(entry.id())) {
                plugins.add(entry);
            } else {
                log.debug("Ignoring plugin with invalid id {} in the plugin index", entry.id());
            }
        }

        return new PluginIndex(index.schemaVersion(), plugins);
    }

    /**
     * @param id the id to check.
     * @return {@code true} if the id is a valid plugin id, lowercase letters, digits, {@code -} and {@code _}.
     */
    public static boolean isValidId(@Nullable String id) {
        return id != null && ID.matcher(id).matches();
    }

    /**
     * @param version the version to check.
     * @return {@code true} if the version only contains letters, digits, {@code .}, {@code +}, {@code -} and {@code _}.
     */
    public static boolean isValidVersion(@Nullable String version) {
        return version != null && VERSION.matcher(version).matches();
    }

    /**
     * @param release the release to check.
     * @return {@code true} if the {@link PluginIndex.Release#headlessmc()} range contains this version of HeadlessMc.
     */
    public boolean isCompatible(PluginIndex.Release release) {
        String range = release.headlessmc();
        if (range == null) {
            return false;
        }

        try {
            return VersionRange.parse(range).contains(headlessMcVersion);
        } catch (IllegalArgumentException e) {
            log.debug("Invalid headlessmc range {} of {}", range, release, e);
            return false;
        }
    }

    /**
     * @param plugin the plugin.
     * @return the newest release of the plugin that is compatible with this version of HeadlessMc.
     */
    public Optional<PluginIndex.Release> newestCompatible(PluginIndex.Entry plugin) {
        return sorted(plugin).stream().filter(this::isCompatible).findFirst();
    }

    /**
     * @param plugin the plugin.
     * @return the newest release of the plugin, compatible or not.
     */
    public Optional<PluginIndex.Release> newest(PluginIndex.Entry plugin) {
        return sorted(plugin).stream().findFirst();
    }

    /**
     * @param plugin the plugin.
     * @return the releases of the plugin with a valid version, newest first.
     */
    public List<PluginIndex.Release> sorted(PluginIndex.Entry plugin) {
        record Parsed(PluginIndex.Release release, Version version) {}
        List<Parsed> parsed = new ArrayList<>();
        for (PluginIndex.Release release : plugin.releaseList()) {
            if (!isValidVersion(release.version())) {
                continue;
            }

            try {
                parsed.add(new Parsed(release, Version.parse(release.version())));
            } catch (IllegalArgumentException e) {
                log.debug("Ignoring invalid version {} of plugin {}", release.version(), plugin.id(), e);
            }
        }

        return parsed.stream()
            .sorted(Comparator.comparing(Parsed::version).reversed())
            .map(Parsed::release)
            .toList();
    }

    /**
     * Downloads a release of a plugin into the plugins directory as {@code <id>-<version>.jar}, verifying its size
     * and SHA-256 hash. The jar is first downloaded to a temporary file, so that a failed download leaves nothing
     * behind. Afterwards, the other jars in the plugins directory containing a plugin with the same id are deleted,
     * so that installing another version replaces the old one.
     *
     * @param plugin    the plugin to install.
     * @param release   the release of the plugin to install.
     * @param pluginDir the plugins directory.
     * @return the installed jar and the jars that have been removed.
     * @throws HeadlessMcException if the download or its verification failed, or the jar is not the expected plugin.
     */
    public Installation install(PluginIndex.Entry plugin, PluginIndex.Release release, Path pluginDir)
        throws HeadlessMcException {
        String id = plugin.id();
        String version = release.version();
        if (!isValidId(id)) {
            throw new HeadlessMcException("Invalid plugin id '" + id + "', use lowercase letters, digits, - and _");
        }

        if (!isValidVersion(version)) {
            throw new HeadlessMcException("Invalid version '" + version + "' of plugin " + id);
        }

        String url = release.url();
        String sha256 = release.sha256();
        if (url == null || sha256 == null || !SHA256.matcher(sha256).matches()) {
            throw new HeadlessMcException("Plugin " + id + " " + version + " has no download URL or no valid sha256 in the plugin index");
        }

        Path target = pluginDir.resolve(id + "-" + version + ".jar");
        boolean downloaded = false;
        try {
            Files.createDirectories(pluginDir);
            if (!isInstalled(target, sha256)) {
                download(id, version, url, sha256, release.size(), pluginDir, target);
                downloaded = true;
            }

            List<Path> removed = new ArrayList<>();
            for (PluginFile file : scanner.scan(pluginDir)) {
                PluginManifest manifest = file.manifest();
                if (manifest != null && id.equals(manifest.id()) && !file.path().equals(target)) {
                    Files.delete(file.path());
                    removed.add(file.path());
                }
            }

            return new Installation(target, downloaded, removed);
        } catch (IOException e) {
            throw new HeadlessMcException("Failed to install plugin " + id + " " + version + " to " + pluginDir + ": " + e.getMessage(), e);
        }
    }

    private boolean isInstalled(Path target, String sha256) {
        return Files.isRegularFile(target) && scanner.read(target).sha256().equalsIgnoreCase(sha256);
    }

    private void download(String id, String version, String url, String sha256, @Nullable Long size,
                          Path pluginDir, Path target) throws IOException {
        // does not end with .jar, so it is never picked up as a plugin
        Path temp = Files.createTempFile(pluginDir, "." + id + "-" + version + "-", ".tmp");
        try {
            URI uri;
            try {
                uri = URI.create(url);
            } catch (IllegalArgumentException e) {
                throw new HeadlessMcException("Invalid download URL " + url + " of plugin " + id + " " + version, e);
            }

            downloadService.download(uri)
                .size(size)
                .hash(HashService.SHA256, sha256)
                // like for mods, small downloads finish too quickly for a progress bar to be useful
                .progressBar(size == null || size > PROGRESS_BAR_SIZE ? "Downloading plugin " + id + " " + version : null)
                .toFile(temp);

            PluginFile file = scanner.read(temp);
            PluginManifest manifest = file.manifest();
            if (manifest == null || !id.equals(manifest.id())) {
                throw new HeadlessMcException("The downloaded jar " + url + " is not the plugin " + id
                    + (manifest == null ? ": " + file.error() : ", but " + manifest.id()));
            }

            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /**
     * The result of {@link #install(PluginIndex.Entry, PluginIndex.Release, Path)}.
     *
     * @param file       the installed plugin jar.
     * @param downloaded {@code false} if the jar had already been installed.
     * @param removed    the jars containing other versions of the plugin that have been deleted.
     */
    public record Installation(Path file, boolean downloaded, List<Path> removed) {

    }

}
