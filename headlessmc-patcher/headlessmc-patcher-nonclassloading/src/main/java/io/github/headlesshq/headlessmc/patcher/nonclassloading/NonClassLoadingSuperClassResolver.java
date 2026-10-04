package io.github.headlesshq.headlessmc.patcher.nonclassloading;

import io.github.headlesshq.headlessmc.exceptions.ExceptionUtil;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import io.github.headlesshq.headlessmc.patcher.asm.SuperClassResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
final class NonClassLoadingSuperClassResolver implements SuperClassResolver {
    private final TypeHierarchyReader typeHierarchy = new TypeHierarchyReader(this::findType);
    private final Map<ClassSource.Provider, ClassSource> openedSources = new HashMap<>();
    private final List<ClassSource.Provider> sources;

    @Override
    public String getCommonSuperClass(String type1, String type2) {
        Type t1 = Type.getObjectType(type1);
        Type t2 = Type.getObjectType(type2);

        if (typeHierarchy.isAssignableFrom(t1, t2)) {
            return type1;
        }
        if (typeHierarchy.isAssignableFrom(t2, t1)) {
            return type2;
        }
        if (typeHierarchy.isInterface(t1) || typeHierarchy.isInterface(t2)) {
            return "java/lang/Object";
        } else {
            do {
                t1 = typeHierarchy.getSuperClass(t1);
                if (t1 == null) {
                    return "java/lang/Object";
                }
            } while (!typeHierarchy.isAssignableFrom(t1, t2));
            return t1.getInternalName();
        }
    }

    @Override
    public void close() throws HeadlessMcException {
        HeadlessMcIOException exception = new HeadlessMcIOException("Failed to close NonClassLoadingSuperClassResolver");
        boolean failed = false;
        for (ClassSource openedSource : openedSources.values()) {
            try {
                openedSource.close();
            } catch (IOException e) {
                exception.addSuppressed(e);
                failed = true;
            }
        }

        if (failed) {
            throw exception;
        }
    }

    private ClassReader findType(Type type) {
        return findType(type.getInternalName(), openedSources.values().stream())
            .or(() -> findType( // fallback to opened jars
                type.getInternalName(),
                sources.stream()
                    .filter(provider -> !openedSources.containsKey(provider))
                    .flatMap(provider -> {
                        ClassSource source = provider.open();
                        openedSources.put(provider, source);
                        return Optional.of(source).stream();
                    })
            )).or(() -> {
                try {
                    // this is not that great...
                    log.error("Trying to load class from Runtime {}", type.getInternalName());
                    return Optional.of(new ClassReader(type.getInternalName()));
                } catch (/* okay to catch marker */Throwable t) {
                    //noinspection ThrowableNotThrown
                    ExceptionUtil.handleInterruptions(t);
                    // TODO: confirm this is not an issue in native images?!
                    log.info("Failed to read type {}", type.getInternalName(), t);
                    return Optional.empty();
                }
            }).orElseThrow(() -> new PatchException("Failed to find class " + type));
    }

    private Optional<ClassReader> findType(String type, Stream<ClassSource> sources) {
        return sources.flatMap(source -> source.getClass(type).stream()).findFirst();
    }

}
