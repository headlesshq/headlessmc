package io.github.headlesshq.headlessmc.files;

import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

@ApplicationScoped
public class DefaultFileSystemProvider implements FileSystemProvider {
    @Override
    public FileSystem getFileSystem() {
        return FileSystems.getDefault();
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

}
