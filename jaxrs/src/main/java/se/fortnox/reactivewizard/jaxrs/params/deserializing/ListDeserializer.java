package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListDeserializer<T> implements Deserializer<List<T>> {

    private final Deserializer<T> inner;

    public ListDeserializer(Deserializer<T> inner) {
        this.inner = inner;
    }

    @Override
    public List<T> deserialize(String value) throws DeserializerException {
        if (value == null) {
            return null;
        }

        if(value.isEmpty()){
            return Collections.emptyList();
        }

        String[] rawValues = value.split(",");
        List<T> deserialized = new ArrayList<>(rawValues.length);
        for (String rawValue : rawValues) {
            deserialized.add(inner.deserialize(rawValue));
        }
        return deserialized;
    }
}