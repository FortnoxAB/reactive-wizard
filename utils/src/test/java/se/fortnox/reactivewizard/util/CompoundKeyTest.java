package se.fortnox.reactivewizard.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompoundKeyTest {

    @Test
    void shouldBeComparableWhenSame() {
        CompoundKey key1 = new CompoundKey("one", "two", "three");
        CompoundKey key2 = new CompoundKey("one", "two", "three");

        assertThat(key1).isEqualTo(key2);
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
    }

    @Test
    void shouldBeComparableWhenDifferent() {
        CompoundKey key1 = new CompoundKey("one", "two", "three");
        CompoundKey key2 = new CompoundKey("one", "two", "four");

        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1.hashCode()).isNotEqualTo(key2.hashCode());
    }

    @Test
    void shouldCompareKeysWithDifferentLength() {
        CompoundKey key1 = new CompoundKey("one", "two", "three");
        CompoundKey key2 = new CompoundKey("one", "two");

        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1.hashCode()).isNotEqualTo(key2.hashCode());
    }

    @Test
    void shouldCompareNullValues() {
        CompoundKey key1 = new CompoundKey(null, null);
        CompoundKey key2 = new CompoundKey(null, null, null);

        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1.hashCode()).isNotEqualTo(key2.hashCode());
    }

    @Test
    void shouldCompareWithNullValues() {
        CompoundKey key1 = new CompoundKey(null, null);

        assertThat(key1).isNotEqualTo(null);
    }


    @Test
    void shouldCompareWithOtherTypes() {
        CompoundKey key1 = new CompoundKey("one");

        assertThat(key1).isNotEqualTo("one");
    }

}
