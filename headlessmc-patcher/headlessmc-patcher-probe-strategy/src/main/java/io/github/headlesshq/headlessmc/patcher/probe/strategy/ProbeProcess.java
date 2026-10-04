package io.github.headlesshq.headlessmc.patcher.probe.strategy;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcess;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessException;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.time.Duration;

@RequiredArgsConstructor
final class ProbeProcess implements AutoCloseable {
    private final JavaProcess process;
    private final BufferedReader out;
    private final PrintStream in;

    public String getCommonSuperClass(String type1, String type2) {
        try {
            in.println(type1);
            in.println(type2);
            String line = out.readLine();
            if (line == null) {
                throw new HeadlessMcIOException("Probe process ended");
            }

            if ("error".equalsIgnoreCase(line)) {
                throw new PatchException("Failed to find super class of " + type1 + ", " + type2);
            }

            return line;
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to use probe process", e);
        }
    }

    @Override
    public void close() {
        try {
            out.close();
            in.close();
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to close probe process in/out");
        } finally {
            try {
                process.kill();
                process.waitFor(Duration.ofSeconds(1));
            } catch (JavaProcessException ignored) {
                process.killForcibly();
            }
        }
    }

    public static ProbeProcess of(JavaProcess process) {
        Process actualProcess = process.getProcess()
            .orElseThrow(() -> new PatchException(
                "Virtual java process " + process.getId() + " cannot be used for probing"
            ));

        BufferedReader out = new BufferedReader(new InputStreamReader(actualProcess.getInputStream()));
        PrintStream in = new PrintStream(actualProcess.getOutputStream());
        return new ProbeProcess(process, out, in);
    }

}
