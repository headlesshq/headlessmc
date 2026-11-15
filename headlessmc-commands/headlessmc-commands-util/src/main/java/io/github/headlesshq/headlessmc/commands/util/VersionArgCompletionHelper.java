package io.github.headlesshq.headlessmc.commands.util;

import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.launcher.profile.VersionArgCandidates;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.version.arg.Side;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class VersionArgCompletionHelper {
    private final PlatformService platformService;

    /**
     * @param values the arguments typed so far, the last one is being completed.
     * @return candidates for the last argument, see {@link VersionArgCandidates}.
     */
    public List<Completions.Candidate> complete(@Nullable List<String> values) {
        return new VersionArgCandidates(platformService).complete(values).stream()
            .map(candidate -> new Completions.Candidate(candidate.value(), candidate.description()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

}
