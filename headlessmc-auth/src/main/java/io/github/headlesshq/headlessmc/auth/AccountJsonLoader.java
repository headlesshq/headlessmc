package io.github.headlesshq.headlessmc.auth;

import com.google.gson.*;
import lombok.CustomLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@CustomLog
public class AccountJsonLoader {
    public void save(Path location, List<ValidatedAccount> accounts) throws IOException {
        JsonArray array = new JsonArray();
        for (ValidatedAccount account : accounts) {
            try {
                JsonObject object = account.toJson();
                array.add(object);
            } catch (Exception e) {
                log.error("Failed to serialize JavaSession " + account + ": " + e.getMessage(), e);
            }
        }

        JsonObject object = new JsonObject();
        object.add("accounts", array);
        String string = new GsonBuilder().setPrettyPrinting().create().toJson(object);
        Files.createDirectories(location.getParent());
        try (OutputStream os = Files.newOutputStream(location)) {
            os.write(string.getBytes(StandardCharsets.UTF_8));
        }
    }

    public List<ValidatedAccount> load(Path location) throws IOException {
        if (!Files.exists(location)) {
            return new ArrayList<>();
        }

        JsonElement je;
        try (InputStream is = Files.newInputStream(location);
             InputStreamReader ir = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            je = JsonParser.parseReader(ir);
        }

        if (je == null || je.isJsonNull()) {
            return new ArrayList<>(0);
        }

        if (!je.isJsonObject()) {
            throw new IOException("Not a JSON object: " + je);
        }

        JsonElement accountsArray = je.getAsJsonObject().get("accounts");
        if (accountsArray == null || !accountsArray.isJsonArray()) {
            return new ArrayList<>(0);
        }

        List<ValidatedAccount> accounts = new ArrayList<>(accountsArray.getAsJsonArray().size());
        for (JsonElement element : accountsArray.getAsJsonArray()) {
            if (element instanceof JsonObject) {
                try {
                    ValidatedAccount loadedSession = ValidatedAccount.fromJson(element.getAsJsonObject());
                    accounts.add(loadedSession);
                } catch (Exception e) {
                    log.error("Couldn't read account in .accounts.json " + e.getMessage(), e);
                }
            }
        }

        return accounts;
    }

}
