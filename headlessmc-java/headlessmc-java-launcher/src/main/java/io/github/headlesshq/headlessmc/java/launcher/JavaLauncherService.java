package io.github.headlesshq.headlessmc.java.launcher;

public interface JavaLauncherService {
    JavaProcessBuilder buildProcess();

    String getName();

}
