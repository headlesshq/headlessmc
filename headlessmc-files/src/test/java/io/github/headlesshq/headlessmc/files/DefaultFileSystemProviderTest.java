package io.github.headlesshq.headlessmc.files;

import org.junit.jupiter.api.Test;

import java.nio.file.FileSystems;

import static org.junit.jupiter.api.Assertions.*;

class DefaultFileSystemProviderTest {
    private final DefaultFileSystemProvider provider = new DefaultFileSystemProvider();

    @Test
    void usesTheDefaultFileSystemAndIsNotVirtual() {
        assertEquals(FileSystems.getDefault(), provider.getFileSystem());
        assertFalse(provider.isVirtual());
    }

}
