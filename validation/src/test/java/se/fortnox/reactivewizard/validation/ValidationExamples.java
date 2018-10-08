package se.fortnox.reactivewizard.validation;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.lang.annotation.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.Valid;
import javax.validation.constraints.Future;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import se.fortnox.reactivewizard.jaxrs.Wrap;
import se.fortnox.reactivewizard.jaxrs.FieldError;
import se.fortnox.reactivewizard.jaxrs.WebException;
import org.junit.Test;
import rx.Observable;


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

    /***********************************************************
     * Now let's create some custom validations.
     *
     * We start with a custom field validation. We create our entity with a date
     * and we add a custom validation @Before meaning that the date must be before
     * the date we pass to the validator.
     */

    class EntityWithDate {
        @Before("2016-01-01")
        private Date date;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }

    /**
     * Then we define the annotation @Before. We mark it as applicable to fields
     * and we set the validatedBy to a class com.fortnox.reactivewizard.validation.BeforeValidator.
     *
     * We set the message to the error code that we want to output in our
     * response. This should be a string intended to be used in a mapping (or
     * if-statement). The "groups" and "payload" is something hibernate
     * requires but which is of no interest to us at this point.
     *
     * The String value is the parameter to our annotation. You can add
     * multiple of these (with outher names than value), and the will be part
     * of the error response.
     */
    @Target(value={ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy=BeforeValidator.class)
    @Documented
    public @interface Before {
        String message() default "not.before";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        String value();
    }

    /**
     * This is our validator. It has an initialize method where you can grab
     * your parameters. In this case we parse the date from the String.
     *
     * It also has an isValid method where you return true or false.
     */
    public static class BeforeValidator extends CustomFieldValidator<Before, Date> {

        private Date date;

        @Override
        public void initialize(Before before) {
            try {
                date = new SimpleDateFormat("yyyy-MM-dd").parse(before.value());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isValid(Date d) {
            if (d != null && !d.before(date)) {
                return false;
            }
            return true;
        }
    }

    /**
     * Now we try it out. We pass an entity which has a date after 2016-01-01
     * to our service and expect it to give us an error back.
     *
     * Note that the response contains our error code and also the value we set
     * on the annotation.
     */
    @Test
    public void shouldFailIfFieldValidationIsNotMet() throws ParseException {
        assertValidationException(callService(new EntityWithDate() {{
                setDate(new SimpleDateFormat("yyyy-MM-dd").parse("2016-02-01"));
            }}),
            "{'id':'.*','error':'validation','fields':[{'field':'date','error':'validation.not.before','errorParams':{'value':'2016-01-01'}}]}");
    }

    /**
     * Next is validation on the entity level, which is useful when you have a
     *      * dependency between fields. A common thing is that you have a startDate
     *      * and endDate and the endDate, if present, must not be before the
     *      * startDate. Field level validation is of no use here, so we need to put
     *      * the validation on the entity.
     *      *
     *      * We create a new annotation @PeriodValidation and add that to a Period
     *      * class.
     */
    @PeriodValidation
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
     * This is how the annotation looks like. It is exactly like the @Before
     * annotation above, but in this case we do not define a message. That is
     * because we want the validator to give us a message per field that fails
     * validation, not for the whole entity.
     */
    @Target(value={ElementType.TYPE, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy=PeriodValidator.class)
    @Documented
    public @interface PeriodValidation {
        String message() default "";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    /**
     * And this is our validator. It resembles the BeforeValidator but since it
     * extends CustomEntityValidator the isValid method should return a list of
     * errors, where we get one or more errors for every field that is invalid.
     */
    public static class PeriodValidator extends CustomEntityValidator<PeriodValidation, Period> {

        @Override
        public void initialize(PeriodValidation periodValidation) {
        }

        @Override
        public List<FieldError> isValid(Period entity) {
            if (entity != null
                && entity.getStartDate() != null
                && entity.getEndDate() != null
                && entity.getEndDate().before(entity.getStartDate())) {
                return asList(new FieldError("endDate", "enddate.before.startdate"));
            }
            return null;
        }
    }

    /**
     * When we send in a period with an endDate before the startDate we expect
     * to get a validation error. The error is pointing to the endDate field
     * and we get the error code from the validator.
     */
    @Test
    public void shouldFailIfEndDateBeforeStartDate() {
        assertValidationException(callService(new Period() {{
                setStartDate(new Date(10));
                setEndDate(new Date(8));
            }}),
            "{'id':'.*','error':'validation','fields':[{'field':'endDate','error':'validation.enddate.before.startdate'}]}");

    }

    /**
     * And if we send in an ok entity we get no error.
     */
    @Test
    public void shouldNotFailIfValidationIsMet() {
        // pass a valid instance, and you get no exception
        callService(new Period() {{
            setStartDate(new Date(10));
            setEndDate(new Date(18));
        }}).run();
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
    @PeriodPrivateValidation
    class PeriodPrivate extends PeriodPublic {
    }

    @Target(value = {ElementType.TYPE, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = PeriodPrivateValidator.class)
    @Documented
    public @interface PeriodPrivateValidation {
        String message() default "";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class PeriodPrivateValidator extends CustomEntityValidator<PeriodPrivateValidation, PeriodPrivate> {

        @Override
        public void initialize(PeriodPrivateValidation periodValidation) {
        }

        @Override
        public List<FieldError> isValid(PeriodPrivate entity) {
            if (entity != null
                && entity.getStartDate() != null
                && entity.getEndDate() != null
                && entity.getEndDate().before(entity.getStartDate())) {
                return asList(new FieldError("endDate", "validation.enddate.before.startdate"));
            }
            return null;
        }
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
        }}), "{'id':'.*','error':'validation','fields':[{'field':'endDate','error':'validation.enddate.before.startdate'},{'field':'endDate','error':'validation.future'}]}");
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

    private static <T> Runnable callService(T inputClass) {
        AcceptingService<T> service          = mock(AcceptingService.class);
        AcceptingService<T> validatedService = ValidatingProxy.create(AcceptingService.class, service, new ValidatorUtil());
        return () -> validatedService.call(inputClass);
    }

    private static void assertValidationException(Runnable runnable, Consumer<WebException> validationAsserter) {
        try {
            runnable.run();
        } catch (WebException e) {
            validationAsserter.accept(e);
            return;
        }
        fail("expected validation exception");
    }

    private static void assertValidationException(Runnable runnable, String exceptionAsJson) {
        String pattern = exceptionAsJson
            .replaceAll("'", "\\\"")
            .replaceAll("\\{", "\\\\{")
            .replaceAll("\\}", "\\\\}")
            .replaceAll("\\[", "\\\\[")
            .replaceAll("\\]", "\\\\]");
        assertValidationException(runnable, e -> {
            try {
                assertThat(new ObjectMapper().writeValueAsString(e)).matches(pattern);
            } catch (JsonProcessingException e1) {
                throw new RuntimeException(e1);
            }
        });
    }

    // And a service accepting the class.
    interface AcceptingService<T> {
        Observable<T> call(T input);
    }
}

