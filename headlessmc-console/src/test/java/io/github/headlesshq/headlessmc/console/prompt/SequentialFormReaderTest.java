package io.github.headlesshq.headlessmc.console.prompt;

import io.github.headlesshq.headlessmc.console.CensoringConsole;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.ConsoleException;
import io.github.headlesshq.headlessmc.console.Password;
import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.console.output.SecretServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class SequentialFormReaderTest {
    private static Form accountForm() {
        return Form.builder("Create an account")
            .text("name", "Name")
            .optionalText("type", "Type", "msa")
            .optionalSecret("token", "Token")
            .build();
    }

    @Test
    public void asksPromptsInOrderAndAppliesDefaults() {
        RecordingConsole console = new RecordingConsole().addInput("Steve").addInput("  ").addPassword("secret");

        try (Answers answers = console.ask(accountForm())) {
            assertEquals("Steve", answers.getString("name"));
            assertEquals("msa", answers.getString("type"));
            assertEquals("secret", new String(answers.getPassword("token").get()));
        }

        assertEquals(Arrays.asList(
            "Create an account (Ctrl+C to abort)",
            "Name: ",
            "Type (default: msa): ",
            "Token (optional, hidden): "
        ), console.lines());
    }

    @Test
    public void singlePromptFormsDoNotPrintTheTitle() {
        RecordingConsole console = new RecordingConsole().addInput("y");

        assertTrue(console.confirm("Delete everything?", false));
        assertEquals(Collections.singletonList("Delete everything? [y/N]: "), console.lines());
    }

    @Test
    public void confirmUsesTheDefaultAndAsksAgainOnInvalidInput() {
        assertFalse(new RecordingConsole().addInput("").confirm("Continue?", false));
        assertTrue(new RecordingConsole().addInput("").confirm("Continue?", true));
        assertFalse(new RecordingConsole().addInput("NO").confirm("Continue?", true));

        RecordingConsole console = new RecordingConsole().addInput("maybe").addInput("yes");
        assertTrue(console.confirm("Continue?", false));
        assertTrue(console.lines().contains("Please answer y or n."));
    }

    @Test
    public void choiceAcceptsNumbersAndNames() {
        Form form = Form.builder("Profiles").choice("profile", "Select a profile", Arrays.asList("Steve", "Alex")).build();

        RecordingConsole byNumber = new RecordingConsole().addInput("2");
        try (Answers answers = byNumber.ask(form)) {
            assertEquals("Alex", answers.getString("profile"));
        }

        assertEquals(Arrays.asList("  1. Steve", "  2. Alex", "Select a profile (1-2): "), byNumber.lines());

        try (Answers answers = new RecordingConsole().addInput("3").addInput("steve").ask(form)) {
            assertEquals("Steve", answers.getString("profile"));
        }
    }

    @Test
    public void requiredPromptsAreAskedAgainButNotForever() {
        RecordingConsole console = new RecordingConsole().addInput("").addInput("Steve");
        try (Answers answers = console.ask(accountForm())) {
            assertEquals("Steve", answers.getString("name"));
        }

        assertTrue(console.lines().contains("A value is required."));

        // RecordingConsole answers "" once it runs out of input, like a closed input would never answer
        ConsoleException e = assertThrows(ConsoleException.class, () -> new RecordingConsole().ask(accountForm()));
        assertFalse(e instanceof ConsoleException.Interrupted);
        assertTrue(e.getMessage().contains("Name"));
    }

    @Test
    public void requiredSecretsAreAskedAgain() {
        Form form = Form.builder("Login").secret("password", "Password").build();
        RecordingConsole console = new RecordingConsole().addPassword("").addPassword("hunter2");
        try (Answers answers = console.ask(form)) {
            assertEquals("hunter2", new String(answers.getPassword("password").get()));
        }
    }

    @Test
    public void interruptAbortsTheFormAndClearsSecrets() {
        AtomicReference<Password> read = new AtomicReference<>();
        RecordingConsole console = new RecordingConsole() {
            @Override
            public Password readPassword() {
                Password password = super.readPassword();
                read.set(password);
                return password;
            }
        };
        console.addPassword("secret").addInput("Steve").addInterrupt();

        Form form = Form.builder("Login")
            .optionalSecret("token", "Token")
            .text("name", "Name")
            .text("server", "Server")
            .build();
        assertThrows(ConsoleException.Interrupted.class, () -> console.ask(form));
        assertThrows(IllegalStateException.class, () -> read.get().get());
    }

    @Test
    public void closingAnswersClearsSecrets() {
        Password password = Password.of("secret");
        Answers answers = new Answers().put("token", password);
        answers.close();
        assertThrows(IllegalStateException.class, password::get);
    }

    @Test
    public void answersRejectWrongTypes() {
        try (Answers answers = new Answers().put("name", "Steve")) {
            assertThrows(IllegalArgumentException.class, () -> answers.getBoolean("name"));
            assertThrows(IllegalArgumentException.class, () -> answers.getString("missing"));
        }
    }

    @Test
    public void builderRejectsDuplicateIdsAndEmptyChoices() {
        assertThrows(IllegalArgumentException.class, () -> Form.builder("t").text("a", "A").text("a", "B"));
        assertThrows(IllegalArgumentException.class, () -> Form.builder("t").choice("a", "A", Collections.emptyList()));
    }

    @Test
    public void censoringConsolePassesTheWholeFormOnCensored() {
        List<Form> asked = new ArrayList<>();
        Console delegate = new RecordingConsole() {
            @Override
            public Answers ask(Form form) {
                asked.add(form);
                return new Answers().put("name", "Steve");
            }
        };

        Console console = new CensoringConsole(delegate, new SecretServiceImpl(Collections.singletonList(
            () -> Collections.singletonList("access-token"))));
        Form form = Form.builder("Login with access-token").text("name", "Name for access-token").build();
        try (Answers answers = console.ask(form)) {
            assertEquals("Steve", answers.getString("name"));
        }

        assertEquals(1, asked.size());
        assertFalse(asked.get(0).getTitle().contains("access-token"));
        assertFalse(asked.get(0).getPrompts().get(0).getMessage().contains("access-token"));
        assertEquals("name", asked.get(0).getPrompts().get(0).getId());
    }

    @Test
    public void interruptedIsFoundInTheCauseChain() {
        ConsoleException.Interrupted interrupted = new ConsoleException.Interrupted();
        RuntimeException wrapped = new RuntimeException(new IllegalStateException(interrupted));
        assertSame(interrupted, ConsoleException.Interrupted.find(wrapped));
        assertNull(ConsoleException.Interrupted.find(new RuntimeException()));
        assertNull(ConsoleException.Interrupted.find(null));
    }

}
