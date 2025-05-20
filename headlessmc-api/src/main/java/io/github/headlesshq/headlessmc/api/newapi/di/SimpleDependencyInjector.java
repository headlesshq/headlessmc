package io.github.headlesshq.headlessmc.api.newapi.di;

import io.github.headlesshq.headlessmc.api.util.ReflectionUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;

@RequiredArgsConstructor
public class SimpleDependencyInjector implements Injector {
    private final Injector injector;

    @Override
    public <T> T getInstance(Class<T> clazz) throws InjectorException {
        if (clazz.isInterface()) {
            throw new InjectorException("Cannot inject an interface");
        }

        Constructor<?> constructor = getConstructor(clazz);
        Object[] parameters = fillParameters(constructor);

        try {
            constructor.setAccessible(true);
            T instance = clazz.cast(constructor.newInstance(parameters));
            init(instance);
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new InjectorException(e);
        }
    }

    @Override
    public boolean isGuaranteedToInject(Class<?> clazz) {
        return false;
    }

    protected boolean isInjectAnnotation(Annotation annotation) {
        return "javax.inject.Inject".equals(annotation.annotationType().getName())
                || "jakarta.inject.Inject".equals(annotation.annotationType().getName())
                || "jakarta.enterprise.inject.Inject".equals(annotation.annotationType().getName());
    }

    private Object[] fillParameters(Executable executable) {
        Object[] parameters = new Object[executable.getParameterCount()];
        for (int i = 0; i < executable.getParameterCount(); i++) {
            parameters[i] = injector.getInstance(executable.getParameterTypes()[i]);
        }

        return parameters;
    }

    private void init(Object instance) throws IllegalAccessException, InvocationTargetException {
        for (Method method : instance.getClass().getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            for (Annotation annotation : method.getAnnotations()) {
                if (isInjectAnnotation(annotation)) {
                    Object[] parameters = fillParameters(method);
                    method.setAccessible(true);
                    method.invoke(instance, parameters);
                    break;
                }
            }
        }

        ReflectionUtil.iterate(instance.getClass(), cls -> {
            initFields(cls, instance);
        });
    }

    private Constructor<?> getConstructor(Class<?> clazz) throws InjectorException {
        Constructor<?> noArgsConstructor = null;
        for (Constructor<?> ctr : clazz.getDeclaredConstructors()) {
            for (Annotation annotation : ctr.getAnnotations()) {
                if (isInjectAnnotation(annotation)) {
                    return ctr;
                }
            }

            if (ctr.getParameterCount() == 0) {
                noArgsConstructor = ctr;
            }
        }

        if (noArgsConstructor != null) {
            return noArgsConstructor;
        } else {
            throw new InjectorException("No @Inject or no-args constructor found in " + clazz.getName());
        }
    }

    @SneakyThrows
    private void initFields(Class<?> clazz, Object instance) {
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            for (Annotation annotation : field.getAnnotations()) {
                if (isInjectAnnotation(annotation)) {
                    field.setAccessible(true);
                    Object value = field.get(instance);
                    if (value == null) {
                        Object newValue = injector.getInstance(field.getType());
                        field.set(instance, newValue);
                    }

                    break;
                }
            }
        }
    }

}
