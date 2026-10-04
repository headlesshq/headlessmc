package io.github.headlesshq.headlessmc.java;

/**
 * Represents a Java installation.
 *
 * @param name       the name of the installation.
 * @param version    the Java specification version of the installation.
 * @param home       the (JAVA_HOME) folder containing the installation.
 * @param executable the executable to use to run java (/bin/java).
 * @param current    if this objects represents the Java used to run this JVM.
 * @param source     the {@link JavaSource#sort()} of the source used to find this installation.
 */
public record Java(
    String name,
    int version,
    SafePath home,
    SafePath executable,
    boolean current,
    int source
) implements Comparable<Java> {
    /**
     * Magic constant that describes {@code java.specification.version} 0.9,
     * which can potentially be returned on Android.
     */
    public static final int JAVA_VERSION_0_9 = -9;
    public static final int JAVA_VERSION_UNKNOWN = -100;

    @Override
    public int compareTo(Java o) {
        int result = Integer.compare(o.version, version);
        if (result == 0) {
            result = Integer.compare(source, o.source);
            if (result == 0) {
                return Boolean.compare(current, o.current);
            }
        }

        return result;
    }

}
