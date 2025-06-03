package io.github.headlesshq.headlessmc.platform;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ModFileReader {
    List<Mod> read(Path file) throws IOException;

}
