package io.github.headlesshq.headlessmc.jline;

import io.github.headlesshq.headlessmc.api.settings.Config;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.github.headlesshq.headlessmc.api.command.ProgressBarProvider;
import io.github.headlesshq.headlessmc.api.command.Progressbar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

@Getter
@Setter
@NoArgsConstructor
public class JlineProgressbarProvider implements ProgressBarProvider {
    private volatile String progressBarStyle = null;

    @Override
    public Progressbar displayProgressBar(Progressbar.Configuration configuration) {
        ProgressBarBuilder builder = new ProgressBarBuilder();
        builder.setTaskName(configuration.getTaskName());
        builder.setInitialMax(configuration.getInitialMax());
        Progressbar.Configuration.Unit unit = configuration.getUnit();
        if (unit != null) {
            builder.setUnit(unit.getName(), unit.getMax());
        }
        String style = progressBarStyle;
        if (style != null) {
            // ProgressBarStyle is not an enum anymore after 0.10.0
            switch (style) {
                case "COLORFUL_UNICODE_BLOCK":
                    builder.setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK);
                    break;
                case "COLORFUL_UNICODE_BAR":
                    builder.setStyle(ProgressBarStyle.COLORFUL_UNICODE_BAR);
                    break;
                case "UNICODE_BLOCK":
                    builder.setStyle(ProgressBarStyle.UNICODE_BLOCK);
                    break;
                case "ASCII":
                    builder.setStyle(ProgressBarStyle.ASCII);
                    break;
                default:
            }
        }

        return new JlineProgressbar(builder.build());
    }

}
