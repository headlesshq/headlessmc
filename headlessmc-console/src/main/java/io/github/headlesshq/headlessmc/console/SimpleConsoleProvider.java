package io.github.headlesshq.headlessmc.console;

import io.github.headlesshq.headlessmc.console.input.Input;
import io.github.headlesshq.headlessmc.console.input.InputProvider;
import io.github.headlesshq.headlessmc.console.output.Output;
import io.github.headlesshq.headlessmc.console.output.OutputProvider;
import io.github.headlesshq.headlessmc.console.spi.SPIService;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class SimpleConsoleProvider implements ConsoleProvider {
    private final List<OutputProvider> outputs;
    private final List<InputProvider> inputs;

    @Override
    public Console get() throws ConsoleException {
        SPIService<Output, OutputProvider> output = new SPIService<>(outputs, "output");
        SPIService<Input, InputProvider> input = new SPIService<>(inputs, "input");
        return new Console() {
            @Override
            public void write(String message) {
                output.get().write(message);
            }

            @Override
            public void write(String message, String nextLine) {
                output.get().write(message, nextLine);
            }

            @Override
            public String read() {
                return input.get().read();
            }

            @Override
            public Password readPassword() {
                return input.get().readPassword();
            }
        };
    }

    @Override
    public int sort() {
        return SORT_DEFAULT;
    }

    public static ConsoleProvider of(List<OutputProvider> outputs, List<InputProvider> inputs) {
        List<OutputProvider> sortedOutputs = new ArrayList<>(outputs);
        Collections.sort(sortedOutputs);
        List<InputProvider> sortedInputs = new ArrayList<>(inputs);
        Collections.sort(sortedInputs);
        return new SimpleConsoleProvider(sortedOutputs, sortedInputs);
    }

}
