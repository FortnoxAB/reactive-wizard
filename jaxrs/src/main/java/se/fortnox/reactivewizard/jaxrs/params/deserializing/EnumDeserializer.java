package se.fortnox.reactivewizard.jaxrs.params.deserializing;

/**
 * Deserializes enums.
 */
public class EnumDeserializer<T extends Enum<T>> implements Deserializer<T> {
    private final Class<T> paramType;

    public EnumDeserializer(Class<T> paramType) {
        this.paramType = paramType;
    }

    @Override
    public T deserialize(String value) throws DeserializerException {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(paramType, value);
        } catch (Exception parseException) {
            try {
                return Enum.valueOf(paramType, value.toUpperCase());
            } catch (Exception e) {
                throw new DeserializerException("invalid.enum");
            }

        }
    }
}
