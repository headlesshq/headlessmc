package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * The arguments a script passed to the {@code java} command, e.g.
 * {@code java @user_jvm_args.txt @libraries/.../unix_args.txt "$@"},
 * parsed so that they can be applied to a {@link JavaProcessBuilder}.
 *
 * @param jvmArgs   the jvm options before the main class or jar, without the classpath.
 * @param classpath the entries of the {@code -cp}, {@code -classpath} or {@code --class-path} option.
 * @param mainClass the main class, if no jar is launched.
 * @param jar       the jar launched with {@code -jar}.
 * @param args      the arguments passed to the main class or jar.
 */
public record JavaCommand(
    List<String> jvmArgs,
    List<String> classpath,
    @Nullable String mainClass,
    @Nullable Path jar,
    List<String> args
) {
    /**
     * Java options that take the next argument as their value.
     */
    private static final Set<String> OPTIONS_WITH_VALUE = Set.of(
        "-p", "--module-path", "--upgrade-module-path", "--add-modules", "--limit-modules",
        "--add-exports", "--add-opens", "--add-reads", "--patch-module", "--enable-native-access", "--source"
    );

    private static final Set<String> CLASSPATH_OPTIONS = Set.of("-cp", "-classpath", "--class-path");

    public JavaProcessBuilder applyTo(JavaProcessBuilder builder) {
        builder.jvmArg(jvmArgs.toArray(String[]::new));
        if (jar != null) {
            builder.jar(jar);
        } else {
            builder.classpath(classpath.toArray(String[]::new)).mainClass(requireMainClass());
        }

        return builder.arg(args.toArray(String[]::new));
    }

    private String requireMainClass() {
        if (mainClass == null) {
            throw new IllegalStateException("Neither jar nor main class have been specified");
        }

        return mainClass;
    }

    /**
     * Parses the arguments given to a java command, like the java launcher does.
     * Argument files ({@code @file}) are expanded until the main class has been found.
     *
     * @param arguments  the arguments given to the java command.
     * @param workingDir the directory the java command is run in, relative argument files are resolved against it.
     * @param charset    the charset of argument files.
     * @return the parsed java command.
     * @throws ServerException if the arguments could not be parsed.
     */
    public static JavaCommand parse(List<String> arguments, Path workingDir, Charset charset) throws ServerException {
        Deque<String> remaining = new ArrayDeque<>(arguments);
        List<String> jvmArgs = new ArrayList<>();
        List<String> classpath = new ArrayList<>();
        boolean expand = true;
        while (!remaining.isEmpty()) {
            String arg = remaining.poll();
            if (expand && arg.startsWith("@") && !arg.startsWith("@@")) {
                List<String> fileArgs = readArgFile(workingDir.resolve(arg.substring(1)), charset);
                for (int i = fileArgs.size() - 1; i >= 0; i--) {
                    remaining.push(fileArgs.get(i));
                }

                continue;
            }

            if (expand && arg.startsWith("@@")) {
                arg = arg.substring(1);
            }

            if ("--disable-@files".equals(arg)) {
                expand = false;
            } else if (CLASSPATH_OPTIONS.contains(arg)) {
                classpath.addAll(splitClasspath(requireValue(arg, remaining)));
            } else if (arg.startsWith("--class-path=")) {
                classpath.addAll(splitClasspath(arg.substring("--class-path=".length())));
            } else if ("-jar".equals(arg)) {
                Path jar = workingDir.resolve(requireValue(arg, remaining));
                return new JavaCommand(jvmArgs, List.of(), null, jar, new ArrayList<>(remaining));
            } else if ("-m".equals(arg) || "--module".equals(arg) || arg.startsWith("--module=")) {
                throw new ServerException("Launching java modules (" + arg + ") is not supported");
            } else if (OPTIONS_WITH_VALUE.contains(arg)) {
                jvmArgs.add(arg);
                jvmArgs.add(requireValue(arg, remaining));
            } else if (arg.startsWith("-")) {
                jvmArgs.add(arg);
            } else {
                return new JavaCommand(jvmArgs, classpath, arg, null, new ArrayList<>(remaining));
            }
        }

        throw new ServerException("No main class or jar found in java command " + arguments);
    }

    private static String requireValue(String option, Deque<String> remaining) {
        String value = remaining.poll();
        if (value == null) {
            throw new ServerException("Java option " + option + " requires a value");
        }

        return value;
    }

    private static List<String> splitClasspath(String classpath) {
        return Arrays.stream(classpath.split(File.pathSeparator)).filter(entry -> !entry.isEmpty()).toList();
    }

    private static List<String> readArgFile(Path file, Charset charset) {
        try {
            return parseArgFile(Files.readString(file, charset));
        } catch (IOException e) {
            throw new ServerException("Failed to read java argument file " + file, e);
        }
    }

    /**
     * Splits the content of a java argument file into arguments.
     * Arguments are separated by whitespace, can be quoted with {@code "} or {@code '}
     * and lines starting with {@code #} are comments.
     * Inside quotes a {@code \} escapes the next character.
     *
     * @param content the content of the argument file.
     * @return the arguments in the file.
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html#java-command-line-argument-files">
     * Java Command-Line Argument Files</a>
     */
    static List<String> parseArgFile(String content) {
        List<String> result = new ArrayList<>();
        StringBuilder current = null;
        char quote = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                } else if (c == '\\' && i + 1 < content.length()) {
                    char next = content.charAt(++i);
                    switch (next) {
                        case 'n' -> current.append('\n');
                        case 'r' -> current.append('\r');
                        case 't' -> current.append('\t');
                        case 'f' -> current.append('\f');
                        case '\n', '\r' -> {
                            // line continuation, skip leading whitespace of the next line
                            while (i + 1 < content.length() && Character.isWhitespace(content.charAt(i + 1))) {
                                i++;
                            }
                        }
                        default -> current.append(next);
                    }
                } else {
                    current.append(c);
                }
            } else if (Character.isWhitespace(c)) {
                if (current != null) {
                    result.add(current.toString());
                    current = null;
                }
            } else if (c == '#' && current == null) {
                while (i + 1 < content.length() && content.charAt(i + 1) != '\n' && content.charAt(i + 1) != '\r') {
                    i++;
                }
            } else {
                if (current == null) {
                    current = new StringBuilder();
                }

                if (c == '"' || c == '\'') {
                    quote = c;
                } else {
                    current.append(c);
                }
            }
        }

        if (current != null) {
            result.add(current.toString());
        }

        return result;
    }

}
