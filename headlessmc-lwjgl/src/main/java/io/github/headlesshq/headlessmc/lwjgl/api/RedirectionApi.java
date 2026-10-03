package io.github.headlesshq.headlessmc.lwjgl.api;

import io.github.headlesshq.headlessmc.lwjgl.transformer.LwjglTransformer;
import lombok.experimental.UtilityClass;
import io.github.headlesshq.headlessmc.lwjgl.RedirectionManagerImpl;

@UtilityClass
public class RedirectionApi {
    /*
        Not using a ServiceLoader for now, modularized environments with
        multiple ClassLoaders cause some issues.
    */
    private static final RedirectionManager REDIRECTION_MANAGER =
        new RedirectionManagerImpl();

    /**
     * @return an implementation of the {@link RedirectionManager}.
     */
    public static RedirectionManager getRedirectionManager() {
        return REDIRECTION_MANAGER;
    }

    /**
     * {@link LwjglTransformer}.
     *
     * @see Redirection
     */
    @SuppressWarnings("unused") // used by the transformer
    public static Object invoke(Object obj, String desc, Class<?> type,
                                Object... args) throws Throwable {
        return REDIRECTION_MANAGER.invoke(obj, desc, type, args);
    }

}
