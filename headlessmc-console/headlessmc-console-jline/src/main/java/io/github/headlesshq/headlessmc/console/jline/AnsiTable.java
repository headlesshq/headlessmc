package io.github.headlesshq.headlessmc.console.jline;

import io.github.headlesshq.headlessmc.console.format.SimpleTable;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.List;

/**
 * A {@link SimpleTable} that uses {@link org.jline.utils.AttributedString}
 * to color the table:
 * <ul>
 *     <li>Colors the column titles in light gray</li>
 *     <li>Makes the entries in the first, primary, column bold</li>
 * </ul>
 *
 * @param <T> the type of items to display in the table.
 */
public class AnsiTable<T> extends SimpleTable<T> {
    @Override
    protected String build(List<List<String>> columns, List<Integer> columnWidths) {
        AttributedStringBuilder builder = new AttributedStringBuilder();

        for (int row = 0; !columns.isEmpty() && row < columns.getFirst().size(); row++) {
            for (int col = 0; col < columns.size(); col++) {
                String entry = columns.get(col).get(row);
                int width = columnWidths.get(col);

                if (row == 0) {
                    builder.style(
                        AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT)
                    );
                } else if (col == 0) {
                    builder.style(
                        AttributedStyle.DEFAULT.bold()
                    );
                } else {
                    builder.style(AttributedStyle.DEFAULT);
                }

                builder.append(entry);
                builder.style(AttributedStyle.DEFAULT);

                if (col == columns.size() - 1) {
                    continue;
                }

                for (int k = 0; k < width - length.apply(entry) + 3; k++) {
                    builder.append(' ');
                }
            }

            if (row < columns.getFirst().size() - 1) {
                builder.append('\n');
            }
        }

        return builder.toAnsi();
    }

}
