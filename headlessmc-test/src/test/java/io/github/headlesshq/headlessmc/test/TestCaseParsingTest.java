package io.github.headlesshq.headlessmc.test;

import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.github.headlesshq.headlessmc.test.TestCase.Action.Type.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestCaseParsingTest {
    private final JsonService jsonService = new DefaultJacksonJsonService();

    @Test
    public void parsesTheBundledServerTest() throws IOException {
        TestCase testCase;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CommandTest.SERVER_TEST_RESOURCE)) {
            assertNotNull(is, "server test resource is missing");
            testCase = jsonService.parse(is, TestCase.class);
        }

        assertEquals("Server Test", testCase.getName());
        assertEquals(120L, testCase.getTimeout());
        assertTrue(testCase.getImplicitWaitForEnd());
        assertNull(testCase.getTotalTimeout());

        List<TestCase.Action> steps = testCase.getSteps();
        assertEquals(2, steps.size());
        assertEquals(ENDS_WITH, steps.get(0).getType());
        assertEquals("For help, type \"help\"", steps.get(0).getMessage());
        assertEquals(SEND, steps.get(1).getType());
        assertEquals("stop", steps.get(1).getMessage());
        assertEquals(120L, steps.get(1).getTimeout());
    }

    @Test
    public void parsesAllProperties() throws IOException {
        TestCase testCase = parse("""
            {
              "name": "Full",
              "timeout": 30,
              "totalTimeout": 600,
              "implicitWaitForEnd": false,
              "steps": [
                {
                  "type": "CONTAINS",
                  "message": "Done",
                  "ignoreCase": true,
                  "timeout": 5,
                  "and": [ { "type": "ENDS_WITH", "message": "!" } ],
                  "or": [ { "type": "REGEX", "message": ".*Ready.*" } ],
                  "then": [ { "type": "SEND", "message": "stop" }, { "type": "SUCCESS" } ]
                }
              ]
            }
            """);

        assertEquals("Full", testCase.getName());
        assertEquals(30L, testCase.getTimeout());
        assertEquals(600L, testCase.getTotalTimeout());
        assertFalse(testCase.getImplicitWaitForEnd());

        TestCase.Action action = testCase.getSteps().getFirst();
        assertEquals(CONTAINS, action.getType());
        assertEquals("Done", action.getMessage());
        assertTrue(action.isIgnoreCase());
        assertEquals(5L, action.getTimeout());

        assertNotNull(action.getAnd());
        assertEquals(ENDS_WITH, action.getAnd().getFirst().getType());
        assertNotNull(action.getOr());
        assertEquals(REGEX, action.getOr().getFirst().getType());
        assertNotNull(action.getThen());
        assertEquals(List.of(SEND, SUCCESS), action.getThen().stream().map(TestCase.Action::getType).toList());
    }

    @Test
    public void missingOptionalPropertiesAreNull() throws IOException {
        TestCase testCase = parse("""
            { "name": "Minimal", "steps": [ { "type": "SUCCESS" } ] }
            """);

        TestCase.Action action = testCase.getSteps().getFirst();
        assertNull(action.getMessage());
        assertNull(action.getIgnoreCase());
        assertNull(action.getTimeout());
        assertNull(action.getAnd());
        assertNull(action.getOr());
        assertNull(action.getThen());
    }

    @Test
    public void singleStepIsAcceptedAsArray() throws IOException {
        TestCase testCase = parse("""
            { "name": "Single", "steps": { "type": "SUCCESS" } }
            """);

        assertEquals(1, testCase.getSteps().size());
        assertEquals(SUCCESS, testCase.getSteps().getFirst().getType());
    }

    @Test
    public void unknownActionTypeFails() {
        assertThrows(JsonParseException.class, () -> parse("""
            { "name": "Invalid", "steps": [ { "type": "DOES_NOT_EXIST" } ] }
            """));
    }

    private TestCase parse(String json) throws IOException {
        return jsonService.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), TestCase.class);
    }

}
