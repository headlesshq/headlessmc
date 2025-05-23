package io.github.headlesshq.headlessmc.java.download;

import lombok.Data;
import io.github.headlesshq.headlessmc.api.command.ProgressBarProvider;
import io.github.headlesshq.headlessmc.os.OS;
import org.jetbrains.annotations.Nullable;

@Data
public class JavaDownloadRequest {
    private final DownloadClient client;
    private final ProgressBarProvider progressBarProvider;
    private final int version;
    private final @Nullable String distribution;
    private final OS os;
    private final boolean jdk;

    public String getProgressBarTitle() {
        return "Downloading Java " + version;
    }

}
