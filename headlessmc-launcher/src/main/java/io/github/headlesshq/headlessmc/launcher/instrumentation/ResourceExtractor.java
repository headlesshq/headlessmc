package io.github.headlesshq.headlessmc.launcher.instrumentation;

import io.github.headlesshq.headlessmc.api.util.ResourceUtil;
import io.github.headlesshq.headlessmc.launcher.files.FileManager;
import io.github.headlesshq.headlessmc.launcher.util.IOUtil;
import lombok.Cleanup;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@CustomLog
@RequiredArgsConstructor
public class ResourceExtractor extends AbstractTransformer {
    private final FileManager fileManager;
    private final String resourceName;

    @Override
    public List<Target> transform(List<Target> targets) throws IOException {
        log.debug("Extracting resource " + resourceName);
        val file = extract();
        targets.add(new Target(false, file.getAbsolutePath()));
        setRun(true);
        return targets;
    }

    public File extract() throws IOException {
        val file = getFile();
        if (file.exists()) {
            log.warn("Resource " + resourceName + " already exists.");
            return file;
        }

        @Cleanup
        val is = ResourceUtil.getHmcResource(resourceName);
        @Cleanup
        val os = new FileOutputStream(file);
        IOUtil.copy(is, os);
        return file;
    }

    public File getFile() {
        return fileManager.get(false, false, resourceName);
    }

}
