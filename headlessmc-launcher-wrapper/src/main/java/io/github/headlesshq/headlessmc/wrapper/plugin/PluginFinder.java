package io.github.headlesshq.headlessmc.wrapper.plugin;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class PluginFinder {
    public List<URL> find(Path pluginsDirectory) throws IOException {
        List<URL> plugins = new ArrayList<>();
        if (Files.exists(pluginsDirectory)) {
            if (!Files.isDirectory(pluginsDirectory)) {
                throw new IOException(pluginsDirectory + " is not a directory!");
            }

            try (Stream<Path> stream = Files.list(pluginsDirectory)) {
                Iterator<Path> itr = stream.iterator();
                while (itr.hasNext()) {
                    Path path = itr.next();
                    if (path.toString().endsWith(".jar")) {
                        plugins.add(path.toUri().toURL());
                    }
                }
            }
        }

        return plugins;
    }

}
