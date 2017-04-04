package se.fortnox.reactivewizard.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import se.fortnox.reactivewizard.MockHttpServerRequest;
import se.fortnox.reactivewizard.MockHttpServerResponse;
import se.fortnox.reactivewizard.jaxrs.params.*;
import se.fortnox.reactivewizard.jaxrs.params.annotated.AnnotatedParamResolverFactories;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.DeserializerFactory;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResult;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResultFactoryFactory;
import se.fortnox.reactivewizard.jaxrs.response.ResultTransformerFactories;
import com.google.inject.*;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;
import rx.Single;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static se.fortnox.reactivewizard.jaxrs.JaxRsTestUtil.*;
import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

public class JaxRsResourceTest {

	public enum TestEnum {
		ONE, TWO, THREE
	}

	private final TestresourceInterface service = new TestresourceImpl();

	@Test
	public void shouldConcatPaths() throws InvocationTargetException, IllegalAccessException {
		JaxRsResources resources = new JaxRsResources(new Object[]{new Testresource()}, new JaxRsResourceFactory(), false);
		assertThat(resources.call(new MockHttpServerRequest("/test/acceptsString"))).isNotNull();
	}

	@Test
	public void shouldResolveArgs() throws Exception {
		assertThat(body(get(new Testresource(), "/test/acceptsString", qp("myarg", "hepp"))))
						.isEqualTo("\"inp: hepp\"");
	}

	@Test
	public void shouldHandleInterfaceAnnotations() throws Exception {
		assertThat(body(get(service, "/test/accepts", qp("myarg", "hepp"))))
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

		Observable<JaxRsResult<?>> result = new JaxRsResources(
				new Object[] { new FooTest() },
				new JaxRsResourceFactory(
						new ParamResolverFactories(
								new DeserializerFactory(),
								paramResolvers,
								new AnnotatedParamResolverFactories(),
								new WrapSupportingParamTypeResolver()),
						new JaxRsResultFactoryFactory(),
						new BlockingResourceScheduler()),
				false).call(req);

		MockHttpServerResponse response = new MockHttpServerResponse();

		result.toBlocking()
				.single()
				.write(response)
				.toBlocking()
				.lastOrDefault(null);

		assertThat(response.getOutp()).isEqualTo("\"foo: 5678\"");
	}

	@Test
	public void shouldSupportSimpleQueryParamTypes() throws Exception {
		assertThat(
				body(get(service, "/test/acceptsBoolean", qp("myarg", "true"))))
						.isEqualTo("\"Boolean: true\"");

		assertThat(body(get(service, "/test/acceptsInteger", qp("myarg", "678"))))
				.isEqualTo("\"Integer: 678\"");

		assertThat(body(get(service, "/test/acceptsLong", qp("myarg", "678"))))
				.isEqualTo("\"Long: 678\"");

		assertThat(body(get(service, "/test/acceptsDate", qp("myarg", "1234567890123"))))
						.isEqualTo("\"2009-02-13T23:31:30.123+0000\"");

		assertThat(body(get(service, "/test/acceptsEnum", qp("myarg", "ONE"))))
				.isEqualTo("\"Enum: ONE\"");
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
				body(JaxRsTestUtil.post(service, "/test/acceptsPostString", "myString=accepts")))
						.isEqualTo("\"String: accepts\"");

		assertThat(
				body(JaxRsTestUtil.post(service, "/test/acceptsPostBoolean", "myBoolean=true")))
						.isEqualTo("\"Boolean: true\"");

		assertThat(
				body(JaxRsTestUtil.post(service, "/test/acceptsPostInteger", "myInteger=678")))
						.isEqualTo("\"Integer: 678\"");

		assertThat(body(JaxRsTestUtil.post(service, "/test/acceptsPostLong", "myLong=678")))
				.isEqualTo("\"Long: 678\"");

		assertThat(
				body(JaxRsTestUtil.post(service, "/test/acceptsPostDouble", "myDouble=678.78")))
						.isEqualTo("\"Double: 678.78\"");

		assertThat(body(
				JaxRsTestUtil.post(service, "/test/acceptsPostDate", "myDate=1234567890123")))
						.isEqualTo("\"Date: " + new Date(1234567890123L) + "\"");

		assertThat(body(JaxRsTestUtil.post(service, "/test/acceptsPostEnum", "myEnum=ONE")))
				.isEqualTo("\"Enum: ONE\"");
	}

	@Test
	public void shouldSupportSimpleHeaderParamTypes() {
		assertThat(body(JaxRsTestUtil.getWithHeaders(service,
				"/test/acceptsHeaderString",
				qp("myHeader", "accepts")))).isEqualTo("\"header: accepts\"");
		assertThat(body(JaxRsTestUtil.getWithHeaders(service,
				"/test/acceptsHeaderInteger",
				qp("myHeader", "4")))).isEqualTo("\"header: 4\"");
		assertThat(body(JaxRsTestUtil.getWithHeaders(service, "/test/acceptsHeaderEnum", qp("myHeader", "ONE")))).isEqualTo("\"header: ONE\"");
	}

	@Test
	public void shouldSupportMissingHeaderParams() {
		assertThat(body(JaxRsTestUtil.getWithHeaders(service,
				"/test/acceptsHeaderString",
				qp("dummy", "accepts")))).isEqualTo("\"header: null\"");
	}

	@Test
	public void shouldSupportMissingNullableParams() {
		assertThat(body(JaxRsTestUtil.post(service, "/test/acceptsPostBoolean", "dummy=true")))
				.isEqualTo("\"Boolean: null\"");

		assertThat(body(JaxRsTestUtil.post(service, "/test/acceptsPostInteger", "dummy=678")))
				.isEqualTo("\"Integer: null\"");

		assertThat(body(JaxRsTestUtil.post(service, "/test/acceptsPostLong", "dummy=678")))
				.isEqualTo("\"Long: null\"");

		assertThat(body(JaxRsTestUtil.post(service, "/test/acceptsPostDouble", "dummy=678")))
				.isEqualTo("\"Double: null\"");

	}

	@Test
	public void shouldGiveErrorForMissingNotNullableParams() {
		assertBadRequest(JaxRsTestUtil.post(service, "/test/acceptsPostNotNullBool", "dummy=true"),
				"{'id':'.*','error':'validation','fields':[{'field':'myBoolean','error':'validation.invalid.boolean'}]}");
		assertBadRequest(JaxRsTestUtil.post(service, "/test/acceptsPostNotNullInt", "dummy=678"),
				"{'id':'.*','error':'validation','fields':[{'field':'myInteger','error':'validation.invalid.int'}]}");
		assertBadRequest(JaxRsTestUtil.post(service, "/test/acceptsPostNotNullLong", "dummy=678"),
				"{'id':'.*','error':'validation','fields':[{'field':'myLong','error':'validation.invalid.long'}]}");
		assertBadRequest(JaxRsTestUtil.post(service, "/test/acceptsPostNotNullDouble", "dummy=678"),
				"{'id':'.*','error':'validation','fields':[{'field':'myDouble','error':'validation.invalid.double'}]}");
	}

	@Test
	public void shouldSupportOverloadedMethods() throws Exception {
		assertThat(body(get(service, "/test/overloadedMethod", qp("param1", "myparam"))))
				.isEqualTo("\"Param1: myparam, Param2: null\"");
		assertThat(body(get(service, "/test/overloadedMethod")))
				.isEqualTo("\"Param1: null, Param2: null\"");
		Map<String, List<String>> params = qp("param1", "myparam");
		params.put("param2", asList("myparam2"));
		assertThat(body(get(service, "/test/overloadedMethod", params)))
				.isEqualTo("\"Param1: myparam, Param2: myparam2\"");
	}

	private void assertBadRequest(MockHttpServerResponse response,
			String expectedBodyRegex) {
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
		assertBadRequest(get(service, "/test/acceptsInteger", qp("myarg", "badvalue")),
				"{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.int'}]}");
		assertBadRequest(get(service, "/test/acceptsLong", qp("myarg", "badvalue")),
				"{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.long'}]}");
		assertBadRequest(get(service, "/test/acceptsDouble", qp("myarg", "badvalue")),
				"{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.double'}]}");
	}

	@Test
	public void shouldGiveErrorForBadDates() throws Exception {
		assertBadRequest(get(service, "/test/acceptsDate", qp("myarg", "20aa-01-01")),
				"{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.date'}]}");

		assertBadRequest(get(service, "/test/acceptsDate", qp("myarg", "2010-0b-01 00:00:00")),
				"{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.date'}]}");

		assertBadRequest(get(service, "/test/acceptsDate", qp("myarg", "2010-01-qehjeq:00:00.000")),
				"{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.date'}]}");

		assertBadRequest(get(service, "/test/acceptsDate", qp("myarg", "97697697676767688675767")),
				"{'id':'.*','error':'validation','fields':[{'field':'myarg','error':'validation.invalid.date'}]}");

	}

	@Test
	public void shouldSupportDatesAsString() throws Exception {
		assertThat(body(
				get(service, "/test/acceptsDate", qp("myarg", "2010-01-01"))))
						.isEqualTo("\"2010-01-01T00:00:00.000+0000\"");

		assertThat(body(get(service, "/test/acceptsDate", qp("myarg", "2010-01-01T00:00:00.000"))))
						.isEqualTo("\"2010-01-01T00:00:00.000+0000\"");
	}

	class CustomDateFormat extends StdDateFormat {

		public CustomDateFormat() {
			super(TimeZone.getTimeZone("Europe/Stockholm"), Locale.getDefault(), true);
		}
		@Override
		public Date parse(String source) throws ParseException  {
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
				bind(DateFormat.class).toProvider(()->new CustomDateFormat());
				bind(JaxRsServiceProvider.class).toInstance(()->new Object[]{service});
				bind(new TypeLiteral<Set<ParamResolver>>(){{}}).toInstance(Collections.EMPTY_SET);
				bind(new TypeLiteral<Set<ParamResolverFactory>>(){{}}).toInstance(Collections.EMPTY_SET);
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
						JaxRsTestUtil.processRequestWithHandler(handler, new MockHttpServerRequest("/test/acceptsDate", qp("myarg", "2010-01-01 00:00:00")))))
				.isEqualTo("\"2010-01-01T00:00:00.000+0100\"");
	}

	@Test
	public void shouldSupportLocalDate() {
		assertThat(body(
				get(service, "/test/acceptsLocalDate", qp("myarg", "2010-01-01"))))
				.isEqualTo("{\"localDate\":\"2010-01-01\"}");
	}

	@Test
	public void shouldSupportLocalTime() {
		assertThat(body(
				get(service, "/test/acceptsLocalTime", qp("myarg", "13:37:37"))))
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
		assertBadRequest(JaxRsTestUtil.post(service, "/test/jsonParam", "{hej}"),
				"{\"id\":\".*\",\"error\":\"invalidjson\",\"message\":\"Unexpected character ('h' (code 104)): was expecting double-quote to start field name\"}");
		assertBadRequest(JaxRsTestUtil.post(service, "/test/jsonParam", "{\"age\": \"nan\"}"),
				"{\"id\":\".*\",\"error\":\"invalidjson\",\"message\":\"Can not construct instance of int from String value (\\\\\"nan\\\\\"): not a valid Integer value\"}");
		assertBadRequest(JaxRsTestUtil.post(service, "/test/jsonParam", (String)null),
				"{\"id\":\".*\",\"error\":\"invalidjson\",\"message\":\"No content to map due to end-of-input\"}");
	}

	@Test
	public void shouldAcceptTextPlainInput() {
		String text = "my plain text";
		MockHttpServerResponse resp = JaxRsTestUtil.post(service, "/test/textPlain", text);
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
		assertBadRequest(JaxRsTestUtil.post(service, "/test/acceptsPostEnum", "myEnum=BAD"),
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
		} catch(RuntimeException e) {
			assertThat(e.getMessage()).isEqualTo("Field of type class se.fortnox.reactivewizard.jaxrs.ParamEntity is not allowed to be used in query/form/header");
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
	public void shouldSupportUUIDAsQueryParameter() {
		UUID uuid = UUID.randomUUID();
		assertThat(body(
				get(service, "/test/acceptsUUID", qp("id", uuid.toString()))))
				.isEqualTo("\"Id: " + uuid + "\"");
	}

	@Test
	public void shouldSupportEmptyUUIDAsQueryParameter() {
		assertThat(body(
				get(service, "/test/acceptsUUID", qp("id", ""))))
				.isEqualTo("\"Id: null\"");

		assertThat(body(
				get(service, "/test/acceptsUUID")))
				.isEqualTo("\"Id: null\"");
	}

	@Test
	public void shouldSupportUUIDAsFormParameter() {
		UUID uuid = UUID.randomUUID();
		assertThat(body(
				JaxRsTestUtil.post(service, "/test/acceptsUUID", "id=" + uuid)))
				.isEqualTo("\"Id: " + uuid + "\"");
	}

	@Test
	public void shouldSupportUUIDAsPathParameter() {
		UUID uuid = UUID.randomUUID();
		assertThat(body(
				get(service, "/test/acceptsUUID/" + uuid)))
				.isEqualTo("\"Id: " + uuid + "\"");
	}

	@Test
	public void shouldSupportUUIDAsHeader() {
		UUID uuid = UUID.randomUUID();
		assertThat(body(JaxRsTestUtil.getWithHeaders(service,
				"/test/acceptsUUIDHeader",
				qp("id", uuid.toString())))).isEqualTo("\"Id: " + uuid.toString() + "\"");
	}

	@Test
	public void shouldSetContentType() {
		assertThat(get(new Testresource(), "/test/acceptsString").getHeaders().get("Content-Type"))
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
		TestresourceInterface proxy = (TestresourceInterface) Proxy.newProxyInstance(
				TestresourceInterface.class.getClassLoader(),
				new Class[]{TestresourceInterface.class},
				(instance, method, args)-> method.invoke(service, args)
		);
		assertThat(body(post(proxy, "/test/generic-param", "[{\"name\":\"test\"}]"))).isEqualTo("\"ParamEntity\"");
	}

	@Test
	public void shouldResolveCookieParam() {
		assertThat(body(getWithHeaders(service, "/test/acceptsCookieParam", new HashMap() {{
			put("Cookie", asList("fnox_session=testcookie"));
		}}))).isEqualTo("\"testcookie\"");
	}

	@Test
	public void shouldAcceptBodyForPUT_POST_PATCH_DELETE() throws Exception {
		assertThat(body(put(service, "/test/acceptBodyPut", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0}");
		assertThat(body(post(service, "/test/acceptBodyPost", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0}");
		assertThat(body(patch(service, "/test/acceptBodyPatch", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0}");
		assertThat(body(delete(service, "/test/acceptBodyDelete", "{\"name\":\"test\"}"))).isEqualTo("{\"name\":\"test\",\"age\":0}");
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

		@Path("acceptsHeaderString")
		@GET
		Observable<String> acceptsHeader(@HeaderParam("myHeader") String myHeader);

		@Path("acceptsHeaderInteger")
		@GET
		Observable<String> acceptsHeaderInteger(@HeaderParam("myHeader") Integer myHeader);

		@Path("acceptsHeaderEnum")
		@GET
		Observable<String> acceptsHeaderEnum(@HeaderParam("myHeader") TestEnum myHeader);

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

		@GET
		@Path("acceptsUUID")
		Observable<String> acceptsUUIDQueryParam(@QueryParam("id") UUID id);

		@POST
		@Path("acceptsUUID")
		Observable<String> acceptsUUIDFormParam(@FormParam("id") UUID id);

		@GET
		@Path("acceptsUUID/{id}")
		Observable<String> acceptsUUIDPathParam(@PathParam("id") UUID id);

		@GET
		@Path("acceptsUUIDHeader")
		Observable<String> acceptsUUIDHeader(@HeaderParam("id") UUID id);

		@POST
		@Path("generic-param")
		Observable<String> acceptsGenericParam(List<ParamEntity> list);

		@GET
		@Path("trailingSlash/")
		Observable<String> assceptsTrailingSlash();

		@Path("acceptsCookieParam")
		@GET
		Observable<String> acceptsCookieParam(@CookieParam("fnox_session") String cookie);

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
			@QueryParam("Integerlist") List<Integer> integers);

		@Path("acceptsQueryArray")
		@GET
		Observable<Integer> acceptsQueryArray(@QueryParam("Stringarray") String[] strings,
			@QueryParam("Integerarray") Integer[] integers);

		@Path("acceptsQueryListWithEnum")
		@GET
		Observable<Integer> acceptsQueryListWithEnum(@QueryParam("EnumList") List<TestEnum> enums);
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
		public Observable<LocalDateContainer> acceptsLocalDate(@QueryParam("myarg") LocalDate myarg) {
			return just(new LocalDateContainer(myarg));
		}

		@Override
		public Observable<LocalTimeContainer> acceptsLocalTime(@QueryParam("myarg") LocalTime myarg) {
			return just(new LocalTimeContainer(myarg));
		}

		@Override
		public Observable<String> acceptsEnum(TestEnum myarg) {
			return just("Enum: " + myarg);
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
		public Observable<String> acceptsUUIDQueryParam(@QueryParam("id") UUID id) {
			return just("Id: " + (id!=null ? id.toString() : null));
		}

		@Override
		public Observable<String> acceptsUUIDFormParam(@FormParam("id") UUID id) {
			return just("Id: " + id.toString());
		}

		@Override
		public Observable<String> acceptsUUIDPathParam(@PathParam("id") UUID id) {
			return just("Id: " + id.toString());
		}

		@Override
		public Observable<String> acceptsUUIDHeader(@HeaderParam("id") UUID id) {
			return just("Id: " + id.toString());
		}

		@Override
		public Observable<String> acceptsGenericParam(List<ParamEntity> list) {
			Object listItem = list.get(0);
			return just(listItem.getClass().getSimpleName());
		}

		@Override
		public Observable<String> assceptsTrailingSlash() {
			return just("OK");
		}

		public Observable<String> acceptsCookieParam(@CookieParam("fnox_session") String cookie) {
			return just(cookie);
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
		public Observable<Integer> acceptsQueryListWithEnum(
				@QueryParam("EnumList") List<TestEnum> enums) {
			return just(enums.size());
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

}

interface Foo {

	String getStr();

}
