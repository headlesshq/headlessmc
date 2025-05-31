package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.version.ExtractionRules;
import io.github.headlesshq.headlessmc.version.Library;
import io.github.headlesshq.headlessmc.version.Rule;
import jakarta.inject.Inject;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Parses a library entry:
 * <pre>
 {
     "downloads": {
         "classifiers": {
             "artifact": {
                "path": "io/netty/netty-transport-native-epoll/...",
                "sha1": "82f94d0a9d837f6b6a580379373310ff7288c0f8",
                "size": 42321,
                "url": "https://libraries.minecraft.net/..."
             }
            "natives-linux": {
                "path": "net/java/jinput/jinput-platform/...",
                ...
            },
            ...
        },
    "extract": {
        "exclude": [
            "META-INF/"
        ]
    },
    "name": "net.java.jinput:jinput-platform:2.0.5",
    "natives": {
        "linux": "natives-linux",
        "osx": "natives-osx",
        "windows": "natives-windows"
    }
 }</pre>
 */
@CustomLog
@RequiredArgsConstructor(onConstructor_ = {@Inject})
final class LibraryParser {
    private final ExtractorParser extractorParser;
    private final NativesParser nativesParser;
    private final RuleParser ruleParser;

    public List<Library> parseLibraries(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return Collections.emptyList();
        }

        List<Library> result = new ArrayList<>(element.getAsJsonArray().size());
        for (JsonElement library : element.getAsJsonArray()) {
            if (library.isJsonObject()) {
                result.addAll(parse(library.getAsJsonObject()));
            }
        }

        return result;
    }

    public List<Library> parse(JsonObject json) {
        Rule rule = ruleParser.parse(json.get("rules"));
        ExtractionRules extractor = extractorParser.parse(json.get("extract"));
        Map<String, String> natives = nativesParser.parse(json.get("natives"));
        List<Library> result = new ArrayList<>(natives.isEmpty() ? 1 : natives.size() + 1);
        String name = json.get("name").getAsString();
        String baseUrl = JsonUtil.getString(json, "url");

        JsonElement downloads = json.get("downloads");
        if (downloads != null && downloads.isJsonObject()) {
            JsonElement artifact = downloads.getAsJsonObject().get("artifact");
            if (artifact != null && artifact.isJsonObject()) {
                JsonObject jo = artifact.getAsJsonObject();
                String url = JsonUtil.getString(jo, "url");
                String path = JsonUtil.getString(jo, "path");
                String sha1 = JsonUtil.getString(jo, "sha1");
                Long size = JsonUtil.getLong(jo, "size");
                URI uri = url == null ? null : URI.create(url);
                result.add(new LibraryImpl(natives, null, name, rule, baseUrl, sha1, size, uri, path, false));
            }

            JsonElement classifiers = downloads.getAsJsonObject().get("classifiers");
            if (classifiers != null && classifiers.isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : classifiers.getAsJsonObject().entrySet()) {
                    Map.Entry<String, String> nativeEntry = getNativeEntry(natives, e.getKey());
                    if (nativeEntry == null || !e.getValue().isJsonObject()) {
                        continue;
                    }

                    // nativeWithReplace for this library, ${arch} is replaced
                    String nativeName = e.getKey();
                    // name of the os
                    String os = nativeEntry.getKey();
                    // native name containing ${arch} to be replaced with 32/64
                    String nativeWithReplace = nativeEntry.getValue();
                    Rule osRule = (osIn, f) -> {
                        // check that we have the right os
                        if (os.equalsIgnoreCase(osIn.getType().getName())
                            // check that we have the right arch version
                            && nativeWithReplace.replace("${arch}", osIn.is64bit() ? "64" : "32").equals(nativeName)) {
                            return rule.apply(osIn, f);
                        }

                        return Rule.Action.DISALLOW;
                    };

                    ExtractionRules nativeExtractor = extractor;
                    // if there was no extraction rule specified.
                    if (nativeExtractor == null) {
                        nativeExtractor = new ExtractionRulesImpl(Collections.emptyList());
                    }

                    JsonObject jo = e.getValue().getAsJsonObject();
                    String url = JsonUtil.getString(jo, "url");
                    String path = JsonUtil.getString(jo, "path");
                    String sha1 = JsonUtil.getString(jo, "sha1");
                    Long size = JsonUtil.getLong(jo, "size");
                    URI uri = url == null ? null : URI.create(url);
                    result.add(new LibraryImpl(natives, nativeExtractor, name, osRule, baseUrl, sha1, size, uri, path, true));
                }
            }
        }

        if (result.isEmpty()) {
            result.add(new LibraryImpl(natives, extractor, name, rule, baseUrl, null, null, null, null, false));
        }

        return result;
    }

    private Map.Entry<String, String> getNativeEntry(Map<String, String> map, String classifier) {
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getValue().replace("${arch}", "32").equals(classifier)
                || e.getValue().replace("${arch}", "64").equals(classifier)) {
                return e;
            }
        }

        return null;
    }

}
