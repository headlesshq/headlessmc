package io.github.headlesshq.headlessmc.console.format;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

// TODO: limit width of table
// TODO: keep indentation on line break!
public class SimpleTable<T> implements TableBuilder<T> {
    protected Function<String, Integer> length = String::length;
    protected final List<Column<T>> columns = new ArrayList<>();
    protected final List<T> elements = new ArrayList<>();

    @Override
    public TableBuilder<T> withColumn(String name, Function<T, @Nullable String> value) {
        //noinspection DataFlowIssue
        columns.add(new Column<>(value::apply, name));
        return this;
    }

    @Override
    public TableBuilder<T> withStringValueOf(String name, Function<T, @Nullable Object> value) {
        columns.add(new Column<>(value, name));
        return this;
    }

    @Override
    public TableBuilder<T> add(T element) {
        elements.add(element);
        return this;
    }

    @Override
    public TableBuilder<T> addAll(Iterable<T> elements) {
        elements.forEach(this.elements::add);
        return this;
    }

    @Override
    public TableBuilder<T> withLength(Function<String, Integer> length) {
        this.length = length;
        return this;
    }

    @Override
    public String toString() {
        List<Integer> columnWidths = new ArrayList<>(this.columns.size());
        List<List<String>> columns = this.columns.stream().map(e -> {
            List<String> entries = this.elements.isEmpty()
                ? new ArrayList<>(Collections.singletonList("-"))
                : this.elements.stream()
                .map(e.function)
                .map(str -> str == null ? "-" : str)
                .map(String::valueOf)
                .collect(Collectors.toList());
            entries.add(0, String.valueOf(e.name));
            // let's hope the Terminal uses a fixed-width font
            columnWidths.add(entries.stream()
                .map(length)
                .max(Integer::compareTo)
                .get());

            return entries;
        }).collect(Collectors.toList());
        return build(columns, columnWidths);
    }

    protected String build(List<List<String>> columns, List<Integer> columnWidths) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; !columns.isEmpty() && i < columns.get(0).size(); i++) {
            for (int j = 0; j < columns.size(); j++) {
                String entry = columns.get(j).get(i);
                int width = columnWidths.get(j);
                builder.append(entry);
                // last column doesn't need to be filled up
                if (j == columns.size() - 1) {
                    continue;
                }

                for (int k = 0; k < width - length.apply(entry) + 3; k++) {
                    builder.append(' ');
                }
            }

            // no need to append a linebreak on the last row
            if (i < columns.get(0).size() - 1) {
                builder.append('\n');
            }
        }

        return builder.toString();
    }

    @RequiredArgsConstructor
    protected static final class Column<T> {
        public final Function<T, @Nullable Object> function;
        public final String name;
    }

}
