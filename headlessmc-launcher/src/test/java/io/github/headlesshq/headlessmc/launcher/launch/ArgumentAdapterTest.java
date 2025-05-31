package io.github.headlesshq.headlessmc.launcher.launch;

import io.github.headlesshq.headlessmc.launcher.UsesResources;
import io.github.headlesshq.headlessmc.launcher.version.Features;
import io.github.headlesshq.headlessmc.os.OS;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;

public class ArgumentAdapterTest implements UsesResources {
    @Test
    public void testArgumentAdapter() {
        val child = getVersion("version_child.json", "child_file", 0);
        val parent = getVersion("version_parent.json", "parent_file", 1);
        child.setParent(parent);
        val merger = new VersionMerger(child);

        val mac_os = new OS("mac", OS.Type.OSX, "1", true);
        // val windows = new OS("win", OS.Type.WINDOWS, "11", true);
        val featureMap = new HashMap<String, Boolean>();
        val features = new Features(featureMap);

        val adapter = new ArgumentAdapter(merger.getArguments());
        Assertions.assertEquals(Arrays.asList(
                                    "-XstartOnFirstThread", "-DSomeSystemProperty=${some_arg}",
                                    "-cp", "${classpath}", "-DThing=Thing"),
                                adapter.build(mac_os, features, "jvm"));

        Assertions.assertEquals(Arrays.asList(
                                    "--username", "${auth_player_name}",
                                    "--versionType", "${version_type}"),
                                adapter.build(mac_os, features, "game"));

        // TODO: too lazy to add more rn, but test around with features, os etc.
    }

}
