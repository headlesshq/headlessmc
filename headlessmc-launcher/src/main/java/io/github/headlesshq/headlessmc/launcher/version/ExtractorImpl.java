package io.github.headlesshq.headlessmc.launcher.version;

import io.github.headlesshq.headlessmc.launcher.files.FileManager;
import io.github.headlesshq.headlessmc.launcher.util.IOUtil;
import lombok.Cleanup;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;

@CustomLog
@RequiredArgsConstructor
class ExtractorImpl implements Extractor {
    private final List<String> exceptions;

    public ExtractorImpl() {
        this(Collections.emptyList());
    }

    @Override
    public void extract(String from, FileManager fileManager) throws IOException {
        log.debug("Extracting " + from);
        @Cleanup
        val jar = new JarFile(from);
        val enumeration = jar.entries();
        while (enumeration.hasMoreElements()) {
            val je = enumeration.nextElement();
            if (shouldExtract(je.getName()) && !je.isDirectory()) {
                log.debug(
                    String.format("Extracting  : %s from %s to %s%s%s",
                                  je.getName(), jar.getName(),
                                  fileManager.getBase(), File.separator,
                                  je.getName()));
                @Cleanup
                val is = jar.getInputStream(je);
                File file = fileManager.create(je.getName());
                @Cleanup
                OutputStream os = new FileOutputStream(file);
                IOUtil.copy(is, os);
            }
        }
    }

    @Override
    public boolean shouldExtract(String name) {
        return name != null && exceptions.stream().noneMatch(name::startsWith);
    }

    @Override
    public boolean isExtracting() {
        return true;
    }

}
