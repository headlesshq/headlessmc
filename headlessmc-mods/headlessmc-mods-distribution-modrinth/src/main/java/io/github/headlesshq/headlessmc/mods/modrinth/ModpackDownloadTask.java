package io.github.headlesshq.headlessmc.mods.modrinth;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.exceptions.VerificationException;
import io.github.headlesshq.headlessmc.net.DownloadContext;
import io.github.headlesshq.headlessmc.net.DownloadRetry;
import io.github.headlesshq.headlessmc.net.parallel.ParallelTask;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.net.URI;
import java.nio.file.Path;

/**
 * Downloads a single {@link Mrpack.File} of a modpack, trying each of its mirrors in order.
 */
@ToString(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
final class ModpackDownloadTask implements ParallelTask {
    private final DownloadContext context;
    @ToString.Include
    private final Mrpack.File file;
    @Getter
    private final Path target;

    @Override
    public void run() throws HeadlessMcException {
        HeadlessMcIOException downloadFailed = new HeadlessMcIOException("Failed to download file " + file);
        boolean onlyVerificationFailures = true;
        for (URI url : file.downloads()) {
            try {
                context.download(url)
                    .retry(DownloadRetry.none()) // handled by ParallelTaskService
                    .hashes(file.hashes())
                    .size(file.fileSize())
                    .toFile(target);
                return;
            } catch (HeadlessMcException e) {
                onlyVerificationFailures &= e instanceof VerificationException;
                downloadFailed.addSuppressed(e);
            }
        }

        if (onlyVerificationFailures) {
            // every mirror served a file with the wrong hash or size, retrying will not help
            VerificationException verificationFailed = new VerificationException("Failed to download file " + file);
            for (Throwable suppressed : downloadFailed.getSuppressed()) {
                verificationFailed.addSuppressed(suppressed);
            }

            throw verificationFailed;
        }

        throw downloadFailed;
    }

    String path() {
        return file.path();
    }

    @Override
    public long getSize() {
        return file.fileSize();
    }

}
