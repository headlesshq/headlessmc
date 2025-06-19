package io.github.headlesshq.headlessmc.java.distribution;

import io.github.headlesshq.headlessmc.os.OS;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public class JavaDownloadRequest {
    private final int version;
    private final @Nullable String distribution;
    private final OS os;
    private final boolean jdk;

    public String getProgressBarTitle() {
        return "Downloading Java " + version;
    }

}
