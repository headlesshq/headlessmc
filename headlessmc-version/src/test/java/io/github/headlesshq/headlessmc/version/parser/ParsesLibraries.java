package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.version.Library;

import java.util.List;

interface ParsesLibraries {
    default LibraryParser getFactory() {
        RuleParser rp = new RuleParser();
        ExtractorParser ep = new ExtractorParser();
        NativesParser np = new NativesParser();
        return new LibraryParser(ep, np, rp);
    }

    default List<Library> parse(JsonObject jsonObject) {
        return getFactory().parse(jsonObject);
    }

}
