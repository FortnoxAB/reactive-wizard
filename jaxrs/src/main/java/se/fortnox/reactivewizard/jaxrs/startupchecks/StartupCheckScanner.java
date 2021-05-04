package se.fortnox.reactivewizard.jaxrs.startupchecks;

import se.fortnox.reactivewizard.binding.scanners.AbstractClassScanner;
import se.fortnox.reactivewizard.binding.scanners.ClassScanner;

import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class StartupCheckScanner extends AbstractClassScanner {
    private final Set<Class<? extends StartupCheck>> startupChecks = new HashSet<>();

    @Override
    public void visit(ClassScanner classpathScanner) {
        classpathScanner.findClassesImplementing(StartupCheck.class).forEach(startupChecks::add);
    }

    public Set<Class<? extends StartupCheck>> getStartupChecks() {
        return startupChecks;
    }
}
