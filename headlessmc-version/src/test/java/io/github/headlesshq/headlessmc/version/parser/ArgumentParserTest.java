package io.github.headlesshq.headlessmc.version.parser;

import io.github.headlesshq.headlessmc.version.Rule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ArgumentParserTest implements UsesResources {
    @Test
    @SneakyThrows
    public void testParseArguments() {
        ArgumentParser factory = new ArgumentParser(new RuleParser());

        ArgumentParseResult args = factory.parseLegacyArguments(getJsonElement("arguments_old.json").getAsString());
        Assertions.assertNull(args.getJvmArguments());
        Assertions.assertNotNull(args.getGameArguments());
        Assertions.assertFalse(args.getGameArguments().isInNewFormat());
        Assertions.assertEquals(4, args.getGameArguments().getArguments().size());
        args.getGameArguments().forEach(arg -> {
            Assertions.assertEquals(Rule.ALLOW, arg.getRule());
        });

        Assertions.assertEquals("--userType", args.getGameArguments().getArguments().get(0).getValue());
        Assertions.assertEquals("${user_type}", args.getGameArguments().getArguments().get(1).getValue());
        Assertions.assertEquals("--versionType", args.getGameArguments().getArguments().get(2).getValue());
        Assertions.assertEquals("${version_type}", args.getGameArguments().getArguments().get(3).getValue());

        args = factory.parseArguments(getJsonObject("arguments.json"));
        Assertions.assertNotNull(args.getJvmArguments());
        Assertions.assertNotNull(args.getGameArguments());
        Assertions.assertTrue(args.getJvmArguments().isInNewFormat());
        Assertions.assertTrue(args.getGameArguments().isInNewFormat());
        Assertions.assertEquals(5, args.getGameArguments().getArguments().size());

        Assertions.assertEquals(Rule.ALLOW, args.getGameArguments().getArguments().get(0).getRule());
        Assertions.assertEquals(Rule.ALLOW, args.getGameArguments().getArguments().get(1).getRule());
        Assertions.assertEquals(Rule.ALLOW, args.getJvmArguments().getArguments().get(0).getRule());
        Assertions.assertEquals(Rule.ALLOW, args.getJvmArguments().getArguments().get(1).getRule());

        Assertions.assertNotEquals(Rule.ALLOW, args.getGameArguments().getArguments().get(2).getRule());
        Assertions.assertNotEquals(Rule.ALLOW, args.getGameArguments().getArguments().get(3).getRule());
        Assertions.assertNotEquals(Rule.ALLOW, args.getGameArguments().getArguments().get(4).getRule());

        Assertions.assertEquals("--arg", args.getGameArguments().getArguments().get(0).getValue());
        Assertions.assertEquals("${arg}", args.getGameArguments().getArguments().get(1).getValue());
        Assertions.assertEquals("--demo", args.getGameArguments().getArguments().get(2).getValue());
        Assertions.assertEquals("value1", args.getGameArguments().getArguments().get(3).getValue());
        Assertions.assertEquals("value2", args.getGameArguments().getArguments().get(4).getValue());
        Assertions.assertEquals("--something", args.getJvmArguments().getArguments().get(0).getValue());
        Assertions.assertEquals("${something}", args.getJvmArguments().getArguments().get(1).getValue());
    }

}
