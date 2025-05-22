package io.github.headlesshq.headlessmc.launcher.command.forge;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import io.github.headlesshq.headlessmc.api.traits.HasName;

import java.util.List;

@Data
public class ForgeVersion implements HasName, Comparable<ForgeVersion> {
    @SerializedName("requires")
    private List<Requires> requires;

    @SerializedName("version")
    private String name;

    @Override
    public int compareTo(ForgeVersion other) {
        if (this.name.equals(other.getName())) {
            return 0;
        }

        String[] version1 = this.name.split("[-.]");
        String[] version2 = other.getName().split("[-.]");
        for (int i = 0; i < version1.length && i < version2.length; i++) {
            int compare;
            try {
                compare = Integer.compare(Integer.parseInt(version1[i]), Integer.parseInt(version2[i]));
            } catch (NumberFormatException e) {
                compare = String.CASE_INSENSITIVE_ORDER.compare(version1[i], version2[i]);
            }

            if (compare != 0) {
                return compare;
            }
        }

        return Integer.compare(version2.length, version1.length);
    }

    public String getVersion() {
        return getRequires().get(0).equals;
    }

    public String getFullName() {
        return getVersion() + "-" + getName();
    }

    @Data
    public static class Requires {
        @SerializedName("equals")
        private String equals;

        // @SerializedName("uid") <- check this
        // private String netMinecraft; seems to always be "net.minecraft"
    }

}
