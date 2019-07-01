package se.fortnox.reactivewizard.db.query.parts;

import org.junit.Test;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ParamQueryPartTest {
    @Test
    public void shouldIncludePropertiesAndTypeOnSubPathError() throws SQLException {
        ParamQueryPart paramQueryPart = new ParamQueryPart(0,
            ReflectionUtil.getPropertyResolver(Foo.class).orElseThrow(IllegalArgumentException::new));

        try {
            paramQueryPart.subPath(new String[] { "bar", "zap" });
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo(String.format("Properties [bar, zap] cannot be found in class %s",
                Foo.class.getName()));
        }
    }

    public static class Foo {
        private final Bar bar;

        public Foo(Bar bar) {
            this.bar = bar;
        }

        public Bar getBar() {
            return bar;
        }
    }

    public static class Bar {
        private final String zip;

        public Bar(String zip) {
            this.zip = zip;
        }

        public String getZip() {
            return zip;
        }
    }
}
