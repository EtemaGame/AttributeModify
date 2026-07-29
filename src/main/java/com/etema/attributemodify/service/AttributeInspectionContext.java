package com.etema.attributemodify.service;

import java.util.function.Supplier;

/** Prevents AttributeModify's own rules from appearing as base attributes during inspection. */
public final class AttributeInspectionContext {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private AttributeInspectionContext() {
    }

    public static boolean isInspectingExternalAttributes() {
        return DEPTH.get() > 0;
    }

    public static <T> T inspectExternalAttributes(Supplier<T> operation) {
        DEPTH.set(DEPTH.get() + 1);
        try {
            return operation.get();
        } finally {
            int depth = DEPTH.get() - 1;
            if (depth == 0) {
                DEPTH.remove();
            } else {
                DEPTH.set(depth);
            }
        }
    }
}
