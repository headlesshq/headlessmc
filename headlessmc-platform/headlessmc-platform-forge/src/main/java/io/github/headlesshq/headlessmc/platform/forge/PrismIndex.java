package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;
import java.util.NoSuchElementException;

// TODO: mirrors: https://raw.githubusercontent.com/PrismLauncher/meta-launcher/refs/heads/master/net.minecraftforge/index.json
// TODO: mirrors: https://raw.githubusercontent.com/headlesshq/meta-launcher/refs/heads/master/net.minecraftforge/index.json

// TODO: mirrors: https://raw.githubusercontent.com/PrismLauncher/meta-launcher/refs/heads/master/net.neoforged/index.json
// TODO: mirrors: https://raw.githubusercontent.com/headlesshq/meta-launcher/refs/heads/master/net.neoforged/index.json

/**
 * We use the prism launcher meta for indexing forge installers.
 * <p>(TODO: it would be nice to move away from a dependency on a third party launcher)</p>
 *
 * @param uid the group id of the platform, e.g. {@code "net.neoforged"} or {@code "net.minecraftforge"}
 * @param versions the version entries for the platform.
 *
 * @see <a href=https://github.com/PrismLauncher/meta-launcher>https://github.com/PrismLauncher/meta-launcher</a>
 * @see <a href=https://meta.prismlauncher.org/v1/net.minecraftforge/index.json>
 * https://meta.prismlauncher.org/v1/net.minecraftforge/index.json
 * </a>
 * @see <a href=https://meta.prismlauncher.org/v1/net.neoforged/index.json>
 * https://meta.prismlauncher.org/v1/net.neoforged/index.json
 * </a>
 */
@RegisterForReflection
public record PrismIndex(String uid, List<Meta> versions) implements ReflectionRegistered {
    /**
     * </a>
     * <pre>
     * {@code
     * {
     *  "recommended": false,
     *  "releaseTime": "2026-06-09T18:25:56+00:00",
     *  "requires": [
     *      {
     *          "equals": "26.1.2",
     *          "uid": "net.minecraft"
     *      }
     *  ],
     *  "sha256": "88a14766f747...",
     *  "version": "64.0.9"
     * }
     * }
     * </pre>
     *
     * @param requires usually only contains the mc version, see example.
     * @param sha256   the hash (TODO: OF WHAT?)
     * @param version  the name of the forge build.
     */
    @RegisterForReflection
    public record Meta(List<Requires> requires, String sha256, String version)
        implements Comparable<Meta>, ReflectionRegistered {
        /**
         * @return the name of the Mc version for this forge version.
         */
        public String getMcVersion() throws NoSuchElementException {
            return requires()
                .stream()
                .filter(requires -> "net.minecraft".equals(requires.uid()))
                .findFirst()
                .map(Requires::equals)
                .orElseThrow();
        }

        @Override
        public int compareTo(Meta other) {
            if (this.version.equals(other.version())) {
                return 0;
            }

            int betaCompare = Boolean.compare(this.version.contains("beta"), other.version.contains("beta"));
            if (betaCompare != 0) {
                return betaCompare;
            }

            String[] version1 = this.version.split("[-.]");
            String[] version2 = other.version().split("[-.]");
            for (int i = 0; i < version1.length && i < version2.length; i++) {
                int compare;
                try {
                    compare = Integer.compare(Integer.parseInt(version1[i]), Integer.parseInt(version2[i]));
                } catch (NumberFormatException e) {
                    compare = String.CASE_INSENSITIVE_ORDER.compare(version1[i], version2[i]);
                }

                if (compare != 0) {
                    return compare;
                }
            }

            return Integer.compare(version2.length, version1.length);
        }

        /**
         * For Forge this only contains the Mc version.
         *
         * @param equals name of the mc version
         * @param uid    net.minecraft
         */
        @RegisterForReflection
        public record Requires(String equals, String uid) implements ReflectionRegistered {

        }
    }

}
