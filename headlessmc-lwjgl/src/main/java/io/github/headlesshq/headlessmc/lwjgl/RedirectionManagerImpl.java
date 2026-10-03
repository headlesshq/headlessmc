package io.github.headlesshq.headlessmc.lwjgl;

import io.github.headlesshq.headlessmc.lwjgl.api.Redirection;
import io.github.headlesshq.headlessmc.lwjgl.api.RedirectionManager;
import io.github.headlesshq.headlessmc.lwjgl.redirections.CastRedirection;
import io.github.headlesshq.headlessmc.lwjgl.redirections.DefaultRedirections;
import io.github.headlesshq.headlessmc.lwjgl.redirections.LwjglRedirections;
import io.github.headlesshq.headlessmc.lwjgl.redirections.ObjectRedirection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RedirectionManagerImpl implements RedirectionManager {
    private final Map<String, Redirection> redirects = new HashMap<>();
    private final Redirection object = new ObjectRedirection(this);
    private final Redirection cast = new CastRedirection(this);

    public RedirectionManagerImpl() {
        LwjglRedirections.register(this);
    }

    @Override
    public void redirect(String desc, Redirection redirection) {
        redirects.put(desc, redirection);
    }

    @Override
    public Object invoke(Object obj, String desc, Class<?> type, Object... args) throws Throwable {
        return invoke(desc, type, obj, () -> getFallback(desc, type), args);
    }

    @Override
    public Object invoke(String desc, Class<?> type, Object obj, Supplier<Redirection> fb, Object... args)
        throws Throwable {
        Redirection redirection = redirects.get(desc);
        if (redirection == null) {
            redirection = fb.get();
        }

        return redirection.invoke(obj, desc, type, args);
    }

    private Redirection getFallback(String desc, Class<?> type) {
        if (desc.startsWith(Redirection.CAST_PREFIX)) {
            // TODO: currently cast redirection looks like this:
            //  <cast> java/lang/String
            //  <init> <cast> java/lang/String
            //  It contains no information about the calling class!
            return cast;
        }

        return DefaultRedirections.fallback(type, object);
    }

}
