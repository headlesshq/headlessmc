package io.github.headlesshq.headlessmc.commons;

import lombok.experimental.UtilityClass;

import java.io.*;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;

@UtilityClass
public class IOUtil {
    public static byte[] toBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(is, baos);
        return baos.toByteArray();
    }

    public static void copy(InputStream i, OutputStream o) throws IOException {
        int length;
        byte[] bytes = new byte[1024];
        while ((length = i.read(bytes)) != -1) {
            o.write(bytes, 0, length);
        }
    }

    public static JarOutputStream jarOutput(File file) throws IOException {
        return new JarOutputStream(Files.newOutputStream(file.toPath()));
    }

    public static BufferedReader reader(InputStream stream) {
        return new BufferedReader(new InputStreamReader(stream));
    }

    public static String read(BufferedReader br) throws IOException {
        return read(br, false);
    }

    public static String read(BufferedReader br, boolean appendNewLine) throws IOException {
        StringBuilder sb = new StringBuilder();
        read(br, line -> {
            sb.append(line);
            if (appendNewLine) {
                sb.append(System.lineSeparator());
            }
        });

        return sb.toString();
    }

    public static void read(BufferedReader br, Consumer<String> callback)
            throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            callback.accept(line);
        }
    }

}
