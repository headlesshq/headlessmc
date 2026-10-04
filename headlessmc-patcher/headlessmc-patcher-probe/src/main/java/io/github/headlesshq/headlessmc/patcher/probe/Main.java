package io.github.headlesshq.headlessmc.patcher.probe;

import org.jetbrains.annotations.VisibleForTesting;

import java.io.*;

public class Main {
    public static void main(String[] args) throws IOException {
        run(System.in, System.out);
    }

    // TODO: test
    @SuppressWarnings("SameParameterValue")
    @VisibleForTesting
    static void run(InputStream in, PrintStream out) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String type1 = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (type1 == null) {
                    type1 = line;
                } else {
                    out.println(tryGetCommonSuperClass(type1, line));
                    type1 = null;
                }
            }
        }
    }

    @VisibleForTesting
    static String tryGetCommonSuperClass(String type1, String type2) {
        try {
            ClassLoader classLoader = Main.class.getClassLoader();
            Class<?> class1 = Class.forName(type1.replace('/', '.'), false, classLoader);
            Class<?> class2 = Class.forName(type2.replace('/', '.'), false, classLoader);
            return getCommonSuperClass(class1, class2);
        } catch (ClassNotFoundException e) {
            return "error";
        }
    }

    @VisibleForTesting
    static String getCommonSuperClass(Class<?> class1, Class<?> class2) {
        if (class1.isAssignableFrom(class2)) {
            return class1.getName().replace('.', '/');
        }

        if (class2.isAssignableFrom(class1)) {
            return class2.getName().replace('.', '/');
        }

        if (class1.isInterface() || class2.isInterface()) {
            return "java/lang/Object";
        } else {
            do {
                class1 = class1.getSuperclass();
            } while (!class1.isAssignableFrom(class2));
            return class1.getName().replace('.', '/');
        }
    }

}
