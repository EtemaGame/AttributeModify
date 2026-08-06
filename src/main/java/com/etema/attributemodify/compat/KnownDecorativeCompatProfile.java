package com.etema.attributemodify.compat;

import java.util.EnumSet;
import java.util.Set;

public record KnownDecorativeCompatProfile(
        String modId,
        String displayName,
        Set<DecorativeCompatCapabilities> capabilities,
        boolean knownForKeybindAbilities,
        boolean knownForInventoryTickEffects,
        boolean knownForPassiveEffects
) {
    public static KnownDecorativeCompatProfile of(String modId, String displayName,
            DecorativeCompatCapabilities... capabilities) {
        return new KnownDecorativeCompatProfile(modId, displayName,
                capabilities == null || capabilities.length == 0
                        ? EnumSet.noneOf(DecorativeCompatCapabilities.class)
                        : EnumSet.copyOf(java.util.List.of(capabilities)),
                false, false, false);
    }

    public boolean supports(DecorativeCompatCapabilities capability) {
        return capabilities != null && capabilities.contains(capability);
    }
}
