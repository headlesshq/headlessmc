package io.github.headlesshq.headlessmc.patcher.asm;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@RequiredArgsConstructor
final class AggregateSuperClassResolver implements SuperClassResolver {
    private final Map<SuperClassStrategy, SuperClassResolver> openResolvers = new ConcurrentHashMap<>();
    private final List<SuperClassStrategy> strategies;
    private final PatchContext context;

    @Override
    public String getCommonSuperClass(String type1, String type2) throws HeadlessMcException {
        return find(type1, type2, openResolvers.values().stream())
            .or(() -> find(
                type1,
                type2,
                strategies.stream()
                    .filter(strategy -> !openResolvers.containsKey(strategy))
                    .flatMap(strategy -> {
                        try {
                            SuperClassResolver resolver = strategy.apply(context);
                            openResolvers.put(strategy, resolver);
                            return Stream.of(resolver);
                        } catch (HeadlessMcException e) {
                            return Stream.empty();
                        }
                    })
            )).orElseThrow(() -> new PatchException("Failed to find common super class of " + type1 + ", " + type2));
    }

    @Override
    public void close() throws HeadlessMcException {
        HeadlessMcIOException exception = new HeadlessMcIOException("Failed to close");
        boolean failed = false;
        for (SuperClassResolver openResolver : openResolvers.values()) {
            try {
                openResolver.close();
            } catch (HeadlessMcException e) {
                exception.addSuppressed(e);
                failed = true;
            }
        }

        if (failed) {
            throw exception;
        }
    }

    private Optional<String> find(String type1, String type2, Stream<SuperClassResolver> stream) {
        return stream.flatMap(resolver -> {
            try {
                return Stream.of(resolver.getCommonSuperClass(type1, type2));
            } catch (HeadlessMcException e) {
                return Stream.empty();
            }
        }).findFirst();
    }

}
