package io.github.headlesshq.headlessmc.api.classloading;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.Suggestion;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class ReflectionRemoteApplication implements RemoteApplication {
    private final Object remoteApiWrapper;

    @Override
    public void sendCommand(String command) throws CommandException {
        try {
            Method sendCommand = remoteApiWrapper.getClass().getMethod("sendCommand", String.class);
            sendCommand.invoke(remoteApiWrapper, command);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CommandException) {
                throw (CommandException) e.getCause();
            } else if (cause.getClass().getName().equalsIgnoreCase(CommandException.class.getName())) {
                // CommandException could have been loaded by another classloader
                throw new CommandException(cause.getMessage(), cause.getCause());
            }

            throw new RemoteException(e.getCause());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RemoteException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Suggestion> getSuggestions(int argIndex, int positionInArg, int cursor, String... args) {
        try {
            Method getSuggestions = remoteApiWrapper.getClass().getMethod("getSuggestions", int.class, int.class, int.class, String[].class);
            List<Object> result = (List<Object>) getSuggestions.invoke(remoteApiWrapper, argIndex, positionInArg, cursor, args);
            return convertRemoteSuggestions(result);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new CommandException(e);
        }
    }

    private List<Suggestion> convertRemoteSuggestions(List<Object> suggestions) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        List<Suggestion> result = new ArrayList<>(suggestions.size());
        for (Object suggestion : suggestions) {
            if (suggestion instanceof Suggestion) {
                result.add((Suggestion) suggestion);
            } else {
                String value = (String) suggestion.getClass().getMethod("getValue").invoke(suggestion);
                String description = (String) suggestion.getClass().getMethod("getDescription").invoke(suggestion);
                boolean complete = (boolean) suggestion.getClass().getMethod("isComplete").invoke(suggestion);
                result.add(new Suggestion(value, description, complete));
            }
        }

        return result;
    }

    public static ReflectionRemoteApplication of(Object apiWrapper) {
        // TODO:
        Class<?> apiWrapperClass = apiWrapper.getClass();

        return new ReflectionRemoteApplication(apiWrapper);
    }

}
