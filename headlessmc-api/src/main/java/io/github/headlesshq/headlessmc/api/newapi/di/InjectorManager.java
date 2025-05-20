package io.github.headlesshq.headlessmc.api.newapi.di;

import java.util.Deque;

public interface InjectorManager extends Injector {
    Deque<Injector> getInjectors();

}
