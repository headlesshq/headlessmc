package io.github.headlesshq.headlessmc.java.inmemory.programs;

/**
 * Main classes run by the InMemoryProcessBuilderTest.
 * They are loaded by the in-memory classloader, so they report their results through system properties.
 */
public final class Programs {
    private Programs() {
    }

    public static final class Echo {
        public static void main(String[] args) {
            System.setProperty("hmc.test.echo", String.join(",", args) + ":" + System.getProperty("hmc.test.property"));
            System.setProperty(
                "hmc.test.loader",
                String.valueOf(Echo.class.getClassLoader() == Thread.currentThread().getContextClassLoader())
            );
        }
    }

    public static final class Throws {
        public static void main(String[] args) {
            throw new IllegalStateException("expected by test");
        }
    }

    public static final class StartsThread {
        public static void main(String[] args) {
            new Thread(() -> {
                try {
                    Thread.sleep(Long.parseLong(args[0]));
                } catch (InterruptedException e) {
                    System.setProperty("hmc.test.thread", "interrupted");
                    return;
                }

                System.setProperty("hmc.test.thread", "done");
            }).start();
        }
    }

}
