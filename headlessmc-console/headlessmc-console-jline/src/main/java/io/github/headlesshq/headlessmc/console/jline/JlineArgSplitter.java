package io.github.headlesshq.headlessmc.console.jline;

import io.github.headlesshq.headlessmc.console.ArgSplitter;
import jakarta.enterprise.context.ApplicationScoped;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;

@ApplicationScoped
public class JlineArgSplitter implements ArgSplitter {
    @Override
    public String[] split(String line) {
        // TODO: differentiate between splitting for completions or for args
        Parser parser = new DefaultParser();
        return parser.parse(line, line.length(), Parser.ParseContext.ACCEPT_LINE)
            .words()
            .toArray(String[]::new);
    }

}
