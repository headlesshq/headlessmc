package io.github.headlesshq.headlessmc.distribution;

public record JavaDistribution(String provider, String name, String id) {
    public static final String DEFAULT = "temurin";

}
