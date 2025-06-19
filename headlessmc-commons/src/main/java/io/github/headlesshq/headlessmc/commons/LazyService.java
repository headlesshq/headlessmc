package io.github.headlesshq.headlessmc.commons;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

public abstract class LazyService<T> extends Service<T> {
    protected boolean initialized;

    public void ensureInitialized() {
        if (!initialized) {
            initialized = true;
            refresh();
        }
    }

    @Override
    public Collection<T> getContents() {
        ensureInitialized();
        return super.getContents();
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        ensureInitialized();
        return super.iterator();
    }

    @Override
    public Stream<T> stream() {
        ensureInitialized();
        return super.stream();
    }

    @Override
    public boolean isEmpty() {
        ensureInitialized();
        return super.isEmpty();
    }

    @Override
    public void add(T value) {
        ensureInitialized();
        super.add(value);
    }

    @Override
    public void clear() {
        ensureInitialized();
        super.clear();
    }

    @Override
    public int size() {
        ensureInitialized();
        return super.size();
    }

}
