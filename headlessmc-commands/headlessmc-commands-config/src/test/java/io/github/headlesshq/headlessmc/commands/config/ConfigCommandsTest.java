package io.github.headlesshq.headlessmc.commands.config;

import io.github.headlesshq.headlessmc.config.ConfigDescriptionService;
import io.github.headlesshq.headlessmc.config.ConfigException;
import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.config.ConfigServiceImpl;
import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.console.ConsoleExtensions;
import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.console.format.SimpleTableProvider;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ConfigCommandsTest {
    private static final String PROPERTY = "hmc.java.download";

    @Inject
    ConfigService configService;

    @Inject
    ConfigDescriptionService descriptionService;

    @Inject
    @Any
    Instance<DynamicConfig> dynamicConfigs;

    @Inject
    Config config;

    @TempDir
    Path dir;

    private final RecordingConsole console = new RecordingConsole();
    private final TableProvider tables = new SimpleTableProvider();

    private String output() {
        return console.output();
    }

    @Test
    public void theConfigCommandListsAllHmcProperties() {
        ConfigCommand command = new ConfigCommand(descriptionService, configService.fork(), tables, console);

        command.run();

        String output = output();
        assertTrue(output.contains(PROPERTY), () -> "was: " + output);
        assertTrue(output.contains("name"), () -> "was: " + output);
        assertTrue(output.contains("description"), () -> "was: " + output);
    }

    @Test
    public void getShowsProperty() {
        GetCommand command = new GetCommand(descriptionService, configService.fork(), tables, console);
        command.setProperty(PROPERTY);

        command.run();

        assertTrue(output().contains(PROPERTY));
        assertTrue(output().contains("true"));
    }

    @Test
    public void getWithoutPropertyThrows() {
        GetCommand command = new GetCommand(descriptionService, configService.fork(), tables, console);
        assertThrows(IllegalArgumentException.class, command::run);
    }

    @Test
    public void setChangesProperty() {
        ConfigService fork = configService.fork();
        SetCommand command = new SetCommand(descriptionService, fork, tables, console);
        command.setTemp(true);
        command.setProperty(PROPERTY);
        command.setValue("false");

        command.run();

        assertEquals("false", fork.getConfig().getValue(PROPERTY, String.class));
        assertTrue(output().contains("Set " + PROPERTY + " to false temporarily"));
    }

    @Test
    public void setWithoutPropertyThrows() {
        SetCommand command = new SetCommand(descriptionService, configService.fork(), tables, console);
        command.setTemp(true);
        assertThrows(IllegalArgumentException.class, command::run);
    }

    @Test
    public void setWithoutValueThrowsWithoutConsoleExtensions() {
        SetCommand command = new SetCommand(descriptionService, configService.fork(), tables, console);
        command.setTemp(true);
        command.setProperty(PROPERTY);
        assertThrows(IllegalArgumentException.class, command::run);
    }

    @Test
    public void setInvalidValueThrowsAndKeepsTheOldValue() {
        ConfigService fork = configService.fork();
        SetCommand command = new SetCommand(descriptionService, fork, tables, console);
        command.setTemp(true);
        command.setProperty("hmc.test.commands.number");
        command.setValue("notAnInt");

        assertThrows(ConfigException.class, command::run);

        assertEquals(6, fork.getConfig().getValue("hmc.test.commands.number", Integer.class));
        assertFalse(output().contains("Set "), () -> "was: " + output());
    }

    @Test
    public void setWithoutValueUsesTheConsoleEditor() {
        ConfigService fork = configService.fork();
        List<String> edited = new ArrayList<>();
        RecordingConsole editingConsole = new RecordingConsole() {
            @Override
            public Optional<ConsoleExtensions> extensions() {
                return Optional.of(new ConsoleExtensions() {
                    @Override
                    public String edit(String initialString) {
                        edited.add(initialString);
                        return "false";
                    }

                    @Override
                    public String read(String prompt, Completions completions) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public int getWidth() {
                        return 80;
                    }
                });
            }
        };

        SetCommand command = new SetCommand(descriptionService, fork, tables, editingConsole);

        command.setTemp(true);
        command.setProperty(PROPERTY);
        command.run();

        assertEquals(List.of("true"), edited, "the editor must start with the current value");
        assertEquals("false", fork.getConfig().getValue(PROPERTY, String.class));
        assertTrue(editingConsole.output().contains("Set " + PROPERTY + " to false temporarily"));
    }

    @Test
    public void setDoesNotAffectTheSharedConfigService() {
        SetCommand command = new SetCommand(descriptionService, configService.fork(), tables, console);
        command.setTemp(true);
        command.setProperty(PROPERTY);
        command.setValue("false");
        command.run();

        assertEquals("true", configService.getConfig().getValue(PROPERTY, String.class));
    }

    private ConfigServiceImpl fileBackedConfigService() {
        ConfigServiceImpl result = new ConfigServiceImpl(dynamicConfigs, new ConcurrentHashMap<>(), config);
        result.loadConfigFile(configFile());
        return result;
    }

    private Path configFile() {
        return dir.resolve("config.properties");
    }

    @Test
    public void setPersistsToTheConfigFileByDefault() throws IOException {
        ConfigServiceImpl fileBacked = fileBackedConfigService();
        SetCommand command = new SetCommand(descriptionService, fileBacked, tables, console);
        command.setProperty(PROPERTY);
        command.setValue("false");

        command.run();

        assertEquals("false", fileBacked.getConfig().getValue(PROPERTY, String.class));
        assertEquals(PROPERTY + "=false" + System.lineSeparator(), Files.readString(configFile()));
        assertTrue(output().contains("Set " + PROPERTY + " to false"), () -> "was: " + output());
        assertFalse(output().contains("temporarily"), () -> "was: " + output());
        // a restart picks up the value
        assertEquals("false", fileBackedConfigService().getConfig().getValue(PROPERTY, String.class));
    }

    @Test
    public void setWithTempDoesNotWriteTheConfigFile() {
        ConfigServiceImpl fileBacked = fileBackedConfigService();
        SetCommand command = new SetCommand(descriptionService, fileBacked, tables, console);
        command.setProperty(PROPERTY);
        command.setValue("false");
        command.setTemp(true);

        command.run();

        assertEquals("false", fileBacked.getConfig().getValue(PROPERTY, String.class));
        assertFalse(Files.exists(configFile()));
        assertEquals("true", fileBackedConfigService().getConfig().getValue(PROPERTY, String.class));
    }

    @Test
    public void setWithoutTempOnAConfigWithoutFileThrows() {
        ConfigService fork = configService.fork();
        SetCommand command = new SetCommand(descriptionService, fork, tables, console);
        command.setProperty(PROPERTY);
        command.setValue("false");

        assertThrows(ConfigException.class, command::run);
        assertEquals("true", fork.getConfig().getValue(PROPERTY, String.class));
    }

    private CommandLine commandLine(SetCommand command) {
        return new CommandLine(command, new CommandLine.IFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <K> K create(Class<K> cls) throws Exception {
                if (cls == ConfigCompletions.class) {
                    return (K) new ConfigCompletions(configService);
                }

                return CommandLine.defaultFactory().create(cls);
            }
        });
    }

    @Test
    public void tempOptionIsParsedByPicocli() {
        SetCommand command = new SetCommand(descriptionService, configService.fork(), tables, console);
        commandLine(command).parseArgs("--temp", PROPERTY, "false");

        assertTrue(command.isTemp());
        assertEquals(PROPERTY, command.getProperty());
        assertEquals("false", command.getValue());
    }

    @Test
    public void tempOptionDefaultsToFalse() {
        SetCommand command = new SetCommand(descriptionService, configService.fork(), tables, console);
        commandLine(command).parseArgs(PROPERTY, "false");

        assertFalse(command.isTemp());
    }

    @Test
    public void tempOptionCanComeAfterTheParameters() {
        SetCommand command = new SetCommand(descriptionService, configService.fork(), tables, console);
        commandLine(command).parseArgs(PROPERTY, "false", "--temp");

        assertTrue(command.isTemp());
        assertEquals("false", command.getValue());
    }

    @Test
    public void listShowsHmcProperties() {
        ListCommand command = new ListCommand(descriptionService, configService.fork(), tables, console);

        command.run();

        assertTrue(output().contains(PROPERTY));
    }

    @Test
    public void listAllShowsMoreProperties() {
        ListCommand hmcOnly = new ListCommand(descriptionService, configService.fork(), tables, console);
        hmcOnly.run();
        int hmcOutputLength = output().length();
        console.lines().clear();

        ListCommand all = new ListCommand(descriptionService, configService.fork(), tables, console);
        all.setAll(true);
        all.run();

        assertTrue(output().length() > hmcOutputLength);
    }

}
