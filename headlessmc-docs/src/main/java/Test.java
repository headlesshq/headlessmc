import io.github.headlesshq.headlessmc.commands.HeadlessMcCommand;
import picocli.CommandLine;

public class Test {

    static void main() {
        CommandLine commandLine = new CommandLine(HeadlessMcCommand.class);
    }
}
