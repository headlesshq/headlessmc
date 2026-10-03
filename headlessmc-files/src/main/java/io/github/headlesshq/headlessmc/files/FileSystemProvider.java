package io.github.headlesshq.headlessmc.files;

import java.nio.file.FileSystem;

public interface FileSystemProvider {
    FileSystem getFileSystem();

    boolean isVirtual();

}
