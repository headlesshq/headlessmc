package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.version.ExtractionRules;
import jakarta.enterprise.context.ApplicationScoped;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
final class ExtractorParser {
    public @Nullable ExtractionRules parse(@Nullable JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }

        JsonObject extractJo = element.getAsJsonObject();
        JsonArray array = JsonUtil.toArray(extractJo.get("exclude"));
        List<String> ex = new ArrayList<>(array.size());
        for (JsonElement exclusion : array) {
            ex.add(exclusion.getAsString());
        }

        return new ExtractionRulesImpl(ex);
    }

}
