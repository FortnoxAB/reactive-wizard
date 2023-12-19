package se.fortnox.reactivewizard.jaxrs;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import java.util.List;

public record ParamEntityRecord(
    @QueryParam("name") String name,
    @QueryParam("age") @DefaultValue("123") int age,
    @QueryParam("items") List<String> items
) {
}
