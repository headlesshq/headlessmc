package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import io.github.headlesshq.headlessmc.version.Library;
import jakarta.inject.Inject;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        val rule = ruleParser.parse(json.get("rules"));
        val extractor = extractorParser.parse(json.get("extract"));
        val natives = nativesParser.parse(json.get("natives"));
        val result = new ArrayList<Library>(natives.isEmpty() ? 1 : natives.size() + 1);
        val name = json.get("name").getAsString();
        val baseUrl = JsonUtil.getString(json, "url");

        val downloads = json.get("downloads");
        if (downloads != null && downloads.isJsonObject()) {
            val artifact = downloads.getAsJsonObject().get("artifact");
            if (artifact != null && artifact.isJsonObject()) {
                JsonObject jo = artifact.getAsJsonObject();
                String url = JsonUtil.getString(jo, "url");
                String path = JsonUtil.getString(jo, "path");
                String sha1 = JsonUtil.getString(jo, "sha1");
                Long size = JsonUtil.getLong(jo, "size");
                result.add(new LibraryImpl(natives, Extractor.NO_EXTRACTION, name, rule, baseUrl, sha1, size, url, path, false));
            }

            val classifiers = downloads.getAsJsonObject().get("classifiers");
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

                    Extractor nativeExtractor = extractor;
                    // if there was no extraction rule specified.
                    if (!extractor.isExtracting()) {
                        nativeExtractor = new ExtractorImpl();
                    }

                    JsonObject jo = e.getValue().getAsJsonObject();
                    String url = JsonUtil.getString(jo, "url");
                    String path = JsonUtil.getString(jo, "path");
                    String sha1 = JsonUtil.getString(jo, "sha1");
                    Long size = JsonUtil.getLong(jo, "size");
                    result.add(new LibraryImpl(natives, nativeExtractor, name, osRule, baseUrl, sha1, size, url, path, true));
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
