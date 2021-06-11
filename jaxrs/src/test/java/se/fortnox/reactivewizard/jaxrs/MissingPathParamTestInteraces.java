package se.fortnox.reactivewizard.jaxrs;

import rx.Observable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static rx.Observable.empty;

public class MissingPathParamTestInteraces {

    @Path("1")
    interface NoPathParamAnnotation {
        @GET
        @Path("/test/{param}")
        default Observable<String> getParam() {
            return empty();
        }
    }

    @Path("2")
    interface AnnotatedMethodShouldSuppressMissingPathParams {
        @GET
        @Path("/test/{param1}/{param2}")
        @SuppressMissingPathParam(paramName = {"param1", "param2"})
        default Observable<String> getParam() {
            return empty();
        }
    }

    @Path("3")
    interface AnnotatedMethodShouldOnlySuppressSuppliedParamName {
        @GET
        @Path("/test/{param1}/{param2}")
        @SuppressMissingPathParam(paramName = "param1")
        default Observable<String> getParam() {
            return empty();
        }
    }

    @Path("4/{param1}")
    @SuppressMissingPathParam(paramName = {"param1", "param2"})
    interface AnnotatedClassShouldSuppressOnAllEndpoints {
        @GET
        @Path("/test/{param2}")
        default Observable<String> getParam() {
            return empty();
        }


        @GET
        @Path("/test2/{param2}")
        default Observable<String> getParam2() {
            return empty();
        }

    }

    @Path("5")
    @SuppressMissingPathParam(paramName = "param1")
    interface AnnotatedClassShouldOnlySuppressSuppliedParamName {
        @GET
        @Path("/test/{param1}/{param2}")
        default Observable<String> getParam() {
            return empty();
        }
    }
}
