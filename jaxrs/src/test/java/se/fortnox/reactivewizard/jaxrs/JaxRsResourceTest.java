package se.fortnox.reactivewizard.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.MockHttpServerRequest;
import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;
import rx.Single;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolverFactories;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolverFactory;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolvers;
import se.fortnox.reactivewizard.jaxrs.params.WrapSupportingParamTypeResolver;
import se.fortnox.reactivewizard.jaxrs.params.annotated.AnnotatedParamResolverFactories;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.DeserializerFactory;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResult;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResultFactoryFactory;
import se.fortnox.reactivewizard.jaxrs.response.ResultTransformerFactories;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;
import se.fortnox.reactivewizard.utils.JaxRsTestUtil;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static rx.Observable.just;
import static se.fortnox.reactivewizard.utils.JaxRsTestUtil.body;
import static se.fortnox.reactivewizard.utils.JaxRsTestUtil.delete;
import static se.fortnox.reactivewizard.utils.JaxRsTestUtil.get;
import static se.fortnox.reactivewizard.utils.JaxRsTestUtil.getWithHeaders;
import static se.fortnox.reactivewizard.utils.JaxRsTestUtil.patch;
import static se.fortnox.reactivewizard.utils.JaxRsTestUtil.post;
import static se.fortnox.reactivewizard.utils.JaxRsTestUtil.put;

public class JaxRsResourceTest {

	public enum TestEnum {
		ONE, TWO, THREE
	}

	private final TestresourceInterface service = new TestresourceImpl();

    @Test
    public void shouldConcatPaths() throws InvocationTargetException, IllegalAccessException {
        JaxRsResources resources    = new JaxRsResources(new Object[]{new Testresource()}, new JaxRsResourceFactory(), false);
        JaxRsRequest   jaxRsRequest = new JaxRsRequest(new MockHttpServerRequest("/test/acceptsString"), new ByteBufCollector());
        assertThat(resources.findResource(jaxRsRequest).call(jaxRsRequest)).isNotNull();
    }

    @Test
    public void shouldResolveArgs() throws Exception {
        assertThat(body(get(new Testresource(), "/test/acceptsString?myarg=hepp")))
            .isEqualTo("\"inp: hepp\"");
    }

    @Test
    public void shouldHandleHeaderOnClassAsAResource() {
        assertThat(get(new Testresource(), "/test/serverSideAnnotationsOnClassAsResource").getHeader("Content-Disposition")).isNotNull();
    }

    @Test
    public void shouldIgnoreHeaderOnInterface() {
        assertThat(get(new Testresource(), "/test/shouldIgnoreHeadersAnnotationOnInterface").getHeader("Content-Disposition")).isNull();
    }

    @Test
    public void shouldAcceptHeaderOnImplementingClass() {
        assertThat(get(service, "/test/acceptsHeadersOnImplementation").getHeader("Content-Disposition")).isNotNull();
    }

    @Test
    public void shouldHandleInterfaceAnnotations() throws Exception {
        assertThat(body(get(service, "/test/accepts?myarg=hepp")))
            .isEqualTo("\"accepts from interface: hepp\"");
    }

    @Test
    public void shouldResolveCustomType() throws IllegalAccessException,
        IllegalArgumentException, InvocationTargetException, JsonProcessingException {

        MockHttpServerRequest req = new MockHttpServerRequest("/test/accepts/res?fid=5678");
        req.addCookie("fnox_5678", "888");

        Foo foo = Mockito.mock(Foo.class);
        when(foo.getStr()).thenReturn("5678");
        ParamResolver<Foo> fooResolver = new ParamResolver<Foo>() {
            @Override
            public Observable<Foo> resolve(JaxRsRequest request) {
                return just(foo);
            }
        };

        ParamResolvers paramResolvers = new ParamResolvers(fooResolver);

        JaxRsResources jaxRsResources = new JaxRsResources(
            new Object[]{new FooTest()},
            new JaxRsResourceFactory(
                new ParamResolverFactories(
                    new DeserializerFactory(),
                    paramResolvers,
                    new AnnotatedParamResolverFactories(),
                    new WrapSupportingParamTypeResolver()),
                new JaxRsResultFactoryFactory(),
                new BlockingResourceScheduler()),
            false);
        JaxRsRequest                         jaxRsRequest = new JaxRsRequest(req, new ByteBufCollector());
        Observable<? extends JaxRsResult<?>> result       = jaxRsResources.findResource(jaxRsRequest).call(jaxRsRequest);

        MockHttpServerResponse response = new MockHttpServerResponse();

        result.toBlocking()
            .single()
            .write(response)
            .toBlocking()
            .lastOrDefault(null);

        assertThat(response.getOutp()).isEqualTo("\"foo: 5678\"");
    }

    @Test
    public void shouldSupportDefaultValue() throws Exception {
        assertThat(body(get(service, "/test/defaultQuery"))).isEqualTo("\"Default: 5\"");
        assertThat(body(post(service, "/test/acceptsDefaultForm", ""))).isEqualTo("\"Default: 5\"");
        assertThat(body(getWithHeaders(service, "/test/acceptsDefaultHeader", new HashMap<>()))).isEqualTo("\"Default: 5\"");
        assertThat(body(getWithHeaders(service, "/test/acceptsDefaultCookie", new HashMap<>()))).isEqualTo("\"Default: 5\"");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotSupportDefaultValueForPathParam() throws Exception {
        body(get(new DefaultPathParamResource(), "/default/param"));
    }

    @Test
    public void shouldSupportSimpleQueryParamTypes() throws Exception {
        assertThat(
            body(get(service, "/test/acceptsBoolean?myarg=true")))
            .isEqualTo("\"Boolean: true\"");

        assertThat(body(get(service, "/test/acceptsInteger?myarg=678")))
            .isEqualTo("\"Integer: 678\"");

        assertThat(body(get(service, "/test/acceptsLong?myarg=678")))
            .isEqualTo("\"Long: 678\"");

        assertThat(body(get(service, "/test/acceptsDate?myarg=1234567890123")))
            .isEqualTo("\"2009-02-13T23:31:30.123+0000\"");

        assertThat(body(get(service, "/test/acceptsEnum?myarg=ONE")))
            .isEqualTo("\"Enum: ONE\"");

        assertThat(body(get(service, "/test/acceptsEnum?myarg=")))
            .isEqualTo("\"Enum: null\"");

    }

    @Test
    public void shouldSupportSimplePathParamTypes() throws Exception {
        assertThat(body(get(service, "/test/acceptsBoolean/true")))
            .isEqualTo("\"Boolean: true\"");

        assertThat(body(get(service, "/test/acceptsInteger/678")))
            .isEqualTo("\"Integer: 678\"");

        assertThat(body(get(service, "/test/acceptsLong/678")))
            .isEqualTo("\"Long: 678\"");

        assertThat(body(get(service, "/test/acceptsStrictLong/678")))
            .isEqualTo("\"long: 678\"");

        assertThat(body(get(service, "/test/acceptsDate/1234567890123")))
            .isEqualTo("\"Date: " + new Date(1234567890123L) + "\"");

        assertThat(body(get(service, "/test/acceptsEnum/ONE")))
            .isEqualTo("\"Enum: ONE\"");

    }

    @Test
    public void shouldSupportregexInPathParams() throws Exception {
        assertThat(
            body(get(service, "/test/acceptsSlashVar/my/var/with/slashes")))
            .isEqualTo("\"var: my/var/with/slashes\"");

    }

    @Test
    public void shouldSupportTrailingSlashInResource() {
        assertThat(get(service, "/test/trailingSlash/").getStatus()).isEqualTo(HttpResponseStatus.OK);
        assertThat(get(service, "/test/trailingSlash").getStatus()).isEqualTo(HttpResponseStatus.OK);
    }

    @Test
    public void shouldSupportWhitespace() {
        assertThat(get(service, "/test/accepts").getStatus()).isEqualTo(HttpResponseStatus.OK);
        assertThat(get(service, "/test/accepts    ").getStatus()).isEqualTo(HttpResponseStatus.OK);
        assertThat(get(service, "/test/accepts\t\t").getStatus()).isEqualTo(HttpResponseStatus.OK);
    }

    @Test
    public void shouldSupportSimpleFormParamTypes() {
        assertThat(
            body(post(service, "/test/acceptsPostString", "myString=accepts")))
            .isEqualTo("\"String: accepts\"");

        assertThat(
            body(post(service, "/test/acceptsPostBoolean", "myBoolean=true")))
            .isEqualTo("\"Boolean: true\"");

        assertThat(
            body(post(service, "/test/acceptsPostInteger", "myInteger=678")))
            .isEqualTo("\"Integer: 678\"");

        assertThat(body(post(service, "/test/acceptsPostLong", "myLong=678")))
            .isEqualTo("\"Long: 678\"");

        assertThat(
            body(post(service, "/test/acceptsPostDouble", "myDouble=678.78")))
            .isEqualTo("\"Double: 678.78\"");

        assertThat(body(
            post(service, "/test/acceptsPostDate", "myDate=1234567890123")))
            .isEqualTo("\"Date: " + new Date(1234567890123L) + "\"");

        assertThat(body(post(service, "/test/acceptsPostEnum", "myEnum=ONE")))
            .isEqualTo("\"Enum: ONE\"");
    }

    @Test
    public void shouldSupportSimpleHeaderParamTypes() {
        assertThat(body(JaxRsTestUtil.getWithHeaders(service,
            "/test/acceptsHeaderString",
            new HashMap<String, List<String>>() {
                {
                    put("myHeader", asList("accepts"));
                }
            }
        ))).isEqualTo("\"header: accepts\"");

        assertThat(body(JaxRsTestUtil.getWithHeaders(service,
            "/test/acceptsHeaderInteger",
            new HashMap<String, List<String>>() {
                {
                    put("myHeader", asList("4"));
                }
            }
        ))).isEqualTo("\"header: 4\"");
        assertThat(body(JaxRsTestUtil.getWithHeaders(service, "/test/acceptsHeaderEnum",
            new HashMap<String, List<String>>() {
                {
                    put("myHeader", asList("ONE"));
                }
            }
        ))).isEqualTo("\"header: ONE\"");
    }

    @Test
    public void shouldSupportMissingHeaderParams() {
        assertThat(body(JaxRsTestUtil.getWithHeaders(service,
            "/test/acceptsHeaderString",
            new HashMap<String, List<String>>() {
                {
                    put("dummy", asList("accepts"));
                }
            }
        ))).isEqualTo("\"header: null\"");
    }

    @Test
    public void shouldSupportMissingNullableParams() {
        assertThat(body(post(service, "/test/acceptsPostBoolean", "dummy=true")))
            .isEqualTo("\"Boolean: null\"");

        assertThat(body(post(service, "/test/acceptsPostInteger", "dummy=678")))
            .isEqualTo("\"Integer: null\"");

        assertThat(body(post(service, "/test/acceptsPostLong", "dummy=678")))
            .isEqualTo("\"Long: null\"");

        assertThat(body(post(service, "/test/acceptsPostDouble", "dummy=678")))
            .isEqualTo("\"Double: null\"");

    }

    @Test
    public void shouldGiveErrorForMissingNotNullableParams() {
        assertBadRequest(post(service, "/test/acceptsPostNotNullBool", "dummy=true"),
            "{'id':'.*','error':'validation','fields':[{'field':'myBoolean','error':'validation.invalid.boolean'}]}");
        assertBadRequest(post(service, "/test/acceptsPostNotNullInt", "dummy=678"),
            "{'id':'.*','error':'validation','fields':[{'field':'myInteger','error':'validation.invalid.int'}]}");
        assertBadRequest(post(service, "/test/acceptsPostNotNullLong", "dummy=678"),
            "{'id':'.*','error':'validation','fields':[{'field':'myLong','error':'validation.invalid.long'}]}");
        assertBadRequest(post(service, "/test/acceptsPostNotNullDouble", "dummy=678"),
            "{'id':'.*','error':'validation','fields':[{'field':'myDouble','error':'validation.invalid.double'}]}");
    }

    @Test
    public void shouldSupportOverloadedMethods() throws Exception {
        assertThat(body(get(service, "/test/overloadedMethod?param1=myparam")))
            .isEqualTo("\"Param1: myparam, Param2: null\"");
        assertThat(body(get(service, "/test/overloadedMethod")))
            .isEqualTo("\"Param1: null, Param2: null\"");
        assertThat(body(get(service, "/test/overloadedMethod?param1=myparam&param2=myparam2")))
            .isEqualTo("\"Param1: myparam, Param2: myparam2\"");
    }

    private void assertBadRequest(MockHttpServerResponse response,
        String expectedBodyRegex
    ) {
        if (!expectedBodyRegex.contains("\"")) {
            expectedBodyRegex = expectedBodyRegex.replaceAll("'", "\\\"");
        } else {
            expectedBodyRegex = expectedBodyRegex.replaceAll("'", "\\'");
            expectedBodyRegex = expectedBodyRegex.replaceAll("\"", "\\\"");
        }
        expectedBodyRegex = expectedBodyRegex
            .replaceAll("\\(", "\\\\(")
            .replaceAll("\\)", "\\\\)")
            .replaceAll("\\{", "\\\\{")
            .replaceAll("\\}", "\\\\}")
            .replaceAll("\\[", "\\\\[")
            .replaceAll("\\]", "\\\\]");
        assertThat(response.getStatus())
            .isEqualTo(HttpResponseStatus.BAD_REQUEST);
        assertThat(response.getOutp())
            .matches(expectedBodyRegex);
    }

    @Test
    public void shouldGiveErrorForBadRequests() throws Exception {
        assertBadRequest(get(service, "/test/acceptsInteger?myarg=badvalue"),
            "{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.int'}]}");
        assertBadRequest(get(service, "/test/acceptsLong?myarg=badvalue"),
            "{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.long'}]}");
        assertBadRequest(get(service, "/test/acceptsDouble?myarg=badvalue"),
            "{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.double'}]}");
    }

    @Test
    public void shouldGiveErrorForBadDates() throws Exception {
        assertBadRequest(get(service, "/test/acceptsDate?myarg=20aa-01-01"),
            "{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.date'}]}");

        assertBadRequest(get(service, "/test/acceptsDate?myarg=2010-0b-01 00:00:00"),
            "{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.date'}]}");

        assertBadRequest(get(service, "/test/acceptsDate?myarg=2010-01-qehjeq:00:00.000"),
            "{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.date'}]}");

        assertBadRequest(get(service, "/test/acceptsDate?myarg=97697697676767688675767"),
            "{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.date'}]}");

    }

    @Test
    public void shouldSupportDatesAsString() throws Exception {
        assertThat(body(
            get(service, "/test/acceptsDate?myarg=2010-01-01")))
            .isEqualTo("\"2010-01-01T00:00:00.000+0000\"");

        assertThat(body(get(service, "/test/acceptsDate?myarg=2010-01-01T00:00:00.000")))
            .isEqualTo("\"2010-01-01T00:00:00.000+0000\"");
    }

    class CustomDateFormat extends StdDateFormat {

        public CustomDateFormat() {
            super(TimeZone.getTimeZone("Europe/Stockholm"), Locale.getDefault(), true);
        }

        @Override
        public Date parse(String source) throws ParseException {
            try {
                return super.parse(source);
            } catch (ParseException e) {
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                fmt.setTimeZone(TimeZone.getTimeZone("Europe/Stockholm"));
                return fmt.parse(source);
            }
        }

        @Override
        public StdDateFormat clone() {
            return new CustomDateFormat();
        }
    }


    @Test
    public void shouldSupportCustomDates() throws Exception {
        Module customDateModule = new AbstractModule() {
            @Override
            protected void configure() {
                bind(DateFormat.class).toProvider(() -> new CustomDateFormat());
                bind(JaxRsResourcesProvider.class).toInstance(() -> new Object[]{service});
                bind(ByteBufCollector.class).toInstance(new ByteBufCollector());
                bind(new TypeLiteral<Set<ParamResolver>>() {{
                }}).toInstance(Collections.EMPTY_SET);
                bind(new TypeLiteral<Set<ParamResolverFactory>>() {{
                }}).toInstance(Collections.EMPTY_SET);
                bind(ResultTransformerFactories.class).toInstance(new ResultTransformerFactories());
                bind(ObjectMapper.class).toInstance(new ObjectMapper()
                    .findAndRegisterModules()
                    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    .setDateFormat(new CustomDateFormat()));
            }
        };
        Injector            injector = Guice.createInjector(customDateModule);
        JaxRsRequestHandler handler  = injector.getInstance(JaxRsRequestHandler.class);

        assertThat(
            body(
                JaxRsTestUtil.processRequestWithHandler(handler, new MockHttpServerRequest("/test/acceptsDate?myarg=2010-01-01 00:00:00"))))
            .isEqualTo("\"2010-01-01T00:00:00.000+0100\"");
    }

    @Test
    public void shouldSupportLocalDate() {
        assertThat(body(
            get(service, "/test/acceptsLocalDate?myarg=2010-01-01")))
            .isEqualTo("{\"localDate\":\"2010-01-01\"}");
    }

    @Test
    public void shouldSupportLocalTime() {
        assertThat(body(
            get(service, "/test/acceptsLocalTime?myarg=13:37:37")))
            .isEqualTo("{\"localTime\":\"13:37:37\"}");
    }

    @Test
    public void shouldReturnErrorWhenServiceThrowsError() {
        assertThat(get(service,
            "/test/throwInsufficientStorage").getStatus()).isEqualTo(HttpResponseStatus.INSUFFICIENT_STORAGE);
    }

    @Test
    public void shouldReturnErrorWhenServiceThrowsRuntimeException() {
        assertThat(get(service,
            "/test/throwRuntimeException").getStatus()).isEqualTo(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void shouldReturnErrorWhenServiceThrowsException() {
        assertThat(get(service,
            "/test/throwException").getStatus()).isEqualTo(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void shouldReturnErrorForBadJson() {
        String expectedBodyRegex1 = "{\"id\":\".*\",\"error\":\"invalidjson\"," +
            "\"message\":\"Unexpected character ('h' (code 104)): was expecting double-quote to start field name\"}";
        assertBadRequest(post(service, "/test/jsonParam", "{hej}"), expectedBodyRegex1);

        String expectedBodyRegex2 = "{\"id\":\".*\",\"error\":\"invalidjson\"," +
            "\"message\":\"Cannot deserialize value of type `int` from String \\\\\"nan\\\\\": not a valid Integer value\"}";
        assertBadRequest(post(service, "/test/jsonParam", "{\"age\": \"nan\"}"), expectedBodyRegex2);

        String expectedBodyRegex3 = "{\"id\":\".*\",\"error\":\"invalidjson\"," +
            "\"message\":\"No content to map due to end-of-input\"}";
        assertBadRequest(post(service, "/test/jsonParam", (String)null), expectedBodyRegex3);
    }

    @Test
    public void shouldAcceptTextPlainInput() {
        String                 text = "my plain text";
        MockHttpServerResponse resp = post(service, "/test/textPlain", text);
        assertThat(resp.getStatus()).isEqualTo(HttpResponseStatus.CREATED);
        assertThat(resp.getOutp()).isEqualTo("\"" + text + "\"");
    }

    @Test
    public void shouldAcceptByteArrayInput() {
        String                 text = "my bytes";
        MockHttpServerResponse resp = post(service, "/test/byteArray", text);
        assertThat(resp.getStatus()).isEqualTo(HttpResponseStatus.CREATED);
        assertThat(resp.getOutp()).isEqualTo("\"" + text + "\"");
    }

    @Test
    public void shouldAcceptNonObservableReturnTypes() {
        assertThat(get(service,
            "/test/returningString").getStatus()).isEqualTo(HttpResponseStatus.OK);
    }

    @Test
    public void shouldGiveErrorForBadEnumValue() {
        assertBadRequest(post(service, "/test/acceptsPostEnum", "myEnum=BAD"),
            "{'id':'.*','error':'validation','fields':[{'field':'myEnum','error':'validation.invalid.enum'}]}");
    }

    @Test
    public void shouldGiveErrorForUnparsableType() {

        @Path("test")
        class InvalidQueryParam {
            @Path("acceptsComplexQueryParam")
            @GET
            public Observable<String> acceptsComplexQueryParam(@QueryParam("paramEntity") ParamEntity paramEntity) {
                return null;
            }
        }

        try {
            JaxRsTestUtil.resources(new InvalidQueryParam());
            fail("expected exception");
        } catch (RuntimeException e) {
            String expected = "Field of type class se.fortnox.reactivewizard.jaxrs.ParamEntity is not allowed to be used in query/form/header";
            assertThat(e.getMessage()).isEqualTo(expected);
            return;
        }
        fail("expected exception");
    }

    @Test
    public void shouldHandleNoMatchingResource() {
        assertThat(get(service, "/noservice/nomethod").getStatus())
            .isEqualTo(HttpResponseStatus.NOT_FOUND);
    }

    @Test
    public void shouldSupportUuidAsQueryParameter() {
        UUID uuid = UUID.randomUUID();
        assertThat(body(
            get(service, "/test/acceptsUuid?id=" + uuid.toString())))
            .isEqualTo("\"Id: " + uuid + "\"");
    }

    @Test
    public void shouldSupportEmptyUuidAsQueryParameter() {
        assertThat(body(
            get(service, "/test/acceptsUuid?id=")))
            .isEqualTo("\"Id: null\"");

        assertThat(body(
            get(service, "/test/acceptsUuid")))
            .isEqualTo("\"Id: null\"");
    }

    @Test
    public void shouldSupportUuidAsFormParameter() {
        UUID uuid = UUID.randomUUID();
        assertThat(body(
            post(service, "/test/acceptsUuid", "id=" + uuid)))
            .isEqualTo("\"Id: " + uuid + "\"");
    }

    @Test
    public void shouldSupportUuidAsPathParameter() {
        UUID uuid = UUID.randomUUID();
        assertThat(body(
            get(service, "/test/acceptsUuid/" + uuid)))
            .isEqualTo("\"Id: " + uuid + "\"");
    }

    @Test
    public void shouldGive400ErrorForBadUuid() {
        MockHttpServerResponse response = get(service, "/test/acceptsUuid/baduuid");
        assertThat(response.getStatus()).isEqualTo(HttpResponseStatus.BAD_REQUEST);
        assertThat(body(response)).contains("\"error\":\"validation\",\"fields\":[{\"field\":\"id\",\"error\":\"validation.invalid.uuid\"}]}");
    }

    @Test
    public void shouldSupportUuidAsHeader() {
        UUID uuid = UUID.randomUUID();
        assertThat(body(JaxRsTestUtil.getWithHeaders(service,
            "/test/acceptsUuidHeader",
            new HashMap<String, List<String>>() {
                {
                    put("id", asList(uuid.toString()));
                }
            }
        ))).isEqualTo("\"Id: " + uuid.toString() + "\"");
    }

    @Test
    public void shouldSetContentType() {
        assertThat(get(new Testresource(), "/test/acceptsString").getHeader("Content-Type"))
            .isEqualTo("application/json");
    }

    @Test
    public void shouldSupportReturningNullFromResource() {
        assertThat(get(new Testresource(), "/test/returnsNull").getStatus())
            .isEqualTo(HttpResponseStatus.NO_CONTENT);
    }

    @Test
    public void shouldMatchExactPathBeforePathparam() {
        SpecialResource service = new SpecialResource();
        assertThat(body(get(service, "/special/single/frenberg"))).isEqualTo("\"frenberg\"");
        assertThat(body(get(service, "/special/single/strings"))).isEqualTo("[\"string\",\"string\"]");
        assertThat(body(get(service, "/special/observable/frenberg"))).isEqualTo("\"frenberg\"");
        assertThat(body(get(service, "/special/observable/strings"))).isEqualTo("[\"string\",\"string\"]");
    }

    @Test
    public void shouldSupportGenericParams() {
        assertThat(body(post(service, "/test/generic-param", "[{\"name\":\"test\"}]"))).isEqualTo("\"ParamEntity\"");
    }

    @Test
    public void shouldSupportGenericParamsWhenProxied() {
        TestresourceInterface proxy = (TestresourceInterface)Proxy.newProxyInstance(
            TestresourceInterface.class.getClassLoader(),
            new Class[]{TestresourceInterface.class},
            (instance, method, args) -> method.invoke(service, args)
        );
        assertThat(body(post(proxy, "/test/generic-param", "[{\"name\":\"test\"}]"))).isEqualTo("\"ParamEntity\"");
    }

    @Test
    public void shouldResolveCookieParam() {
        assertThat(body(getWithHeaders(service, "/test/acceptsCookieParam", new HashMap() {
            {
                put("Cookie", asList("fnox_session=testcookie"));
            }
        }))).isEqualTo("\"testcookie\"");
    }

    @Test
    public void shouldAcceptBodyForPut() throws Exception {
        assertThat(body(put(service, "/test/acceptBodyPut", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0,\"items\":null}");
    }

    @Test
    public void shouldAcceptBodyForPost() throws Exception {
        assertThat(body(post(service, "/test/acceptBodyPost", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0,\"items\":null}");
    }

    @Test
    public void shouldAcceptBodyForPatch() throws Exception {
        assertThat(body(patch(service, "/test/acceptBodyPatch", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0,\"items\":null}");
    }

    @Test
    public void shouldAcceptBodyForDelete() throws Exception {
        assertThat(body(delete(service, "/test/acceptBodyDelete", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0,\"items\":null}");
    }

    @Test
    public void shouldDenyLargeBodies() {
        assertBadRequest(
            post(service, "/test/acceptsPostString", new byte[11 * 1024 * 1024]),
            "{'id':'.*','error':'too.large.input'}");
    }

    @Test
    public void shouldAcceptLargeBodiesWithinLimits() {
        assertThat(post(service, "/test/acceptsPostString", new byte[10 * 1024 * 1024]).getStatus())
            .isEqualTo(HttpResponseStatus.CREATED);
    }

    @Test
    public void shouldAcceptQueryParamArrayValues() {
        assertThat(get(service, "/test/acceptsQueryArray?Stringarray=val1,val2,val3").getOutp()).isEqualTo("3");
        assertThat(get(service, "/test/acceptsQueryArray?Integerarray=1,2,3,4").getOutp()).isEqualTo("4");
    }

    @Test
    public void shouldAcceptQueryParamListValues() {
        assertThat(get(service, "/test/acceptsQueryList?Stringlist=val1,val2,val3").getOutp()).isEqualTo("3");
        assertThat(get(service, "/test/acceptsQueryList?Integerlist=1,2,3,4").getOutp()).isEqualTo("4");
    }

    @Test
    public void shouldAcceptQueryParamListWithEnumValues() {
        assertThat(get(service, "/test/acceptsQueryListWithEnum?EnumList=ONE,TWO,THREE").getOutp()).isEqualTo("3");
    }

    @Test
    public void shouldAcceptBeanParam() {
        assertThat(get(service, "/test/acceptsBeanParam?name=foo&age=3&items=1,2").getOutp()).isEqualTo("\"foo - 3 2\"");
    }

    @Test
    public void shouldAcceptBeanParamInherited() {
        assertThat(get(service, "/test/acceptsBeanParamInherited?name=foo&age=3&items=1,2&inherited=YES").getOutp()).isEqualTo("\"foo - 3 2 - YES\"");
    }

    @Test
    public void shouldGive400ErrorForUnescapedUrls() {
        try {
            get(service, "/test/acceptsString/something% ");
            fail("Expected exception");
        } catch (WebException e) {
            assertThat(e.getStatus().code()).isEqualTo(400);
        }
    }


    @Path("test")
    class Testresource {
        @Path("acceptsString")
        @GET
        public Observable<String> acceptsString(@QueryParam("myarg") String myarg) {
            return just("inp: " + myarg);
        }

        @Path("returnsNull")
        @GET
        public Observable<String> returnsNull() {
            return null;
        }

        @Path("serverSideAnnotationsOnClassAsResource")
        @GET
        @Headers("Content-Disposition: attachment; filename=auditlog.csv")
        public Observable<String> shouldIgnoreHeaderAnnotation() {
            return just("");
        }
    }

    class LocalDateContainer {
        private LocalDate localDate;

        public LocalDateContainer(LocalDate localDate) {
            this.localDate = localDate;
        }

        public LocalDate getLocalDate() {
            return localDate;
        }

        public void setLocalDate(LocalDate localDate) {
            this.localDate = localDate;
        }
    }

    class LocalTimeContainer {
        private LocalTime localTime;

        LocalTimeContainer(LocalTime localTime) {
            this.localTime = localTime;
        }

        public LocalTime getLocalTime() {
            return localTime;
        }

        public void setLocalTime(LocalTime localTime) {
            this.localTime = localTime;
        }
    }

    @Path("test")
    public interface TestresourceInterface {
        @Path("accepts")
        @GET
        Observable<String> acceptsString(@QueryParam("myarg") String myarg);

        @Path("acceptsBoolean")
        @GET
        Observable<String> acceptsBoolean(@QueryParam("myarg") Boolean myarg);

        @Path("acceptsInteger")
        @GET
        Observable<String> acceptsInteger(@QueryParam("myarg") Integer myarg);

        @Path("acceptsLong")
        @GET
        Observable<String> acceptsLong(@QueryParam("myarg") Long myarg);

        @Path("acceptsDouble")
        @GET
        Observable<String> acceptsDouble(@QueryParam("myarg") Double myarg);

        @Path("acceptsDate")
        @GET
        Observable<Date> acceptsDate(@QueryParam("myarg") Date myarg);

        @Path("acceptsLocalDate")
        @GET
        Observable<LocalDateContainer> acceptsLocalDate(@QueryParam("myarg") LocalDate myarg);

        @Path("acceptsLocalTime")
        @GET
        Observable<LocalTimeContainer> acceptsLocalTime(@QueryParam("myarg") LocalTime myarg);

        @Path("acceptsEnum")
        @GET
        Observable<String> acceptsEnum(@QueryParam("myarg") TestEnum myarg);

        @Path("defaultQuery")
        @GET
        Observable<String> acceptDefaultQueryParam(@QueryParam("myarg") @DefaultValue("5") int myarg);

        @Path("acceptsString/{myarg}")
        @GET
        Observable<String> acceptsStringPath(@PathParam("myarg") String myarg);

        @Path("acceptsBoolean/{myarg}")
        @GET
        Observable<String> acceptsBooleanPath(@PathParam("myarg") Boolean myarg);

        @Path("acceptsInteger/{myarg}")
        @GET
        Observable<String> acceptsIntegerPath(@PathParam("myarg") Integer myarg);

        @Path("acceptsLong/{myarg}")
        @GET
        Observable<String> acceptsLongPath(@PathParam("myarg") Long myarg);

        @Path("acceptsStrictLong/{myarg}")
        @GET
        Observable<String> acceptsStrictLongPath(@PathParam("myarg") long myarg);

        @Path("acceptsDate/{myarg}")
        @GET
        Observable<String> acceptsDatePath(@PathParam("myarg") Date myarg);

        @Path("acceptsEnum/{myarg}")
        @GET
        Observable<String> acceptsEnumPath(@PathParam("myarg") TestEnum myarg);

        @Path("acceptsSlashVar/{myarg:.*}")
        @GET
        Observable<String> acceptsSlashVar(@PathParam("myarg") String myarg);

        @Path("acceptsPostString")
        @POST
        Observable<String> acceptsPostString(@FormParam("myString") String myarg);

        @Path("acceptsPostBoolean")
        @POST
        Observable<String> acceptsPostBoolean(@FormParam("myBoolean") Boolean myarg);

        @Path("acceptsPostInteger")
        @POST
        Observable<String> acceptsPostInteger(@FormParam("myInteger") Integer myarg);

        @Path("acceptsPostLong")
        @POST
        Observable<String> acceptsPostLong(@FormParam("myLong") Long myarg);

        @Path("acceptsPostDouble")
        @POST
        Observable<String> acceptsPostDouble(@FormParam("myDouble") Double myarg);

        @Path("acceptsPostDate")
        @POST
        Observable<String> acceptsPostDate(@FormParam("myDate") Date myarg);

        @Path("acceptsPostEnum")
        @POST
        Observable<String> acceptsPostEnum(@FormParam("myEnum") TestEnum myarg);

        @Path("acceptsPostNotNullBool")
        @POST
        Observable<String> acceptsPostNotNullBoolean(@FormParam("myBoolean") boolean myarg);

        @Path("acceptsPostNotNullInt")
        @POST
        Observable<String> acceptsPostNotNullInteger(@FormParam("myInteger") int myarg);

        @Path("acceptsPostNotNullLong")
        @POST
        Observable<String> acceptsPostNotNullLong(@FormParam("myLong") long myarg);

        @Path("acceptsPostNotNullDouble")
        @POST
        Observable<String> acceptsPostNotNullDouble(@FormParam("myDouble") double myarg);

        @Path("acceptsDefaultForm")
        @POST
        Observable<String> acceptsDefaultFormParam(@FormParam("myarg") @DefaultValue("5") int myarg);

        @Path("acceptsHeaderString")
        @GET
        Observable<String> acceptsHeader(@HeaderParam("myHeader") String myHeader);

        @Path("acceptsHeaderInteger")
        @GET
        Observable<String> acceptsHeaderInteger(@HeaderParam("myHeader") Integer myHeader);

        @Path("acceptsHeaderEnum")
        @GET
        Observable<String> acceptsHeaderEnum(@HeaderParam("myHeader") TestEnum myHeader);

        @Path("acceptsDefaultHeader")
        @GET
        Observable<String> acceptsDefaultHeaderParam(@HeaderParam("myHeader") @DefaultValue("5") int myHeader);

        @Path("overloadedMethod")
        @GET
        Observable<String> overloadedMethod(@QueryParam("param1") String param1, @QueryParam("param2") String param2);

        @Path("overloadedMethod")
        @GET
        Observable<String> overloadedMethod(@QueryParam("param1") String param1);

        @Path("overloadedMethod")
        @GET
        Observable<String> overloadedMethod();

        @GET
        @Path("throwInsufficientStorage")
        Observable<String> throwInsufficientStorage();

        @GET
        @Path("throwRuntimeException")
        Observable<String> throwRuntimeException();

        @GET
        @Path("throwException")
        Observable<String> throwException();

        @GET
        @Path("returningString")
        String returningString();

        @POST
        @Path("jsonParam")
        Observable<String> jsonParam(ParamEntity paramEntity);

        @POST
        @Path("textPlain")
        @Consumes(MediaType.TEXT_PLAIN)
        Observable<String> textPlain(String input);

        @POST
        @Path("byteArray")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        Observable<String> byteArray(byte[] input);

        @GET
        @Path("acceptsUuid")
        Observable<String> acceptsUuidQueryParam(@QueryParam("id") UUID id);

        @POST
        @Path("acceptsUuid")
        Observable<String> acceptsUuidFormParam(@FormParam("id") UUID id);

        @GET
        @Path("acceptsUuid/{id}")
        Observable<String> acceptsUuidPathParam(@PathParam("id") UUID id);

        @GET
        @Path("acceptsUuidHeader")
        Observable<String> acceptsUuidHeader(@HeaderParam("id") UUID id);

        @POST
        @Path("generic-param")
        Observable<String> acceptsGenericParam(List<ParamEntity> list);

        @GET
        @Path("trailingSlash/")
        Observable<String> acceptsTrailingSlash();

        @Path("acceptsCookieParam")
        @GET
        Observable<String> acceptsCookieParam(@CookieParam("fnox_session") String cookie);

        @Path("acceptsDefaultCookie")
        @GET
        Observable<String> acceptsDefaultCookieParam(@CookieParam("myCookie") @DefaultValue("5") int myCookie);

        @Path("acceptBodyGet")
        @GET
        Observable<ParamEntity> acceptBodyGet(ParamEntity paramEntity);

        @Path("acceptBodyPut")
        @PUT
        Observable<ParamEntity> acceptBodyPut(ParamEntity paramEntity);

        @Path("acceptBodyPost")
        @POST
        Observable<ParamEntity> acceptBodyPost(ParamEntity paramEntity);

        @Path("acceptBodyPatch")
        @PATCH
        Observable<ParamEntity> acceptBodyPatch(ParamEntity paramEntity);

        @Path("acceptBodyDelete")
        @DELETE
        Observable<ParamEntity> acceptBodyDelete(ParamEntity paramEntity);

        @Path("acceptsQueryList")
        @GET
        Observable<Integer> acceptsQueryList(@QueryParam("Stringlist") List<String> strings,
            @QueryParam("Integerlist") List<Integer> integers
        );

        @Path("acceptsQueryArray")
        @GET
        Observable<Integer> acceptsQueryArray(@QueryParam("Stringarray") String[] strings,
            @QueryParam("Integerarray") Integer[] integers
        );

        @Path("acceptsQueryListWithEnum")
        @GET
        Observable<Integer> acceptsQueryListWithEnum(@QueryParam("EnumList") List<TestEnum> enums);

        @Path("shouldIgnoreHeadersAnnotationOnInterface")
        @GET
        @Headers("Content-Disposition: attachment; filename=test.csv")
        Observable<String> ignoresHeaderAnnotationOnInterface();

        @Path("acceptsHeadersOnImplementation")
        @GET
        Observable<String> acceptsHeadersOnImplementation();

        @Path("acceptsBeanParam")
        @GET
        Observable<String> acceptsBeanParam(@BeanParam ParamEntity beanParam);

        @Path("acceptsBeanParamInherited")
        @GET
        Observable<String> acceptsBeanParamInherited(@BeanParam InheritedParamEntity beanParam);
    }

    class TestresourceImpl implements TestresourceInterface {

        @Override
        public Observable<String> acceptsString(String myarg) {
            return just("accepts from interface: " + myarg);
        }

        @Override
        public Observable<String> acceptsBoolean(Boolean myarg) {
            return just("Boolean: " + myarg);
        }

        @Override
        public Observable<String> acceptsInteger(Integer myarg) {
            return just("Integer: " + myarg);
        }

        @Override
        public Observable<String> acceptsLong(Long myarg) {
            return just("Long: " + myarg);
        }

        @Override
        public Observable<Date> acceptsDate(Date myarg) {
            return just(myarg);
        }

        @Override
        public Observable<LocalDateContainer> acceptsLocalDate(LocalDate myarg) {
            return just(new LocalDateContainer(myarg));
        }

        @Override
        public Observable<LocalTimeContainer> acceptsLocalTime(LocalTime myarg) {
            return just(new LocalTimeContainer(myarg));
        }

        @Override
        public Observable<String> acceptsEnum(TestEnum myarg) {
            return just("Enum: " + myarg);
        }

        @Override
        public Observable<String> acceptDefaultQueryParam(int myarg) {
            return just("Default: " + myarg);
        }

        @Override
        public Observable<String> acceptsStringPath(String myarg) {
            return just("String: "+myarg);
        }

        @Override
        public Observable<String> acceptsBooleanPath(Boolean myarg) {
            return just("Boolean: " + myarg);
        }

        @Override
        public Observable<String> acceptsIntegerPath(Integer myarg) {
            return just("Integer: " + myarg);
        }

        @Override
        public Observable<String> acceptsLongPath(Long myarg) {
            return just("Long: " + myarg);
        }

        @Override
        public Observable<String> acceptsStrictLongPath(long myarg) {
            return just("long: " + myarg);
        }

        @Override
        public Observable<String> acceptsDatePath(Date myarg) {
            return just("Date: " + myarg);
        }

        @Override
        public Observable<String> acceptsEnumPath(TestEnum myarg) {
            return just("Enum: " + myarg);
        }

        @Override
        public Observable<String> acceptsPostString(String myarg) {
            return just("String: " + myarg);
        }

        @Override
        public Observable<String> acceptsPostBoolean(Boolean myarg) {
            return just("Boolean: " + myarg);
        }

        @Override
        public Observable<String> acceptsPostInteger(Integer myarg) {
            return just("Integer: " + myarg);
        }

        @Override
        public Observable<String> acceptsPostLong(Long myarg) {
            return just("Long: " + myarg);
        }

        @Override
        public Observable<String> acceptsPostDate(Date myarg) {
            return just("Date: " + myarg);
        }

        @Override
        public Observable<String> acceptsPostEnum(TestEnum myarg) {
            return just("Enum: " + myarg);
        }

        @Override
        public Observable<String> acceptsPostNotNullBoolean(boolean myarg) {
            return just("bool: " + myarg);
        }

        @Override
        public Observable<String> acceptsPostNotNullInteger(int myarg) {
            return just("int: " + myarg);
        }

        @Override
        public Observable<String> acceptsPostNotNullLong(long myarg) {
            return just("long: " + myarg);
        }

        @Override
        public Observable<String> acceptsHeader(String myHeader) {
            return just("header: " + myHeader);
        }

        @Override
        public Observable<String> acceptsHeaderInteger(Integer myHeader) {
            return just("header: " + myHeader);
        }

        @Override
        public Observable<String> acceptsHeaderEnum(TestEnum myHeader) {
            return just("header: " + myHeader);
        }

        @Override
        public Observable<String> acceptsDefaultHeaderParam(int myHeader) {
            return just("Default: " + myHeader);
        }

        @Override
        public Observable<String> throwInsufficientStorage() {
            throw new WebException(HttpResponseStatus.INSUFFICIENT_STORAGE);
        }

        @Override
        public Observable<String> throwRuntimeException() {
            throw new RuntimeException();
        }

        @Override
        public Observable<String> throwException() {
            throw new AssertionError();
        }

        @Override
        public String returningString() {
            return "";
        }

        @Override
        public Observable<String> jsonParam(ParamEntity paramEntity) {
            return just("");
        }

        @Override
        public Observable<String> textPlain(String input) {
            return just(input);
        }

        @Override
        public Observable<String> byteArray(byte[] input) {
            return just(new String(input));
        }

        @Override
        public Observable<String> acceptsUuidQueryParam(UUID id) {
            return just("Id: " + (id != null ? id.toString() : null));
        }

        @Override
        public Observable<String> acceptsUuidFormParam(UUID id) {
            return just("Id: " + id.toString());
        }

        @Override
        public Observable<String> acceptsUuidPathParam(UUID id) {
            return just("Id: " + id.toString());
        }

        @Override
        public Observable<String> acceptsUuidHeader(UUID id) {
            return just("Id: " + id.toString());
        }

        @Override
        public Observable<String> acceptsGenericParam(List<ParamEntity> list) {
            Object listItem = list.get(0);
            return just(listItem.getClass().getSimpleName());
        }

        @Override
        public Observable<String> acceptsTrailingSlash() {
            return just("OK");
        }

        public Observable<String> acceptsCookieParam(String cookie) {
            return just(cookie);
        }

        @Override
        public Observable<String> acceptsDefaultCookieParam(int myCookie) {
            return just("Default: " + myCookie);
        }

        @Override
        public Observable<ParamEntity> acceptBodyGet(ParamEntity paramEntity) {
            return just(paramEntity);
        }

        @Override
        public Observable<ParamEntity> acceptBodyPut(ParamEntity paramEntity) {
            return just(paramEntity);
        }

        @Override
        public Observable<ParamEntity> acceptBodyPost(ParamEntity paramEntity) {
            return just(paramEntity);
        }

        @Override
        public Observable<ParamEntity> acceptBodyPatch(ParamEntity paramEntity) {
            return just(paramEntity);
        }

        @Override
        public Observable<ParamEntity> acceptBodyDelete(ParamEntity paramEntity) {
            return just(paramEntity);
        }

        @Override
        public Observable<Integer> acceptsQueryList(List<String> strings, List<Integer> integers) {
            if (strings != null) {
                return just(strings.size());
            }
            if (integers != null) {
                return just(integers.size());
            }
            throw new IllegalArgumentException();
        }

        @Override
        public Observable<Integer> acceptsQueryArray(String[] strings, Integer[] integers) {
            if (strings != null) {
                return just(strings.length);
            }
            if (integers != null) {
                return just(integers.length);
            }
            throw new IllegalArgumentException();
        }

        @Override
        public Observable<Integer> acceptsQueryListWithEnum(List<TestEnum> enums) {
            return just(enums.size());
        }

        @Override
        public Observable<String> ignoresHeaderAnnotationOnInterface() {
            return just("");
        }

        @Override
        @Headers("Content-Disposition: attachment; filename=auditlog.csv")
        public Observable<String> acceptsHeadersOnImplementation() {
            return just("");
        }

        @Override
        public Observable<String> acceptsBeanParam(ParamEntity beanParam) {
            return just(String.format("%s - %d %d", beanParam.getName(), beanParam.getAge(), beanParam.getItems().size()));
        }

        @Override
        public Observable<String> acceptsBeanParamInherited(InheritedParamEntity beanParam) {
            return just(String.format("%s - %d %d - %s", beanParam.getName(), beanParam.getAge(), beanParam.getItems().size(), beanParam.getInherited()));
        }

        @Override
        public Observable<String> acceptsSlashVar(String myarg) {
            return just("var: " + myarg);
        }

        @Override
        public Observable<String> acceptsDouble(Double myarg) {
            return just("Double: " + myarg);
        }

        @Override
        public Observable<String> acceptsPostDouble(Double myarg) {
            return just("Double: " + myarg);
        }

        @Override
        public Observable<String> acceptsPostNotNullDouble(double myarg) {
            return just("double: " + myarg);
        }

        @Override
        public Observable<String> acceptsDefaultFormParam(int myarg) {
            return just("Default: " + myarg);
        }

        @Override
        public Observable<String> overloadedMethod(String param1, String param2) {
            return Observable.just("Param1: " + param1 + ", Param2: " + param2);
        }

        @Override
        public Observable<String> overloadedMethod(String param1) {
            return Observable.just("Param1: " + param1);
        }

        @Override
        public Observable<String> overloadedMethod() {
            return null;
        }
    }

    @Path("/test/accepts/res")
    class FooTest {
        @GET
        public Observable<String> test(Foo foo) {
            return just("foo: " + foo.getStr());
        }
    }

    @Path("special")
    class SpecialResource {
        @GET
        @Path("single/{string}")
        public Single<String> getStringSingle(@PathParam("string") String string) {
            return Single.just(string);
        }

        @GET
        @Path("single/strings")
        public Single<List<String>> getStringsSingle() {
            return Single.just(asList("string", "string"));
        }

        @GET
        @Path("observable/{string}")
        public Observable<String> getStringObservable(@PathParam("string") String string) {
            return just(string);
        }

        @GET
        @Path("observable/strings")
        public Observable<List<String>> getStringsObservable() {
            return just(asList("string", "string"));
        }
    }

    @Path("default")
    class DefaultPathParamResource {
        @GET
        @Path("param/{path}")
        public Observable<String> defaultPath(@PathParam("path") @DefaultValue("defaultPath") String path) {
            return just("");
        }
    }

}

interface Foo {

	String getStr();

}
