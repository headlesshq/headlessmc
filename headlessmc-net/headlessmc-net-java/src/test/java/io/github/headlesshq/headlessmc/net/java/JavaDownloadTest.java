package io.github.headlesshq.headlessmc.net.java;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.headlesshq.headlessmc.exceptions.FileException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.RequestException;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.net.DownloadBuilder;
import io.github.headlesshq.headlessmc.net.DownloadContext;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.net.HttpVersion;
import io.github.headlesshq.headlessmc.net.NetConfig;
import io.github.headlesshq.headlessmc.net.hash.DefaultHashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.github.headlesshq.headlessmc.net.hash.HashServiceImpl;
import io.github.headlesshq.headlessmc.progressbar.ProgressBar;
import io.github.headlesshq.headlessmc.progressbar.ProgressBarServiceManager;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the {@link JavaDownloadService} against a local {@link HttpServer}.
 */
@Timeout(60)
class JavaDownloadTest {
    private static final String BODY = "Hello HeadlessMc!";
    private static final String BODY_SHA1 = sha("SHA-1", BODY);
    private static final String BODY_SHA256 = sha("SHA-256", BODY);

    @TempDir
    Path root;

    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final List<HttpExchange> requests = new CopyOnWriteArrayList<>();
    private final List<RecordingProgressBar> progressBars = new CopyOnWriteArrayList<>();
    private final List<ProgressBar.Configuration> progressBarConfigurations = new CopyOnWriteArrayList<>();
    final JsonService jsonService = new DefaultJacksonJsonService();
    private HttpServer server;
    HashService hashService;

    public record Message(String text, int number) implements ReflectionRegistered {
    }

    record Config(
        Optional<HttpVersion> httpVersion,
        boolean cookies,
        String userAgent,
        boolean deleteFailedFiles,
        int retries
    ) implements NetConfig {
        static Config defaults() {
            return new Config(Optional.empty(), true, "hmc-test-agent", true, 0);
        }

        Config withRetries(int retries) {
            return new Config(httpVersion, cookies, userAgent, deleteFailedFiles, retries);
        }

        Config withDeleteFailedFiles(boolean deleteFailedFiles) {
            return new Config(httpVersion, cookies, userAgent, deleteFailedFiles, retries);
        }

        Config withCookies(boolean cookies) {
            return new Config(httpVersion, cookies, userAgent, deleteFailedFiles, retries);
        }

        Config withHttpVersion(HttpVersion httpVersion) {
            return new Config(Optional.of(httpVersion), cookies, userAgent, deleteFailedFiles, retries);
        }
    }

    private static final class RecordingProgressBar implements ProgressBar {
        private final AtomicLong steps = new AtomicLong();
        private volatile boolean closed;

        @Override
        public boolean isDummy() {
            return false;
        }

        @Override
        public void stepBy(long n) {
            steps.addAndGet(n);
        }

        @Override
        public void stepTo(long n) {
            steps.set(n);
        }

        @Override
        public void step() {
            steps.incrementAndGet();
        }

        @Override
        public void maxHint(long n) {
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    final ProgressBarServiceManager progressBarService = configuration -> {
        progressBarConfigurations.add(configuration);
        RecordingProgressBar progressBar = new RecordingProgressBar();
        progressBars.add(progressBar);
        return progressBar;
    };

    private static String sha(String algorithm, String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length == 0 ? -1 : bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() throws IOException {
        Instance<HashAlgorithmProvider> providers = Mockito.mock(Instance.class);
        Mockito.when(providers.stream()).thenAnswer(unused -> Stream.of(new DefaultHashAlgorithmProvider()));
        hashService = new HashServiceImpl(providers);

        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requests.add(exchange);
            String path = exchange.getRequestURI().getPath();
            int count = requestCounts.computeIfAbsent(path, unused -> new AtomicInteger()).incrementAndGet();
            switch (path) {
                case "/text" -> respond(exchange, 200, BODY);
                case "/json" -> respond(exchange, 200, "{\"text\": \"hi\", \"number\": 3}");
                case "/list" -> respond(exchange, 200, "[\"a\", \"b\"]");
                case "/missing" -> respond(exchange, 404, "not here");
                case "/flaky" -> {
                    if (count < 3) {
                        respond(exchange, 503, "try again");
                    } else {
                        respond(exchange, 200, BODY);
                    }
                }
                case "/cookie" -> {
                    exchange.getResponseHeaders().add("Set-Cookie", "session=abc; Path=/");
                    respond(exchange, 200, "set");
                }
                case "/echo-cookie" -> respond(exchange, 200, String.valueOf(exchange.getRequestHeaders().getFirst("Cookie")));
                default -> respond(exchange, 500, "unknown path " + path);
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private URI uri(String path) {
        return URI.create("http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort() + path);
    }

    private int requestCount(String path) {
        AtomicInteger count = requestCounts.get(path);
        return count == null ? 0 : count.get();
    }

    DownloadService service(Config config) {
        return new JavaDownloadService(progressBarService, jsonService, hashService, () -> config);
    }

    DownloadService service() {
        return service(Config.defaults());
    }

    @Test
    void downloadsTheResponseBody() {
        Optional<Long> size = service().download(uri("/text")).map(download -> {
            assertEquals(200, download.getStatusCode());
            assertEquals(BODY, download.getAsString(StandardCharsets.UTF_8));
            return download.getPotentialSize();
        });

        assertEquals(Optional.of((long) BODY.length()), size);
    }

    @Test
    void startConsumesTheDownload() {
        StringBuilder result = new StringBuilder();

        service().download(uri("/text")).start(download -> result.append(download.getAsString(StandardCharsets.UTF_8)));

        assertEquals(BODY, result.toString());
    }

    @Test
    void userAgentIsSent() {
        service().download(uri("/text")).start(unused -> {});

        assertEquals("hmc-test-agent", requests.get(0).getRequestHeaders().getFirst("User-Agent"));
    }

    @Test
    void parsesJson() {
        Message message = service().download(uri("/json")).json(Message.class);

        assertEquals(new Message("hi", 3), message);
    }

    @Test
    void parsesJsonTypeLiterals() {
        TypeLiteral<List<String>> type = new TypeLiteral<>() {};

        assertEquals(List.of("a", "b"), service().download(uri("/list")).jsonRegisteredForReflection(type));
    }

    @Test
    void writesToFileAndCreatesParentDirectories() throws IOException {
        Path file = root.resolve("nested/dir/file.txt");

        assertEquals(file, service().download(uri("/text")).toFile(file));

        assertEquals(BODY, Files.readString(file));
    }

    @Test
    void writingToADirectoryFails() throws IOException {
        Path dir = Files.createDirectories(root.resolve("dir"));

        assertThrows(FileException.class, () -> service().download(uri("/text")).toFile(dir));
        assertEquals(0, requestCount("/text"));
    }

    @Test
    void errorStatusCodesFailWithTheResponse() {
        RequestException e = assertThrows(
            RequestException.class,
            () -> service().download(uri("/missing")).start(unused -> fail("action must not run"))
        );

        assertTrue(e.getMessage().contains("Status code: 404"), e.getMessage());
        assertTrue(e.getMessage().contains("not here"), e.getMessage());
    }

    @Test
    void statusCodeHandlerCanBeReplaced() {
        int status = service().download(uri("/missing"))
            .statusCodeHandler(null)
            .map(download -> download.getStatusCode());

        assertEquals(404, status);
    }

    @Test
    void matchingHashesAndSizeAreVerified() {
        String body = service().download(uri("/text"))
            .sha1(BODY_SHA1)
            .sha256(BODY_SHA256)
            .size((long) BODY.length())
            .map(download -> download.getAsString(StandardCharsets.UTF_8));

        assertEquals(BODY, body);
    }

    @Test
    void hashesAreVerifiedEvenIfTheStreamIsNotRead() {
        assertThrows(
            HeadlessMcException.class,
            () -> service().download(uri("/text")).sha1("0".repeat(40)).start(unused -> {})
        );
    }

    @Test
    void wrongHashesFail() {
        assertThrows(
            HeadlessMcException.class,
            () -> service().download(uri("/text"))
                .hash("SHA-256", "0".repeat(64))
                .map(download -> download.getAsString(StandardCharsets.UTF_8))
        );
    }

    @Test
    void wrongHashesDoNotLeaveAFileBehind() {
        Path file = root.resolve("file.txt");

        assertThrows(HeadlessMcException.class, () -> service().download(uri("/text")).sha1("0".repeat(40)).toFile(file));

        assertFalse(Files.exists(file), "file with wrong hash should be deleted");
    }

    @Test
    void failedFilesAreKeptIfConfigured() throws IOException {
        Path file = root.resolve("file.txt");
        DownloadService service = service(Config.defaults().withDeleteFailedFiles(false));

        assertThrows(HeadlessMcException.class, () -> service.download(uri("/text")).sha1("0".repeat(40)).toFile(file));

        assertEquals(BODY, Files.readString(file));
    }

    @Test
    void existingFilesAreKeptIfTheRequestFailsBeforeWriting() throws IOException {
        Path file = Files.writeString(root.resolve("file.txt"), "existing");

        assertThrows(RequestException.class, () -> service().download(uri("/missing")).toFile(file));

        assertEquals("existing", Files.readString(file));
    }

    @Test
    void existingFilesAreKeptIfTheyCannotBeOpened() throws IOException {
        Path file = Files.writeString(root.resolve("file.txt"), "existing");
        assumeTrue(file.toFile().setWritable(false) && !Files.isWritable(file), "cannot make file read-only (root?)");
        try {
            assertThrows(RequestException.class, () -> service().download(uri("/text")).toFile(file));

            assertEquals("existing", Files.readString(file));
        } finally {
            // allow the TempDir to clean up
            file.toFile().setWritable(true);
        }
    }

    @Test
    void partialFilesOfFailedRetriesAreDeleted() {
        Path file = root.resolve("file.txt");

        // every attempt writes the file and fails verification
        assertThrows(
            HeadlessMcException.class,
            () -> service().download(uri("/text")).retry(2, 1, 1.0, 0.0).sha1("0".repeat(40)).toFile(file)
        );

        assertFalse(Files.exists(file));
        assertEquals(2, requestCount("/text"));
    }

    @Test
    void wrongSizeFails() {
        assertThrows(
            HeadlessMcException.class,
            () -> service().download(uri("/text"))
                .size(BODY.length() + 1L)
                .map(download -> download.getAsString(StandardCharsets.UTF_8))
        );
    }

    @Test
    void progressBarIsDisplayedForTheDownload() {
        service().download(uri("/text")).progressBar("Downloading text").start(download -> download.getInputStream().readAllBytes());

        assertEquals(1, progressBarConfigurations.size());
        assertEquals("Downloading text", progressBarConfigurations.get(0).taskName());
        assertEquals(BODY.length(), progressBarConfigurations.get(0).initialMax());
        assertEquals(ProgressBar.Configuration.Unit.MB, progressBarConfigurations.get(0).unit());
        RecordingProgressBar progressBar = progressBars.get(0);
        assertEquals(BODY.length(), progressBar.steps.get());
        assertTrue(progressBar.closed);
    }

    @Test
    void noProgressBarIsDisplayedByDefault() {
        service().download(uri("/text")).start(download -> download.getInputStream().readAllBytes());

        assertTrue(progressBarConfigurations.isEmpty());
    }

    @Test
    void failedRequestsAreRetried() {
        String body = service().download(uri("/flaky"))
            .retry(3, 1, 1.0, 0.0)
            .map(download -> download.getAsString(StandardCharsets.UTF_8));

        assertEquals(BODY, body);
        assertEquals(3, requestCount("/flaky"));
    }

    @Test
    void retriesAreConfiguredByTheNetConfig() {
        try (DownloadContext context = service().context(Config.defaults().withRetries(3))) {
            DownloadBuilder builder = context.download(uri("/flaky"));
            assertInstanceOf(Resilience4JRetry.class, ((JavaDownloadBuilder) builder).retry());
        }
    }

    @Test
    void retriesAreExhausted() {
        RequestException e = assertThrows(
            RequestException.class,
            () -> service().download(uri("/flaky")).retry(2, 1, 1.0, 0.0).start(unused -> {})
        );

        assertTrue(e.getMessage().contains("503"), e.getMessage());
        assertEquals(2, requestCount("/flaky"));
    }

    @Test
    void requestsAreNotRetriedByDefault() {
        assertThrows(RequestException.class, () -> service().download(uri("/flaky")).start(unused -> {}));

        assertEquals(1, requestCount("/flaky"));
    }

    @Test
    void connectionFailuresAreRequestExceptions() {
        URI uri = uri("/text");
        server.stop(0);

        assertThrows(RequestException.class, () -> service().download(uri).start(unused -> {}));
    }

    @Test
    void exceptionsOfTheActionAreWrapped() {
        RequestException e = assertThrows(
            RequestException.class,
            () -> service().download(uri("/text")).start(unused -> {
                throw new IOException("action failed");
            })
        );

        assertInstanceOf(IOException.class, e.getCause());
    }

    @Test
    void headlessMcExceptionsOfTheActionArePropagated() {
        FileException exception = new FileException("custom");

        HeadlessMcException e = assertThrows(
            HeadlessMcException.class,
            () -> service().download(uri("/text")).start(unused -> {
                throw exception;
            })
        );

        assertSame(exception, e);
    }

    @Test
    void cookiesAreKeptWithinAContext() {
        try (DownloadContext context = service().context(Config.defaults())) {
            context.download(uri("/cookie")).start(unused -> {});
            String cookie = context.download(uri("/echo-cookie")).map(d -> d.getAsString(StandardCharsets.UTF_8));
            assertEquals("session=abc", cookie);
        }
    }

    @Test
    void cookiesCanBeDisabled() {
        try (DownloadContext context = service().context(Config.defaults().withCookies(false))) {
            context.download(uri("/cookie")).start(unused -> {});
            String cookie = context.download(uri("/echo-cookie")).map(d -> d.getAsString(StandardCharsets.UTF_8));
            assertEquals("null", cookie);
        }
    }

    @Test
    void httpVersionsCanBeConfigured() {
        for (HttpVersion version : List.of(HttpVersion.HTTP_1_1, HttpVersion.HTTP_2)) {
            try (DownloadContext context = service().context(Config.defaults().withHttpVersion(version))) {
                assertEquals(BODY, context.download(uri("/text")).map(d -> d.getAsString(StandardCharsets.UTF_8)));
            }
        }
    }

    @Test
    void contextWithoutConfigUsesDefaults() {
        try (DownloadContext context = new JavaDownloadContext(progressBarService, jsonService, hashService, null)) {
            assertNotNull(context);
        }
    }

    @Test
    void closingAnInterruptedContextFails() {
        DownloadContext context = service().context();
        Thread.currentThread().interrupt();
        try {
            assertThrows(UncheckedInterruptedException.class, context::close);
            assertFalse(Thread.currentThread().isInterrupted());
        } finally {
            // clear the flag in case close did not
            Thread.interrupted();
        }
    }

    @Test
    void progressBarObserverForwardsReads() {
        RecordingProgressBar progressBar = new RecordingProgressBar();
        ProgressBarInputStreamObserver observer = new ProgressBarInputStreamObserver(progressBar);

        observer.byteRead((byte) 1);
        observer.bytesRead(new byte[10], 2, 5);
        observer.eof();
        assertFalse(progressBar.closed);
        observer.close();

        assertEquals(6, progressBar.steps.get());
        assertTrue(progressBar.closed);
    }

}
