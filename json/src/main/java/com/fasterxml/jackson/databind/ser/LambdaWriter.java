package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import se.fortnox.reactivewizard.util.Getter;
import se.fortnox.reactivewizard.util.MethodGetter;

import java.lang.reflect.Modifier;
import java.util.function.Function;

/**
 * A version of @{@link BeanPropertyWriter} that uses lambdas to access getters with better performance.
 *
 * Unfortunately there was some copy and paste needed, which means that we are quite dependent on the version of jackson here.
 */
public class LambdaWriter extends BeanPropertyWriter {
    private Function<Object, Object> lambda;

    public LambdaWriter(BeanPropertyWriter writer) {
        super(writer);
        resolveLambda();
    }

    private void resolveLambda() {
        if (_accessorMethod != null) {
            if (!Modifier.isPublic(_accessorMethod.getModifiers()) || !Modifier.isPublic(_accessorMethod.getDeclaringClass().getModifiers())) {
                return;
            }
            try {
                Getter<Object,Object> getter = MethodGetter.create(_accessorMethod.getDeclaringClass(), _accessorMethod);
                lambda = getter.getterFunction();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }

        }
    }

    Object readResolve() {
        super.readResolve();
        resolveLambda();
        return this;
    }


    @Override
    @SuppressWarnings("checkstyle:LocalVariableName")
    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        Object value;
        if (this.lambda != null) {
            value = this.lambda.apply(bean);
        } else if (this._accessorMethod == null) {
            value = this._field.get(bean);
        } else {
            value = this._accessorMethod.invoke(bean, (Object[]) null);
        }

        ////////////////////////////////
        // BEGIN COPY & PASTE Jackson 2.9.9.3

        // Null handling is bit different, check that first
        if (value == null) {
            if (_nullSerializer != null) {
                gen.writeFieldName(_name);
                _nullSerializer.serialize(null, gen, prov);
            }
            return;
        }
        // then find serializer to use
        JsonSerializer<Object> ser = _serializer;
        if (ser == null) {
            Class<?> cls = value.getClass();
            PropertySerializerMap m = _dynamicSerializers;
            ser = m.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(m, cls, prov);
            }
        }
        // and then see if we must suppress certain values (default, empty)
        if (_suppressableValue != null) {
            if (MARKER_FOR_EMPTY == _suppressableValue) {
                if (ser.isEmpty(prov, value)) {
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                return;
            }
        }
        // For non-nulls: simple check for direct cycles
        if (value == bean) {
            // three choices: exception; handled by call; or pass-through
            if (_handleSelfReference(bean, gen, prov, ser)) {
                return;
            }
        }
        gen.writeFieldName(_name);
        if (_typeSerializer == null) {
            ser.serialize(value, gen, prov);
        } else {
            ser.serializeWithType(value, gen, prov, _typeSerializer);
        }

        // END COPY & PASTE
        ////////////////////////////////

    }

    @Override
    public void serializeAsElement(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        Object value;
        if (this.lambda != null) {
            value = this.lambda.apply(bean);
        } else if (this._accessorMethod == null) {
            value = this._field.get(bean);
        } else {
            value = this._accessorMethod.invoke(bean, (Object[]) null);
        }

        ////////////////////////////////
        // BEGIN COPY & PASTE  Jackson 2.9.9.3

        if (value == null) { // nulls need specialized handling
            if (_nullSerializer != null) {
                _nullSerializer.serialize(null, gen, prov);
            } else { // can NOT suppress entries in tabular output
                gen.writeNull();
            }
            return;
        }
        // otherwise find serializer to use
        JsonSerializer<Object> ser = _serializer;
        if (ser == null) {
            Class<?> cls = value.getClass();
            PropertySerializerMap map = _dynamicSerializers;
            ser = map.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(map, cls, prov);
            }
        }
        // and then see if we must suppress certain values (default, empty)
        if (_suppressableValue != null) {
            if (MARKER_FOR_EMPTY == _suppressableValue) {
                if (ser.isEmpty(prov, value)) { // can NOT suppress entries in
                    // tabular output
                    serializeAsPlaceholder(bean, gen, prov);
                    return;
                }
            } else if (_suppressableValue.equals(value)) { // can NOT suppress
                // entries in tabular
                // output
                serializeAsPlaceholder(bean, gen, prov);
                return;
            }
        }
        // For non-nulls: simple check for direct cycles
        if (value == bean) {
            if (_handleSelfReference(bean, gen, prov, ser)) {
                return;
            }
        }
        if (_typeSerializer == null) {
            ser.serialize(value, gen, prov);
        } else {
            ser.serializeWithType(value, gen, prov, _typeSerializer);
        }

        // END COPY & PASTE
        ////////////////////////////////
    }

}
