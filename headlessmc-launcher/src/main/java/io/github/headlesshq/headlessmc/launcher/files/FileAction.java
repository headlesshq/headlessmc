package io.github.headlesshq.headlessmc.launcher.files;

import lombok.Builder;
import lombok.Singular;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.function.BiConsumer;

@Builder(setterPrefix = "with")
public class FileAction {
    @Singular
    private final List<FileAttribute<?>> attributes;
    @Singular
    private final List<FileAttribute<?>> parentAttributes;
    private final BiConsumer<Path, List<FileAttribute<?>>> action;

    // TODO
    public static class FileActionBuilder {
        public FileActionBuilder mkdirs() {
            return withAction((path, attributes) -> {
                try {
                    Files.createDirectories(path, attributes.toArray(new FileAttribute[0]));
                } catch (IOException e) {
                    throw new IOError(e);
                }
            });
        }

        public FileActionBuilder create() {
            return withAction((path, attributes) -> {
                try {
                    Files.createDirectories(path.getParent(), attributes.toArray(new FileAttribute[0]));
                } catch (IOException e) {
                    throw new IOError(e);
                }
            });
        }

        public FileActionBuilder delete() {
            return withAction((path, attributes) -> {
               // TODO: delete
            });
        }

        public FileActionBuilder attributes(FileAttribute<?>... attributes) {
            for (FileAttribute<?> attribute : attributes) {
                withAttribute(attribute);
            }

            return this;
        }

        public FileActionBuilder parentAttributes(FileAttribute<?>... attributes) {
            for (FileAttribute<?> attribute : attributes) {
                withParentAttribute(attribute);
            }

            return this;
        }
    }

}
