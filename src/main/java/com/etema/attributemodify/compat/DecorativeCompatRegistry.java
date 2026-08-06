package com.etema.attributemodify.compat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class DecorativeCompatRegistry {
    private static final Map<String, KnownDecorativeCompatProfile> KNOWN = new LinkedHashMap<>();

    static {
        register(new KnownDecorativeCompatProfile(
                "soulsweapons",
                "Soulslike Weaponry",
                Set.of(
                        DecorativeCompatCapabilities.EFFECTS,
                        DecorativeCompatCapabilities.KEYBINDS,
                        DecorativeCompatCapabilities.INVENTORY_TICK,
                        DecorativeCompatCapabilities.PASSIVE_ARMOR,
                        DecorativeCompatCapabilities.PASSIVE_WEAPON,
                        DecorativeCompatCapabilities.USE_ACTIONS,
                        DecorativeCompatCapabilities.COMBAT),
                true,
                true,
                true));

        register(new KnownDecorativeCompatProfile(
                "simplyswords",
                "Simply Swords",
                Set.of(
                        DecorativeCompatCapabilities.EFFECTS,
                        DecorativeCompatCapabilities.KEYBINDS,
                        DecorativeCompatCapabilities.PASSIVE_WEAPON,
                        DecorativeCompatCapabilities.USE_ACTIONS,
                        DecorativeCompatCapabilities.COMBAT),
                true,
                false,
                true));

        register(new KnownDecorativeCompatProfile(
                "cataclysm",
                "Cataclysm",
                Set.of(
                        DecorativeCompatCapabilities.EFFECTS,
                        DecorativeCompatCapabilities.PASSIVE_ARMOR,
                        DecorativeCompatCapabilities.PASSIVE_WEAPON,
                        DecorativeCompatCapabilities.USE_ACTIONS,
                        DecorativeCompatCapabilities.COMBAT),
                false,
                false,
                true));

        register(new KnownDecorativeCompatProfile(
                "irons_spellbooks",
                "Iron's Spells 'n Spellbooks",
                Set.of(
                        DecorativeCompatCapabilities.EFFECTS,
                        DecorativeCompatCapabilities.KEYBINDS,
                        DecorativeCompatCapabilities.PASSIVE_ARMOR,
                        DecorativeCompatCapabilities.USE_ACTIONS),
                true,
                true,
                true));
    }

    private DecorativeCompatRegistry() {
    }

    public static void register(KnownDecorativeCompatProfile profile) {
        if (profile == null || profile.modId() == null || profile.modId().isBlank()) {
            return;
        }
        KNOWN.put(profile.modId(), profile);
    }

    public static KnownDecorativeCompatProfile get(String modId) {
        return modId == null ? null : KNOWN.get(modId);
    }

    public static boolean isKnownDecorativeFamily(String modId) {
        return get(modId) != null;
    }

    public static Map<String, KnownDecorativeCompatProfile> all() {
        return Map.copyOf(KNOWN);
    }
}
