package io.github.headlesshq.headlessmc.web.cheerpj.plugin;

import lombok.SneakyThrows;
import io.github.headlesshq.headlessmc.api.command.BufferedCommandLineReader;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.plugin.HeadlessMcPlugin;
import io.github.headlesshq.headlessmc.logging.Logger;
import io.github.headlesshq.headlessmc.logging.LoggerFactory;
import io.github.headlesshq.headlessmc.logging.LoggingService;

import java.awt.*;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

public class CheerpJPlugin implements HeadlessMcPlugin {
    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    @SneakyThrows
    public void init(Launcher launcher) {
        CheerpJGUI gui = CheerpJGUI.getInstance();
        if (gui.isInitialized()) {
            return;
        }

        Logger logger = LoggerFactory.getLogger("GUI");
        if (GraphicsEnvironment.isHeadless()) {
            logger.error("GraphicsEnvironment is headless!");
            return;
        }

        gui.init();
        CheerpJMain.setupStdIO(gui, launcher.getCommandLine().getStdIO());
        LoggingService loggingService = launcher.getLoggingService();
        loggingService.setStreamFactory(() -> launcher.getCommandLine().getStdIO().getOut().get());
        loggingService.init(); // reinitialize

        logger.info("HeadlessMc GUI initialized.");

        PipedInputStream inputStream = new PipedInputStream();
        PrintStream outPipe = new PrintStream(new PipedOutputStream(inputStream));
        gui.getCommandHandler().set(outPipe::println);

        launcher.getCommandLine().getStdIO().setIn(() -> inputStream);
        launcher.getCommandLine().setCommandLineProvider(BufferedCommandLineReader::new);
        // TODO: when starting a process we gotta pump its in and outputstreams to the gui!
    }

    @Override
    public String getName() {
        return "CheerpJ";
    }

    @Override
    public String getDescription() {
        return "Makes HeadlessMc run in your browser.";
    }

}
