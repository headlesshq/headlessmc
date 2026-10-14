package io.github.headlesshq.headlessmc.console.completions;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@NullMarked
public class TentativeMatchTest {
    @Test
    public void testTentativeMatchFieldIsAccessible() {
        CommandLine commandLine = new CommandLine(Cmd.class);
        CommandLine.ParseResult parseResult = commandLine.parseArgs("value");

        // fails if picocli renames or removes the private ParseResult.tentativeMatch field
        List<Object> tentativeMatch = BetterAutoComplete.tentativeMatch(parseResult);
        assertEquals(1, tentativeMatch.size());
        assertInstanceOf(CommandLine.Model.PositionalParamSpec.class, tentativeMatch.get(0));
    }

    @SuppressWarnings("unused")
    @CommandLine.Command(name = "cmd")
    static class Cmd {
        @CommandLine.Parameters(index = "0")
        String param;
    }
}
