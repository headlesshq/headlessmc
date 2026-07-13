package org.lwjgl.system;

/**
 * Minimal stand-in for {@code org.lwjgl.system.Configuration}, mirroring the
 * shape the transformer must preserve: a {@code public static final} field
 * assigned in the static initializer. If the transformer gutted the static
 * initializer (as it does for every other lwjgl class) this field would be
 * {@code null} after loading.
 *
 * @see LwjglInstrumentationTest#testConfigurationStaticInitializerPreserved()
 */
@SuppressWarnings("unused")
public final class Configuration {
    public static final Configuration SHARED_LIBRARY_EXTRACT_PATH =
        new Configuration();

    public Object get() {
        return null;
    }

}
