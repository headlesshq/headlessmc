package io.github.headlesshq.headlessmc.web.cheerpj.plugin;

import io.github.headlesshq.headlessmc.api.process.StdIO;

import javax.swing.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class CheerpJMain {
    public static final PrintStream STDOUT = System.out;

    public static void main(String[] args) {
        CheerpJGUI gui = CheerpJGUI.getInstance();
        gui.init();

        StdIO StdIO = new StdIO();
        setupStdIO(gui, StdIO);

        new CheerpJLauncher(StdIO, gui).launch();
    }

    public static void setupStdIO(CheerpJGUI cheerpJGUI, StdIO StdIO) {
        PrintStream out = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                SwingUtilities.invokeLater(() -> cheerpJGUI.getDisplayArea().append(String.valueOf((char) b)));
                STDOUT.write(b);
            }

            /// idk I failed to implement write(byte[] b, int off, int len), and idk why
        }, true);

        System.setOut(out);
        System.setErr(out);

        StdIO.setConsole(() -> null);
        StdIO.setOut(() -> out);
        StdIO.setErr(() -> out);
        StdIO.setIn(() -> new InputStream() {
            @Override
            public int read() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
    }

}
