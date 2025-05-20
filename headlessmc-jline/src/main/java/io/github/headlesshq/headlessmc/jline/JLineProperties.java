package io.github.headlesshq.headlessmc.jline;

import io.github.headlesshq.headlessmc.api.config.Property;
import io.github.headlesshq.headlessmc.api.config.PropertyTypes;

/**
 * A collection of properties for configuring JLine.
 */
public interface JLineProperties {
    Property<Boolean> ENABLED = PropertyTypes.bool("hmc.jline.enabled");
    Property<Boolean> PROPAGATE_ENABLED = PropertyTypes.bool("hmc.args.propagate.enabled");

    Property<String> PROVIDERS = PropertyTypes.string("hmc.args.providers");
    Property<String> READ_PREFIX = PropertyTypes.string("hmc.args.read.prefix");
    Property<String> TYPE = PropertyTypes.string("hmc.args.type");

    Property<Boolean> DUMB = PropertyTypes.bool("hmc.args.dumb");
    Property<Boolean> FORCE_NOT_DUMB = PropertyTypes.bool("hmc.args.force.not.dumb");
    Property<Boolean> DUMB_WHEN_NO_CONSOLE = PropertyTypes.bool("hmc.args.dumb.when.no.console");
    Property<Boolean> PREVENT_DEPRECATION_WARNING = PropertyTypes.bool("hmc.args.no.deprecation.warning");
    Property<Boolean> BRACKETED_PASTE = PropertyTypes.bool("hmc.args.bracketed.paste");
    Property<Boolean> JLINE_IN = PropertyTypes.bool("hmc.args.in");
    Property<Boolean> JLINE_OUT = PropertyTypes.bool("hmc.args.out");
    Property<Boolean> FFM = PropertyTypes.bool("hmc.args.ffm");
    Property<Boolean> JANSI = PropertyTypes.bool("hmc.args.jansi");
    Property<Boolean> JNA = PropertyTypes.bool("hmc.args.jna");
    Property<Boolean> JNI = PropertyTypes.bool("hmc.args.jni");
    Property<Boolean> EXEC = PropertyTypes.bool("hmc.args.exec");
    Property<Boolean> SYSTEM = PropertyTypes.bool("hmc.args.system");

    Property<Boolean> ENABLE_PROGRESS_BAR = PropertyTypes.bool("hmc.args.enable.progressbar");
    Property<String> PROGRESS_BAR_STYLE = PropertyTypes.string("hmc.jline.progressbar.style");

}
