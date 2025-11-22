package io.github.headlesshq.headlessmc.auth;

import com.google.gson.*;
import lombok.CustomLog;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@CustomLog
public class AccountJsonLoader {
    public void save(Path location, List<ValidatedAccount> accounts) throws IOException {
        saveMixed(location, accounts, new ArrayList<>());
    }

    public void saveMixed(Path location, List<ValidatedAccount> msaAccounts, List<YggdrasilAccount> yggdrasilAccounts) throws IOException {
        JsonArray array = new JsonArray();
        for (ValidatedAccount account : msaAccounts) {
            try {
                JsonObject object = account.toJson();
                array.add(object);
            } catch (Exception e) {
                log.error("Failed to serialize JavaSession " + account + ": " + e.getMessage(), e);
            }
        }
        for (YggdrasilAccount account : yggdrasilAccounts) {
            try {
                JsonObject object = account.toJson();
                array.add(object);
            } catch (Exception e) {
                log.error("Failed to serialize YggdrasilAccount " + account + ": " + e.getMessage(), e);
            }
        }

        JsonObject object = new JsonObject();
        object.add("accounts", array);
        String string = new GsonBuilder().setPrettyPrinting().create().toJson(object);
        try (OutputStream os = Files.newOutputStream(location)) {
            os.write(string.getBytes(StandardCharsets.UTF_8));
        }
    }

    public List<ValidatedAccount> load(Path location) throws IOException {
        AccountLoadResult result = loadMixed(location);
        return result.msaAccounts;
    }

    public AccountLoadResult loadMixed(Path location) throws IOException {
        JsonElement je;
        try (InputStream is = Files.newInputStream(location);
             InputStreamReader ir = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            je = JsonParser.parseReader(ir);
        }

        if (je == null || je.isJsonNull()) {
            return new AccountLoadResult(new ArrayList<>(), new ArrayList<>());
        }

        if (!je.isJsonObject()) {
            throw new IOException("Not a JSON object: " + je);
        }

        JsonElement accountsArray = je.getAsJsonObject().get("accounts");
        if (accountsArray == null || !accountsArray.isJsonArray()) {
            return new AccountLoadResult(new ArrayList<>(), new ArrayList<>());
        }

        List<ValidatedAccount> msaAccounts = new ArrayList<>();
        List<YggdrasilAccount> yggdrasilAccounts = new ArrayList<>();
        
        for (JsonElement element : accountsArray.getAsJsonArray()) {
            if (element instanceof JsonObject) {
                JsonObject obj = element.getAsJsonObject();
                try {
                    // 检查账户类型
                    if (obj.has("type") && "yggdrasil".equals(obj.get("type").getAsString())) {
                        YggdrasilAccount account = YggdrasilAccount.fromJson(obj);
                        yggdrasilAccounts.add(account);
                    } else {
                        // 默认是 Microsoft 账户
                        ValidatedAccount account = ValidatedAccount.fromJson(obj);
                        msaAccounts.add(account);
                    }
                } catch (Exception e) {
                    log.error("Couldn't read account in .accounts.json " + e.getMessage(), e);
                }
            }
        }

        return new AccountLoadResult(msaAccounts, yggdrasilAccounts);
    }

    public static class AccountLoadResult {
        public final List<ValidatedAccount> msaAccounts;
        public final List<YggdrasilAccount> yggdrasilAccounts;

        public AccountLoadResult(List<ValidatedAccount> msaAccounts, List<YggdrasilAccount> yggdrasilAccounts) {
            this.msaAccounts = msaAccounts;
            this.yggdrasilAccounts = yggdrasilAccounts;
        }
    }

}
