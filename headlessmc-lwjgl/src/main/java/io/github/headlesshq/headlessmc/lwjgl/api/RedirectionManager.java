package io.github.headlesshq.headlessmc.lwjgl.api;

import java.util.function.Supplier;

public interface RedirectionManager extends Redirection {
    Object invoke(String desc, Class<?> type, Object obj, Supplier<Redirection> fallback, Object... args)
        throws Throwable;

    void redirect(String desc, Redirection redirection);

}
