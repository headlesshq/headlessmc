package io.github.headlesshq.headlessmc.net;

import io.github.headlesshq.headlessmc.exceptions.*;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import jakarta.enterprise.util.TypeLiteral;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;

/**
 * A {@code DownloadBuilder} allows you to build a download.
 * The default implementation is a simple HTTP GET request.
 * DownloadBuilder offers methods for verification of
 * hash/size, displaying progressbars for large downloads,
 * retries and deserialization to JSON and/or files.
 *
 * @see DownloadService
 * @see DownloadContext
 */
public interface DownloadBuilder {
    /**
     * Configures the {@link StatusCodeHandler} which reacts to
     * the HTTP status code of the download.
     *
     * @param handler the handler, if {@code null}, {@link StatusCodeHandler#error()} will be used.
     * @return this builder.
     */
    DownloadBuilder statusCodeHandler(@Nullable StatusCodeHandler handler);

    /**
     * Displays a ProgressBar (for larger downloads).
     * Recommended to be used with {@link #size(Long)} for estimation.
     * If title is {@code null} no ProgressBar will be displayed.
     *
     * @param title the title to display for the progress bar.
     * @return this builder.
     */
    DownloadBuilder progressBar(@Nullable String title);

    /**
     * The expected size of the download in bytes.
     * If the download is less or larger than the expected size,
     * a {@link VerificationException} will be thrown.
     * The size will also be used for the {@link #progressBar(String)}.
     *
     * @param size the expected size of the download in bytes or {@code null} if unspecified.
     * @return this builder.
     */
    DownloadBuilder size(@Nullable Long size);

    /**
     * If {@code algorithmName} is specified, the download checksum will be verified.
     * If the given hash algorithm does not produce the same {@code hash} as the given
     * one, a {@link VerificationException} will be thrown during the download.
     * If no algorithm for the given {@code algorithmName} can be found,
     * a {@link HeadlessMcNoSuchAlgorithmException} will be thrown before
     * the download starts. If multiple hash algorithms have been specified
     * the exception will only be thrown if none of the {@code algorithmNames}
     * could be resolved.
     *
     * @param algorithmName the name of the hash algorithm to use to compute the checksum.
     * @param hash the checksum hex string.
     * @return this builder.
     */
    DownloadBuilder hash(@Nullable String algorithmName, @Nullable String hash);

    /**
     * The download checksum will be verified:
     * If the given hash algorithm does not produce the same {@code hash} as the given
     * one, a {@link VerificationException} will be thrown during the download.
     * Multiple hash algorithms can be specified.
     *
     * @param algorithm the hash algorithm to compute the checksum with.
     * @param hash the checksum hex string.
     * @return this builder.
     */
    DownloadBuilder hash(MessageDigest algorithm, @Nullable String hash);

    /**
     * Calls {@link #hash(String, String)} for the given algorithms
     *
     * @param hashes the hashes to verify.
     * @return this builder.
     */
    DownloadBuilder hashes(Map<String, @Nullable String> hashes);

    /**
     * Does a verification ({@link #hash(MessageDigest, String)}) with
     * the {@code SHA-1} hash algorithm.
     *
     * @param sha1 the checksum hex string.
     * @return this builder.
     * @see #hash(MessageDigest, String)
     */
    DownloadBuilder sha1(@Nullable String sha1);

    /**
     * Does a verification ({@link #hash(MessageDigest, String)}) with
     * the {@code SHA-256} hash algorithm.
     *
     * @param sha256 the checksum hex string.
     * @return this builder.
     * @see #hash(MessageDigest, String)
     */
    DownloadBuilder sha256(@Nullable String sha256);

    /**
     * Configures the download to be retried if it fails.
     *
     * @param retry
     * @return
     */
    DownloadBuilder retry(DownloadRetry retry);

    DownloadBuilder retry(int retries, int initialIntervalMs, double factor, double jitter);

    // we could also support caching? withCache(), build toCache()

    void start(ExConsumer<Download> action) throws HeadlessMcException;

    <T> T map(ExFunction<Download, T> action) throws HeadlessMcException;

    <T extends ReflectionRegistered> T json(Class<T> type) throws HeadlessMcException;

    <T extends ReflectionRegistered> T json(TypeLiteral<T> typeLiteral) throws HeadlessMcException;

    <T> T jsonRegisteredForReflection(TypeLiteral<T> typeLiteral) throws HeadlessMcException;

    Path toFile(Path file) throws HeadlessMcException;

}
