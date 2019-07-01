package se.fortnox.reactivewizard.util;

import org.junit.Test;
import rx.Observable;

import javax.inject.Provider;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class ReflectionUtilTest {
    @Test
    public void shouldFindDeclaredMethods() {
        Getter getter = ReflectionUtil.getGetter(Child.class, "j");
        assertThat(getter).isNotNull();

        Getter isbool = ReflectionUtil.getGetter(Child.class, "a");
        assertThat(isbool).isNotNull();

        Getter hasbool = ReflectionUtil.getGetter(Child.class, "b");
        assertThat(hasbool).isNotNull();

        Setter setter = ReflectionUtil.getSetter(Child.class, "j");
        assertThat(setter).isNotNull();
    }

    @Test
    public void shouldFindSuperclassMethods() {
        Getter getter = ReflectionUtil.getGetter(Child.class, "i");
        assertThat(getter).isNotNull();
        assertThat(getter).isInstanceOf(MethodGetter.class);

        getter = ReflectionUtil.getGetter(Child.class, "k");
        assertThat(getter).isNotNull();
        assertThat(getter).isInstanceOf(MethodGetter.class);

        Setter setter = ReflectionUtil.getSetter(Child.class, "i");
        assertThat(setter).isNotNull();
        assertThat(setter).isInstanceOf(MethodSetter.class);
    }

    @Test
    public void shouldFindSuperclassFields() {
        Getter getter = ReflectionUtil.getGetter(Child.class, "l");
        assertThat(getter).isNotNull();
        assertThat(getter).isInstanceOf(FieldGetter.class);

        Setter setter = ReflectionUtil.getSetter(Child.class, "l");
        assertThat(setter).isNotNull();
        assertThat(setter).isInstanceOf(FieldSetter.class);
    }

    @Test
    public void shouldFindSizeMethod() {
        Getter size = ReflectionUtil.getGetter(List.class, "size");
        assertThat(size).isNotNull();
    }

    @Test
    public void shouldInstantiate() {
        assertThat(ReflectionUtil.instantiator(Parent.class).get()).isNotNull();
        assertThat(ReflectionUtil.instantiator(Child.class).get()).isNotNull();
        assertThat(ReflectionUtil.instantiator(PrivateDefaultConstructor.class).get()).isNotNull();
    }

    @Test
    public void shouldCreateInstantiator() {
        assertThat(ReflectionUtil.instantiator(Parent.class).get()).isInstanceOf(Parent.class);
    }

    @Test
    public void shouldCreateGetterLambda() {
        Parent parent = new Parent();
        parent.setI(3);

        Function<Parent, Integer> getFromParent = ReflectionUtil.<Parent,Integer>getter(Parent.class, "i").get();
        assertThat(getFromParent.apply(parent)).isEqualTo(3);


        Inner inner = new Inner();
        inner.setI(5);
        parent.setInner(inner);

        Function<Parent, Integer> getFromInner = ReflectionUtil.<Parent,Integer>getter(Parent.class, "inner.i").get();
        assertThat(getFromInner.apply(parent)).isEqualTo(5);
    }

    @Test
    public void shouldCreateSetterLambda() {
        Parent parent = new Parent();

        BiConsumer<Parent, Integer> setOnParent = ReflectionUtil.<Parent,Integer>setter(Parent.class, "i").get();
        setOnParent.accept(parent, 2);
        assertThat(parent.getI()).isEqualTo(2);


        Inner inner = new Inner();
        parent.setInner(inner);

        BiConsumer<Parent, Integer> setOnInner = ReflectionUtil.<Parent,Integer>setter(Parent.class, "inner.i").get();
        setOnInner.accept(parent, 4);
        assertThat(parent.getInner().getI()).isEqualTo(4);
    }

    @Test
    public void shouldSupportNullsOnSetterPath() {
        Parent parent = new Parent();
        BiConsumer<Parent, Integer> setter = ReflectionUtil.<Parent,Integer>setter(Parent.class, "inner.i").get();

        setter.accept(parent, 7);

        assertThat(parent.getInner().getI()).isEqualTo(7);

    }


    @Test
    public void shouldThrowHelpfulExceptionWhenNoZeroParametersConstructorExists()  {
        try {
            ReflectionUtil.instantiator(NoZeroParametersConstructorClass.class).get();
            fail("Expected RuntimeException, but none was thrown");
        } catch (RuntimeException exception) {
            assertThat(exception.getMessage())
                .isEqualTo("No constructor with zero parameters found on NoZeroParametersConstructorClass");
        }
    }

    @Test
    public void shouldFindFieldIfNoMethod() {
        Getter getter = ReflectionUtil.getGetter(Child.class, "c");
        assertThat(getter).isNotNull();

        Setter setter = ReflectionUtil.getSetter(Child.class, "c");
        assertThat(setter).isNotNull();
    }

    @Test
    public void shouldFindParameterAnnotationsFromMethod() throws NoSuchMethodException {
        Method resourceGet = TestResource.class.getMethod("resourceGet", String.class, String.class);
        List<List<Annotation>> parameterAnnotations = ReflectionUtil.getParameterAnnotations(resourceGet);
        assertThat(parameterAnnotations).hasSize(2);
        assertThat(parameterAnnotations.get(0)).hasSize(1);
        assertThat(parameterAnnotations.get(0).get(0)).isInstanceOf(QueryParam.class);
        assertThat(parameterAnnotations.get(1).get(0)).isInstanceOf(HeaderParam.class);


        Method instanceMethod = TestResourceImpl.class.getMethod("resourceGet", String.class, String.class);
        parameterAnnotations = ReflectionUtil.getParameterAnnotations(instanceMethod);
        assertThat(parameterAnnotations).hasSize(2);
        assertThat(parameterAnnotations.get(0)).hasSize(1);
        assertThat(parameterAnnotations.get(0).get(0)).isInstanceOf(QueryParam.class);
        assertThat(parameterAnnotations.get(1).get(0)).isInstanceOf(HeaderParam.class);
    }

    @Test
    public void shouldFindParameterAnnotationsFromParameter() throws NoSuchMethodException {
        Method resourceGet = TestResource.class.getMethod("resourceGet", String.class, String.class);
        List<Annotation> parameterAnnotations = ReflectionUtil.getParameterAnnotations(resourceGet.getParameters()[0]);
        assertThat(parameterAnnotations).hasSize(1);
        assertThat(parameterAnnotations.get(0)).isInstanceOf(QueryParam.class);

        Method instanceMethod = TestResourceImpl.class.getMethod("resourceGet", String.class, String.class);
        parameterAnnotations = ReflectionUtil.getParameterAnnotations(instanceMethod.getParameters()[0]);
        assertThat(parameterAnnotations).hasSize(1);
        assertThat(parameterAnnotations.get(0)).isInstanceOf(QueryParam.class);
    }

    @Test
    public void shouldFindMethodAnnotations() throws NoSuchMethodException {
        Method resourceGet = TestResource.class.getMethod("resourceGet", String.class, String.class);
        List<Annotation> methodAnnotations = ReflectionUtil.getAnnotations(resourceGet);
        assertThat(methodAnnotations).hasSize(1);
        assertThat(methodAnnotations.get(0)).isInstanceOf(GET.class);

        Method instanceMethod = TestResourceImpl.class.getMethod("resourceGet", String.class, String.class);
        methodAnnotations = ReflectionUtil.getAnnotations(instanceMethod);
        assertThat(methodAnnotations).hasSize(1);
        assertThat(methodAnnotations.get(0)).isInstanceOf(GET.class);
    }

    @Test
    public void shouldFindMethodAnnotation() throws NoSuchMethodException {
        Method resourceGet = TestResource.class.getMethod("resourceGet", String.class, String.class);
        assertThat(ReflectionUtil.getAnnotation(resourceGet, GET.class)).isNotNull().isInstanceOf(GET.class);

        Method instanceMethod = TestResourceImpl.class.getMethod("resourceGet", String.class, String.class);
        assertThat(ReflectionUtil.getAnnotation(instanceMethod, GET.class)).isNotNull().isInstanceOf(GET.class);
    }

    @Test
    public void shouldFindObservableType() throws NoSuchMethodException {
        Method resourceGet = TestResource.class.getMethod("resourceGet", String.class, String.class);
        assertThat(ReflectionUtil.getTypeOfObservable(resourceGet)).isEqualTo(String.class);

        Method instanceMethod = TestResourceImpl.class.getMethod("resourceGet", String.class, String.class);
        assertThat(ReflectionUtil.getTypeOfObservable(instanceMethod)).isEqualTo(String.class);
    }

    @Test
    public void shouldFailFindingObservableType() throws NoSuchMethodException {
        Method nonGenericReturnMethod = TestResource.class.getMethod("methodWithGenericParameter", List.class);
        try {
            ReflectionUtil.getTypeOfObservable(nonGenericReturnMethod);
            fail("expected exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("method does not have a generic return type");
        }

        Method nonObservableGenericReturnType = TestResource.class.getMethod("nonObservableGenericReturnType");
        try {
            ReflectionUtil.getTypeOfObservable(nonObservableGenericReturnType);
            fail("expected exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("java.util.List<java.lang.String> is not an Observable or Single");
        }
    }

    @Test
    public void shouldFindObservableFromMockedType() throws NoSuchMethodException {
        Method instanceMethod = mock(TestResource.class).getClass().getMethod("resourceGet", String.class, String.class);
        assertThat(ReflectionUtil.getTypeOfObservable(instanceMethod)).isEqualTo(String.class);
    }

    @Test
    public void shouldFindInstanceMethodThroughInvocationHandler() throws NoSuchMethodException {
        Method interfaceMethod = TestResource.class.getMethod("resourceGet", String.class, String.class);

        Object proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{TestResource.class}, new InstanceExposingInvocationHandler(new TestResourceImpl()));
        Method proxyMethod = proxy.getClass().getMethod("resourceGet", String.class, String.class);

        Method instanceMethod = ReflectionUtil.getInstanceMethod(proxyMethod, proxy);
        assertThat(instanceMethod).isNotEqualTo(interfaceMethod);
        assertThat(instanceMethod).isEqualTo(TestResourceImpl.class.getMethod("resourceGet", String.class, String.class));
    }

    @Test
    public void shouldFindGenericParameterType() throws NoSuchMethodException {
        Method interfaceMethod = TestResource.class.getMethod("methodWithGenericParameter", List.class);
        assertThat(ReflectionUtil.getGenericParameter(interfaceMethod.getGenericParameterTypes()[0])).isEqualTo(Integer.class);


        Method methodWithoutGenerics = TestResource.class.getMethod("resourceGet", String.class, String.class);
        try {
            ReflectionUtil.getGenericParameter(methodWithoutGenerics.getGenericParameterTypes()[0]);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("The sent in type class java.lang.String is not a ParameterizedType");
        }

        Method methodWithMultipleGenerics = TestResource.class.getMethod("methodWithMultiGenericParameter", Map.class);
        try {
            ReflectionUtil.getGenericParameter(methodWithMultipleGenerics.getGenericParameterTypes()[0]);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("The sent in type java.util.Map<java.lang.String, java.lang.Integer> should have exactly one type argument, but had 2");
        }
    }

    @Test
    public void shouldSupportSubPathsOfProperties() {
        Optional<PropertyResolver> propertyResolverMaybe = ReflectionUtil.getPropertyResolver(Parent.class, "inner");
        PropertyResolver resolver = propertyResolverMaybe.get();

        assertThat(resolver.getPropertyType()).isEqualTo(Inner.class);

        Optional<PropertyResolver<Parent,Integer>> subPathMaybe = resolver.subPath(new String[]{"i"});
        PropertyResolver<Parent,Integer> subPath = subPathMaybe.get();

        assertThat(subPath.getPropertyType()).isEqualTo(int.class);

        Parent entity = new Parent();
        entity.setInner(new Inner(){{
            setI(4);
        }});
        assertThat(subPath.getter().apply(entity)).isEqualTo(4);
    }

    private static class InstanceExposingInvocationHandler implements InvocationHandler, Provider {

        private final Object instance;

        public InstanceExposingInvocationHandler(Object instance) {
            this.instance = instance;
        }

        @Override
        public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
            return null;
        }

        @Override
        public Object get() {
            return instance;
        }
    }

    static class Parent {
        private int i;
        private int k;
        private int l;
        private Inner inner;

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        protected int getK() {
            return k;
        }

        public Inner getInner() {
            return inner;
        }

        public void setInner(Inner inner) {
            this.inner = inner;
        }
    }

    static class Inner {
        private int i;

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }
    }

    class NoZeroParametersConstructorClass {
        NoZeroParametersConstructorClass(String something) {

        }
    }

    static class Child extends Parent {
        private int     j;
        private boolean a;
        private boolean b;
        private boolean c;

        public boolean isA() {
            return a;
        }

        public boolean hasB() {
            return b;
        }

        public int getJ() {
            return j;
        }

        public void setJ(int j) {
            this.j = j;
        }
    }

    static class PrivateDefaultConstructor {
        private final int a;

        private PrivateDefaultConstructor() {
            a = 0;
        }

        public PrivateDefaultConstructor(int a) {
            this.a = a;
        }
    }

    public interface TestResource {
        @GET
        Observable<String> resourceGet(@QueryParam("name") String name, @HeaderParam("lang") String lang);

        void methodWithGenericParameter(List<Integer> integerList);

        void methodWithMultiGenericParameter(Map<String,Integer> integerList);

        List<String> nonObservableGenericReturnType();
    }

    public static class TestResourceImpl implements TestResource {

        @Override
        public Observable<String> resourceGet(String name, String lang) {
            return null;
        }

        @Override
        public void methodWithGenericParameter(List<Integer> integerList) {

        }

        @Override
        public void methodWithMultiGenericParameter(Map<String, Integer> integerList) {

        }

        @Override
        public List<String> nonObservableGenericReturnType() {
            return null;
        }
    }
}
