package io.github.headlesshq.headlessmc.java.distribution.foojay;

import io.github.headlesshq.headlessmc.distribution.JavaDistribution;

/**
 * A distribution JSON object returned by the foojay disco API.
 * These are just the fields we need for our use cases.
 * <pre>
 * {@code
 * {
 *       "name": "Zulu",
 *       "api_parameter": "zulu",
 *       "vendor": "azul",
 *       "maintained": true,
 *       "available": true,
 *       "build_of_openjdk": true,
 *       "build_of_graalvm": false,
 *       "official_uri": "https://www.azul.com/downloads/?package=jdk",
 *       "synonyms": [
 *         "zulu",
 *         ...
 *       ],
 *       "versions": [
 *         "27-ea+27",
 *         ...
 *       ]
 *     },
 * }
 * </pre>
 *
 * @param name          human-readable name of the Distribution.
 * @param api_parameter string for discovery in the API.
 */
record Distribution(String name, String api_parameter) {
    public JavaDistribution toJavaDistribution(String provider) {
        return new JavaDistribution(provider, name(), api_parameter());
    }

}
