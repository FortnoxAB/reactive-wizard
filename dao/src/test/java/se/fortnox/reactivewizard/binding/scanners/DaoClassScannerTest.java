package se.fortnox.reactivewizard.binding.scanners;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import org.junit.Test;
import se.fortnox.reactivewizard.db.Query;
import se.fortnox.reactivewizard.db.Update;

import java.util.Set;
import java.util.stream.Collectors;

import static org.fest.assertions.Assertions.assertThat;

public class DaoClassScannerTest {

    @Test
    public void testDaoClassScanner() {
        FastClasspathScanner classpathScanner = new FastClasspathScanner("se.fortnox.reactivewizard.binding.scanners");
        DaoClassScanner      daoClassScanner  = new DaoClassScanner();
        daoClassScanner.visit(classpathScanner);
        classpathScanner.scan();

        Set<Class<?>> classes = daoClassScanner.getClasses();

        assertThat(classes).hasSize(2);
        classes.forEach(cls -> assertThat(cls.isInterface()).isTrue());

        Set<Class<?>> otherClasses = classes.stream()
            .filter(cls -> {
                String name = cls.getName();
                return !name.contains("QueryDao") && !name.contains("UpdateDao");
            })
            .collect(Collectors.toSet());
        assertThat(otherClasses).isEmpty();
    }

    public interface QueryDao {
        @Query("AnyString")
        public void query();
    }

    public interface UpdateDao {
        @Update("AnyString")
        public void update();
    }

    public class ClassDao {
        @Update("AnyString")
        public void update() {
        }

        @Query("AnyString")
        public void query() {
        }
    }

    public interface EmptyDao {
    }
}
