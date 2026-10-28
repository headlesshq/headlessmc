package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.platform.VanillaVersionService;
import io.github.headlesshq.headlessmc.platform.VersionService;
import io.github.headlesshq.headlessmc.platform.client.PatternVersionMatcher;
import io.github.headlesshq.headlessmc.platform.client.VersionPattern;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Forge changed the names of its versions over time:
 * <ul>
 *     <li>{@code Forge8.9.0.753}, the first installers only named the build,</li>
 *     <li>{@code 1.5.2-Forge7.8.0.684}, {@code 1.7.10-Forge10.13.2.1300-1.7.10}
 *     or {@code 1.7.10-pre4-Forge10.12.2.1137-prerelease},</li>
 *     <li>{@code 1.10.2-forge1.10.2-12.18.0.2008} or {@code 1.11-forge1.11-13.19.0.2126-1.11.x},</li>
 *     <li>{@code 1.13.2-forge-25.0.10}, since 1.13,</li>
 *     <li>{@code 1.20.3-forge49.0.1}, a few versions for 1.20.3.</li>
 * </ul>
 * The optional suffix after the build is the branch the build was made from, it is not part of the build name.
 * Mc versions in Forge ids never contain whitespace.
 */
public class ForgeVersionMatcher extends PatternVersionMatcher {
    private static final String BUILD = "(?<build>\\d+(?:\\.\\d+)+)";
    private static final String BRANCH = "(?:-(?<branch>[\\w.]+))?";

    static final Pattern MODERN = Pattern.compile("(?<mc>\\S+)-forge-" + BUILD);
    static final Pattern REPEATED_MC = Pattern.compile("(?<mc>\\S+)-forge\\k<mc>-" + BUILD + BRANCH);
    static final Pattern LEGACY = Pattern.compile("(?<mc>\\S+?)-[Ff]orge" + BUILD + BRANCH);
    static final Pattern BUILD_ONLY = Pattern.compile("Forge" + BUILD);

    private final List<VersionPattern> patterns = List.of(
        new VersionPattern(MODERN, (platformService, version, match) ->
            resolve(platformService, version, match.group("mc"), match.group("build"))),
        new VersionPattern(REPEATED_MC, (platformService, version, match) ->
            resolve(platformService, version, match.group("mc"), match.group("build"))),
        new VersionPattern(LEGACY, (platformService, version, match) ->
            resolve(platformService, version, match.group("mc"), match.group("build"))),
        new VersionPattern(BUILD_ONLY, (platformService, version, match) ->
            resolveBuild(platformService, version, match.group("build")))
    );

    public ForgeVersionMatcher(VanillaVersionService vanillaVersionService, VersionService versionService) {
        super(vanillaVersionService, versionService);
    }

    @Override
    protected List<VersionPattern> getPatterns() {
        return patterns;
    }

}
