package se.fortnox.reactivewizard.jaxrs;

import javax.ws.rs.QueryParam;
import java.util.List;

public class ParamEntity {

    @QueryParam("name")
    private String name;

    @QueryParam("age")
    private int age;

    @QueryParam("items")
    private List<String> items;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public List<String> getItems() {
        return this.items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }
}
