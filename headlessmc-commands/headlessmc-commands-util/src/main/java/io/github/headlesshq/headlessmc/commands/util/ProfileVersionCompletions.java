package io.github.headlesshq.headlessmc.commands.util;

import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

// TODO: also completions for version.jsons
// TODO: also different modes, if tab is pressed show more, if not, just show few completions
//  like only start showing completions for vanilla versions if we have typed 1.8, or 1.10
@Slf4j
@Default
@Dependent
@RegisterForReflection
@Named("headlessmc:picocli:completions:profile-versions") // prevent Bean from getting removed by Quarkus during build
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProfileVersionCompletions implements Completions {
    private final VersionArgCompletionHelper completionHelper;
    private final ProfileService profileService;

    @Override
    public Iterable<Candidate> candidates(@Nullable Object spec, Line line) {
        List<Candidate> result = new ArrayList<>();
        profileService.getProfiles().stream()
            .map(Profile::name)
            .map(name -> new Candidate(name, null))
            .forEach(result::add);

        if (spec instanceof VersionArgCommand command) {
            List<String> args = command.getVersionArg();
            result.addAll(completionHelper.complete(args));
        } else {
            log.error("Spec for profile/versions completions {} was not a VersionArgCommand", spec);
        }

        return result;
    }

    @Override
    public Iterator<String> iterator() {
        // TODO: get all versions ever for AutoComplete
        return Collections.emptyIterator();
    }

}
