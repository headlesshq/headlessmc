package io.github.headlesshq.headlessmc.config;

import java.util.Optional;
import java.util.stream.Stream;

public interface ConfigDescriptionService {
    Optional<String> getDescription(String name);

    Stream<ConfigDescriptionSource> sources();

}
