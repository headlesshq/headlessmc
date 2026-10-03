package io.github.headlesshq.headlessmc.launcher.args;

import io.github.headlesshq.headlessmc.java.args.ArgPair;
import io.github.headlesshq.headlessmc.version.util.TemplateString;
import io.github.headlesshq.headlessmc.version.util.TemplateStrings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ArgumentTemplateServiceTest {
    private final ArgumentTemplateService service = new ArgumentTemplateService();

    @Test
    public void processesTemplatesInAllArgumentTypes() {
        Arguments arguments = Arguments.mutable();
        arguments.addVmArg("-Djava.library.path=${natives_directory}");
        arguments.addVmArg("-Xmx1G");
        arguments.gameArgs().add("--username");
        arguments.gameArgs().add("${auth_player_name}");

        TemplateStrings templates = new TemplateStrings();
        templates.add(TemplateString.NATIVES_DIRECTORY, "/natives");
        templates.add(TemplateString.AUTH_PLAYER_NAME, "Steve");

        Arguments processed = service.process(arguments, templates);

        assertEquals(Map.of("java.library.path", "/natives"), processed.systemProperties());
        assertEquals(List.of("-Xmx1G"), processed.vmArgs());
        assertEquals(List.of("--username", "Steve"), processed.gameArgs());
    }

    @Test
    public void removesUnknownTemplates() {
        Arguments arguments = Arguments.mutable();
        arguments.gameArgs().add("${unknown_template}");

        Arguments processed = service.process(arguments, new TemplateStrings());

        assertEquals(List.of(""), processed.gameArgs());
    }

    @Test
    public void keepsSystemPropertiesWithoutValue() {
        Arguments arguments = Arguments.mutable();
        arguments.addVmArg("-Dflag");

        Arguments processed = service.process(arguments, new TemplateStrings());

        assertTrue(processed.systemProperties().containsKey("flag"));
        assertNull(processed.systemProperties().get("flag"));
    }

    @Test
    public void argPairAsArray() {
        assertArrayEquals(new String[]{"--arg", "value"}, new ArgPair("--arg", "value").asArray());
        assertArrayEquals(new String[]{"--flag"}, new ArgPair("--flag", null).asArray());
    }

}
