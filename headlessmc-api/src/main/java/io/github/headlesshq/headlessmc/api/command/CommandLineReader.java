package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.Application;
import io.github.headlesshq.headlessmc.api.logging.StdIO;

import java.io.IOError;
import java.io.IOException;
import java.util.concurrent.ThreadFactory;

/**
 * Represents a blocking task that reads commands from a terminal, console or other InputStream.
 * Might implement {@link HidesPasswords}.
 */
@FunctionalInterface
public interface CommandLineReader extends ProgressBarProvider {
    /**
     * Every Thread produced by this ThreadFactory is a daemon thread and has the name "HeadlessMc-CommandLine".
     */
    ThreadFactory DEFAULT_THREAD_FACTORY = runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("HeadlessMc-CommandLine");
        return thread;
    };

    /**
     * Listens for commands on a terminal, console or other InputStreams provided by the {@link StdIO}.
     * This reader is configured by the {@link CommandLineManager} provided by the given {@link Application} instance.
     * This method will block.
     *
     * @param application the instance of HeadlessMc providing the CommandLine.
     * @throws IOError if something goes wrong.
     */
    void read(Application application) throws IOError;

    /**
     * Starts {@link #read(Application)} on a Thread provided by the {@link #DEFAULT_THREAD_FACTORY}.
     *
     * @param application the Application instance providing the {@link CommandLineManager}.
     */
    default void readAsync(Application application) {
        readAsync(application, DEFAULT_THREAD_FACTORY);
    }

    /**
     * Starts {@link #read(Application)} on a Thread provided by the given {@link ThreadFactory}.
     *
     * @param application the Application instance providing the {@link CommandLineManager}.
     * @param factory the factory providing the Thread to run this listener on.
     */
    default void readAsync(Application application, ThreadFactory factory) {
        // TODO: schedule executables on command line thread that run when command is entered
        // TODO: expose lock for command execution, so that we can change HeadlessMc safely from another Thread!

        // TODO: it would be useful to know which thread the command line is listening on?
        factory.newThread(() -> read(application)).start();
    }

    /**
     * This is here for the JLineCommandLineReader.
     * Only one JLine terminal can be open at a time, so we gotta close it while the game is running, if we want to read commands inside the game.
     * After we have closed with {@link #close()} we can restart it with this method.
     * <p>Restarting does not mean that this call will block.
     * Closing and opening should only be called inside the {@link #read(Application)} function, e.g. through a command that is called.
     * Opening this reader again after closing it simply means that the loop in the listen function will continue to read
     * after the command that has closed and opened it returns to the listen function.
     * <p>CommandLineReaders that allow multiple readers on their input streams do not need to implement this.
     * Method might be synchronized on this instance.
     *
     * @param application the Application instance whose {@link CommandLineManager} will be used to configure this CommandLineReader.
     * @throws IOException if something goes wrong when opening the terminal.
     */
    default void open(Application application) throws IOException {
        // see JLineCommandLineReader
    }

    /**
     * This is here for the JLineCommandLineReader.
     * Only one JLine terminal can be open at a time, so we gotta close it while the game is running, if we want to read commands inside the game.
     * After we have closed with this method we can restart it with this {@link #open(Application)}.
     * CommandLineReaders that allow multiple readers on their input streams do not need to implement this.
     * Method might be synchronized on this instance.
     *
     * @throws IOException if something goes wrong when closing the terminal.
     */
    default void close() throws IOException {
        // see JLineCommandLineReader
    }

    /**
     * @return a Progressbar to display progress with.
     */
    @Override
    default Progressbar displayProgressBar(Progressbar.Configuration configuration) {
        return Progressbar.dummy();
    }

}
