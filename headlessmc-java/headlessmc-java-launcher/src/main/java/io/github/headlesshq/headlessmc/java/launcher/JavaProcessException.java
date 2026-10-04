package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import lombok.experimental.StandardException;

@StandardException
public class JavaProcessException extends HeadlessMcException {
    @StandardException
    public static class InstallationException extends JavaProcessException {

    }

}
