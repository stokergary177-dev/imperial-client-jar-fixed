package com.imperial.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/** Players on this list are skipped by KillAura and TriggerBot. Manage with /imperial friend add|remove|list. */
public final class Friends {
    private Friends() {}

    /** false = friends are treated like anyone else (toggle with /imperial friend protect). */
    public static boolean protect = true;
    private static final Set<String> NAMES = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    public static boolean isFriend(String name) { return protect && NAMES.contains(name); }
    public static boolean isFriend(Entity e) { return e instanceof Player && isFriend(e.getName().getString()); }

    public static boolean add(String name) { return NAMES.add(name); }
    public static boolean remove(String name) { return NAMES.remove(name); }
    public static Set<String> names() { return NAMES; }

    static void load(Properties props) {
        NAMES.clear();
        for (String n : props.getProperty("friends", "").split(","))
            if (!n.isBlank()) NAMES.add(n.trim());
        protect = Boolean.parseBoolean(props.getProperty("friends.protect", "true"));
    }

    static void save(Properties props) {
        props.setProperty("friends", String.join(",", NAMES));
        props.setProperty("friends.protect", Boolean.toString(protect));
    }
}
