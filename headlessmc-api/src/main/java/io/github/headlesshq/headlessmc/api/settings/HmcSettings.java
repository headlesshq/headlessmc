package io.github.headlesshq.headlessmc.api.settings;

public class HmcSettings extends Module {
    public final SettingGroup hmc = SettingGroup.create("hmc", "HeadlessMc settings.");

    public final SettingKey<String> main = hmc.setting(String.class)
            .withName("hmc.main_method")
            .withDescription("The class with the Minecraft main method passed to the launched Minecraft instance.")
            .withValue("net.minecraft.client.Main")
            .build();

    public final SettingKey<Boolean> deencapsulate = hmc.setting(Boolean.class)
            .withName("hmc.deencapsulate")
            .withDescription("Enables some reflection hacks needed for Java 9+. On by default.")
            .withValue(true)
            .build();

    public final SettingKey<Boolean> exitOnFailedCommand = hmc.setting(Boolean.class)
            .withName("hmc.exit.on.failed.command")
            .withDescription("Quits on a failed command. For more strictness in CI/CD pipelines.")
            .withValue(false)
            .build();

    public HmcSettings(SettingGroup group) {
        super(group);
    }

}
