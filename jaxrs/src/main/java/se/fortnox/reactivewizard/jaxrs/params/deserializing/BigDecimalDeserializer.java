package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import java.math.BigDecimal;

/**
 * Deserializes BigDecimals.
 */
public class BigDecimalDeserializer extends NumberDeserializer<BigDecimal> {

    public BigDecimalDeserializer() {
        super(BigDecimal::new, "invalid.bigdecimal");
    }
}
