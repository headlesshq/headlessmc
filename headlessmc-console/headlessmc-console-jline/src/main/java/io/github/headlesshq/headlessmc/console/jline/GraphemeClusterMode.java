package io.github.headlesshq.headlessmc.console.jline;

/**
 * Whether Jline treats grapheme clusters, like emojis made of multiple code points, as a single character.
 */
public enum GraphemeClusterMode {
    /**
     * Grapheme clusters are not supported.
     */
    OFF,
    /**
     * Grapheme clusters are always supported, without detecting if the terminal supports them.
     */
    ON,
    /**
     * Detects if the terminal supports grapheme clusters once.
     * For this Jline probes the terminal with escape sequences and might print a few emojis to measure the cursor movement.
     */
    AUTO
}
