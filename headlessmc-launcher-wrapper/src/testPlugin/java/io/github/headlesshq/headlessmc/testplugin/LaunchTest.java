package io.github.headlesshq.headlessmc.testplugin;

import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.mods.files.ModFileReadResult;
import io.github.headlesshq.headlessmc.launcher.server.Server;
import io.github.headlesshq.headlessmc.launcher.version.Version;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class LaunchTest {
    public static void build(Java java, Launcher launcher, TestInputStream is) {
        String vanilla = java.getVersion() <= 8 ? "1.12.2" : (java.getVersion() <= 17 ? "1.20.4" : "1.21");
        String modlauncher = java.getVersion() <= 8 ? "forge" : "neoforge";
        boolean inMemory = Boolean.parseBoolean(System.getProperty("integrationTestRunInMemory", "false"));
        String inMemoryFlag = inMemory ? "-inmemory" : "";

        if (Boolean.parseBoolean(System.getProperty("integrationTestRunServer", "false"))) {
            serverTest(launcher, is, vanilla, modlauncher);
            return;
        }

        is.add("help");

        is.add("plugins");

        is.add("versions");

        is.add("download 1.12.2");
        is.add("n"); // potentially if already downloaded
        is.add(ps -> assertTrue(launcher.getVersionService().stream().anyMatch(v -> "1.12.2".equals(v.getName()))));

        is.add("download 1.20.4");
        is.add("n"); // potentially if already downloaded
        is.add(ps -> assertTrue(launcher.getVersionService().stream().anyMatch(v -> "1.20.4".equals(v.getName()))));

        is.add("download 1.21");
        is.add("n"); // potentially if already downloaded
        is.add(ps -> assertTrue(launcher.getVersionService().stream().anyMatch(v -> "1.21".equals(v.getName()))));

        is.add("versions");

        is.add("login");

        is.add("login -cancel 0");

        is.add("login -webview");

        is.add("login -cancel 0");

        is.add("login test.mail@test.com");

        is.add("abort");

        if (java.getVersion() > 8) {
            is.add("fabric " + vanilla);
            is.add("n");
            is.add(ps -> assertTrue(launcher.getVersionService().stream().anyMatch(v -> v.getName().toLowerCase(Locale.ENGLISH).contains("fabric"))));
        }

        is.add(modlauncher + " " + vanilla);
        is.add("n");
        is.add(ps -> assertTrue(launcher.getVersionService().stream().anyMatch(v -> v.getName().toLowerCase(Locale.ENGLISH).contains(modlauncher))));

        is.add("multi \"json " + vanilla + "\" versions");

        is.add("java");

        is.add("java -current");

        is.add("memory");

        is.add("loglevel INFO");

        is.add("offline true");

        is.add(ps -> {
            Optional<Version> version = launcher
                .getVersionService()
                .stream()
                .filter(v -> v.getName().toLowerCase(Locale.ENGLISH).contains(modlauncher) && vanilla.equals(v.getParentName())).findFirst();

            assertTrue(version.isPresent(), "Failed to find a version with name containing " + modlauncher + " in "
                + launcher.getVersionService().stream().map(HasName::getName).collect(Collectors.toList()));
            ps.println("specifics " + version.get().getId() + " mc-runtime-test -id");
        });

        AtomicBoolean returnedFromLaunching = new AtomicBoolean();
        is.add(ps -> {
            Optional<Version> version = launcher
                .getVersionService()
                .stream()
                .filter(v -> v.getName().toLowerCase(Locale.ENGLISH).contains(modlauncher) && vanilla.equals(v.getParentName())).findFirst();

            assertTrue(version.isPresent());
            Thread timeOutThread = new Thread(() -> {
                try {
                    Thread.sleep(TimeUnit.MINUTES.toMillis(10));
                    if (!returnedFromLaunching.get()) {
                        System.exit(-1);
                    }
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            });

            timeOutThread.start();
            assertTrue(launcher.getAccountManager().getOfflineChecker().isOffline());
            if (inMemory) {
                ExitTrap.trapExit();
            }

            ps.println("launch " + version.get().getId() + " -id -commands -lwjgl -stay " + inMemoryFlag);
        });

        // for some reason test might hang here?
        is.add(PrintStream::println);
        is.add(PrintStream::flush);

        is.add(ps -> returnedFromLaunching.set(true));
        is.add(ps -> ExitTrap.remove());
    }

    private static void serverTest(Launcher launcher,
                                   TestInputStream is,
                                   String vanilla,
                                   String modlauncher) {
        System.setProperty(LauncherProperties.SERVER_ACCEPT_EULA.getName(), "true");
        System.setProperty(LauncherProperties.SERVER_LAUNCH_FOR_EULA.getName(), "true");
        System.setProperty(LauncherProperties.SERVER_TEST.getName(), "true");

        launcher.getLoggingService().setLevel(Level.FINE);

        is.add("server");
        is.add(ps -> assertFalse(launcher.getServerManager().stream().findAny().isPresent()));

        is.add("server add vanilla " + vanilla);
        is.add("server list");
        is.add(ps -> assertTrue(launcher.getServerManager()
                .stream()
                .anyMatch(s -> s.getVersion().getServerType().getName().equals("vanilla"))));

        is.add("server remove 0 -id");

        is.add("server add fabric " + vanilla);
        is.add("server list");
        is.add(ps -> assertTrue(launcher.getServerManager()
                .stream()
                .anyMatch(s -> s.getVersion().getServerType().getName().equals("fabric"))));

        is.add("server remove 0 -id");

        is.add("server add " + modlauncher + " " + vanilla);
        is.add("server list");
        is.add(ps -> assertTrue(launcher.getServerManager()
                .stream()
                .anyMatch(s -> s.getVersion().getServerType().getName().equals(modlauncher))));

        is.add("server remove 0 -id");

        is.add("server add purpur " + vanilla);
        is.add("server server mod search simple-voice-chat");
        is.add("server list");
        is.add(ps -> assertTrue(launcher.getServerManager()
                .stream()
                .anyMatch(s -> s.getVersion().getServerType().getName().equals("purpur"))));

        is.add("server mod add 0 simple-voice-chat");
        is.add("server mod list 0");
        is.add(ps -> {
            Server server = launcher.getServerManager()
                    .stream()
                    .filter(s -> s.getVersion().getServerType().getName().equals("purpur"))
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);

            try {
                ModFileReadResult result = launcher.getModManager().getModFileReaderManager().read(server);
                assertEquals(1, result.getMods().size());
                assertEquals("voicechat", result.getMods().get(0).getName());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });

        is.add("server mod remove 0 0");
        is.add("server mod list 0");
        is.add(ps -> {
            Server server = launcher.getServerManager()
                    .stream()
                    .filter(s -> s.getVersion().getServerType().getName().equals("purpur"))
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);
            try {
                ModFileReadResult result = launcher.getModManager().getModFileReaderManager().read(server);
                assertEquals(0, result.getMods().size());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });

        is.add("server remove 0 -id");

        is.add("server add paper " + vanilla);
        is.add("server list");

        is.add(ps -> assertTrue(launcher.getServerManager()
                .stream()
                .anyMatch(s -> s.getVersion().getServerType().getName().equals("paper"))));

        is.add("server eula 0 -id accept");

        is.add("server cache 0 -id");

        is.add("server remove 0 -id");

        is.add("server list");

        is.add(ps -> assertFalse(launcher.getServerManager().stream().findAny().isPresent()));

        is.add(ps -> {
            System.setProperty(
                    LauncherProperties.SERVER_TEST_DIR.getName(),
                    launcher.getFileManager().getDir("servertest").getAbsolutePath()
            );
            System.setProperty(LauncherProperties.SERVER_TEST_NAME.getName(), "test");
            System.setProperty(LauncherProperties.SERVER_TEST_TYPE.getName(), "paper");
            System.setProperty(LauncherProperties.SERVER_TEST_VERSION.getName(), vanilla);
        });

        is.add("server add paper " + vanilla);
        is.add("server list");

        is.add(ps -> assertTrue(launcher.getServerManager()
                .stream()
                .filter(s -> "test".equals(s.getName()))
                .anyMatch(s -> s.getVersion().getServerType().getName().equals("paper"))));

        is.add("server launch test");
    }

}
