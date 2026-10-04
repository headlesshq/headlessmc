package io.github.headlesshq.headlessmc.mods.modrinth;

import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.version.arg.Side;
import lombok.Builder;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Documentation from:
 * <a href=https://docs.modrinth.com/api/operations/searchprojects/>
 * https://docs.modrinth.com/api/operations/searchprojects/
 * </a>
 * Represents facet filters used for project search queries.
 *
 * <p>A facet consists of three parts:
 *
 * <pre>{@code
 * {type} {operation} {value}
 * }</pre>
 *
 * <p>The default operation is {@code :} (equivalent to {@code =}), but the
 * following comparison operators are also supported:
 *
 * <ul>
 *   <li>{@code :} or {@code =} – equals</li>
 *   <li>{@code !=} – not equals</li>
 *   <li>{@code >} – greater than</li>
 *   <li>{@code >=} – greater than or equal to</li>
 *   <li>{@code <} – less than</li>
 *   <li>{@code <=} – less than or equal to</li>
 * </ul>
 *
 * <h2>Common Facet Types</h2>
 * <ul>
 *   <li>{@code project_type}</li>
 *   <li>{@code all_project_types} – Matches every project type across all project
 *       versions, not just the primary/version-specific type.</li>
 *   <li>{@code categories} – Includes loaders as part of category searches.</li>
 *   <li>{@code versions}</li>
 *   <li>{@code client_side}</li>
 *   <li>{@code server_side}</li>
 *   <li>{@code open_source}</li>
 * </ul>
 *
 * <h2>Specialized Facet Types</h2>
 * <p>These are intended for specific use cases and should generally be avoided
 * unless required:
 *
 * <ul>
 *   <li>{@code title}</li>
 *   <li>{@code author}</li>
 *   <li>{@code follows}</li>
 *   <li>{@code project_id}</li>
 *   <li>{@code license}</li>
 *   <li>{@code downloads}</li>
 *   <li>{@code created_timestamp} (Unix timestamp)</li>
 *   <li>{@code modified_timestamp} (Unix timestamp)</li>
 * </ul>
 *
 * <h2>Examples</h2>
 *
 * <pre>{@code
 * categories:adventure
 * versions!=1.20.1
 * downloads<=100
 * }</pre>
 *
 * <h2>Combining Facets</h2>
 *
 * <p>Facets are grouped into nested arrays to express logical operators:
 *
 * <ul>
 *   <li><b>OR</b> – Multiple facet expressions within the same array are treated
 *       as OR conditions.</li>
 *   <li><b>AND</b> – Separate arrays are treated as AND conditions.</li>
 * </ul>
 *
 * <h3>OR Example</h3>
 *
 * <pre>{@code
 * [
 *   ["versions:1.16.5", "versions:1.17.1"]
 * ]
 * }</pre>
 *
 * <p>Matches projects supporting <b>1.16.5 OR 1.17.1</b>.
 *
 * <h3>AND Example</h3>
 *
 * <pre>{@code
 * [
 *   ["versions:1.16.5"],
 *   ["project_type:modpack"]
 * ]
 * }</pre>
 *
 * <p>Matches projects supporting <b>1.16.5 AND</b> having the project type
 * <b>modpack</b>.
 *
 * <h3>Complex Example</h3>
 *
 * <pre>{@code
 * [
 *   ["categories:forge"],
 *   ["versions:1.17.1"],
 *   ["project_type:mod"],
 *   ["license:mit"]
 * ]
 * }</pre>
 *
 * <p>This query matches Forge mods for Minecraft 1.17.1 that are licensed under
 * the MIT license.
 */
@Builder
class Facets {
    @Builder.Default
    private final Set<ModType> modTypes = Set.of();
    @Builder.Default
    private final List<String> loaders = List.of();
    @Builder.Default
    private final List<String> versions = List.of();
    @Builder.Default
    private final List<Side> sides = List.of();

    public String toQuery() {
        StringBuilder result = new StringBuilder("[");
        List<String> projectTypes = modTypes.stream()
            .map(ModType::distributionNames)
            .map(types -> types.getOrDefault(
                ModrinthDistributionPlatform.DEFAULT,
                types.get(ModType.DISTRIBUTION_DEFAULT))
            ).filter(Objects::nonNull)
            .toList();

        append(result, "project_type", projectTypes);
        append(result, "categories", loaders);
        append(result, "versions", versions);

        for (Side side : sides) {
            append(result, side.isClient() ? "client_side" : "server_side", List.of("required"));
        }

        if (result.charAt(result.length() - 1) == ',') {
            result.setLength(result.length() - 1);
        }

        return result.append("]").toString();
    }

    private void append(StringBuilder result, String key, List<String> strings) {
        if (strings.isEmpty()) {
            return;
        }

        result.append("[");
        Iterator<String> itr = strings.iterator();
        while (itr.hasNext()) {
            String value = itr.next();
            result.append("\"").append(key).append(":").append(value).append("\"");
            if (itr.hasNext()) {
                result.append(",");
            }
        }
        result.append("],");
    }

}
