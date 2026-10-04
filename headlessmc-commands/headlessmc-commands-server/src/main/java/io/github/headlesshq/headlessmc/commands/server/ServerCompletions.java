package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;

@Default
@Dependent
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Named("headlessmc:completions:server")
public class ServerCompletions implements Iterable<String> {
    private final ServerService service;

    @Override
    public Iterator<String> iterator() {
        return service.listServers().stream()
            .map(Profile::name)
            .iterator();
    }

}
