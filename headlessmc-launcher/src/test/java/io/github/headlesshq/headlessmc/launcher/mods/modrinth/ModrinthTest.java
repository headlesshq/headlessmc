package io.github.headlesshq.headlessmc.launcher.mods.modrinth;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.launcher.api.VersionId;
import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import io.github.headlesshq.headlessmc.launcher.mods.Mod;
import io.github.headlesshq.headlessmc.launcher.util.URLs;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModrinthTest {
    private static final URL STAGING_API = URLs.url("https://staging-api.modrinth.com/v2/");

    @Test
    public void testModrinth() throws IOException, CommandException {
        DownloadService downloadService = new DownloadService();
        Modrinth modrinth = new Modrinth(downloadService, STAGING_API);
        List<Mod> mods = modrinth.search("test-project");
        assertEquals(2, mods.size());
        Mod mod = mods.get(0);
        assertEquals("test-project", mod.getName());
        assertEquals(0, mod.getId());
        assertEquals("this is a project!", mod.getDescription());
        assertEquals(1, mod.getAuthors().size());
        assertEquals("jai", mod.getAuthors().get(0));

        VersionId id = VersionId.parse("fabric:1.21.5");
        List<ModrinthProjectVersion> versions = modrinth.getVersions(id, "test-project");
        assertEquals(1, versions.size());
        ModrinthProjectVersion projectVersion = versions.get(0);
        assertEquals(1, projectVersion.getGameVersions().size());
        assertEquals("1.21.5", projectVersion.getGameVersions().get(0));
        assertEquals(23, projectVersion.getLoaders().size());
        assertEquals("babric", projectVersion.getLoaders().get(0));
        assertEquals(1, projectVersion.getFiles().size());
        ModrinthFile file = projectVersion.getFiles().get(0);
        assertEquals("fabric-api-0.92.2+1.20.1.jar", file.getFilename());
        assertTrue(file.isPrimary());
    }

}
