package io.github.headlesshq.headlessmc.api.traits;

import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class HasNameTest {
    @Test
    public void testGetByName() {
        val names = new ArrayList<HasName>();
        for (int i = 0; i < 10; i++) {
            val name = "Test" + i;
            val hasName = new HasName() {
                @Override
                public String getName() {
                    return name;
                }
            };

            assertNull(HasName.getByName(name, names));

            names.add(hasName);

            val getByName = HasName.getByName(name, names);
            assertNotNull(getByName);
            assertEquals(name, getByName.getName());
        }
    }

    @Test
    public void testGetByRegex() {
        val names = new ArrayList<HasName>();
        for (int i = 0; i < 10; i++) {
            val name = "Test" + i;
            val hasName = new HasName() {
                @Override
                public String getName() {
                    return name;
                }
            };

            assertNull(HasName.getByRegex(Pattern.compile("Test" + i), names));
            names.add(hasName);

            HasName getByRegex = HasName.getByRegex(Pattern.compile("Test" + i), names);
            assertNotNull(getByRegex);
            assertEquals(name, getByRegex.getName());

            getByRegex = HasName.getByRegex(Pattern.compile("Test[0-9]"), names);
            assertNotNull(getByRegex);
            assertEquals("Test" + i, getByRegex.getName()); // should always be the first
        }
    }

}
