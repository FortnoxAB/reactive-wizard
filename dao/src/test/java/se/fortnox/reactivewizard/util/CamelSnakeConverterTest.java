package se.fortnox.reactivewizard.util;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class CamelSnakeConverterTest {

    @Test
    public void shouldConvertToCamelCase() {
        assertThat(CamelSnakeConverter.snakeToCamel("AAA")).isEqualTo("aaa");
        assertThat(CamelSnakeConverter.snakeToCamel("AAA_AAA_AAA")).isEqualTo("aaaAaaAaa");
        assertThat(CamelSnakeConverter.snakeToCamel("a_a")).isEqualTo("aA");
        assertThat(CamelSnakeConverter.snakeToCamel("aa")).isEqualTo("aa");
        assertThat(CamelSnakeConverter.snakeToCamel("A")).isEqualTo("a");
        assertThat(CamelSnakeConverter.snakeToCamel("a")).isEqualTo("a");
        assertThat(CamelSnakeConverter.snakeToCamel("a_Ab_aA_a")).isEqualTo("aAbAaA");
        assertThat(CamelSnakeConverter.snakeToCamel("")).isEqualTo("");
    }

    @Test
    public void shouldConvertToSnake() {
        assertThat(CamelSnakeConverter.camelToSnake("AAA")).isEqualTo("aaa");
        assertThat(CamelSnakeConverter.camelToSnake("aaaAaaAaa")).isEqualTo("aaa_aaa_aaa");
        assertThat(CamelSnakeConverter.camelToSnake("aA")).isEqualTo("a_a");
        assertThat(CamelSnakeConverter.camelToSnake("aa")).isEqualTo("aa");
        assertThat(CamelSnakeConverter.camelToSnake("A")).isEqualTo("a");
        assertThat(CamelSnakeConverter.camelToSnake("a")).isEqualTo("a");
        assertThat(CamelSnakeConverter.camelToSnake("aAbAaA")).isEqualTo("a_ab_aa_a");
        assertThat(CamelSnakeConverter.camelToSnake("")).isEqualTo("");
    }
}
