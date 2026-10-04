package io.github.headlesshq.headlessmc.platform.util;

import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import jakarta.enterprise.util.TypeLiteral;

import java.net.URI;
import java.util.List;

public interface PlatformCacheService {
    <J extends ReflectionRegistered> Cache<J> versionCache(TypeLiteral<J> type, String platformName, URI url);

    <J extends ReflectionRegistered> Cache<J> versionCache(TypeLiteral<J> type, String platformName, URI url, long version);

    <J extends ReflectionRegistered> Cache<List<J>> versionCacheList(TypeLiteral<List<J>> type, String platformName, URI url, long version);

}
