package io.github.headlesshq.headlessmc.commands.config;

import io.github.headlesshq.headlessmc.config.ConfigDescriptionService;
import io.github.headlesshq.headlessmc.config.ConfigException;
import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.config.ConfigServiceImpl;
import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.github.headlesshq.headlessmc.console.CensoringConsole;
import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.ConsoleExtensions;
import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.console.format.SimpleTableProvider;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.console.output.SecretService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    private static final String TOKEN = "hmc.test.commands.token";
    private static final String SECRET = "ghp_secret";

    @Inject
    ConfigService configService;

    @Inject
    ConfigDescriptionService descriptionService;

    @Inject
    SecretService secretService;

    @Inject
    @Any
    Instance<DynamicConfig> dynamicConfigs;

    @Inject
    Config config;

    @TempDir
    Path dir;

    private final RecordingConsole console = new RecordingConsole();
    private final TableProvider tables = new SimpleTableProvider();
    private ConfigTable configTable;

    @AfterEach
    public void removeSecret() {
        configService.remove(TOKEN);
    }

    @BeforeEach
    public void setupConfigTable() {
        configTable = new ConfigTable(descriptionService, tables);
    }

    private static RecordingConsole editingConsole(List<String> edited, String result) {
        return new RecordingConsole() {
            @Override
            public Optional<ConsoleExtensions> extensions() {
                return Optional.of(new ConsoleExtensions() {
                    @Override
                    public String edit(String initialString) {
                        edited.add(initialString);
                        return result;
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
    }

    private String output() {
        return console.output();
    }

    @Test
    public void theConfigCommandListsAllHmcProperties() {
        ConfigCommand command = new ConfigCommand(configService.fork(), configTable, console);

        command.run();

        String output = output();
        assertTrue(output.contains(PROPERTY), () -> "was: " + output);
        assertTrue(output.contains("name"), () -> "was: " + output);
        assertTrue(output.contains("description"), () -> "was: " + output);
    }

    @Test
    public void getShowsProperty() {
        GetCommand command = new GetCommand(configService.fork(), configTable, console);
        command.setProperty(PROPERTY);

        command.run();

        assertTrue(output().contains(PROPERTY));
        assertTrue(output().contains("true"));
    }

    @Test
    public void setCommandOutputIsCensoredByTheConsole() {
        Console censoring = new CensoringConsole(console, secretService);
        SetCommand set = new SetCommand(descriptionService, configService, tables, censoring);
        set.setTemp(true);
        set.setProperty(TOKEN);
        set.setValue(SECRET);

        set.run();

        assertEquals(SECRET, configService.getConfig().getValue(TOKEN, String.class));
        assertEquals(List.of("Set " + TOKEN + " to " + SecretService.MASK + " temporarily"), console.lines());
    }

    @Test
    public void configCommandsOutputIsCensoredByTheConsole() {
        Console censoring = new CensoringConsole(console, secretService);
        configService.set(TOKEN, SECRET, false);

        GetCommand get = new GetCommand(configService, configTable, censoring);
        get.setProperty(TOKEN);
        get.run();
        new ListCommand(configService, configTable, censoring).run();
        new ConfigCommand(configService, configTable, censoring).run();

        assertFalse(output().contains(SECRET), () -> "was: " + output());
        assertTrue(output().contains(SecretService.MASK + " "), () -> "was: " + output());
    }

    @Test
    public void editingASecretStartsWithAnEmptyEditor() {
        configService.set(TOKEN, SECRET, false);
        List<String> edited = new ArrayList<>();
        Console censoring = new CensoringConsole(editingConsole(edited, "ghp_new_secret"), secretService);
        SetCommand command = new SetCommand(descriptionService, configService, tables, censoring);
        command.setTemp(true);
        command.setProperty(TOKEN);

        command.run();

        assertEquals(List.of(""), edited);
        assertEquals("ghp_new_secret", configService.getConfig().getValue(TOKEN, String.class));
    }

    @Test
    public void getWithoutPropertyThrows() {
        GetCommand command = new GetCommand(configService.fork(), configTable, console);
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
        RecordingConsole editingConsole = editingConsole(edited, "false");

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
        assertEquals(PROPERTY + "=false\n", Files.readString(configFile()));
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
        ListCommand command = new ListCommand(configService.fork(), configTable, console);

        command.run();

        assertTrue(output().contains(PROPERTY));
    }

    @Test
    public void optionalPropertiesAreListedWithoutValue() {
        String token = "hmc.test.commands.token";
        new ConfigCommand(configService.fork(), configTable, console).run();
        new ListCommand(configService.fork(), configTable, console).run();

        List<String> lines = output().lines().filter(line -> line.startsWith(token)).toList();
        assertEquals(2, lines.size(), () -> "was: " + output());
        for (String line : lines) {
            assertTrue(line.matches(token + " +- +Optional.*"), () -> "was: " + line);
        }
    }

    @Test
    public void listAllShowsMoreProperties() {
        ListCommand hmcOnly = new ListCommand(configService.fork(), configTable, console);
        hmcOnly.run();
        int hmcOutputLength = output().length();
        console.lines().clear();

        ListCommand all = new ListCommand(configService.fork(), configTable, console);
        all.setAll(true);
        all.run();

        assertTrue(output().length() > hmcOutputLength);
    }

}
