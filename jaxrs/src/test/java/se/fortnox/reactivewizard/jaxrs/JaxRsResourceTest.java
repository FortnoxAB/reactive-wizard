package se.fortnox.reactivewizard.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.google.inject.Module;
import com.google.inject.*;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import se.fortnox.reactivewizard.jaxrs.params.*;
import se.fortnox.reactivewizard.jaxrs.params.annotated.AnnotatedParamResolverFactories;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.DeserializerFactory;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResult;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResultFactoryFactory;
import se.fortnox.reactivewizard.jaxrs.response.ResultTransformerFactories;
import se.fortnox.reactivewizard.mocks.MockHttpServerRequest;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;
import se.fortnox.reactivewizard.test.LoggingVerifier;
import se.fortnox.reactivewizard.test.LoggingVerifierExtension;
import se.fortnox.reactivewizard.utils.JaxRsTestUtil;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Proxy;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static java.util.Arrays.asList;
import static org.apache.logging.log4j.Level.INFO;
import static org.apache.logging.log4j.Level.WARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.just;
import static se.fortnox.reactivewizard.utils.JaxRsTestUtil.*;

@ExtendWith(LoggingVerifierExtension.class)
class JaxRsResourceTest {

    enum TestEnum {
        ONE, TWO, THREE
    }

    private final TestresourceInterface service = new TestresourceImpl();

    LoggingVerifier jaxRsRequestLoggingVerifier = new LoggingVerifier(JaxRsRequest.class);

    LoggingVerifier paramResolverFactoriesLoggingVerifier = new LoggingVerifier(ParamResolverFactories.class);

    @Test
    void shouldConcatPaths() {
        JaxRsResources resources = new JaxRsResources(new Object[]{new Testresource()}, new JaxRsResourceFactory(), false);
        JaxRsRequest jaxRsRequest = new JaxRsRequest(new MockHttpServerRequest("/test/acceptsString"), new ByteBufCollector());
        assertThat(resources.findResource(jaxRsRequest).call(jaxRsRequest)).isNotNull();
    }

    @Test
    void shouldfailToInitializeResourcesReturningObservables() {
        Assertions.assertThatThrownBy(() -> new JaxRsResources(new Object[]{new TestResourceWithObservables()}, new JaxRsResourceFactory(), false))
            .hasMessage("Can only serve methods that are of type Flux or Mono. " +
                "public rx.Observable se.fortnox.reactivewizard.jaxrs.JaxRsResourceTest$TestResourceWithObservables.shouldFail()" +
                " had unsupported return type class rx.Observable");
    }

    @Test
    void shouldResolveArgs() {
        assertThat(body(get(new Testresource(), "/test/acceptsString?myarg=hepp")))
            .isEqualTo("\"inp: hepp\"");
    }

    @Test
    void shouldHandleHeaderOnClassAsAResource() {
        assertThat(get(new Testresource(), "/test/serverSideAnnotationsOnClassAsResource").responseHeaders().get("Content-Disposition")).isNotNull();
    }

    @Test
    void shouldIgnoreHeaderOnInterface() {
        assertThat(get(new Testresource(), "/test/shouldIgnoreHeadersAnnotationOnInterface").responseHeaders().get("Content-Disposition")).isNull();
    }

    @Test
    void shouldAcceptHeaderOnImplementingClass() {
        assertThat(get(service, "/test/acceptsHeadersOnImplementation").responseHeaders().get("Content-Disposition")).isNotNull();
    }

    @Test
    void shouldHandleInterfaceAnnotations() throws Exception {
        assertThat(body(get(service, "/test/accepts?myarg=hepp")))
            .isEqualTo("\"accepts from interface: hepp\"");
    }

    @Test
    void shouldResolveCustomType() throws IllegalArgumentException {

        MockHttpServerRequest req = new MockHttpServerRequest("/test/accepts/res?fid=5678");
        req.cookies().put("fnox_5678", new HashSet<>(asList(new DefaultCookie("fnox_5678", "888"))));

        Foo foo = mock(Foo.class);
        when(foo.getStr()).thenReturn("5678");
        ParamResolver<Foo> fooResolver = new ParamResolver<Foo>() {
            @Override
            public Mono<Foo> resolve(JaxRsRequest request) {
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
                new RequestLogger()),
            false);
        JaxRsRequest jaxRsRequest = new JaxRsRequest(req, new ByteBufCollector());
        Mono<? extends JaxRsResult<?>> result = jaxRsResources.findResource(jaxRsRequest).call(jaxRsRequest);

        MockHttpServerResponse response = new MockHttpServerResponse();

        Flux.from(result.block().write(response)).count().block();

        assertThat(response.getOutp()).isEqualTo("\"foo: 5678\"");
    }

    @Test
    void shouldSupportDefaultValue() {
        assertThat(body(get(service, "/test/defaultQuery"))).isEqualTo("\"Default: 5\"");
        assertThat(body(post(service, "/test/acceptsDefaultForm", ""))).isEqualTo("\"Default: 5\"");
        assertThat(body(getWithHeaders(service, "/test/acceptsDefaultHeader", new HashMap<>()))).isEqualTo("\"Default: 5\"");
        assertThat(body(getWithHeaders(service, "/test/acceptsDefaultCookie", new HashMap<>()))).isEqualTo("\"Default: 5\"");
    }

    @Test
    void shouldNotSupportDefaultValueForPathParam() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> body(get(new DefaultPathParamResource(), "/default/param")));
    }

    @Test
    void shouldSupportSimpleQueryParamTypes() {
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
    void shouldSupportCustomClassesAsQueryParams() {
        assertThat(body(get(service, "/test/acceptsCustomClass/valueOf?myarg=the-custom-value")))
            .isEqualTo("\"the-custom-value\"");

        assertThat(body(get(service, "/test/acceptsCustomClass/fromString?myarg=the-custom-value")))
            .isEqualTo("\"the-custom-value\"");

        assertThat(body(get(service, "/test/acceptsCustomClass/constructor?myarg=the-custom-value")))
            .isEqualTo("\"the-custom-value\"");
    }

    @Test
    void shouldSupportCustomClassesAsNullableQueryParams() {
        assertThat(body(get(service, "/test/acceptsCustomClass/valueOf")))
            .isEqualTo("\"null\"");

        assertThat(body(get(service, "/test/acceptsCustomClass/fromString")))
            .isEqualTo("\"null\"");

        assertThat(body(get(service, "/test/acceptsCustomClass/constructor")))
            .isEqualTo("\"null\"");
    }

    @Test
    void shouldSupportSimplePathParamTypes() {
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
    void shouldSupportCustomClassesAsPathParams() {
        assertThat(body(get(service, "/test/acceptsCustomClass/valueOf/the-custom-value")))
            .isEqualTo("\"the-custom-value\"");

        assertThat(body(get(service, "/test/acceptsCustomClass/fromString/the-custom-value")))
            .isEqualTo("\"the-custom-value\"");

        assertThat(body(get(service, "/test/acceptsCustomClass/constructor/the-custom-value")))
            .isEqualTo("\"the-custom-value\"");
    }

    @Test
    void shouldSupportregexInPathParams() {
        assertThat(
            body(get(service, "/test/acceptsSlashVar/my/var/with/slashes")))
            .isEqualTo("\"var: my/var/with/slashes\"");

    }

    @Test
    void shouldSupportTrailingSlashInResource() {
        assertThat(get(service, "/test/trailingSlash/").status()).isEqualTo(HttpResponseStatus.OK);
        assertThat(get(service, "/test/trailingSlash").status()).isEqualTo(HttpResponseStatus.OK);
    }

    @Test
    void shouldSupportWhitespace() {
        assertThat(get(service, "/test/accepts").status()).isEqualTo(HttpResponseStatus.OK);
        assertThat(get(service, "/test/accepts%20%20%20").status()).isEqualTo(HttpResponseStatus.OK);
        assertThat(get(service, "/test/accepts%09%09").status()).isEqualTo(HttpResponseStatus.OK);
    }

    @Test
    void shouldSupportSimpleFormParamTypes() {
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
    void shouldSupportCustomClassesAsFormParams() {
        assertThat(body(post(service, "/test/acceptsPostCustomClass/valueOf", "myarg=the-custom-value")))
            .isEqualTo("\"the-custom-value\"");

        assertThat(body(post(service, "/test/acceptsPostCustomClass/fromString", "myarg=the-custom-value")))
            .isEqualTo("\"the-custom-value\"");

        assertThat(body(post(service, "/test/acceptsPostCustomClass/constructor", "myarg=the-custom-value")))
            .isEqualTo("\"the-custom-value\"");
    }

    @Test
    void shouldSupportCustomClassesAsNullableFormParams() {
        assertThat(body(post(service, "/test/acceptsPostCustomClass/valueOf", "")))
            .isEqualTo("\"null\"");

        assertThat(body(post(service, "/test/acceptsPostCustomClass/fromString", "")))
            .isEqualTo("\"null\"");

        assertThat(body(post(service, "/test/acceptsPostCustomClass/constructor", "")))
            .isEqualTo("\"null\"");
    }

    @Test
    void shouldSupportSimpleHeaderParamTypes() {
        assertThat(body(JaxRsTestUtil.getWithHeaders(service, "/test/acceptsHeaderString",
            Map.of("myHeader", List.of("accepts"))
        ))).isEqualTo("\"header: accepts\"");

        assertThat(body(JaxRsTestUtil.getWithHeaders(service, "/test/acceptsHeaderInteger",
            Map.of("myHeader", List.of("4"))
        ))).isEqualTo("\"header: 4\"");

        assertThat(body(JaxRsTestUtil.getWithHeaders(service, "/test/acceptsHeaderEnum",
            Map.of("myHeader", List.of("ONE"))
        ))).isEqualTo("\"header: ONE\"");
    }

    @Test
    void shouldSupportCustomClassesAsHeaderParams() {
        assertThat(body(JaxRsTestUtil.getWithHeaders(service, "/test/acceptsHeaderCustomClass/valueOf",
            Map.of("myHeader", List.of("the-custom-value"))
        ))).isEqualTo("\"the-custom-value\"");


        assertThat(body(JaxRsTestUtil.getWithHeaders(service, "/test/acceptsHeaderCustomClass/fromString",
            Map.of("myHeader", List.of("the-custom-value"))
        ))).isEqualTo("\"the-custom-value\"");

        assertThat(body(JaxRsTestUtil.getWithHeaders(service, "/test/acceptsHeaderCustomClass/constructor",
            Map.of("myHeader", List.of("the-custom-value"))
        ))).isEqualTo("\"the-custom-value\"");
    }

    @Test
    void shouldSupportCustomClassesAsNullableHeaderParams() {
        assertThat(body(JaxRsTestUtil.getWithHeaders(service, "/test/acceptsHeaderCustomClass/valueOf", Map.of())))
            .isEqualTo("\"null\"");

        assertThat(body(JaxRsTestUtil.getWithHeaders(service, "/test/acceptsHeaderCustomClass/fromString", Map.of())))
            .isEqualTo("\"null\"");

        assertThat(body(JaxRsTestUtil.getWithHeaders(service, "/test/acceptsHeaderCustomClass/constructor", Map.of())))
            .isEqualTo("\"null\"");
    }

    @Test
    void shouldSupportMissingHeaderParams() {
        assertThat(body(JaxRsTestUtil.getWithHeaders(service, "/test/acceptsHeaderString",
            Map.of("dummy", List.of("accepts"))
        ))).isEqualTo("\"header: null\"");
    }

    @Test
    void shouldSupportMissingNullableParams() {
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
    void shouldGiveErrorForMissingNotNullableParams() {
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
    void shouldSupportOverloadedMethods() throws Exception {
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
        assertThat(response.status())
            .isEqualTo(BAD_REQUEST);
        assertThat(response.getOutp())
            .matches(expectedBodyRegex);
    }

    @Test
    void shouldGiveErrorForBadRequests() {
        assertBadRequest(get(service, "/test/acceptsInteger?myarg=badvalue"),
            "{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.int'}]}");
        assertBadRequest(get(service, "/test/acceptsLong?myarg=badvalue"),
            "{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.long'}]}");
        assertBadRequest(get(service, "/test/acceptsDouble?myarg=badvalue"),
            "{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.double'}]}");
    }

    @Test
    void shouldGiveErrorForBadDates() {
        assertBadRequest(get(service, "/test/acceptsDate?myarg=20aa-01-01"),
            "{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.date'}]}");

        assertBadRequest(get(service, "/test/acceptsDate?myarg=2010-0b-01%2000:00:00"),
            "{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.date'}]}");

        assertBadRequest(get(service, "/test/acceptsDate?myarg=2010-01-qehjeq:00:00.000"),
            "{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.date'}]}");

        assertBadRequest(get(service, "/test/acceptsDate?myarg=97697697676767688675767"),
            "{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.date'}]}");

    }

    @Test
    void shouldSupportDatesAsString() {
        assertThat(body(
            get(service, "/test/acceptsDate?myarg=2010-01-01")))
            .isEqualTo("\"2010-01-01T00:00:00.000+0000\"");

        assertThat(body(get(service, "/test/acceptsDate?myarg=2010-01-01T00:00:00.000")))
            .isEqualTo("\"2010-01-01T00:00:00.000+0000\"");
    }

    class CustomDateFormat extends StdDateFormat {

        CustomDateFormat() {
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
    void shouldSupportCustomDates() {
        Module customDateModule = new AbstractModule() {
            @Override
            protected void configure() {
                bind(DateFormat.class).toProvider(CustomDateFormat::new);
                bind(JaxRsResourcesProvider.class).toInstance(() -> new Object[]{service});
                bind(ByteBufCollector.class).toInstance(new ByteBufCollector());
                bind(new TypeLiteral<Set<ParamResolver>>() {{
                }}).toInstance(Collections.EMPTY_SET);
                bind(new TypeLiteral<Set<ParamResolverFactory>>() {{
                }}).toInstance(Collections.EMPTY_SET);
                bind(new TypeLiteral<Set<Deserializer>>() {{
                }}).toInstance(Collections.EMPTY_SET);
                bind(ResultTransformerFactories.class).toInstance(new ResultTransformerFactories());
                bind(ObjectMapper.class).toInstance(new ObjectMapper()
                    .findAndRegisterModules()
                    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    .setDateFormat(new CustomDateFormat()));
            }
        };
        Injector injector = Guice.createInjector(customDateModule);
        JaxRsRequestHandler handler = injector.getInstance(JaxRsRequestHandler.class);

        assertThat(
            body(
                JaxRsTestUtil.processRequestWithHandler(handler, new MockHttpServerRequest("/test/acceptsDate?myarg=2010-01-01%2000:00:00"))))
            .isEqualTo("\"2010-01-01T00:00:00.000+0100\"");
    }

    @Test
    void shouldSupportLocalDate() {
        assertThat(body(
            get(service, "/test/acceptsLocalDate?myarg=2010-01-01")))
            .isEqualTo("{\"localDate\":\"2010-01-01\"}");
    }

    @Test
    void shouldSupportLocalTime() {
        assertThat(body(
            get(service, "/test/acceptsLocalTime?myarg=13:37:37")))
            .isEqualTo("{\"localTime\":\"13:37:37\"}");
    }

    @Test
    void shouldReturnErrorWhenServiceThrowsError() {
        assertThat(get(service,
            "/test/throwInsufficientStorage").status()).isEqualTo(HttpResponseStatus.INSUFFICIENT_STORAGE);
    }

    @Test
    void shouldReturnErrorWhenServiceThrowsRuntimeException() {
        assertThat(get(service,
            "/test/throwRuntimeException").status()).isEqualTo(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldReturnErrorWhenServiceThrowsException() {
        assertThat(get(service,
            "/test/throwException").status()).isEqualTo(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldReturnErrorForBadJson() {
        String expectedBodyRegex1 = "{\"id\":\".*\",\"error\":\"invalidjson\"," +
            "\"message\":\"Unexpected character ('h' (code 104)): was expecting double-quote to start field name\"}";
        assertBadRequest(post(service, "/test/jsonParam", "{hej}"), expectedBodyRegex1);

        String expectedBodyRegex2 = "{\"id\":\".*\",\"error\":\"invalidjson\"," +
            "\"message\":\"Cannot deserialize value of type `int` from String \\\\\"nan\\\\\": not a valid `int` value\"}";
        assertBadRequest(post(service, "/test/jsonParam", "{\"age\": \"nan\"}"), expectedBodyRegex2);

        String expectedBodyRegex3 = "{\"id\":\".*\",\"error\":\"invalidjson\"," +
            "\"message\":\"No content to map due to end-of-input\"}";
        assertBadRequest(post(service, "/test/jsonParam", (String) null), expectedBodyRegex3);
    }

    @Test
    void shouldAcceptTextPlainInput() {
        String text = "my plain text";
        MockHttpServerResponse resp = post(service, "/test/textPlain", text);
        assertThat(resp.status()).isEqualTo(HttpResponseStatus.CREATED);
        assertThat(resp.getOutp()).isEqualTo("\"" + text + "\"");
    }

    @Test
    void shouldAcceptByteArrayInput() {
        String text = "my bytes";
        MockHttpServerResponse resp = post(service, "/test/byteArray", text);
        assertThat(resp.status()).isEqualTo(HttpResponseStatus.CREATED);
        assertThat(resp.getOutp()).isEqualTo("\"" + text + "\"");
    }

    @Test
    void shouldAcceptByteArrayInputAnyMimeType() {
        String text = "my bytes";
        MockHttpServerResponse resp = post(service, "/test/byteArrayAnyType", text);
        assertThat(resp.status()).isEqualTo(HttpResponseStatus.CREATED);
        assertThat(resp.getOutp()).isEqualTo("\"" + text + "\"");
    }

    @Test
    void shouldErrorForUnknownBodyType() {
        @Path("test")
        class InvalidQueryParam {
            @POST
            @Consumes("application/whatever")
            public Mono<String> acceptsComplexQueryParam(ParamEntity paramEntity) {
                return null;
            }
        }

        try {
            JaxRsTestUtil.resources(new InvalidQueryParam());
            fail("expected exception");
        } catch (RuntimeException e) {
            String expected = "Could not find any deserializer for param of type class se.fortnox.reactivewizard.jaxrs.ParamEntity";
            assertThat(e.getMessage()).isEqualTo(expected);
            return;
        }
        fail("expected exception");

    }

    @Test
    void shouldGiveErrorForBadEnumValue() {
        assertBadRequest(post(service, "/test/acceptsPostEnum", "myEnum=BAD"),
            "{'id':'.*','error':'validation','fields':[{'field':'myEnum','error':'validation.invalid.enum'}]}");
    }

    @Test
    void shouldGiveErrorForUnparsableType() {

        @Path("test")
        class InvalidQueryParam {
            @Path("acceptsComplexQueryParam")
            @GET
            public Mono<String> acceptsComplexQueryParam(@QueryParam("paramEntity") ParamEntity paramEntity) {
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
    void shouldHandleNoMatchingResource() {
        assertThat(get(service, "/noservice/nomethod").status())
            .isEqualTo(HttpResponseStatus.NOT_FOUND);
    }

    @Test
    void shouldSupportUuidAsQueryParameter() {
        UUID uuid = UUID.randomUUID();
        assertThat(body(
            get(service, "/test/acceptsUuid?id=" + uuid.toString())))
            .isEqualTo("\"Id: " + uuid + "\"");
    }

    @Test
    void shouldSupportEmptyUuidAsQueryParameter() {
        assertThat(body(
            get(service, "/test/acceptsUuid?id=")))
            .isEqualTo("\"Id: null\"");

        assertThat(body(
            get(service, "/test/acceptsUuid")))
            .isEqualTo("\"Id: null\"");
    }

    @Test
    void shouldSupportUuidAsFormParameter() {
        UUID uuid = UUID.randomUUID();
        assertThat(body(
            post(service, "/test/acceptsUuid", "id=" + uuid)))
            .isEqualTo("\"Id: " + uuid + "\"");
    }

    @Test
    void shouldSupportUuidAsPathParameter() {
        UUID uuid = UUID.randomUUID();
        assertThat(body(
            get(service, "/test/acceptsUuid/" + uuid)))
            .isEqualTo("\"Id: " + uuid + "\"");
    }

    @Test
    void shouldGive400ErrorForBadUuid() {
        MockHttpServerResponse response = get(service, "/test/acceptsUuid/baduuid");
        assertThat(response.status()).isEqualTo(BAD_REQUEST);
        assertThat(body(response)).contains("\"error\":\"validation\",\"fields\":[{\"field\":\"id\",\"error\":\"validation.invalid.uuid\"}]}");
    }

    @Test
    void shouldSupportUuidAsHeader() {
        UUID uuid = UUID.randomUUID();
        assertThat(body(JaxRsTestUtil.getWithHeaders(service,
            "/test/acceptsUuidHeader",
            new HashMap<>() {
                {
                    put("id", asList(uuid.toString()));
                }
            }
        ))).isEqualTo("\"Id: " + uuid.toString() + "\"");
    }

    @Test
    void shouldSetContentType() {
        assertThat(get(new Testresource(), "/test/acceptsString").responseHeaders().get("Content-Type"))
            .isEqualTo("application/json");
    }

    @Test
    void shouldSupportReturningNullFromResource() {
        assertThat(get(new Testresource(), "/test/returnsNull").status())
            .isEqualTo(HttpResponseStatus.NO_CONTENT);
    }

    @Test
    void shouldMatchExactPathBeforePathparam() {
        SpecialResource service = new SpecialResource();
        assertThat(body(get(service, "/special/frenberg"))).isEqualTo("\"frenberg\"");
        assertThat(body(get(service, "/special/strings"))).isEqualTo("[\"string\",\"string\"]");
    }

    @Test
    void shouldSupportGenericParams() {
        assertThat(body(post(service, "/test/generic-param", "[{\"name\":\"test\"}]"))).isEqualTo("\"ParamEntity\"");
    }

    @Test
    void shouldSupportGenericParamsWhenProxied() {
        TestresourceInterface proxy = (TestresourceInterface) Proxy.newProxyInstance(
            TestresourceInterface.class.getClassLoader(),
            new Class[]{TestresourceInterface.class},
            (instance, method, args) -> method.invoke(service, args)
        );
        assertThat(body(post(proxy, "/test/generic-param", "[{\"name\":\"test\"}]"))).isEqualTo("\"ParamEntity\"");
    }

    @Test
    void shouldResolveCookieParam() {
        assertThat(body(getWithHeaders(service, "/test/acceptsCookieParam", new HashMap() {
            {
                put("Cookie", asList("fnox_session=testcookie"));
            }
        }))).isEqualTo("\"testcookie\"");
    }

    @Test
    void shouldResolveCustomClassesAsCookieParam() {
        assertThat(body(getWithHeaders(service, "/test/acceptsCookieParamCustomClass/valueOf",
            Map.of("Cookie", List.of("myarg=the-custom-value"))
        ))).isEqualTo("\"the-custom-value\"");

        assertThat(body(getWithHeaders(service, "/test/acceptsCookieParamCustomClass/fromString",
            Map.of("Cookie", List.of("myarg=the-custom-value"))
        ))).isEqualTo("\"the-custom-value\"");

        assertThat(body(getWithHeaders(service, "/test/acceptsCookieParamCustomClass/constructor",
            Map.of("Cookie", List.of("myarg=the-custom-value"))
        ))).isEqualTo("\"the-custom-value\"");
    }

    @Test
    void shouldResolveCustomClassesAsNullableCookieParam() {
        assertThat(body(getWithHeaders(service, "/test/acceptsCookieParamCustomClass/valueOf", Map.of())))
            .isEqualTo("\"null\"");

        assertThat(body(getWithHeaders(service, "/test/acceptsCookieParamCustomClass/fromString", Map.of())))
            .isEqualTo("\"null\"");

        assertThat(body(getWithHeaders(service, "/test/acceptsCookieParamCustomClass/constructor", Map.of())))
            .isEqualTo("\"null\"");
    }

    @Test
    void shouldAcceptBodyForPut() {
        assertThat(body(put(service, "/test/acceptBodyPut", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0,\"items\":null}");
    }

    @Test
    void shouldAcceptBodyForPutRecord() {
        assertThat(body(put(service, "/test/acceptBodyPutRecord", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0,\"items\":null}");
    }

    @Test
    void shouldAcceptBodyForPost() {
        assertThat(body(post(service, "/test/acceptBodyPost", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0,\"items\":null}");
    }

    @Test
    void shouldAcceptBodyForPostRecord() {
        assertThat(body(post(service, "/test/acceptBodyPostRecord", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0,\"items\":null}");
    }

    @Test
    void shouldAcceptBodyForPatch() {
        assertThat(body(patch(service, "/test/acceptBodyPatch", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0,\"items\":null}");
    }

    @Test
    void shouldAcceptBodyForPatchRecord() {
        assertThat(body(patch(service, "/test/acceptBodyPatchRecord", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0,\"items\":null}");
    }

    @Test
    void shouldAcceptBodyForDelete() {
        assertThat(body(delete(service, "/test/acceptBodyDelete", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0,\"items\":null}");
    }

    @Test
    void shouldAcceptBodyForDeleteRecord() {
        assertThat(body(delete(service, "/test/acceptBodyDeleteRecord", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0,\"items\":null}");
    }

    @Test
    void shouldDenyLargeBodies() {
        assertBadRequest(
            post(service, "/test/acceptsPostString", new byte[11 * 1024 * 1024]),
            "{'id':'.*','error':'too.large.input'}");
    }

    @Test
    void shouldAcceptLargeBodiesWithinLimits() {
        assertThat(post(service, "/test/acceptsPostString", new byte[10 * 1024 * 1024]).status())
            .isEqualTo(HttpResponseStatus.CREATED);
    }

    @Test
    void shouldAcceptQueryParamArrayValues() {
        assertThat(get(service, "/test/acceptsQueryArray?Stringarray=val1,val2,val3").getOutp()).isEqualTo("3");
        assertThat(get(service, "/test/acceptsQueryArray?Integerarray=1,2,3,4").getOutp()).isEqualTo("4");
    }

    @Test
    void shouldAcceptQueryParamListValues() {
        assertThat(get(service, "/test/acceptsQueryList?Stringlist=val1,val2,val3").getOutp()).isEqualTo("3");
        assertThat(get(service, "/test/acceptsQueryList?Integerlist=1,2,3,4").getOutp()).isEqualTo("4");
    }

    @Test
    void shouldAcceptQueryParamListWithEnumValues() {
        assertThat(get(service, "/test/acceptsQueryListWithEnum?EnumList=ONE,TWO,THREE").getOutp()).isEqualTo("3");
    }

    @Test
    void shouldAcceptQueryParamListWithCustomClasses() {
        assertThat(get(service, "/test/acceptsQueryListWithCustomClasses?list=ONE,TWO,THREE").getOutp()).isEqualTo("\"ONETWOTHREE\"");
    }

    @Test
    void shouldAcceptQueryParamArrayWithCustomClasses() {
        assertThat(get(service, "/test/acceptsQueryArrayWithCustomClasses?array=ONE,TWO,THREE").getOutp()).isEqualTo("\"ONETWOTHREE\"");
    }

    @Test
    void shouldAcceptBeanParam() {
        assertThat(get(service, "/test/acceptsBeanParam?name=foo&age=3&items=1,2").getOutp()).isEqualTo("\"foo - 3 2\"");
    }

    @Test
    void shouldAcceptBeanParamWithDefaults() {
        assertThat(get(service, "/test/acceptsBeanParam?name=foo&items=1,2").getOutp()).isEqualTo("\"foo - 123 2\"");
    }

    @Test
    void shouldAcceptBeanParamRecord() {
        assertThat(get(service, "/test/acceptsBeanParamRecord?name=foo&age=3&items=1,2").getOutp()).isEqualTo("\"ParamEntityRecord[name=foo, age=3, items=[1, 2]]\"");
    }

    @Test
    void shouldAcceptBeanParamRecordWithDefaults() {
        assertThat(get(service, "/test/acceptsBeanParamRecord?name=foo&items=1,2").getOutp()).isEqualTo("\"ParamEntityRecord[name=foo, age=123, items=[1, 2]]\"");
    }

    @Test
    void shouldAcceptBeanParamRecordWithDefaultsAndMissingFields() {
        assertThat(get(service, "/test/acceptsBeanParamRecord").getOutp()).isEqualTo("\"ParamEntityRecord[name=null, age=123, items=null]\"");
    }

    @Test
    void shouldAcceptBeanParamInherited() {
        assertThat(get(service, "/test/acceptsBeanParamInherited?name=foo&age=3&items=1,2&inherited=YES").getOutp()).isEqualTo("\"foo - 3 2 - YES\"");
    }

    @Test
    void shouldGiveErrorWhenBodyIsNullString() {
        assertThat(post(service, "/test/applicationJson", "null").status()).isEqualTo(BAD_REQUEST);
        paramResolverFactoriesLoggingVerifier.verify(WARN, "Body deserializer returned null when deserializing body: 'null'");
    }

    @Test
    void shouldGive400ErrorForInvalidHexByteInQuery() {
        assertBadRequestOnInvalidHexByteInQuery("/test/accepts?myarg=%A");
        assertBadRequestOnInvalidHexByteInQuery("/test/accepts?%A");
        assertBadRequestOnInvalidHexByteInQuery("/test/accepts?%=");
        assertBadRequestOnInvalidHexByteInQuery("/test/accepts?%?");
        assertBadRequestOnInvalidHexByteInQuery("/test/accepts?%AG");
        assertBadRequestOnInvalidHexByteInQuery("/test/accepts?%A@");
    }

    private void assertBadRequestOnInvalidHexByteInQuery(String uri) {
        MockHttpServerResponse mockHttpServerResponse = get(service, uri);

        assertThat(mockHttpServerResponse.status()).isEqualTo(BAD_REQUEST);

        jaxRsRequestLoggingVerifier.verify(INFO, String.format("Failed to decode HTTP query params for request GET %s", uri));
    }

    @Path("test")
    class Testresource {
        @Path("acceptsString")
        @GET
        public Mono<String> acceptsString(@QueryParam("myarg") String myarg) {
            return just("inp: " + myarg);
        }

        @Path("returnsNull")
        @GET
        public Mono<String> returnsNull() {
            return null;
        }

        @Path("serverSideAnnotationsOnClassAsResource")
        @GET
        @Headers("Content-Disposition: attachment; filename=auditlog.csv")
        public Mono<String> shouldIgnoreHeaderAnnotation() {
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

    @Path("testobservable")
    public class TestResourceWithObservables {
        @GET
        public Observable<String> shouldFail() {
            return Observable.just("");
        }
    }

    @Path("test")
    public interface TestresourceInterface {
        @Path("accepts")
        @GET
        Mono<String> acceptsString(@QueryParam("myarg") String myarg);

        @Path("acceptsBoolean")
        @GET
        Mono<String> acceptsBoolean(@QueryParam("myarg") Boolean myarg);

        @Path("acceptsInteger")
        @GET
        Mono<String> acceptsInteger(@QueryParam("myarg") Integer myarg);

        @Path("acceptsLong")
        @GET
        Mono<String> acceptsLong(@QueryParam("myarg") Long myarg);

        @Path("acceptsDouble")
        @GET
        Mono<String> acceptsDouble(@QueryParam("myarg") Double myarg);

        @Path("acceptsDate")
        @GET
        Mono<Date> acceptsDate(@QueryParam("myarg") Date myarg);

        @Path("acceptsLocalDate")
        @GET
        Mono<LocalDateContainer> acceptsLocalDate(@QueryParam("myarg") LocalDate myarg);

        @Path("acceptsLocalTime")
        @GET
        Mono<LocalTimeContainer> acceptsLocalTime(@QueryParam("myarg") LocalTime myarg);

        @Path("acceptsEnum")
        @GET
        Mono<String> acceptsEnum(@QueryParam("myarg") TestEnum myarg);

        @Path("acceptsCustomClass/valueOf")
        @GET
        Mono<String> acceptsCustomClassQueryParamWithValueOf(@QueryParam("myarg") CustomParamWithValueOf myarg);

        @Path("acceptsCustomClass/fromString")
        @GET
        Mono<String> acceptsCustomClassQueryParamWithFromString(@QueryParam("myarg") CustomParamWithFromString myarg);

        @Path("acceptsCustomClass/constructor")
        @GET
        Mono<String> acceptsCustomClassQueryParamWithConstructor(@QueryParam("myarg") CustomParamWithConstructor myarg);

        @Path("defaultQuery")
        @GET
        Mono<String> acceptDefaultQueryParam(@QueryParam("myarg") @DefaultValue("5") int myarg);

        @Path("acceptsString/{myarg}")
        @GET
        Mono<String> acceptsStringPath(@PathParam("myarg") String myarg);

        @Path("acceptsBoolean/{myarg}")
        @GET
        Mono<String> acceptsBooleanPath(@PathParam("myarg") Boolean myarg);

        @Path("acceptsInteger/{myarg}")
        @GET
        Mono<String> acceptsIntegerPath(@PathParam("myarg") Integer myarg);

        @Path("acceptsLong/{myarg}")
        @GET
        Mono<String> acceptsLongPath(@PathParam("myarg") Long myarg);

        @Path("acceptsStrictLong/{myarg}")
        @GET
        Mono<String> acceptsStrictLongPath(@PathParam("myarg") long myarg);

        @Path("acceptsDate/{myarg}")
        @GET
        Mono<String> acceptsDatePath(@PathParam("myarg") Date myarg);

        @Path("acceptsEnum/{myarg}")
        @GET
        Mono<String> acceptsEnumPath(@PathParam("myarg") TestEnum myarg);

        @Path("acceptsSlashVar/{myarg:.*}")
        @GET
        Mono<String> acceptsSlashVar(@PathParam("myarg") String myarg);

        @Path("acceptsCustomClass/valueOf/{myarg}")
        @GET
        Mono<String> acceptsCustomClassPathParamWithValueOf(@PathParam("myarg") CustomParamWithValueOf myarg);

        @Path("acceptsCustomClass/fromString/{myarg}")
        @GET
        Mono<String> acceptsCustomClassPathParamWithFromString(@PathParam("myarg") CustomParamWithFromString myarg);

        @Path("acceptsCustomClass/constructor/{myarg}")
        @GET
        Mono<String> acceptsCustomClassPathParamWithConstructor(@PathParam("myarg") CustomParamWithConstructor myarg);

        @Path("acceptsPostString")
        @POST
        Mono<String> acceptsPostString(@FormParam("myString") String myarg);

        @Path("acceptsPostBoolean")
        @POST
        Mono<String> acceptsPostBoolean(@FormParam("myBoolean") Boolean myarg);

        @Path("acceptsPostInteger")
        @POST
        Mono<String> acceptsPostInteger(@FormParam("myInteger") Integer myarg);

        @Path("acceptsPostLong")
        @POST
        Mono<String> acceptsPostLong(@FormParam("myLong") Long myarg);

        @Path("acceptsPostDouble")
        @POST
        Mono<String> acceptsPostDouble(@FormParam("myDouble") Double myarg);

        @Path("acceptsPostDate")
        @POST
        Mono<String> acceptsPostDate(@FormParam("myDate") Date myarg);

        @Path("acceptsPostEnum")
        @POST
        Mono<String> acceptsPostEnum(@FormParam("myEnum") TestEnum myarg);

        @Path("acceptsPostNotNullBool")
        @POST
        Mono<String> acceptsPostNotNullBoolean(@FormParam("myBoolean") boolean myarg);

        @Path("acceptsPostNotNullInt")
        @POST
        Mono<String> acceptsPostNotNullInteger(@FormParam("myInteger") int myarg);

        @Path("acceptsPostNotNullLong")
        @POST
        Mono<String> acceptsPostNotNullLong(@FormParam("myLong") long myarg);

        @Path("acceptsPostNotNullDouble")
        @POST
        Mono<String> acceptsPostNotNullDouble(@FormParam("myDouble") double myarg);

        @Path("acceptsPostCustomClass/valueOf")
        @POST
        Mono<String> acceptsPostCustomClassWithValueOf(@FormParam("myarg") CustomParamWithValueOf myarg);

        @Path("acceptsPostCustomClass/fromString")
        @POST
        Mono<String> acceptsPostCustomClassWithFromString(@FormParam("myarg") CustomParamWithFromString myarg);

        @Path("acceptsPostCustomClass/constructor")
        @POST
        Mono<String> acceptsPostCustomClassWithConstructor(@FormParam("myarg") CustomParamWithConstructor myarg);

        @Path("acceptsDefaultForm")
        @POST
        Mono<String> acceptsDefaultFormParam(@FormParam("myarg") @DefaultValue("5") int myarg);

        @Path("acceptsHeaderString")
        @GET
        Mono<String> acceptsHeader(@HeaderParam("myHeader") String myHeader);

        @Path("acceptsHeaderInteger")
        @GET
        Mono<String> acceptsHeaderInteger(@HeaderParam("myHeader") Integer myHeader);

        @Path("acceptsHeaderEnum")
        @GET
        Mono<String> acceptsHeaderEnum(@HeaderParam("myHeader") TestEnum myHeader);

        @Path("acceptsHeaderCustomClass/valueOf")
        @GET
        Mono<String> acceptsHeaderCustomClassWithValueOf(@HeaderParam("myHeader") CustomParamWithValueOf myHeader);

        @Path("acceptsHeaderCustomClass/fromString")
        @GET
        Mono<String> acceptsHeaderCustomClassWithFromString(@HeaderParam("myHeader") CustomParamWithFromString myHeader);

        @Path("acceptsHeaderCustomClass/constructor")
        @GET
        Mono<String> acceptsHeaderCustomClassWithConstructor(@HeaderParam("myHeader") CustomParamWithConstructor myHeader);

        @Path("acceptsDefaultHeader")
        @GET
        Mono<String> acceptsDefaultHeaderParam(@HeaderParam("myHeader") @DefaultValue("5") int myHeader);

        @Path("overloadedMethod")
        @GET
        Mono<String> overloadedMethod(@QueryParam("param1") String param1, @QueryParam("param2") String param2);

        @Path("overloadedMethod")
        @GET
        Mono<String> overloadedMethod(@QueryParam("param1") String param1);

        @Path("overloadedMethod")
        @GET
        Mono<String> overloadedMethod();

        @GET
        @Path("throwInsufficientStorage")
        Mono<String> throwInsufficientStorage();

        @GET
        @Path("throwRuntimeException")
        Mono<String> throwRuntimeException();

        @GET
        @Path("throwException")
        Mono<String> throwException();

        @POST
        @Path("jsonParam")
        Mono<String> jsonParam(ParamEntity paramEntity);

        @POST
        @Path("textPlain")
        @Consumes(MediaType.TEXT_PLAIN)
        Mono<String> textPlain(String input);

        @POST
        @Path("applicationJson")
        @Consumes(MediaType.APPLICATION_JSON)
        Mono<String> applicationJson(String input);

        @POST
        @Path("byteArray")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        Mono<String> byteArray(byte[] input);

        @POST
        @Path("byteArrayAnyType")
        @Consumes("application/whatever")
        Mono<String> byteArrayAnyType(byte[] input);

        @GET
        @Path("acceptsUuid")
        Mono<String> acceptsUuidQueryParam(@QueryParam("id") UUID id);

        @POST
        @Path("acceptsUuid")
        Mono<String> acceptsUuidFormParam(@FormParam("id") UUID id);

        @GET
        @Path("acceptsUuid/{id}")
        Mono<String> acceptsUuidPathParam(@PathParam("id") UUID id);

        @GET
        @Path("acceptsUuidHeader")
        Mono<String> acceptsUuidHeader(@HeaderParam("id") UUID id);

        @POST
        @Path("generic-param")
        Mono<String> acceptsGenericParam(List<ParamEntity> list);

        @GET
        @Path("trailingSlash/")
        Mono<String> acceptsTrailingSlash();

        @Path("acceptsCookieParam")
        @GET
        Mono<String> acceptsCookieParam(@CookieParam("fnox_session") String cookie);

        @Path("acceptsCookieParamCustomClass/valueOf")
        @GET
        Mono<String> acceptsCookieParamCustomClassesWithValueOf(@CookieParam("myarg") CustomParamWithValueOf myarg);

        @Path("acceptsCookieParamCustomClass/fromString")
        @GET
        Mono<String> acceptsCookieParamCustomClassesWithFromString(@CookieParam("myarg") CustomParamWithValueOf myarg);

        @Path("acceptsCookieParamCustomClass/constructor")
        @GET
        Mono<String> acceptsCookieParamCustomClassesWithConstructor(@CookieParam("myarg") CustomParamWithValueOf myarg);

        @Path("acceptsDefaultCookie")
        @GET
        Mono<String> acceptsDefaultCookieParam(@CookieParam("myCookie") @DefaultValue("5") int myCookie);

        @Path("acceptBodyGet")
        @GET
        Mono<ParamEntity> acceptBodyGet(ParamEntity paramEntity);

        @Path("acceptBodyPut")
        @PUT
        Mono<ParamEntity> acceptBodyPut(ParamEntity paramEntity);

        @Path("acceptBodyPutRecord")
        @PUT
        Mono<ParamEntityRecord> acceptBodyPutRecord(ParamEntityRecord paramEntity);

        @Path("acceptBodyPost")
        @POST
        Mono<ParamEntity> acceptBodyPost(ParamEntity paramEntity);

        @Path("acceptBodyPostRecord")
        @POST
        Mono<ParamEntityRecord> acceptBodyPostRecord(ParamEntityRecord paramEntity);

        @Path("acceptBodyPatch")
        @PATCH
        Mono<ParamEntity> acceptBodyPatch(ParamEntity paramEntity);

        @Path("acceptBodyPatchRecord")
        @PATCH
        Mono<ParamEntityRecord> acceptBodyPatchRecord(ParamEntityRecord paramEntity);

        @Path("acceptBodyDelete")
        @DELETE
        Mono<ParamEntity> acceptBodyDelete(ParamEntity paramEntity);

        @Path("acceptBodyDeleteRecord")
        @DELETE
        Mono<ParamEntityRecord> acceptBodyDeleteRecord(ParamEntityRecord paramEntity);

        @Path("acceptsQueryList")
        @GET
        Mono<Integer> acceptsQueryList(@QueryParam("Stringlist") List<String> strings,
                                             @QueryParam("Integerlist") List<Integer> integers
        );

        @Path("acceptsQueryArray")
        @GET
        Mono<Integer> acceptsQueryArray(@QueryParam("Stringarray") String[] strings,
                                              @QueryParam("Integerarray") Integer[] integers
        );

        @Path("acceptsQueryListWithEnum")
        @GET
        Mono<Integer> acceptsQueryListWithEnum(@QueryParam("EnumList") List<TestEnum> enums);

        @Path("acceptsQueryListWithCustomClasses")
        @GET
        Mono<String> acceptsQueryListWithCustomClasses(@QueryParam("list") List<CustomParamWithValueOf> objects);

        @Path("acceptsQueryArrayWithCustomClasses")
        @GET
        Mono<String> acceptsQueryArrayWithCustomClasses(@QueryParam("array") CustomParamWithValueOf[] objects);

        @Path("shouldIgnoreHeadersAnnotationOnInterface")
        @GET
        @Headers("Content-Disposition: attachment; filename=test.csv")
        Mono<String> ignoresHeaderAnnotationOnInterface();

        @Path("acceptsHeadersOnImplementation")
        @GET
        Mono<String> acceptsHeadersOnImplementation();

        @Path("acceptsBeanParam")
        @GET
        Mono<String> acceptsBeanParam(@BeanParam ParamEntity beanParam);

        @Path("acceptsBeanParamRecord")
        @GET
        Mono<String> acceptsBeanParamRecord(@BeanParam ParamEntityRecord beanParam);

        @Path("acceptsBeanParamInherited")
        @GET
        Mono<String> acceptsBeanParamInherited(@BeanParam InheritedParamEntity beanParam);
    }

    class TestresourceImpl implements TestresourceInterface {

        @Override
        public Mono<String> acceptsString(String myarg) {
            return just("accepts from interface: " + myarg);
        }

        @Override
        public Mono<String> acceptsBoolean(Boolean myarg) {
            return just("Boolean: " + myarg);
        }

        @Override
        public Mono<String> acceptsInteger(Integer myarg) {
            return just("Integer: " + myarg);
        }

        @Override
        public Mono<String> acceptsLong(Long myarg) {
            return just("Long: " + myarg);
        }

        @Override
        public Mono<Date> acceptsDate(Date myarg) {
            return just(myarg);
        }

        @Override
        public Mono<LocalDateContainer> acceptsLocalDate(LocalDate myarg) {
            return just(new LocalDateContainer(myarg));
        }

        @Override
        public Mono<LocalTimeContainer> acceptsLocalTime(LocalTime myarg) {
            return just(new LocalTimeContainer(myarg));
        }

        @Override
        public Mono<String> acceptsEnum(TestEnum myarg) {
            return just("Enum: " + myarg);
        }

        @Override
        public Mono<String> acceptsCustomClassQueryParamWithValueOf(CustomParamWithValueOf myarg) {
            return just(myarg != null ? myarg.value : "null");
        }

        @Override
        public Mono<String> acceptsCustomClassQueryParamWithFromString(CustomParamWithFromString myarg) {
            return just(myarg != null ? myarg.value : "null");
        }

        @Override
        public Mono<String> acceptsCustomClassQueryParamWithConstructor(CustomParamWithConstructor myarg) {
            return just(myarg != null ? myarg.value : "null");
        }

        @Override
        public Mono<String> acceptDefaultQueryParam(int myarg) {
            return just("Default: " + myarg);
        }

        @Override
        public Mono<String> acceptsStringPath(String myarg) {
            return just("String: " + myarg);
        }

        @Override
        public Mono<String> acceptsBooleanPath(Boolean myarg) {
            return just("Boolean: " + myarg);
        }

        @Override
        public Mono<String> acceptsIntegerPath(Integer myarg) {
            return just("Integer: " + myarg);
        }

        @Override
        public Mono<String> acceptsLongPath(Long myarg) {
            return just("Long: " + myarg);
        }

        @Override
        public Mono<String> acceptsStrictLongPath(long myarg) {
            return just("long: " + myarg);
        }

        @Override
        public Mono<String> acceptsDatePath(Date myarg) {
            return just("Date: " + myarg);
        }

        @Override
        public Mono<String> acceptsEnumPath(TestEnum myarg) {
            return just("Enum: " + myarg);
        }

        @Override
        public Mono<String> acceptsPostString(String myarg) {
            return just("String: " + myarg);
        }

        @Override
        public Mono<String> acceptsPostBoolean(Boolean myarg) {
            return just("Boolean: " + myarg);
        }

        @Override
        public Mono<String> acceptsPostInteger(Integer myarg) {
            return just("Integer: " + myarg);
        }

        @Override
        public Mono<String> acceptsPostLong(Long myarg) {
            return just("Long: " + myarg);
        }

        @Override
        public Mono<String> acceptsPostDate(Date myarg) {
            return just("Date: " + myarg);
        }

        @Override
        public Mono<String> acceptsPostEnum(TestEnum myarg) {
            return just("Enum: " + myarg);
        }

        @Override
        public Mono<String> acceptsPostNotNullBoolean(boolean myarg) {
            return just("bool: " + myarg);
        }

        @Override
        public Mono<String> acceptsPostNotNullInteger(int myarg) {
            return just("int: " + myarg);
        }

        @Override
        public Mono<String> acceptsPostNotNullLong(long myarg) {
            return just("long: " + myarg);
        }

        @Override
        public Mono<String> acceptsHeader(String myHeader) {
            return just("header: " + myHeader);
        }

        @Override
        public Mono<String> acceptsHeaderInteger(Integer myHeader) {
            return just("header: " + myHeader);
        }

        @Override
        public Mono<String> acceptsHeaderEnum(TestEnum myHeader) {
            return just("header: " + myHeader);
        }

        @Override
        public Mono<String> acceptsHeaderCustomClassWithValueOf(CustomParamWithValueOf myHeader) {
            return just(myHeader != null ? myHeader.value : "null");
        }

        @Override
        public Mono<String> acceptsHeaderCustomClassWithFromString(CustomParamWithFromString myHeader) {
            return just(myHeader != null  ? myHeader.value : "null");
        }

        @Override
        public Mono<String> acceptsHeaderCustomClassWithConstructor(CustomParamWithConstructor myHeader) {
            return just(myHeader != null  ? myHeader.value : "null");
        }

        @Override
        public Mono<String> acceptsDefaultHeaderParam(int myHeader) {
            return just("Default: " + myHeader);
        }

        @Override
        public Mono<String> throwInsufficientStorage() {
            throw new WebException(HttpResponseStatus.INSUFFICIENT_STORAGE);
        }

        @Override
        public Mono<String> throwRuntimeException() {
            throw new RuntimeException();
        }

        @Override
        public Mono<String> throwException() {
            throw new AssertionError();
        }

        @Override
        public Mono<String> jsonParam(ParamEntity paramEntity) {
            return just("");
        }

        @Override
        public Mono<String> textPlain(String input) {
            return just(input);
        }

        @Override
        public Mono<String> applicationJson(String input) {
            return just(input);
        }

        @Override
        public Mono<String> byteArray(byte[] input) {
            return just(new String(input));
        }

        @Override
        public Mono<String> byteArrayAnyType(byte[] input) {
            return just(new String(input));
        }

        @Override
        public Mono<String> acceptsUuidQueryParam(UUID id) {
            return just("Id: " + (id != null ? id.toString() : null));
        }

        @Override
        public Mono<String> acceptsUuidFormParam(UUID id) {
            return just("Id: " + id.toString());
        }

        @Override
        public Mono<String> acceptsUuidPathParam(UUID id) {
            return just("Id: " + id.toString());
        }

        @Override
        public Mono<String> acceptsUuidHeader(UUID id) {
            return just("Id: " + id.toString());
        }

        @Override
        public Mono<String> acceptsGenericParam(List<ParamEntity> list) {
            Object listItem = list.getFirst();
            return just(listItem.getClass().getSimpleName());
        }

        @Override
        public Mono<String> acceptsTrailingSlash() {
            return just("OK");
        }

        public Mono<String> acceptsCookieParam(String cookie) {
            return just(cookie);
        }

        @Override
        public Mono<String> acceptsCookieParamCustomClassesWithValueOf(CustomParamWithValueOf myarg) {
            return just(myarg != null ? myarg.value : "null");
        }

        @Override
        public Mono<String> acceptsCookieParamCustomClassesWithFromString(CustomParamWithValueOf myarg) {
            return just(myarg != null ? myarg.value : "null");
        }

        @Override
        public Mono<String> acceptsCookieParamCustomClassesWithConstructor(CustomParamWithValueOf myarg) {
            return just(myarg != null ? myarg.value : "null");
        }

        @Override
        public Mono<String> acceptsDefaultCookieParam(int myCookie) {
            return just("Default: " + myCookie);
        }

        @Override
        public Mono<ParamEntity> acceptBodyGet(ParamEntity paramEntity) {
            return just(paramEntity);
        }

        @Override
        public Mono<ParamEntity> acceptBodyPut(ParamEntity paramEntity) {
            return just(paramEntity);
        }

        @Override
        public Mono<ParamEntityRecord> acceptBodyPutRecord(ParamEntityRecord paramEntity) {
            return just(paramEntity);
        }

        @Override
        public Mono<ParamEntity> acceptBodyPost(ParamEntity paramEntity) {
            return just(paramEntity);
        }

        @Override
        public Mono<ParamEntityRecord> acceptBodyPostRecord(ParamEntityRecord paramEntity) {
            return just(paramEntity);
        }

        @Override
        public Mono<ParamEntity> acceptBodyPatch(ParamEntity paramEntity) {
            return just(paramEntity);
        }

        @Override
        public Mono<ParamEntityRecord> acceptBodyPatchRecord(ParamEntityRecord paramEntity) {
            return just(paramEntity);
        }

        @Override
        public Mono<ParamEntity> acceptBodyDelete(ParamEntity paramEntity) {
            return just(paramEntity);
        }

        @Override
        public Mono<ParamEntityRecord> acceptBodyDeleteRecord(ParamEntityRecord paramEntity) {
            return just(paramEntity);
        }

        @Override
        public Mono<Integer> acceptsQueryList(List<String> strings, List<Integer> integers) {
            if (strings != null) {
                return just(strings.size());
            }
            if (integers != null) {
                return just(integers.size());
            }
            throw new IllegalArgumentException();
        }

        @Override
        public Mono<Integer> acceptsQueryArray(String[] strings, Integer[] integers) {
            if (strings != null) {
                return just(strings.length);
            }
            if (integers != null) {
                return just(integers.length);
            }
            throw new IllegalArgumentException();
        }

        @Override
        public Mono<Integer> acceptsQueryListWithEnum(List<TestEnum> enums) {
            return just(enums.size());
        }

        @Override
        public Mono<String> acceptsQueryListWithCustomClasses(List<CustomParamWithValueOf> objects) {
            return just(objects.stream().map(it -> it.value).collect(Collectors.joining()));
        }

        @Override
        public Mono<String> acceptsQueryArrayWithCustomClasses(CustomParamWithValueOf[] objects) {
            return just(Arrays.stream(objects).map(it -> it.value).collect(Collectors.joining()));
        }

        @Override
        public Mono<String> ignoresHeaderAnnotationOnInterface() {
            return just("");
        }

        @Override
        @Headers("Content-Disposition: attachment; filename=auditlog.csv")
        public Mono<String> acceptsHeadersOnImplementation() {
            return just("");
        }

        @Override
        public Mono<String> acceptsBeanParam(ParamEntity beanParam) {
            return just(String.format("%s - %d %d", beanParam.getName(), beanParam.getAge(), beanParam.getItems().size()));
        }

        @Override
        public Mono<String> acceptsBeanParamRecord(ParamEntityRecord beanParam) {
            return just(beanParam.toString());
        }

        @Override
        public Mono<String> acceptsBeanParamInherited(InheritedParamEntity beanParam) {
            return just(String.format("%s - %d %d - %s", beanParam.getName(), beanParam.getAge(), beanParam.getItems().size(), beanParam.getInherited()));
        }

        @Override
        public Mono<String> acceptsSlashVar(String myarg) {
            return just("var: " + myarg);
        }

        @Override
        public Mono<String> acceptsCustomClassPathParamWithValueOf(CustomParamWithValueOf myarg) {
            return just(myarg != null ? myarg.value : "null");
        }

        @Override
        public Mono<String> acceptsCustomClassPathParamWithFromString(CustomParamWithFromString myarg) {
            return just(myarg != null ? myarg.value : "null");
        }

        @Override
        public Mono<String> acceptsCustomClassPathParamWithConstructor(CustomParamWithConstructor myarg) {
            return just(myarg != null ? myarg.value : "null");
        }

        @Override
        public Mono<String> acceptsDouble(Double myarg) {
            return just("Double: " + myarg);
        }

        @Override
        public Mono<String> acceptsPostDouble(Double myarg) {
            return just("Double: " + myarg);
        }

        @Override
        public Mono<String> acceptsPostNotNullDouble(double myarg) {
            return just("double: " + myarg);
        }

        @Override
        public Mono<String> acceptsPostCustomClassWithValueOf(CustomParamWithValueOf myarg) {
            return just(myarg != null ? myarg.value : "null");
        }

        @Override
        public Mono<String> acceptsPostCustomClassWithFromString(CustomParamWithFromString myarg) {
            return just(myarg != null ? myarg.value : "null");
        }

        @Override
        public Mono<String> acceptsPostCustomClassWithConstructor(CustomParamWithConstructor myarg) {
            return just(myarg != null ? myarg.value : "null");
        }

        @Override
        public Mono<String> acceptsDefaultFormParam(int myarg) {
            return just("Default: " + myarg);
        }

        @Override
        public Mono<String> overloadedMethod(String param1, String param2) {
            return just("Param1: " + param1 + ", Param2: " + param2);
        }

        @Override
        public Mono<String> overloadedMethod(String param1) {
            return just("Param1: " + param1);
        }

        @Override
        public Mono<String> overloadedMethod() {
            return null;
        }
    }

    @Path("/test/accepts/res")
    class FooTest {
        @GET
        public Mono<String> test(Foo foo) {
            return just("foo: " + foo.getStr());
        }
    }

    @Path("special")
    class SpecialResource {

        @GET
        @Path("/{string}")
        public Mono<String> getString(@PathParam("string") String string) {
            return just(string);
        }

        @GET
        @Path("/strings")
        public Flux<String> getStrings() {
            return Flux.just("string", "string");
        }
    }

    @Path("default")
    class DefaultPathParamResource {
        @GET
        @Path("param/{path}")
        public Mono<String> defaultPath(@PathParam("path") @DefaultValue("defaultPath") String path) {
            return just("");
        }
    }

    public static class CustomParamWithValueOf {

        public final String value;

        private CustomParamWithValueOf(String value) {
            this.value = value;
        }

        @SuppressWarnings("unused")
        public static CustomParamWithValueOf valueOf(String value) {
            return new CustomParamWithValueOf(value);
        }
    }

    public static class CustomParamWithFromString {

        public final String value;

        private CustomParamWithFromString(String value) {
            this.value = value;
        }

        @SuppressWarnings("unused")
        public static CustomParamWithFromString fromString(String value) {
            return new CustomParamWithFromString(value);
        }
    }

    public static class CustomParamWithConstructor {

        public final String value;

        public CustomParamWithConstructor(String value) {
            this.value = value;
        }
    }
}

interface Foo {

    String getStr();

}
