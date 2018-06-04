package se.fortnox.reactivewizard.json;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.LambdaWriter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Modifier that uses lambdas to access getters of serialized beans.
 */
public class LambdaSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
        return beanProperties.stream().map(LambdaWriter::new).collect(Collectors.toList());
    }
}
