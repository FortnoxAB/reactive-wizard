package se.fortnox.reactivewizard.jaxrs;

import rx.Observable;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.time.LocalDate;

import static rx.Observable.empty;

public class CollidingTestInterfaces {

    @Path("test")
    public interface SamePathDifferentVerb {
        @Path("dateentity")
        @GET
        default Observable<Void> returnsDateEntity() {
            return empty();
        }

        @Path("dateentity")
        @POST
        default Observable<Void> accepptsDateEntity(String dateEntity) {
            return empty();
        }
    }

    @Path("testfail")
    public interface CustomRegexPathParam {
        @Path("{name:[^\\d]*}{duration}seconds.jfr")
        @GET
        default Observable<Void> testCustomRegex(@PathParam("duration") int duration, @QueryParam("settings") String settings) {
            return empty();
        }
    }

    @Path("1")
    interface OtherParametersDiffer {
        @GET
        @Path("/test/{string}")
        default Observable<String> getString(@PathParam("string") String string, @QueryParam("queryparamone") String value) {
            return empty();
        }

        @GET
        @Path("/test/{string}")
        default Observable<String> getString2(@PathParam("string") String string, @QueryParam("queryparamtwo") String value) {
            return empty();
        }
    }

    @Path("1")
    interface OtherParametersAreTheSame {
        @GET
        @Path("/test/{string}")
        default Observable<String> getString(@PathParam("string") String string, @QueryParam("queryparamone") String value) {
            return empty();
        }

        @GET
        @Path("/test/{string}")
        default Observable<String> getString2(@PathParam("string") String string, @QueryParam("queryparamone") String value) {
            return empty();
        }
    }

    @Path("1")
    interface OtherParametersAreTheSameInDifferentOrder {
        @GET
        @Path("/test/{string}")
        default Observable<String> getString(@PathParam("string") String string, @QueryParam("One") String value, @QueryParam("Two") String value2) {
            return empty();
        }

        @GET
        @Path("/test/{string}")
        default Observable<String> getString2(@PathParam("string") String string, @QueryParam("Two") String value, @QueryParam("One") String value2) {
            return empty();
        }
    }

    @Path("1")
    interface SamePathAndVerbDifferentType {
        @GET
        @Path("/test/{date}")
        default Observable<String> getDate(@PathParam("date") LocalDate date) {
            return empty();
        }

        @GET
        @Path("/test/{string}")
        default Observable<String> getString(@PathParam("string") String string) {
            return empty();
        }
    }

    @Path("1")
    interface VerbDiffers {
        @GET
        @Path("/test/{date}")
        default Observable<String> getDate(@PathParam("date") LocalDate date) {
            return empty();
        }

        @POST
        @Path("/test/{string}")
        default Observable<String> getString(@PathParam("string") String string) {
            return empty();
        }
    }

    @Path("1")
    interface NoPathParamAnnotation {
        @GET
        @Path("/test/{date}")
        default Observable<String> getDate(LocalDate date) {
            return empty();
        }
    }

    @Path("1")
    interface One {
        @GET()
        @Path("{id}")
        default Observable<String> get(@PathParam("id") String id) {
            return empty();
        }
    }

    @Path("2")
    interface Two {
        @GET()
        @Path("{id}")
        default Observable<String> get(@PathParam("id") LocalDate id) {
            return empty();
        }
    }


    @Path("ignoredfail")
    interface IgnoreErrorsWhenSuppressAnnotated {

        @GET
        @Path("{id}")
        @SuppressPathCollision
        default Observable<String> get(@PathParam("id") String id) {
            return empty();
        }

        @GET
        @Path("{id}")
        default Observable<String> get2(@PathParam("id") String id) {
            return empty();
        }
    }

}
