package io.github.headlesshq.headlessmc.api.settings;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;

@Getter
@ApplicationScoped
public class HmcSettings extends Module {
    private final SettingKey<String> main = getRoot().setting(String.class)
            .withName("hmc.main_method")
            .withDescription("The class with the Minecraft main method passed to the launched Minecraft instance.")
            .withValue("net.minecraft.client.Main")
            .build();

    private final SettingKey<Boolean> deencapsulate = getRoot().setting(Boolean.class)
            .withName("hmc.deencapsulate")
            .withDescription("Enables some reflection hacks needed for Java 9+. On by default.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> exitOnFailedCommand = getRoot().setting(Boolean.class)
            .withName("hmc.exit.on.failed.command")
            .withDescription("Quits on a failed command. For more strictness in CI/CD pipelines.")
            .withValue(false)
            .build();

    @Inject
    public HmcSettings(SettingGroup group) {
        super(group);
    }

}
