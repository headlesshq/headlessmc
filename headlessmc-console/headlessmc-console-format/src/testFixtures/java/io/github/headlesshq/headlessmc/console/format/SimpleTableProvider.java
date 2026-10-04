package io.github.headlesshq.headlessmc.console.format;

/**
 * A {@link TableProvider} that always renders plain, un-styled tables,
 * so tests can assert on the output without ANSI escapes getting in the way.
 */
public class SimpleTableProvider implements TableProvider {
    @Override
    public <T> TableBuilder<T> get() {
        return new SimpleTable<>();
    }

}
