package io.github.headlesshq.headlessmc.launcher.profile;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.util.TypeLiteral;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * A CDI {@link Event} for tests: records what is fired and passes it to observers registered with
 * {@link #observe(Consumer)}, which can throw to veto, like real synchronous observers.
 *
 * @param <T> the type of the event.
 */
public class TestEvent<T> implements Event<T> {
    private final List<T> fired = new ArrayList<>();
    private final List<Consumer<T>> observers = new ArrayList<>();

    /**
     * @return the events that have been fired, including those an observer threw for.
     */
    public List<T> fired() {
        return fired;
    }

    public TestEvent<T> observe(Consumer<T> observer) {
        observers.add(observer);
        return this;
    }

    @Override
    public void fire(T event) {
        fired.add(event);
        observers.forEach(observer -> observer.accept(event));
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event, NotificationOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Event<T> select(Annotation... qualifiers) {
        return this;
    }

    @Override
    public <U extends T> Event<U> select(Class<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Event<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

}
