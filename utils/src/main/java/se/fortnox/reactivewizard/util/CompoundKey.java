package se.fortnox.reactivewizard.util;

import java.util.Objects;

/**
 * Utility class for keys that consist of multiple values.
 * Usable for things like cache keys etc.
 */
public class CompoundKey {
    private Object[] keys;

    public CompoundKey(Object... keys) {
        this.keys = keys;
    }

    @Override
    public int hashCode() {
        final int prime  = 31;
        int       result = 1;
        for (Object k : keys) {
            result = result * prime + (k == null ? 0 : k.hashCode());
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CompoundKey other)) {
            return false;
        }

        if (other.keys.length != keys.length) {
            return false;
        }

        for (int i = 0; i < keys.length; i++) {
            if (!Objects.equals(keys[i], other.keys[i])) {
                return false;
            }
        }

        return true;
    }
}
