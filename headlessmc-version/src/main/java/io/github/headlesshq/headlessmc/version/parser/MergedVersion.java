package io.github.headlesshq.headlessmc.version.parser;

import io.github.headlesshq.headlessmc.version.*;
import lombok.Data;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MergedVersion implements Version {
    private final List<Version> childToParent;
    private final List<Version> parentToChild;

    public MergedVersion(List<Version> childToParent) {
        if (childToParent.isEmpty()) {
            throw new IllegalArgumentException("Versions cannot be empty");
        }

        this.childToParent = childToParent;
        this.parentToChild = new ArrayList<>(childToParent);
        Collections.reverse(parentToChild);
    }

    public <T> T getFirstNotNull(Function<Version, @Nullable T> getter, String message) {
        for (Version version : childToParent) {
            T result = getter.apply(version);
            if (result != null) {
                return result;
            }
        }

        throw new VersionParseException(message);
    }

    @Override
    public String getParentName() {
        return childToParent.get(childToParent.size() - 1).getName();
    }

    @Override
    public String getName() {
        return childToParent.get(0).getName();
    }

    @Override
    public Integer getJava() {
        return getFirstNotNull(Version::getJava, "java");
    }

    @Override
    public String getMainClass() {
        return getFirstNotNull(Version::getMainClass, "mainClass");
    }

    @Override
    public Arguments getGameArguments() {
        Arguments arguments = getFirstNotNull(Version::getGameArguments, "gameArguments");
        if (arguments.isInNewFormat()) {
            return mergeArguments(Version::getGameArguments);
        } else {
            return arguments;
        }
    }

    @Override
    public Arguments getJvmArguments() {
        Arguments arguments = getFirstNotNull(Version::getJvmArguments, "jvmArguments");
        if (arguments.isInNewFormat()) {
            return mergeArguments(Version::getJvmArguments);
        } else {
            return arguments;
        }
    }

    @Override
    public @Unmodifiable Map<String, Download> getDownloads() {
        Map<String, Download> result = new HashMap<>();
        for (Version version : childToParent) {
            result.putAll(version.getDownloads());
        }

        return result;
    }

    @Override
    public @Unmodifiable List<Library> getLibraries() {
        return mergeLibraries();
    }

    @Override
    public Assets getAssets() {
        return getFirstNotNull(Version::getAssets, "assets");
    }

    @Override
    public Logging getLogging() {
        return getFirstNotNull(Version::getLogging, "logging");
    }

    private Arguments mergeArguments(Function<Version, @Nullable Arguments> getter) {
        LinkedList<Argument> result = new LinkedList<>();
        for (Version version : parentToChild) {
            Arguments args = getter.apply(version);
            if (args != null) {
                for (Argument arg : args) {
                    result.add(arg);
                }
            }
        }

        return new ArgumentsImpl(result, true);
    }

    private List<Library> mergeLibraries() {
        List<Pair<Library, Version>> result = new ArrayList<>();
        for (Version version : parentToChild) {
            for (Library library : version.getLibraries()) {
                // The behaviour seems to be that child versions overwrite
                // libraries of their parent version with the same package and name.
                // overwriting.getValue().equals(v) is there because a version itself might
                // contain libraries with similar packages and names, like this:
                // io.netty:netty-transport-native-epoll:4.1.97.Final:linux-x86_64
                // io.netty:netty-transport-native-epoll:4.1.97.Final:linux-aarch_64
                result.removeIf(overwriting -> !overwriting.getValue().equals(version)
                        && overwriting.getKey().getPackage().equals(library.getPackage())
                        && overwriting.getKey().getNameAfterPackage().equals(library.getNameAfterPackage()));
                result.add(new Pair<>(library, version));
            }
        }

        return result.stream().map(Pair::getKey).collect(Collectors.toList());
    }

    @Data
    private static class Pair<K, V> {
        private final K key;
        private final V value;
    }

}
