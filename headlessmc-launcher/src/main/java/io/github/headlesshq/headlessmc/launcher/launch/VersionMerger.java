package io.github.headlesshq.headlessmc.launcher.launch;

import io.github.headlesshq.headlessmc.launcher.util.Pair;
import io.github.headlesshq.headlessmc.launcher.version.*;
import io.github.headlesshq.headlessmc.launcher.version.family.FamilyUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@CustomLog
class VersionMerger extends DelegatingVersion {
    public VersionMerger(Version version) {
        super(version);
    }

    @Override
    public String getAssets() {
        return get(Version::getAssets);
    }

    @Override
    public Integer getJava() {
        Integer result = get(Version::getJava);
        if (result == null) {
            log.error("Version didn't specify a Java Version!!!");
        }

        return result == null ? 8 : result;
    }

    @Override
    public String getMainClass() {
        return get(Version::getMainClass);
    }

    @Override
    public String getAssetsUrl() {
        return get(Version::getAssetsUrl);
    }

    @Override
    public List<Library> getLibraries() {
        return mergeLibraries();
    }

    @Override
    public List<Argument> getArguments() {
        if (version.isNewArgumentFormat()) {
            return merge(Version::getArguments);
        } else {
            return get(Version::getArguments);
        }
    }

    @Override
    public VersionExecutable getClientDownload() {
        return get(Version::getClientDownload);
    }

    @Override
    public VersionExecutable getServerDownload() {
        return get(Version::getServerDownload);
    }

    @Override
    public @Nullable Logging getLogging() {
        return get(Version::getLogging);
    }

    private <T> List<T> merge(Function<Version, List<T>> func) {
        List<T> result = new ArrayList<>();
        FamilyUtil.iterateTopDown(version, v -> {
            List<T> list = func.apply(v);
            if (list != null) {
                result.addAll(list);
            }
        });

        return result;
    }

    private <T> T get(Function<Version, T> func) {
        return FamilyUtil.iterateParents(version, () -> null, func);
    }

    private List<Library> mergeLibraries() {
        List<Pair<Library, Version>> result = new ArrayList<>();
        FamilyUtil.iterateTopDown(version, v -> {
            for (Library library : v.getLibraries()) {
                // The behaviour seems to be that child versions overwrite
                // libraries of their parent version with the same package and name.
                // overwriting.getValue().equals(v) is there because a version itself might
                // contain libraries with similar packages and names, like this:
                // io.netty:netty-transport-native-epoll:4.1.97.Final:linux-x86_64
                // io.netty:netty-transport-native-epoll:4.1.97.Final:linux-aarch_64
                result.removeIf(overwriting -> !overwriting.getValue().equals(v)
                    && overwriting.getKey().getPackage().equals(library.getPackage())
                    && overwriting.getKey().getNameAfterPackage().equals(library.getNameAfterPackage()));
                result.add(new Pair<>(library, v));
            }
        });

        return result.stream().map(Pair::getKey).collect(Collectors.toList());
    }

}
