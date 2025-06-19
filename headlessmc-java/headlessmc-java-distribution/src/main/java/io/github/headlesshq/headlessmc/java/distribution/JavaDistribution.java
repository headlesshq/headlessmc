package io.github.headlesshq.headlessmc.java.distribution;

import java.io.IOException;
import java.nio.file.Path;

public interface JavaDistribution {
    Path download(Path javaVersionsDir, JavaDownloadRequest request) throws IOException;

    String getDistribution();

}
