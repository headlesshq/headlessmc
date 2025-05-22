package io.github.headlesshq.headlessmc.api.command;

import java.util.List;

// TODO: Commands could implement this to provide custom suggestions based on something?
//  The only reason when this could be important would be to provide setting values based on a previous argument
//  as picocli does not have this feature
//  e.g. config --setting hmc.property --value ... <- suggest value for setting hmc.property
public interface ProvidesSuggestions {
    List<Suggestion> getSuggestions(int argIndex, int positionInArg, int cursor, String... args);

}
