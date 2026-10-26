package io.github.headlesshq.headlessmc.distribution;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;

import java.nio.file.Path;
import java.util.List;

public interface JavaDistributionProvider {
    String DEFAULT = "foojay.io";

    String getName();

    List<JavaDistribution> getDistributions() throws HeadlessMcException;

    JavaDistribution getDistributionByName(String name) throws HeadlessMcException;

    List<JavaRuntime> getRuntimes(JavaDistribution distribution, int version, OS os, CPU cpu) throws HeadlessMcException;

    /**
     * Finds runtimes that are newer than the given one, with the same distribution, major version and package type.
     *
     * @param runtime the installed runtime, needs a {@link JavaRuntime#javaVersion()} to be compared against.
     * @param os      the operating system to find runtimes for.
     * @param cpu     the cpu to find runtimes for.
     * @return the newer runtimes, the newest first, or an empty list if the runtime is up-to-date.
     * @throws HeadlessMcException if the runtimes could not be retrieved.
     */
    List<JavaRuntime> getUpdates(JavaRuntime runtime, OS os, CPU cpu) throws HeadlessMcException;

    void download(JavaRuntime runtime, Path path) throws HeadlessMcException;


}
