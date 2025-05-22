package io.github.headlesshq.headlessmc.api.classloading;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.Application;
import io.github.headlesshq.headlessmc.api.command.Suggestion;

import java.util.List;

public interface RemoteApplication {
    void sendCommand(String command) throws RemoteException, CommandException;

    List<Suggestion> getSuggestions(int argIndex, int positionInArg, int cursor, String... args) throws RemoteException;

    static RemoteApplication wrap(Object object) {
        if (object instanceof RemoteApplication) {
            return (RemoteApplication) object;
        }

        if (object instanceof Application) {
            return new RemoteApplicationImpl((Application) object);
        }

        return ReflectionRemoteApplication.of(object);
    }

}
