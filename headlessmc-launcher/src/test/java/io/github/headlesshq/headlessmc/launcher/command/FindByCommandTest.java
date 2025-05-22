package io.github.headlesshq.headlessmc.launcher.command;

import lombok.SneakyThrows;
import io.github.headlesshq.headlessmc.api.traits.HasId;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.api.command.AbstractCommand;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.FindByCommand;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FindByCommandTest {
    @Test
    public void testFailures() {
        TestFindByCommand command = new TestFindByCommand();
        assertThrows(CommandException.class, () -> command.execute("test", "test"));
        assertThrows(CommandException.class, () -> command.execute("test 1 -id -regex", "test", "1", "-id", "-regex"));
        assertThrows(CommandException.class, () -> command.execute("test 4 -id", "test", "4", "-id"));
        assertThrows(CommandException.class, () -> command.execute("test [a-z -regex", "test", "[a-z", "-regex"));
    }

    @Test
    @SneakyThrows
    public void testFindById() {
        TestFindByCommand command = new TestFindByCommand();
        command.execute("test 1 -id", "test", "1", "-id");
        assertEquals("Object1", command.obj.getName());
        command.execute("test 2 -id", "test", "2", "-id");
        assertEquals("Object2", command.obj.getName());
    }

    @Test
    @SneakyThrows
    public void testFindByName() {
        TestFindByCommand command = new TestFindByCommand();
        command.execute("test Object1", "test", "Object1");
        assertEquals("Object1", command.obj.getName());
        command.execute("test Object3", "test", "Object3");
        assertEquals("Object3", command.obj.getName());
    }

    @Test
    @SneakyThrows
    public void testFindByRegex() {
        TestFindByCommand command = new TestFindByCommand();
        command.execute("test Object1 -regex", "test", "Object1", "-regex");
        assertEquals("Object1", command.obj.getName());
        command.execute("test Object.* -regex", "test", "Object.*", "-regex");
        assertEquals("Object3", command.obj.getName());
    }

    private static class TestObject implements HasName, HasId {
        private final String name;
        private final int id;

        public TestObject(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getId() {
            return id;
        }
    }

    private static class TestFindByCommand extends AbstractCommand implements FindByCommand<TestObject> {
        private final List<TestObject> objects;
        private TestObject obj;

        public TestFindByCommand() {
            super(null, "test", "test");
            objects = new ArrayList<>();
            objects.add(new TestObject("Object1", 1));
            objects.add(new TestObject("Object3", 3));
            objects.add(new TestObject("Object2", 2));
        }

        @Override
        public void execute(TestObject obj, String... args) {
            this.obj = obj;
        }

        @Override
        public Iterable<TestObject> getIterable() {
            return objects;
        }
    }

}
