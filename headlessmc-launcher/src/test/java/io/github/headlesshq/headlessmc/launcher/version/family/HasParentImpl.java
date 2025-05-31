package io.github.headlesshq.headlessmc.launcher.version.family;

import io.github.headlesshq.headlessmc.api.traits.HasName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HasParentImpl implements HasParent<HasParentImpl>, HasName {
    private final String name = "Only because required by the FamilyCleaner.";
    private HasParentImpl parent;

}
