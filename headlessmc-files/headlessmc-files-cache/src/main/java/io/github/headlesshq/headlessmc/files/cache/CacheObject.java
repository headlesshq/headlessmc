package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record CacheObject<V>(long version, V object) implements ReflectionRegistered {

}
