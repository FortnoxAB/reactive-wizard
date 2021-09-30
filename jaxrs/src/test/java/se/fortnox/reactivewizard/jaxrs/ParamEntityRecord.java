package se.fortnox.reactivewizard.jaxrs;

import java.util.List;

public record ParamEntityRecord(
    String name,
    int age,
    List<String> items
) {
}
