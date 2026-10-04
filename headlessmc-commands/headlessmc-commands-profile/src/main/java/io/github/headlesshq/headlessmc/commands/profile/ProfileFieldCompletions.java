package io.github.headlesshq.headlessmc.commands.profile;

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
@Named("headlessmc:picocli:completions:profile:fields") // prevent Bean from getting removed by Quarkus during build
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public final class ProfileFieldCompletions implements Iterable<String> {
    private final EditFieldHelper editFieldHelper;

    @Override
    public Iterator<String> iterator() {
        return editFieldHelper.fields(null).keySet().iterator();
    }

}
