package io.github.headlesshq.headlessmc.version;

import io.github.headlesshq.headlessmc.util.maven.Artifact;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * This class represents a minecraft version.json file.
 * It makes some simple abstractions, but follows the format as close as possible,
 * such that it can be implemented in a few simple POJOs,
 * that can be parsed by a Java JsonParser.
 */
public interface Version {
    /**
     * Example: {@code "id": "1.12.2"}
     *
     * @return the id/name of this version.
     */
    String getId();

    /**
     * Example: {@code "libraries": [...]}
     *
     * @return a list of {@link Library} of this version.
     */
    List<Library> getLibraries();

    /**
     * ModLoader versions (forge/fabric etc.) inherit from vanilla versions.
     * For seeing inheritance is managed see {@link ProcessedVersion}.
     * Example: {@code "inheritsFrom": "1.12.2"}
     *
     * @return the id of the parent version.
     */
    @Nullable String getInheritsFrom();

    /**
     * Example: {@code "type": "release"}
     *
     * @return the type of this version (release, snapshot, old_alpha, old_beta)
     */
    @Nullable String getType();

    /**
     * Example: {@code "mainClass": "net.minecraft.client.main.Main"}
     *
     * @return the main class for the client of this version.
     */
    @Nullable String getMainClass();

    /**
     * This is the legacy version of {@link #getArguments()},
     * which is a single string containing the entire command line.
     * Example: {@code "minecraftArguments": "${auth_player_name} ${auth_session} --gameDir ${game_directory} ..."}
     *
     * @return the game arguments to launch Minecraft with.
     */
    @Nullable String getMinecraftArguments();

    String GAME_ARGUMENTS = "game";
    String JVM_ARGUMENTS = "jvm";
    String DEFAULT_JVM_ARGUMENTS = "default-user-jvm";
    Set<String> KNOWN_ARGUMENTS = Set.of(GAME_ARGUMENTS, JVM_ARGUMENTS, DEFAULT_JVM_ARGUMENTS);
    /**
     * Represents game/jvm arguments to start the game with.
     * Example:
     * <pre>
     * {@code
     *  "arguments": {
     *      "game": [
     *          "--username",
     *          "${auth_player_name}",
     *          "...",
     *          {
     *              "value": "--demo"
     *              "rules": [
     *                  {
     *                      "action": "allow",
     *                      "features": {
     *                          "is_demo_user": true
     *                      }
     *                  }
     *              ]
     *          }
     *      ],
     *      "jvm": [
     *          "-Djava.library.path=${natives_directory}",
     *          "..."
     *      ]
     * }
     * }
     * </pre>
     *
     * @return the jvm/game arguments to start the game with.
     */
    @Nullable Map<String, List<Argument>> getArguments();

    @Nullable AssetIndex getAssetIndex();

    /**
     * The {@code "client"} key for the {@link #getLogging()}.
     */
    String LOGGING_CLIENT = "client";

    /**
     * Represents the logging configuration entry in a version.json.
     * <pre>
     * {@code
     * "logging": {
     *      "client": {
     * 		    "argument": "-Dlog4j.configurationFile=${path}",
     * 		    "file": {
     * 			    "id": "client-1.12.xml",
     * 			    "sha1": "bd65e7d2e3c237be76cfbef4c2405033d7f91521",
     * 			    "size": 888,
     * 			    "url": "https://example.com/client-1.12.xml"
     *          },
     *          "type": "log4j2-xml"
     *      }
     * }
     * }
     * </pre>
     */
    @Nullable Map<String, LoggingConfiguration> getLogging();

    /**
     * The {@code "client"} key for the {@link #getDownloads()}.
     */
    String DOWNLOAD_CLIENT = "client";
    /**
     * The {@code "server"} key for the {@link #getDownloads()}.
     */
    String DOWNLOAD_SERVER = "server";
    /**
     * The {@code "client_mappings"} key for the {@link #getDownloads()}.
     */
    String DOWNLOAD_CLIENT_MAPPINGS = "client_mappings";
    /**
     * The {@code "server_mappings"} key for the {@link #getDownloads()}.
     */
    String DOWNLOAD_SERVER_MAPPINGS = "server_mappings";
    /**
     * The {@code "windows_server"} key for the {@link #getDownloads()}.
     */
    String DOWNLOAD_WINDOWS_SERVER = "windows_server";

    @Nullable Map<String, Download> getDownloads();

    @Nullable JavaVersion getJavaVersion();

    default int requireJavaVersion() {
        return Optional.ofNullable(getJavaVersion())
            .map(JavaVersion::getMajorVersion)
            .orElseThrow(() -> new NullPointerException("Failed to get java version of version " + getId()));
    }

    String OS_LINUX = "linux";
    String OS_WINDOWS = "windows";
    String OS_OSX = "osx";
    String OS_UNKNOWN = "unknown"; // HMCL launcher may add this
    Set<String> KNOWN_OS = Set.of(OS_LINUX, OS_WINDOWS, OS_OSX, OS_UNKNOWN);

    /**
     * Represents a library in a version.json file:
     * <pre>
     * {@code
     * "downloads": {
     * 	"artifact": {
     * 		"path": ".../lwjgl-platform-2.9.4-nightly-20150209.jar",
     * 		"sha1": "b04f3ee8f5e43fa3b162981b50bb72fe1acabb33",
     * 		"size": 22,
     * 		"url": "https://..."
     *     },
     * 	"classifiers": {
     * 		"natives-linux": {
     * 			"path": ".../lwjgl-platform-2.9.4-...-natives-linux.jar",
     * 			"sha1": "931074f46c795d2f7b30ed6395df5715cfd7675b",
     * 			"size": 578680,
     * 			"url": "https://..."
     *      },
     * 		"natives-osx": {
     * 			"path": ".../lwjgl-platform-2.9.4-...-natives-osx.jar",
     * 			...
     *      },
     * 	 ...
     * }
     * "extract": {
     * 	"exclude": [
     * 		"META-INF/"
     * 	]
     * },
     * "name": "org.lwjgl.lwjgl:lwjgl-platform:2.9.4-nightly-20150209",
     * "natives": {
     * 	"linux": "natives-linux",
     * 	"osx": "natives-osx",
     * 	"windows": "natives-windows"
     * },
     * "rules": [
     *      {
     * 		   "action": "allow"
     *      },
     *      {
     * 		   "action": "disallow",
     * 		   "os": {
     * 			   "name": "osx"
     *         }
     *       }
     * ]
     * }
     * </pre>
     */
    interface Library extends Download {
        @Nullable Map<String, String> getNatives();

        String getName();

        @Nullable Extract getExtract();

        @Nullable List<Rule> getRules();

        @Nullable LibraryDownloads getDownloads();

        // these algorithms are fabric only

        @Nullable String getMd5();

        @Nullable String getSha256();

        @Nullable String getSha512();

        @SuppressWarnings({"DataFlowIssue", "ConstantValue"})
        default Map<String, String> getHashAlgorithms() {
            Map<String, String> result = new HashMap<>();
            result.put("MD5", getMd5());
            result.put("SHA-1", getSha1());
            result.put("SHA-256", getSha256());
            result.put("SHA-512", getSha512());
            result.entrySet().removeIf(e -> e.getValue() == null);
            return result;
        }

        default Artifact getArtifact() {
            return Artifact.of(getName());
        }
    }

    /**
     * Extraction rules for a {@link Library}.
     * <pre>
     * {@code
     * "extract": {
     *      "exclude": [
     *          "META-INF/"
     *      ]
     * }
     * }
     * </pre>
     */
    interface Extract {
        /**
         * A list of file paths in a jar.
         * Every file starting with that path will not be extraced.
         *
         * @return the paths to exclude from extraction of jars.
         */
        @Nullable List<String> getExclude();
    }

    interface Rule {
        String ALLOW = "allow";
        String DISALLOW = "disallow";

        String getAction();

        @Nullable OS getOs();

        @Nullable Map<String, Boolean> getFeatures();

        interface OS {
            @Nullable String getName();

            @Nullable String getVersion(); // regex with version

            @Nullable String getArch(); // e.g. x86, 1.13.json

            // TODO: test!
            @Nullable VersionRange getVersionRange();

            /**
             * <pre>
             * {@code
             * {
             *   "action": "allow",
             *   "os": {
             *     "name": "windows",
             *     "versionRange": {
             *       "min": "10.0.17134"
             *     }
             *   }
             * }
             * }
             * </pre>
             */
            interface VersionRange {
                @Nullable String getMin();

                @Nullable String getMax();
            }
        }
    }

    interface LibraryDownloads {
        String NATIVES_LINUX = "natives-linux";
        // older versions
        String NATIVES_OSX = "natives-osx";
        String NATIVES_WINDOWS = "natives-windows";
        String NATIVES_MAC_OS = "natives-macos";
        // older versions 1.8.json
        // natives-windows-${arch}
        String NATIVES_WINDOWS_32 = "natives-windows-32";
        String NATIVES_WINDOWS_64 = "natives-windows-64";
        // snapshot 22w19a.json
        String NATIVES_LINUX_X86_64 = "linux-x86_64";

        @Nullable Download getArtifact();

        @Nullable Map<String, Download> getClassifiers();
    }

    interface Download {
        @Nullable String getId();

        @Nullable String getSha1();

        @Nullable Long getSize();

        @Nullable String getUrl();

        // for LoggingConfiguration
        @Nullable String getPath();
    }

    interface AssetIndex extends Download {
        /**
         * @return the total size of all asset files contained in this index.
         */
        @Nullable Long getTotalSize();
    }

    interface JavaVersion {
        String getComponent();

        Integer getMajorVersion();
    }

    interface Argument {
        @Nullable List<Rule> getRules();

        List<String> value();
    }

    /**
     * Represents the logging configuration entry in a version.json.
     * <pre>
     * {@code
     * "logging": {
     *      "client": {
     * 		   "argument": "-Dlog4j.configurationFile=${path}",
     * 		   "file": {
     * 			    "id": "client-1.12.xml",
     * 			    "sha1": "bd65e7d2e3c237be76cfbef4c2405033d7f91521",
     * 			    "size": 888,
     * 			    "url": "https://example.com/client-1.12.xml"
     *          },
     *          "type": "log4j2-xml"
     *      }
     * }
     * }
     * </pre>
     */
    interface LoggingConfiguration {
        /**
         * @return the SystemProperty that is added to the jvm arguments.
         */
        String getArgument();

        Download getFile();

        String getType();
    }

}
