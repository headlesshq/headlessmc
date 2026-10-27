package io.github.headlesshq.headlessmc.distribution;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface JavaDistributionProvider {
    String DEFAULT = "foojay.io";

    String getName();

    List<JavaDistribution> getDistributions() throws HeadlessMcException;

    JavaDistribution getDistributionByName(String name) throws HeadlessMcException;

    List<JavaRuntime> getRuntimes(JavaDistribution distribution, int version, OS os, CPU cpu) throws HeadlessMcException;

    /**
     * Finds the latest stable runtime with the same distribution, major version and package type as the given one.
     *
     * @param runtime the installed runtime.
     * @param os      the operating system to find runtimes for.
     * @param cpu     the cpu to find runtimes for.
     * @return the latest stable runtime, or empty if the given runtime is the latest one.
     * @throws HeadlessMcException if the runtimes could not be retrieved.
     */
    Optional<JavaRuntime> getUpdate(JavaRuntime runtime, OS os, CPU cpu) throws HeadlessMcException;

    void download(JavaRuntime runtime, Path path) throws HeadlessMcException;


}
