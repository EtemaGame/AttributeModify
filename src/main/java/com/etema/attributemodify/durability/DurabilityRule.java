package com.etema.attributemodify.durability;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public record DurabilityRule(int maxDurability, DurabilityMode mode, Set<String> triggers) {
    public static final String TRIGGER_MELEE_HIT = "melee_hit";
    public static final String TRIGGER_BLOCK_BREAK = "block_break";
    public static final String TRIGGER_RIGHT_CLICK = "right_click";

    private static final Set<String> SUPPORTED_TRIGGERS = Set.of(
            TRIGGER_MELEE_HIT,
            TRIGGER_BLOCK_BREAK,
            TRIGGER_RIGHT_CLICK);

    public DurabilityRule {
        maxDurability = Math.max(1, maxDurability);
        mode = mode != null ? mode : DurabilityMode.CUSTOM;
        triggers = normalizeTriggers(triggers);
    }

    public DurabilityRule(int maxDurability, DurabilityMode mode) {
        this(maxDurability, mode, Set.of());
    }

    public boolean hasTrigger(String trigger) {
        return triggers.contains(normalizeTrigger(trigger));
    }

    public boolean hasTriggers() {
        return !triggers.isEmpty();
    }

    public static boolean isSupportedTrigger(String trigger) {
        return SUPPORTED_TRIGGERS.contains(normalizeTrigger(trigger));
    }

    public static Set<String> supportedTriggers() {
        return SUPPORTED_TRIGGERS;
    }

    private static Set<String> normalizeTriggers(Set<String> triggers) {
        if (triggers == null || triggers.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String trigger : triggers) {
            String normalizedTrigger = normalizeTrigger(trigger);
            if (SUPPORTED_TRIGGERS.contains(normalizedTrigger)) {
                normalized.add(normalizedTrigger);
            }
        }

        if (normalized.isEmpty()) {
            return Set.of();
        }

        return java.util.Collections.unmodifiableSet(normalized);
    }

    private static String normalizeTrigger(String trigger) {
        return trigger == null ? "" : trigger.trim().toLowerCase(Locale.ROOT);
    }
}
