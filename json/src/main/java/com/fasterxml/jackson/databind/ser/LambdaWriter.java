package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import se.fortnox.reactivewizard.json.LambdaSerializerModifier;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.util.function.Function;

/**
 * A version of @{@link BeanPropertyWriter} that uses lambdas to access getters with better performance.
 *
 * Unfortunately there was some copy&paste needed, which means that we are quite dependent on the version of jackson here.
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
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            try {
                MethodHandle methodHandle = lookup.unreflect(_accessorMethod);
                CallSite callSite = LambdaMetafactory.metafactory(
                        lookup,
                        "apply",
                        MethodType.methodType(Function.class),
                        MethodType.methodType(Object.class, Object.class),
                        methodHandle,
                        methodHandle.type()
                );
                lambda = (Function<Object,Object>) callSite.getTarget().invoke();
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
            value = this._accessorMethod.invoke(bean);
        }

        ////////////////////////////////
        // BEGIN COPY & PASTE

        if (value == null) {
            if (this._nullSerializer != null) {
                gen.writeFieldName(this._name);
                this._nullSerializer.serialize((Object)null, gen, prov);
            }

        } else {
            JsonSerializer<Object> ser = this._serializer;
            if (ser == null) {
                Class<?> cls = value.getClass();
                PropertySerializerMap m = this._dynamicSerializers;
                ser = m.serializerFor(cls);
                if (ser == null) {
                    ser = this._findAndAddDynamic(m, cls, prov);
                }
            }

            if (this._suppressableValue != null) {
                if (MARKER_FOR_EMPTY == this._suppressableValue) {
                    if (ser.isEmpty(prov, value)) {
                        return;
                    }
                } else if (this._suppressableValue.equals(value)) {
                    return;
                }
            }

            if (value != bean || !this._handleSelfReference(bean, gen, prov, ser)) {
                gen.writeFieldName(this._name);
                if (this._typeSerializer == null) {
                    ser.serialize(value, gen, prov);
                } else {
                    ser.serializeWithType(value, gen, prov, this._typeSerializer);
                }

            }
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
            value = this._accessorMethod.invoke(bean);
        }

        ////////////////////////////////
        // BEGIN COPY & PASTE

        if (value == null) {
            if (this._nullSerializer != null) {
                this._nullSerializer.serialize((Object)null, gen, prov);
            } else {
                gen.writeNull();
            }

        } else {
            JsonSerializer<Object> ser = this._serializer;
            if (ser == null) {
                Class<?> cls = value.getClass();
                PropertySerializerMap map = this._dynamicSerializers;
                ser = map.serializerFor(cls);
                if (ser == null) {
                    ser = this._findAndAddDynamic(map, cls, prov);
                }
            }

            if (this._suppressableValue != null) {
                if (MARKER_FOR_EMPTY == this._suppressableValue) {
                    if (ser.isEmpty(prov, value)) {
                        this.serializeAsPlaceholder(bean, gen, prov);
                        return;
                    }
                } else if (this._suppressableValue.equals(value)) {
                    this.serializeAsPlaceholder(bean, gen, prov);
                    return;
                }
            }

            if (value != bean || !this._handleSelfReference(bean, gen, prov, ser)) {
                if (this._typeSerializer == null) {
                    ser.serialize(value, gen, prov);
                } else {
                    ser.serializeWithType(value, gen, prov, this._typeSerializer);
                }

            }
        }

        // END COPY & PASTE
        ////////////////////////////////
    }

}
