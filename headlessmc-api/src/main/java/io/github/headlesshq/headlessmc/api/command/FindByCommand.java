package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.HasId;
import io.github.headlesshq.headlessmc.api.HasName;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public interface FindByCommand<T extends HasName & HasId> extends HasName {
    void execute(T obj, String... args) throws CommandException;

    Iterable<T> getIterable();

    default void execute(String line, String... args) throws CommandException {
        if (args.length < 2) {
            throw new CommandException("Please specify an id!");
        }

        /*boolean byId = CommandUtil.hasFlag("-id", args);
        boolean byRegex = CommandUtil.hasFlag("-regex", args);
        if (byId && byRegex) {
            throw new CommandException("Both -id and -regex specified!");
        }

        try {
            T t = findObject(byId, byRegex, args[1], args);
            if (t == null) {
                onObjectNotFound(byId, byRegex, args[1], args);
                return;
            }

            this.execute(t, args);
        } catch (PatternSyntaxException e) {
            throw new CommandException("Failed to parse regex " + args[1], e);
        }*/
    }

    default void onObjectNotFound(boolean byId, boolean byRegex, String objectArg, String... args) throws CommandException {
        throw new CommandException("Couldn't find object for "
                + (byId
                    ? "id '"
                    : (byRegex
                        ? "regex '"
                        : "name '"))
                + objectArg + "'!");
    }

    default @Nullable T findObject(boolean byId, boolean byRegex, String objectArg, String... args) throws CommandException {
        T result = byId
                ? HasId.getById(objectArg, getIterable())
                : byRegex
                 ? HasName.getByRegex(Pattern.compile(objectArg), getIterable())
                 : HasName.getByName(objectArg, getIterable());

        if (result == null && !byId && !byRegex) {
            result = HasId.getById(objectArg, getIterable());
        }

        return result;
    }

    default void getCompletions(String line, List<Map.Entry<String, @Nullable String>> completions, String... args) {
        if (args.length == 2) {
            String arg = args[1].toLowerCase(Locale.ENGLISH);
            for (T t : getIterable()) {
                if (t.getName().toLowerCase(Locale.ENGLISH).startsWith(arg)
                    && !t.getName().equalsIgnoreCase(arg)) {
                    completions.add(new AbstractMap.SimpleEntry<>(t.getName(), t instanceof HasDescription ? ((HasDescription) t).getDescription() : null));
                }
            }
        } else if (args.length == 1 && line.startsWith(getName().toLowerCase(Locale.ENGLISH) + " ")) {
            for (T t : getIterable()) {
                completions.add(new AbstractMap.SimpleEntry<>(t.getName(), t instanceof HasDescription ? ((HasDescription) t).getDescription() : null));
            }
        }
    }

}
