package se.fortnox.reactivewizard.binding.scanners;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.Test;
import se.fortnox.reactivewizard.binding.ClassScannerImpl;
import se.fortnox.reactivewizard.db.Query;
import se.fortnox.reactivewizard.db.Update;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DaoClassScannerTest {

    @Test
    public void testDaoClassScanner() {
        ClassGraph classGraph = new ClassGraph()
                .whitelistPackages("se.fortnox.reactivewizard.binding.scanners")
                .enableAnnotationInfo()
                .enableMethodInfo()
                .enableAllInfo();
        DaoClassScanner daoClassScanner = new DaoClassScanner();
        try (ScanResult scanResult = classGraph.scan()) {
            daoClassScanner.visit(new ClassScannerImpl(scanResult));
        }

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
