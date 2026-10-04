package io.github.headlesshq.headlessmc.launcher.assets;

import io.github.headlesshq.headlessmc.net.HttpVersion;
import io.github.headlesshq.headlessmc.net.NetConfig;
import io.github.headlesshq.headlessmc.net.parallel.ParallelConfig;

import java.net.URI;
import java.util.Optional;

/**
 * Wraps an {@link AssetsConfig} and, if no HTTP version was configured for
 * asset downloads, forces HTTP/1.1.
 *
 * <p>AtLauncher proposes using HTTP/1.1 to fix asset downloading rate
 * limits/timeouts:
 * <a href="https://github.com/ATLauncher/ATLauncher/issues/771#issuecomment-1732489087">771</a>
 * (the problem still persists:
 * <a href="https://github.com/ATLauncher/ATLauncher/issues/976">976</a>).
 */
final class Http1AssetsConfig implements AssetsConfig {
    private final AssetsConfig delegate;

    Http1AssetsConfig(AssetsConfig delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean dummy() {
        return delegate.dummy();
    }

    @Override
    public URI url() {
        return delegate.url();
    }

    @Override
    public NetConfig net() {
        NetConfig net = delegate.net();
        return net.httpVersion().isPresent() ? net : new Http1NetConfig(net);
    }

    @Override
    public ParallelConfig parallel() {
        return delegate.parallel();
    }

    private record Http1NetConfig(NetConfig delegate) implements NetConfig {
        @Override
        public Optional<HttpVersion> httpVersion() {
            return Optional.of(HttpVersion.HTTP_1_1);
        }

        @Override
        public boolean cookies() {
            return delegate.cookies();
        }

        @Override
        public String userAgent() {
            return delegate.userAgent();
        }

        @Override
        public boolean deleteFailedFiles() {
            return delegate.deleteFailedFiles();
        }

        @Override
        public int retries() {
            return delegate.retries();
        }
    }

}
