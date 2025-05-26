package io.github.headlesshq.headlessmc.api.settings;

import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class SettingGroupInitializer {
    private final SettingGroup root;

}
