package io.github.headlesshq.headlessmc.launcher.download;

import io.github.headlesshq.headlessmc.launcher.util.IOConsumer;
import org.jetbrains.annotations.Nullable;

public class MockDownloadService extends DownloadService {
    @Override
    public void download(URL from, @Nullable Long size, @Nullable String hash, IOConsumer<byte[]> action) {
        // NOP
    }

}
