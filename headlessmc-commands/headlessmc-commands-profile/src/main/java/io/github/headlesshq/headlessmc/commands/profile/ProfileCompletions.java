package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;

@Default
@Dependent
@RegisterForReflection
@Named("headlessmc:picocli:completions:profile") // prevent Bean from getting removed by Quarkus during build
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProfileCompletions implements Iterable<String> {
    private final ProfileService profileService;

    @Override
    public Iterator<String> iterator() {
        return profileService.getProfiles().stream()
            .map(Profile::name)
            .iterator();
    }

}
