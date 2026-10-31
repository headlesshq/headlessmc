package io.github.headlesshq.headlessmc.quilt;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.java.launcher.ProcessHandler;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.AbstractClientInstaller;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.platform.util.CommonInstallerServices;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.Version;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Installs Quilt clients and servers by running the Quilt installer:
 * <pre>{@code
 * java -jar quilt-installer.jar install client <mc-version> [loader-version] --install-dir=<dir> --no-profile
 * java -jar quilt-installer.jar install server <mc-version> [loader-version] --install-dir=<dir>
 * }</pre>
 * The vanilla server is installed by HeadlessMc beforehand, the installer creates a quilt-server-launch.jar next to it.
 */
@Quilt
@ApplicationScoped
class QuiltInstaller extends AbstractClientInstaller implements ClientInstaller, ServerInstaller {
    private final QuiltInstallerDownloader installerDownloader;
    private final CommonInstallerServices services;

    @Inject
    QuiltInstaller(QuiltInstallerDownloader installerDownloader, CommonInstallerServices services) {
        this.installerDownloader = installerDownloader;
        this.services = services;
    }

    @Override
    public Installation installServer(VersionID id, Path dir, TypedMap args) throws HeadlessMcException {
        Installation vanillaInstallation = services.getVanillaInstaller()
            .installServer(id.asVanillaVersion(), dir, args);

        install(id, dir, args, true, vanillaInstallation.javaVersion());
        return new Installation(vanillaInstallation.javaVersion());
    }

    @Override
    protected void installClient(VersionID id, Path mcDir, Version vanilla, TypedMap args) throws HeadlessMcException {
        install(id, mcDir, args, false, vanilla.requireJavaVersion());
        deleteEmptyVersionJars(mcDir.resolve("versions"));
    }

    /**
     * The Quilt installer creates an empty {@code versions/<id>/<id>.jar}, because launchers are expected to
     * use the jar of the version it inherits from. HeadlessMc only downloads the client jar into that place
     * if it does not exist yet, so the empty jar would end up on the classpath, which Quilt loader cannot read.
     *
     * @param versionsDir the versions directory of the mc directory.
     * @throws HeadlessMcException if a jar cannot be deleted.
     */
    static void deleteEmptyVersionJars(Path versionsDir) throws HeadlessMcException {
        if (!Files.isDirectory(versionsDir)) {
            return;
        }

        try (Stream<Path> versions = Files.list(versionsDir)) {
            for (Path version : versions.filter(path -> path.getFileName().toString().startsWith("quilt-loader-")).toList()) {
                Path jar = version.resolve(version.getFileName() + ".jar");
                if (Files.isRegularFile(jar) && Files.size(jar) == 0L) {
                    Files.delete(jar);
                }
            }
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to delete empty Quilt version jars in " + versionsDir, e);
        }
    }

    @Override
    protected CommonInstallerServices getServices() {
        return services;
    }

    @Override
    protected String getPlatformName() {
        return Quilt.PLATFORM_NAME;
    }

    private void install(VersionID id, Path dir, TypedMap args, boolean server, int javaVersion)
        throws HeadlessMcException {
        Path installer = installerDownloader.download(args.get(CUSTOM_INSTALLER_URL));
        services.getLauncherService().buildProcess()
            .id(Quilt.PLATFORM_NAME + "-installer")
            .jar(installer)
            .arg(getArgs(id, dir, server).toArray(String[]::new))
            .version(javaVersion)
            .start()
            .waitFor(ProcessHandler.defaultHandler());
    }

    static List<String> getArgs(VersionID id, Path dir, boolean server) {
        List<String> args = new ArrayList<>();
        args.add("install");
        args.add(server ? "server" : "client");
        args.add(id.getVersion().getName());
        id.getBuild().ifPresent(loader -> args.add(loader.getName()));
        args.add("--install-dir=" + dir.toAbsolutePath());
        if (!server) {
            args.add("--no-profile");
        }

        return args;
    }

}
