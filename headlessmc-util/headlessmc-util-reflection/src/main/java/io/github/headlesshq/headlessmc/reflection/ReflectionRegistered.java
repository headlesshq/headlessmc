package io.github.headlesshq.headlessmc.reflection;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Marker interface for objects that can be parsed via a JsonParser,
 * using reflection. If we want to support native images
 * we need register every class that can be accessed via reflection.
 * While it's difficult to enforce that every
 * object implementing this interface is annotated with
 * {@link RegisterForReflection}, it still serves as a reminder,
 * and restricts the use of
 * {@code io.github.headlesshq.headlessmc.util.json.JsonService},
 * to ensure users have made use of the annotation.
 * TODO: eventually a test or a rule to check every implementation
 */
@RegisterForReflection
public interface ReflectionRegistered {

}
