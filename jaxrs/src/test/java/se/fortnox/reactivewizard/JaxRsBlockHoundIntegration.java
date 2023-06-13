package se.fortnox.reactivewizard;

import com.google.auto.service.AutoService;
import reactor.blockhound.integration.BlockHoundIntegration;
import se.fortnox.reactivewizard.test.RwBlockHoundIntegration;

@AutoService(BlockHoundIntegration.class)
public class JaxRsBlockHoundIntegration extends RwBlockHoundIntegration {
}
