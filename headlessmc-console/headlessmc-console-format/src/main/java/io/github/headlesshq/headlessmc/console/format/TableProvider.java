package io.github.headlesshq.headlessmc.console.format;

@FunctionalInterface
public interface TableProvider {
    <T> TableBuilder<T> get();

}
