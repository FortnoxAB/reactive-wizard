package se.fortnox.reactivewizard.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.slf4j.event.Level;
import rx.Observable;
import se.fortnox.reactivewizard.jaxrs.FieldError;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.jaxrs.Wrap;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


/**
 * The purpose of this document is to show you how you can do some simple and
 * some more advanced validation of your input.
 */
public class ValidationExamples {

    /**
     * Create a class representing the input that should be validated
     * In this first example we add a @NotNull validation
     */
    class InputClass {
        @NotNull
        private String name;

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * Create your service. In this document we just create a mock of our
     * service interface.
     */
    AcceptingService<InputClass> service = mock(AcceptingService.class);

    /**
     * In ReactiveWizard validation is done using a
     * wrapper around your service. The reason for this is that we want to add
     * validation between services as well, not just on input coming from an
     * incoming http-request. To be able to create these wrappers your service
     * must have an interface, because java can not extend classes dynamically
     * out of the box.
     */
    AcceptingService<InputClass> validatedService = ValidatingProxy.create(AcceptingService.class, service, new ValidatorUtil());


    @Test
    public void shouldGiveValidationErrorWhenFieldIsNull() {
        /**
         * Pass an instance to your service, and you get an exception.
         * This is what the exception looks like when it is json-serialized and sent back to the caller.
         */
        assertValidationException(() -> validatedService.call(new InputClass()),
                "{'id':'.*','error':'validation','fields':[{'field':'name','error':'validation.notnull'}]}");

        /**
         * No call should have been made to your service
         */
        verify(service, times(0)).call(any());
    }

    @Test
    public void shouldCallServiceIfNoValidationError() {
        /**
         * Pass a valid instance, where name is not null, and you get no exception
         */
        validatedService.call(new InputClass() {{
            setName("some name");
        }});

        /**
         * And the service has been called
         */
        verify(service, times(1)).call(any());
    }

    /******************************************************
     * Next up we try the Min and Max validations
     *
     * In this class a single field has both a @Min and a @Max validation
     */
    class IntInputClass {
        @Min(3)
        @Max(6)
        private int age;

        public void setAge(int age) {
            this.age = age;
        }
    }

    @Test
    public void minMaxValidation() {
        /**
         * First we set the age to 1, which is less than the minimum of 3, so
         * we get a validation error:
         */
        assertValidationException(callService(new IntInputClass() {{
                    setAge(1);
                }}),
                "{'id':'.*','error':'validation','fields':[{'field':'age','error':'validation.min','errorParams':{'value':3}}]}");
        /**
         * Note that we also return the expected minimum in the error here, so
         * that the caller knows how to improve their request.
         */

        /**
         * Now we send in 8 which is higher than the max
         */
        assertValidationException(callService(new IntInputClass() {{
                    setAge(8);
                }}),
                "{'id':'.*','error':'validation','fields':[{'field':'age','error':'validation.max','errorParams':{'value':6}}]}");

        /**
         * And then we use a valid value of 4, which gives no error.
         */
        callService(new IntInputClass() {{
            setAge(4);
        }}).run();
    }

    /**
     * These are the constraints that are part of the javax validation api:
     *
     * @AssertFalse
     * @AssertTrue
     * @DecimalMax
     * @DecimalMin
     * @Digits
     * @Future
     * @Max
     * @Min
     * @NotNull
     * @Null
     * @Past
     * @Pattern
     * @Size
     *
     * And these are shipped with Hibernate:
     *
     * @CreditCardNumber
     * @EAN
     * @Email
     * @Length
     * @LuhnCheck
     * @Mod10Check
     * @Mod11Check
     * @ModCheck
     * @NotBlank
     * @NotEmpty
     * @ParameterScriptAssert
     * @Range
     * @SafeHtml
     * @ScriptAssert
     * @URL
     */


    class Period {

        @NotNull
        private Date startDate;

        private Date endDate;

        public Date getStartDate() {
            return startDate;
        }

        public void setStartDate(Date startDate) {
            this.startDate = startDate;
        }

        public Date getEndDate() {
            return endDate;
        }

        public void setEndDate(Date endDate) {
            this.endDate = endDate;
        }
    }

    /**
     * If we send in an entity where startDate and endDate is null, the
     *
     * @NotNull validation kicks in. This is why we do not return an error
     * if startDate is null in the PeriodValidator, because we would get
     * double validation errors.
     */
    @Test
    public void shouldFailIfStartDateIsNull() throws ParseException {
        assertValidationException(callService(new Period()),
                "{'id':'.*','error':'validation','fields':[{'field':'startDate','error':'validation.notnull'}]}");
    }

    /**
     * Since these validators are referred to in your entity which is part of
     * your api, you end up having the validation logic published in the api
     * module. To avoid this, you can create a subclass of your entity
     * in your impl-module and tell jaxrs to use the subclass when calling your
     * service. This is done using the @Wrap annotation.
     */

    /**
     * This is in your api module:
     */
    class PeriodPublic {
        @NotNull
        private Date startDate;
        @Future()
        private Date endDate;

        public Date getStartDate() {
            return startDate;
        }

        public void setStartDate(Date startDate) {
            this.startDate = startDate;
        }

        public Date getEndDate() {
            return endDate;
        }

        public void setEndDate(Date endDate) {
            this.endDate = endDate;
        }
    }

    interface MyPeriodService {
        Observable<Void> acceptsPeriod(PeriodPublic period);
    }

    /**
     * This is in your impl module:
     */
    class PeriodPrivate extends PeriodPublic {
    }


    /**
     * And in your service you add the @Wrap annotation to ensure that your subclass is injected.
     */

    class MyPeriodServiceImpl implements MyPeriodService {
        @Override
        public Observable<Void> acceptsPeriod(@Wrap(PeriodPrivate.class) PeriodPublic period) {
            return null;
        }
    }

    @Test
    public void shouldUseSubTypeWhenValidating() {
        MyPeriodService service = ValidatingProxy.create(MyPeriodService.class, new MyPeriodServiceImpl(), new ValidatorUtil());
        assertValidationException(() -> service.acceptsPeriod(new PeriodPrivate() {{
            setStartDate(new Date(10));
            setEndDate(new Date(8));
        }}), "{'id':'.*','error':'validation','fields':[{'field':'endDate','error':'validation.future'}]}");
    }

    /**
     * This way you get validation without exposing the logic in the api.
     */

    /****************************************************************************
     * Another form of custom validation is what you do within your service.
     * You can do any checks you like and throw a WebException describing the
     * validation error:
     */
    class MyValidatingService {
        public Observable<Void> acceptsPeriod(Period period) {
            Date startDateInDb = new Date(10);
            if (period.getStartDate().before(startDateInDb)) {
                throw new WebException(new FieldError("startDate", "startdate.before.stored.date",
                        new HashMap<String, Object>() {{
                            put("stored", startDateInDb);
                        }}));
            }
            return null;
        }
    }

    @Test
    public void shouldReturnValidationErrorFromService() {
        MyValidatingService service = new MyValidatingService();
        assertValidationException(() -> service.acceptsPeriod(new Period() {{
            setStartDate(new Date(5));
        }}), "{'id':'.*','error':'validation','fields':[{'field':'startDate','error':'validation.startdate.before.stored.date','errorParams':{'stored':10}}]}");
    }


    /**********************************************************************
     * One more thing to be careful with when doing validation is that if you
     * have entities within your entity which you want to validate, you must
     * annotate them with @Valid.
     */

    public class Child {
        @NotNull
        private String name;

        public void setName(String name) {
            this.name = name;
        }
    }

    public class EntityWithUnvalidatedChild {
        @NotNull
        private Child child;

        public void setChild(Child child) {
            this.child = child;
        }
    }

    @Test
    public void shouldValidateParentButNotChildIfNoValidateAnnotation() {
        assertValidationException(callService(new EntityWithUnvalidatedChild()),
                "{'id':'.*','error':'validation','fields':[{'field':'child','error':'validation.notnull'}]}");
        callService(new EntityWithUnvalidatedChild() {{
            setChild(new Child());
        }});
    }

    public class EntityWithValidatedChild {
        @NotNull
        @Valid
        private Child child;

        public void setChild(Child child) {
            this.child = child;
        }
    }

    @Test
    public void shouldValidateParentAndChildIfValidateAnnotation() {
        assertValidationException(callService(new EntityWithValidatedChild() {{
                    setChild(new Child());
                }}),
                "{'id':'.*','error':'validation','fields':[{'field':'child.name','error':'validation.notnull'}]}");
        callService(new EntityWithValidatedChild() {{
            setChild(new Child() {{

            }});
        }});
    }

    record PeriodRecord(
            @NotNull Date startDate,
            Date endDate) {
    }

    /**
     * Records are also validated
     */
    @Test
    public void shouldFailIfStartDateIsNullInRecord() {
        assertValidationException(callService(new PeriodRecord(null, null)),
                "{'id':'.*','error':'validation','fields':[{'field':'startDate','error':'validation.notnull'}]}");
    }

    private static <T> Runnable callService(T inputClass) {
        AcceptingService<T> service = mock(AcceptingService.class);
        AcceptingService<T> validatedService = ValidatingProxy.create(AcceptingService.class, service, new ValidatorUtil());
        return () -> validatedService.call(inputClass);
    }

    private static void assertValidationException(Runnable runnable, String exceptionAsJson) {
        String pattern = exceptionAsJson
                .replaceAll("'", "\\\"")
                .replaceAll("\\{", "\\\\{")
                .replaceAll("\\}", "\\\\}")
                .replaceAll("\\[", "\\\\[")
                .replaceAll("\\]", "\\\\]");
        assertThatExceptionOfType(WebException.class)
                .isThrownBy(() -> runnable.run())
                .satisfies(webException -> {
                    assertThat(new ObjectMapper().writeValueAsString(webException)).matches(pattern);
                    assertThat(webException.getLogLevel()).isEqualTo(Level.INFO);
                });
    }

    // And a service accepting the class.
    interface AcceptingService<T> {
        Observable<T> call(T input);
    }

    /**********************************************************************
     * If you pass an iterable containing objects to be validated, they
     * are all individually validated by default (just like with single
     * objects) without the need to explicitly add the @Valid annotation
     * to the parameter.
     */
    AcceptingService<Iterable<InputClass>> iterableValidationService = ValidatingProxy.create(
        AcceptingService.class, service, new ValidatorUtil()
    );

    /**
     * Creates an instance of the InputClass with a null value in a field
     * annotated with the @NotNull annotation. Puts the invalid InputObject
     * in a list and passes it to the service. The object is validated.
     */
    @Test
    public void shouldValidateObjectsInIterables() {
        assertValidationException(() -> iterableValidationService.call(List.of(new InputClass())),
            "{'id':'.*','error':'validation','fields':[{'field':'name','error':'validation.notnull'}]}");
        verify(service, times(0)).call(any());
    }

    @Test
    public void shouldPassIterableIfNoErrorsWithin() {
        iterableValidationService.call(List.of(new InputClass() {{ setName("some name"); }}));
        verify(service, times(1)).call(any());
    }
}
