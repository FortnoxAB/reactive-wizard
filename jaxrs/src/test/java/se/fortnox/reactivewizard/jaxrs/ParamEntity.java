package se.fortnox.reactivewizard.jaxrs;

import javax.ws.rs.QueryParam;

public class ParamEntity {

    @QueryParam("name")
    private String name;

    @QueryParam("age")
    private int age;

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
}
