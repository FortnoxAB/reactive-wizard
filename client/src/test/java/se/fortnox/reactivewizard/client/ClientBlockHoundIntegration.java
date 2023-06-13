package se.fortnox.reactivewizard.client;

import com.google.auto.service.AutoService;
import reactor.blockhound.integration.BlockHoundIntegration;
import se.fortnox.reactivewizard.test.RwBlockHoundIntegration;

@AutoService(BlockHoundIntegration.class)
public class ClientBlockHoundIntegration extends RwBlockHoundIntegration {
}
