package io.github.headlesshq.headlessmc.api.command.picocli;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * This class contains code needed to split Lines of text into an array of arguments.
 * This is needed because Picoli never added this kind of support itself,
 * it was discussed in
 * <a href=https://github.com/remkop/picocli/pull/293>https://github.com/remkop/picocli/pull/293</a>
 * and
 * <a href=https://github.com/remkop/picocli/issues/242>https://github.com/remkop/picocli/issues/242</a>.
 * However, it was decided that JLine comes with the
 * {@code org.jline.reader.impl.DefaultParser}, which can tokenize Strings.
 * But for us, JLine might not be available,
 * so we cannot rely on this method.
 * This package contains the needed JLine classes.
 */
@ApplicationScoped
public class CommandLineParser {
    public String[] parse(String line) throws CommandException {
        Parser parser = new DefaultParser();
        try {
            CompletingParsedLine parsedLine = parser.parse(line, line.length(), Parser.ParseContext.ACCEPT_LINE);
            return parsedLine.words().toArray(new String[0]);
        } catch (SyntaxError e) {
            throw new CommandException(e.getMessage());
        }
    }

}
