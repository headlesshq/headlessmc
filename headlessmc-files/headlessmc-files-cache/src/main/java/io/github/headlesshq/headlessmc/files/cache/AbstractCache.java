package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
abstract class AbstractCache<V> implements Cache<V> {
    final Object lock;
    final long version;

    abstract CacheState<V> getState() throws HeadlessMcException;

    abstract CacheState<V> setState(V value) throws HeadlessMcException;

    @Override
    public abstract String toString();

    @Override
    public Optional<V> modify(Consumer<V> consumer) throws HeadlessMcException {
        synchronized (lock) {
            Optional<V> previous = get();
            previous.ifPresent(consumer);
            // call get again, in case the consumer called set (should we verify and throw instead?)
            get().ifPresent(this::setSneaky);
            return previous;
        }
    }

    @Override
    public Optional<V> maybeModify(Function<V, Boolean> action) throws HeadlessMcException {
        synchronized (lock) {
            Optional<V> previous = get();
            if (previous.map(action).orElse(false)) {
                // call get again, in case the Function called set (should we verify and throw instead?)
                get().ifPresent(this::setSneaky);
            }

            return previous;
        }
    }

    @Override
    public Optional<V> modify(UnaryOperator<V> action) throws HeadlessMcException {
        synchronized (lock) {
            Optional<V> previous = get();
            // (should we verify and throw if UnaryOperator has called .set instead?)
            previous.map(action).ifPresent(this::setSneaky);
            return previous;
        }
    }

    @Override
    public <R> Cache<R> mutableView(Mapping<V, R> mapping, Mapping<R, V> reverseMapping) {
        return new DelegatingCache<>(
            this::iterator,
            this::getStateSneaky,
            mapping,
            lock,
            this::setStateSneaky,
            reverseMapping,
            version,
            this::clear
        );
    }

    @Override
    public <R> View<R> view(Mapping<V, R> mapping) {
        return new CacheView<>(this::iterator, this::getStateSneaky, mapping, lock);
    }

    @Override
    public long getVersion() {
        return version;
    }

    @SneakyThrows
    private void setSneaky(V v) {
        this.set(v);
    }

    @SneakyThrows
    private CacheState<V> getStateSneaky() {
        return getState();
    }

    @SneakyThrows
    private CacheState<V> setStateSneaky(V value) {
        return this.setState(value);
    }

    record CacheStateImpl<V>(@Nullable Object parent, Optional<V> value) implements CacheState<V> { }

    @RequiredArgsConstructor
    static class CacheView<V, R> implements View<R> {
        final AtomicReference<@Nullable CacheState<R>> state = new AtomicReference<>();
        final Supplier<Iterator<V>> parentIterator;
        final Supplier<CacheState<V>> parentState;
        final Mapping<V, R> mapping;
        final Object lock;

        @Override
        public Optional<R> get() {
            CacheState<V> cacheState = parentState.get();
            CacheState<R> viewState = state.get();
            if (viewState != null && viewState.parent() == cacheState) {
                return viewState.value();
            }

            // execute locked
            return getState().value();
        }

        protected CacheState<R> getState() {
            synchronized (lock) {
                CacheState<V> cacheState = parentState.get();
                CacheState<R> viewState = state.get();
                if (viewState != null && viewState.parent() == cacheState) {
                    return viewState;
                }

                CacheState<R> finalViewState = viewState;
                viewState = new CacheStateImpl<>(
                    cacheState,
                    cacheState.value().map(value -> mapping.map(
                        value,
                        finalViewState == null ? null : finalViewState.value().orElse(null)
                    ))
                );

                state.set(viewState);
                return viewState;
            }
        }

        @Override
        public <T> View<T> view(Mapping<R, T> mapping) {
            return new CacheView<>(this::iterator, this::getState, mapping, lock);
        }

        @Override
        public Stream<R> stream() {
            return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED),
                false
            );
        }

        @Override
        public Iterator<R> iterator() {
            synchronized (lock) {
                ViewIterator iterator = new ViewIterator(parentIterator.get());
                iterator.previous = Optional.ofNullable(state.get()).flatMap(CacheState::value).orElse(null);
                return iterator;
            }
        }

        @RequiredArgsConstructor
        class ViewIterator implements Iterator<R> {
            private final Iterator<V> iterator;
            private @Nullable R previous;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public R next() {
                V next = iterator.next();
                R value = mapping.map(next, previous);
                previous = value;
                return value;
            }
        }

        @Override
        public String toString() {
            CacheState<R> state = this.state.get();
            return "View{" + (state == null ? "null" : state.value()) + "}";
        }
    }

    static class DelegatingCache<V, R> extends CacheView<V, R> implements Cache<R> {
        final Function<V, CacheState<V>> setter;
        final Mapping<R, V> inverseMapping;
        final long version;
        final Runnable clear;

        public DelegatingCache(
            Supplier<Iterator<V>> parentIterator,
            Supplier<CacheState<V>> parentState,
            Mapping<V, R> mapping,
            Object lock,
            Function<V, CacheState<V>> setter,
            Mapping<R, V> inverseMapping,
            long version,
            Runnable clear
        ) {
            super(parentIterator, parentState, mapping, lock);
            this.setter = setter;
            this.inverseMapping = inverseMapping;
            this.version = version;
            this.clear = clear;
        }

        @Override
        public Optional<R> set(R value) throws HeadlessMcException {
            synchronized (lock) {
                CacheState<R> priorState = getState();
                setState(value);
                return priorState.value();
            }
        }

        protected CacheState<R> setState(R value) throws HeadlessMcException {
            synchronized (lock) {
                CacheState<V> parentState = this.parentState.get();
                CacheState<R> priorState = getState();
                CacheState<V> finalParentState = parentState;
                Optional<V> newV = priorState.value().map(v ->
                    inverseMapping.map(
                        v,
                        finalParentState.value().orElse(null)
                    )
                );

                parentState = newV.map(setter).orElse(parentState);
                CacheState<R> result = new CacheStateImpl<>(parentState, Optional.of(value));
                this.state.set(result);
                return result;
            }
        }

        @Override
        public Optional<R> modify(Consumer<R> consumer) {
            synchronized (lock) {
                CacheState<R> priorState = getState();
                priorState.value().ifPresent(consumer);
                CacheState<R> current = getState();
                current.value().ifPresent(this::setSneaky);
                return priorState.value();
            }
        }

        @Override
        public Optional<R> maybeModify(Function<R, Boolean> action) {
            synchronized (lock) {
                CacheState<R> priorState = getState();
                if (priorState.value().map(action).orElse(false)) {
                    CacheState<R> current = getState();
                    current.value().ifPresent(this::setSneaky);
                }

                return priorState.value();
            }
        }

        @Override
        public Optional<R> modify(UnaryOperator<R> action) {
            synchronized (lock) {
                CacheState<R> priorState = getState();
                priorState.value().map(action).ifPresent(this::setSneaky);
                return priorState.value();
            }
        }

        @Override
        public Optional<R> clear() {
            synchronized (lock) {
                CacheState<R> priorState = getState();
                state.set(null);
                clear.run();
                return priorState.value();
            }
        }

        @Override
        public <R1> Cache<R1> mutableView(Mapping<R, R1> mapping, Mapping<R1, R> reverseMapping) {
            return new DelegatingCache<>(
                this::iterator, this::getState, mapping, lock, this::setStateSneaky, reverseMapping, version, this::clear
            );
        }

        @Override
        public long getVersion() {
            return version;
        }

        @SneakyThrows
        private void setSneaky(R r) {
            this.set(r);
        }

        @SneakyThrows
        private CacheState<R> setStateSneaky(R value) {
            return this.setState(value);
        }

        @Override
        public String toString() {
            CacheState<R> state = this.state.get();
            return "CacheView{" + (state == null ? "null" : state.value()) + "}";
        }
    }

}
