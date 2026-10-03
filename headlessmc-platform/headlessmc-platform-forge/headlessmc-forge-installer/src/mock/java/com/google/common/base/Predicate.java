package com.google.common.base;

@FunctionalInterface
public interface Predicate<T> {
    boolean test(T t);

}
