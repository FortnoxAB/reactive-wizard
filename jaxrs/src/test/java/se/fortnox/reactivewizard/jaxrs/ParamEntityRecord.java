package se.fortnox.reactivewizard.jaxrs;

import javax.ws.rs.QueryParam;
import java.util.List;

public record ParamEntityRecord(
    @QueryParam("name") String name,
    @QueryParam("age") int age,
    @QueryParam("items") List<String> items
) {
}
