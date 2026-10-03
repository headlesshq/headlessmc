package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.console.Completions;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Default
@Dependent
@RegisterForReflection
@Named("headlessmc:picocli:completions:profile:fields:values") // prevent Bean from getting removed by Quarkus during build
@NoArgsConstructor(onConstructor_ = {@Inject})
public class ProfileFieldValueCompletions implements Completions {
    // TODO: this!

    @Override
    public Iterable<Candidate> candidates(@Nullable Object spec, Line line) {
        if (spec instanceof EditCommand command) {
            String profile = command.getName();
            String field = command.getField();
            if (profile != null && field != null) {

            }

            return List.of();
        }

        log.error("Spec {} was not a Profile EditCommand", spec);
        return List.of();
    }

    @Override
    public Iterator<String> iterator() {
        return Collections.emptyIterator();
    }

}
