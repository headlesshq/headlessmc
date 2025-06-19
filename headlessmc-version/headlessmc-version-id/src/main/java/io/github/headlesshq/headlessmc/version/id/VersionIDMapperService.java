package io.github.headlesshq.headlessmc.version.id;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class VersionIDMapperService {
    @Getter
    private final List<VersionIDMapper> parsers;

    @Inject
    public VersionIDMapperService(@Any Instance<VersionIDMapper> parsers) {
        this.parsers = parsers.stream().collect(Collectors.toList());
    }

    public VersionID parse(String name) {
        List<VersionIDMapper> matches = parsers.stream()
                .filter(parser -> parser.matches(name))
                .collect(Collectors.toList());

        List<VersionIDMapper> conflicts = new ArrayList<>(matches);
        if (matches.size() > 1) {
            matches.forEach(match -> match.resolveConflicts(name, conflicts));
        }

        if (conflicts.isEmpty()) {
            throw new VersionIDParseException("No parsers found for name " + name + ", available parsers: "
                    + parsers.stream().map(VersionIDMapper::getName).collect(Collectors.joining(", ")));
        } else if (conflicts.size() == 1) {
            return conflicts.get(0).map(name);
        } else {
            throw new VersionIDParseException("Multiple parser found for version " + name
                + " : " + conflicts.stream().map(VersionIDMapper::getName).collect(Collectors.joining(", "))
                + " matches: " + matches.stream().map(VersionIDMapper::getName).collect(Collectors.joining(", ")));
        }
    }

}
