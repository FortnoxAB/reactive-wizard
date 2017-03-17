package se.fortnox.reactivewizard.jaxrs.params.deserializing;

public class EnumDeserializer<T extends Enum> implements Deserializer<T> {
    private final Class<?> paramType;

    public EnumDeserializer(Class<T> paramType) {
        this.paramType = paramType;
    }

    @Override
    public T deserialize(String value) throws DeserializerException {
        if (value == null) {
            return null;
        }
        try {
            return (T)Enum.valueOf((Class) paramType, value);
        } catch (Exception parseException) {
            throw new DeserializerException("invalid.enum");
        }
    }
}
