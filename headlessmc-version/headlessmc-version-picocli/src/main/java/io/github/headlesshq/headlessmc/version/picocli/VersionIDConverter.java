package io.github.headlesshq.headlessmc.version.picocli;

import io.github.headlesshq.headlessmc.version.id.VersionID;
import io.github.headlesshq.headlessmc.version.id.VersionIDParseException;
import io.github.headlesshq.headlessmc.version.id.VersionIDParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class VersionIDConverter implements CommandLine.ITypeConverter<VersionID> {
    private final VersionIDParser parser;

    @Override
    public VersionID convert(String value) throws VersionIDParseException {
        return parser.parse(value);
    }

}
