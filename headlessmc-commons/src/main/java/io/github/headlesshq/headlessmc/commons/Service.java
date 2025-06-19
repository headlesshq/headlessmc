package io.github.headlesshq.headlessmc.commons;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

@Getter
public abstract class Service<T> implements Iterable<T> {
    protected Collection<T> contents = new ArrayList<>(0);

    public static <S, T extends Service<S>> T refresh(T service) {
        service.refresh();
        return service;
    }

    protected abstract Collection<T> update();

    public void refresh() {
        contents = update();
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return contents.iterator();
    }

    public Stream<T> stream() {
        return contents.stream();
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public void add(T value) {
        contents.add(value);
    }

    public void clear() {
        contents = new ArrayList<>(0);
    }

    public int size() {
        return contents.size();
    }

}
