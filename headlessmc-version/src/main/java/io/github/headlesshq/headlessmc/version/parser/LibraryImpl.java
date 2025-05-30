package io.github.headlesshq.headlessmc.version.parser;

import io.github.headlesshq.headlessmc.version.ExtractionRules;
import io.github.headlesshq.headlessmc.version.Library;
import io.github.headlesshq.headlessmc.version.Rule;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

final class LibraryImpl implements Library {
    @Override
    public Rule getRule() {
        return null;
    }

    @Override
    public @Nullable ExtractionRules getExtractionRules() {
        return null;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public @Nullable String getSha1() {
        return "";
    }

    @Override
    public @Nullable Long getSize() {
        return 0L;
    }

    @Override
    public @Nullable URL getUrl() {
        return null;
    }

}
