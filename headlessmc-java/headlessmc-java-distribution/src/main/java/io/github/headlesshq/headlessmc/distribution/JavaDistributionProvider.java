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

    List<JavaRuntime> getUpdates(JavaRuntime runtime) throws HeadlessMcException;

    void download(JavaRuntime runtime, Path path) throws HeadlessMcException;


}
