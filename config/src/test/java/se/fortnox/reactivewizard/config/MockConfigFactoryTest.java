package se.fortnox.reactivewizard.config;

import org.junit.jupiter.api.Test;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;

class MockConfigFactoryTest {

    @Test
    void shouldCreateLenientMock() {
        assertThat(mockingDetails(MockConfigFactory.create()).getMockCreationSettings().getStrictness())
            .isEqualTo(Strictness.LENIENT);
    }
}
