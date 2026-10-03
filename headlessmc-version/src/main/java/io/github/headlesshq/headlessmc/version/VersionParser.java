package io.github.headlesshq.headlessmc.version;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Interface for parsers that can parse a Minecraft version.json file into a {@link Version}.
 */
public interface VersionParser {
    /**
     * Parses a {@link Version} from the given {@link Reader}.
     *
     * @param reader the reader to parse.
     * @return a Version parsed from the given Reader.
     * @throws HeadlessMcException if the Reader throws, or if a parsing error occurs.
     */
    Version parse(Reader reader) throws HeadlessMcException;

    /**
     * Convenience method to parse a {@link Version} from a {@link Path}.
     *
     * @param jsonFile the Minecraft version.json file to parse.
     * @return the parsed Version.
     * @throws HeadlessMcException if the file does not exist, cannot be read, or parsed.
     */
    default Version parse(Path jsonFile) throws HeadlessMcException {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(jsonFile))) {
            return parse(reader);
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to read version.json file " + jsonFile, e);
        }
    }

}
