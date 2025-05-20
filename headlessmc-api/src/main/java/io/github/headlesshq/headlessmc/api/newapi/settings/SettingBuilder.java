package io.github.headlesshq.headlessmc.api.newapi.settings;

import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class SettingBuilder<V> {
    private final Class<V> type;

    private int id;
    private String name;
    private String description;
    private Supplier<V> defaultValueSupplier;
    private Parser<V> parser;

}
