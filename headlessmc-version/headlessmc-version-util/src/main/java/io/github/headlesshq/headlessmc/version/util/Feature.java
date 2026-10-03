package io.github.headlesshq.headlessmc.version.util;

public record Feature(String name) {
    public static final Feature CUSTOM_RESOLUTION = new Feature("has_custom_resolution");

    public static final Feature QUICK_PLAYS_SUPPORT = new Feature("has_quick_plays_support");
    public static final Feature QUICK_PLAY_MULTIPLAYER = new Feature("is_quick_play_multiplayer");
    public static final Feature QUICK_PLAY_REALMS = new Feature("is_quick_play_realms");
    public static final Feature QUICK_PLAY_SINGLEPLAYER = new Feature("is_quick_play_singleplayer");

    public static final Feature DEMO = new Feature("is_demo_user");

    public static Feature of(String name) {
        return new Feature(name);
    }

}
