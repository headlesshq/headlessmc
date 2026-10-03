package io.github.headlesshq.headlessmc.forge.installer;

import lombok.experimental.UtilityClass;

import java.util.function.Predicate;

@UtilityClass
final class Predicates {
    static final String GOOGLE = "com.google.common.base.Predicate";

    static boolean isGooglePredicate(Class<?> clazz) {
        return clazz.getName().equals(GOOGLE);
    }

    static Object getGooglePredicate() {
        return (com.google.common.base.Predicate<Object>) input -> true;
    }

    static Object getJavaPredicate() {
        return (Predicate<Object>) o -> true;
    }

    static Class<?> getPredicateClass() throws ClassNotFoundException {
        try {
            return Class.forName(GOOGLE);
        } catch (ClassNotFoundException e) {
            return getJavaPredicateClass();
        }
    }

    static Class<?> getJavaPredicateClass() throws ClassNotFoundException {
        return Class.forName("java.util.function.Predicate");
    }

}
