package io.github.headlesshq.headlessmc.commands.util;

import io.github.headlesshq.headlessmc.console.Completions;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

// TODO: service that, when shell is started, provides completable futures
//  with different cache tiers, so user does not get a freeze on completions?
// TODO: Orrrrrrr make Completions a CompletableFuture?
//  and call completion widget later?
@Slf4j
@Default
@Dependent
@RegisterForReflection
@Named("headlessmc:picocli:completions:versions") // prevent Bean from getting removed by Quarkus during build
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class VersionCompletions implements Completions {
    private final VersionArgCompletionHelper completionHelper;

    @Override
    public Iterator<String> iterator() {
        // TODO: get all versions ever for AutoComplete
        return Collections.emptyIterator();
    }

    @Override
    public Iterable<Candidate> candidates(@Nullable Object spec, Line line) {
        if (spec instanceof VersionArgCommand command) {
            List<String> args = command.getVersionArg();
            return completionHelper.complete(args);
        }

        log.error("Spec for versions completions {} was not a VersionArgCommand", spec);
        return List.of();
    }

}
