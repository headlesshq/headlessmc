package io.github.headlesshq.headlessmc.net;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Set;

// TODO: use this instead of doing manual injection in tests
public class MockDownloadTestProfile implements QuarkusTestProfile {
    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(MockDownloadService.class);
    }

}
