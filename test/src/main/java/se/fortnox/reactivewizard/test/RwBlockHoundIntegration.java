package se.fortnox.reactivewizard.test;

import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

/**
 * Common Blockhound config.
 * Extend and use in modules along with @AutoService(BlockHoundIntegration.class).
 */
public abstract class RwBlockHoundIntegration implements BlockHoundIntegration {

    @Override
    public void applyTo(BlockHound.Builder builder) {
        builder
            .allowBlockingCallsInside("se.fortnox.reactivewizard.jaxrs.WebException", "createUUID")
            .allowBlockingCallsInside("com.fasterxml.jackson.databind.cfg.MapperBuilder", "findAndAddModules");
    }
}
