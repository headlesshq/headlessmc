package io.github.headlesshq.headlessmc.version;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class MergedVersion implements Version {
    private final List<Version> versions;

    @Override
    public @Nullable String getParentName() {
        return "";
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public int getJava() {
        return 0;
    }

    @Override
    public @Nullable String getMainClass() {
        return "";
    }

    @Override
    public Arguments getGameArguments() {
        return null;
    }

    @Override
    public Arguments getJvmArguments() {
        return null;
    }

    @Override
    public @Unmodifiable Map<String, Download> getDownloads() {
        return new HashMap<>();
    }

    @Override
    public @Unmodifiable List<Library> getLibraries() {
        return new ArrayList<>();
    }

}
