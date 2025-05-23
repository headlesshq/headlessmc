package io.github.headlesshq.headlessmc.jline;

import io.github.headlesshq.headlessmc.api.settings.Module;
import io.github.headlesshq.headlessmc.api.settings.SettingGroup;
import io.github.headlesshq.headlessmc.api.settings.SettingKey;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
@ApplicationScoped
public class JLineSettings extends Module {
    private final SettingGroup group = getRoot().group("hmc.jline", "JLine configuration");

    private final SettingKey<Boolean> enabled = group.setting(Boolean.class)
            .withName("hmc.jline.enabled")
            .withDescription("Enables/Disables JLine support.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> propagateEnabled = group.setting(Boolean.class)
            .withName("hmc.jline.propagateEnabled")
            .withAlias("hmc.args.propagate.enabled") // legacy name
            .withDescription("Propagates hmc.jline.enabled to the process, to enable/disable JLine support in the hmc-specifics.")
            .withValue(true)
            .build();

    private final SettingKey<String> providers = group.setting(String.class)
            .withName("hmc.jline.providers")
            .withAlias("hmc.args.providers") // legacy name
            .withDescription("Analogue to org.jline.terminal.providers")
            .withValue("jni")
            .build();

    private final SettingKey<String> readPrefix = group.setting(String.class)
            .withName("hmc.jline.readPrefix")
            .withAlias("hmc.args.read.prefix") // legacy name
            .withDescription("A prefix to display when reading with the JLine Command Line.")
            .withValue("> ")
            .build();

    private final SettingKey<@Nullable String> type = group.setting(String.class)
            .withName("hmc.jline.type")
            .withAlias("hmc.args.type") // legacy name
            .withDescription("A prefix to display when reading with the JLine Command Line.")
            .nullable();

    private final SettingKey<Boolean> dumb = group.setting(Boolean.class)
            .withName("hmc.jline.dumb")
            .withAlias("hmc.args.dumb") // legacy name
            .withDescription("If JLine should use a dumb terminal.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> forceNotDumb = group.setting(Boolean.class)
            .withName("hmc.jline.forceNotDumb")
            .withAlias("hmc.args.force.not.dumb") // legacy name
            .withDescription("If JLine should try to get a non-dumb terminal even if its not possible.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> dumbWhenNoConsole = group.setting(Boolean.class)
            .withName("hmc.jline.dumbWhenNoConsole")
            .withAlias("hmc.args.dumb.when.no.console") // legacy name
            .withDescription("Uses a dumb terminal if Java's System.console() is null.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> noDeprecationWarning = group.setting(Boolean.class)
            .withName("hmc.jline.noDeprecationWarning")
            .withAlias("hmc.args.no.deprecation.warning") // legacy name
            .withDescription("Prevents JLine's deprecation warnings from being output.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> bracketedPaste = group.setting(Boolean.class)
            .withName("hmc.jline.bracketedPaste")
            .withAlias("hmc.args.bracketed.paste") // legacy name
            .withDescription("Enables bracketed paste support in the terminal.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> jlineIn = group.setting(Boolean.class)
            .withName("hmc.jline.in")
            .withAlias("hmc.args.in") // legacy name
            .withDescription("Uses System.in for JLine.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> jlineOut = group.setting(Boolean.class)
            .withName("hmc.jline.out")
            .withAlias("hmc.args.out") // legacy name
            .withDescription("Uses System.out for JLine.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> ffm = group.setting(Boolean.class)
            .withName("hmc.jline.ffm")
            .withAlias("hmc.args.ffm") // legacy name
            .withDescription("Enables FFM Terminal support (Java 22+ required).")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> jansi = group.setting(Boolean.class)
            .withName("hmc.jline.jansi")
            .withAlias("hmc.args.jansi") // legacy name
            .withDescription("Enables JANSI Terminal support.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> jna = group.setting(Boolean.class)
            .withName("hmc.jline.jna")
            .withAlias("hmc.args.jna") // legacy name
            .withDescription("Enables JNA Terminal support.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> jni = group.setting(Boolean.class)
            .withName("hmc.jline.jni")
            .withAlias("hmc.args.jni") // legacy name
            .withDescription("Enables JNI Terminal support.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> exec = group.setting(Boolean.class)
            .withName("hmc.jline.exec")
            .withAlias("hmc.args.exec") // legacy name
            .withDescription("Enables EXEC Terminal support.")
            .withValue(false)
            .build();

    private final SettingKey<@Nullable Boolean> system = group.setting(Boolean.class)
            .withName("hmc.jline.system")
            .withAlias("hmc.args.system") // legacy name
            .withDescription("Enables System Terminal support.")
            .nullable();

    private final SettingKey<Boolean> progressBar = group.setting(Boolean.class)
            .withName("hmc.jline.progressbar")
            .withAlias("hmc.args.enable.progressbar") // legacy name
            .withDescription("Enables JLine Progressbar support.")
            .withValue(true)
            .build();

    private final SettingKey<@Nullable String> progressBarStyle = group.setting(String.class)
            .withName("hmc.jline.progressBarStyle")
            .withAlias("hmc.jline.progressbar.style") // legacy name
            .withDescription("Sets the JLine Progressbar Style (COLORFUL_UNICODE_BLOCK, COLORFUL_UNICODE_BAR, UNICODE_BLOCK, or ASCII).")
            .nullable();

    @Inject
    public JLineSettings(SettingGroup group) {
        super(group);
    }

}
