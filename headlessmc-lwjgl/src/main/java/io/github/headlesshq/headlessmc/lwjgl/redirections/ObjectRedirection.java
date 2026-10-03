package io.github.headlesshq.headlessmc.lwjgl.redirections;

import lombok.RequiredArgsConstructor;
import lombok.val;
import io.github.headlesshq.headlessmc.lwjgl.api.Redirection;
import io.github.headlesshq.headlessmc.lwjgl.api.RedirectionManager;
import io.github.headlesshq.headlessmc.lwjgl.util.DescriptionUtil;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;

@RequiredArgsConstructor
public class ObjectRedirection implements Redirection {
    private final RedirectionManager manager;

    @Override
    public Object invoke(Object obj, String d, Class<?> type, Object... args) {
        if (type.isInterface()) {
            return Proxy.newProxyInstance(
                type.getClassLoader(), new Class<?>[]{type},
                new ProxyRedirection(manager, DescriptionUtil.getDesc(type)));
        } else if (type.isArray()) {
            int dimension = 0;
            while (type.isArray()) {
                dimension++;
                type = type.getComponentType();
            }

            val dimensions = new int[dimension];
            Arrays.fill(dimensions, 0);
            return Array.newInstance(type, dimensions);
        } else if (Modifier.isAbstract(type.getModifiers())) {
            // TODO: logger for headlessmc-lwjgl?
            System.err.println("Can't return abstract class: " + d);
            return null;
        }

        try {
            val constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (SecurityException | ReflectiveOperationException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            return null;
        }
    }

}
