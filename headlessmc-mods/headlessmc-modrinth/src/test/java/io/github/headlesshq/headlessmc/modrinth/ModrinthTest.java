package io.github.headlesshq.headlessmc.modrinth;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.commons.DownloadService;
import io.github.headlesshq.headlessmc.mods.ModDownload;
import io.github.headlesshq.headlessmc.version.id.VersionID;
import jakarta.inject.Inject;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static io.github.headlesshq.headlessmc.cditestfixtures.WeldTestInitializer.getWeld;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableWeld
public class ModrinthTest {
    private static final URI STAGING_API = URI.create("https://staging-api.modrinth.com/v2/");

    @WeldSetup
    private final WeldInitiator weldInitiator = WeldInitiator.from(getWeld()).build();

    @Inject
    private DownloadService downloadService;

    @Test
    public void testModrinth() throws IOException, CommandException {
        Modrinth modrinth = new Modrinth(downloadService, STAGING_API);
        List<ModDownload> mods = modrinth.search("test-project");
        assertEquals(1, mods.size());
        ModDownload mod = mods.get(0);
        assertEquals("test-project", mod.getId());
        assertEquals("this is a project!", mod.getDescription());
        assertEquals(1, mod.getAuthors().size());
        assertEquals("jai", mod.getAuthors().get(0));
        System.out.println(mod.getIcon());

        VersionID id = VersionID.builder().platform("fabric").version("1.21.5").build();
        List<ModrinthProjectVersion> versions = modrinth.getVersions(id, "test-project");
        assertEquals(1, versions.size());
        ModrinthProjectVersion projectVersion = versions.get(0);
        assertEquals(1, projectVersion.getGameVersions().size());
        assertEquals("1.21.5", projectVersion.getGameVersions().get(0));
        assertEquals(1, projectVersion.getLoaders().size());
        assertEquals("fabric", projectVersion.getLoaders().get(0));
        assertEquals(1, projectVersion.getFiles().size());
        ModrinthFile file = projectVersion.getFiles().get(0);
        assertEquals("fabric-api-0.92.2+1.20.1.jar", file.getFilename());
        assertTrue(file.isPrimary());
    }

}
