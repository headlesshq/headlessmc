package io.github.headlesshq.headlessmc.launcher.server;

import lombok.RequiredArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

// could be useful to find out since which version the EULA needs to be accepted
/**
 * Represents the Mc EULA file which contains the EULA
 * that needs to be accepted before launching the server.
 */
@RequiredArgsConstructor
public class Eula {
    private final Path file;

    public String read() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file)))) {
            return String.join("\n", reader.readAllLines());
        } catch (IOException e) {
            throw new ServerException("Failed to read EULA", e);
        }
    }

    public void accept() {
        String content = read();
        if (content.contains("eula=true") && !content.contains("eula=false")) {
            return;
        }

        if (!content.contains("eula=false")) {
            throw new ServerException("Failed to accept EULA, no 'eula=false' found!");
        }

        content = content.replace("eula=false", "eula=true");
        try {
            Files.writeString(file, content);
        } catch (IOException e) {
            throw new ServerException("Failed to accept EULA", e);
        }
    }

    public boolean isAccepted() {
        if (!exists()) {
            return false;
        }

        try {
            String content = read().toLowerCase(Locale.ENGLISH);
            return content.contains("eula=true") && !content.contains("eula=false");
        } catch (ServerException ignored) {
            return false;
        }
    }

    public boolean exists() {
        return Files.exists(file);
    }

}
