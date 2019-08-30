package se.fortnox.reactivewizard.binding;

import com.google.inject.Module;

/**
 * Implement this interface in order to create a Guice Module which is located automatically at startup.
 */
public interface AutoBindModule extends Module, Comparable<AutoBindModule> {
    /**
     * Priority of this module. Used to determine what module overrides another module.
     * @return a priority, higher priority module will overwrite lower priority modules
     */
    default Integer getPrio() {
        return 100;
    }

    /**
     * Implement this in order to execute code before the binding takes place.
     */
    default void preBind() { }

    @Override
    default int compareTo(AutoBindModule otherModules) {
        int compare = this.getPrio().compareTo(otherModules.getPrio());
        if (compare == 0) {
            compare = this.getClass()
                    .getName()
                    .compareTo(otherModules.getClass().getName());
        }
        return compare;
    }
}
