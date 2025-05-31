package io.github.headlesshq.headlessmc.launcher.version;

import com.google.gson.JsonElement;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import lombok.CustomLog;
import lombok.val;

import java.util.ArrayList;

@CustomLog
class ExtractorFactory {
    public Extractor parse(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return Extractor.NO_EXTRACTION;
        }

        val extractJo = element.getAsJsonObject();
        val array = JsonUtil.toArray(extractJo.get("exclude"));
        val ex = new ArrayList<String>(array.size());
        for (JsonElement exclusion : array) {
            ex.add(exclusion.getAsString());
        }

        return new ExtractorImpl(ex);
    }

}
