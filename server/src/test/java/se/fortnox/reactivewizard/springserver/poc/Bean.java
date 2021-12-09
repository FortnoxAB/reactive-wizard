package se.fortnox.reactivewizard.springserver.poc;

import javax.ws.rs.FormParam;
import javax.ws.rs.QueryParam;

public class Bean {
    @QueryParam("beanName")
    private String beanName;
    @QueryParam("beanAge")
    private Integer beanAge;
    @FormParam("formparam")
    private String beanForm;

    public Bean(String beanName, Integer beanAge, String beanForm) {
        this.beanName = beanName;
        this.beanAge = beanAge;
        this.beanForm = beanForm;
    }

    public Bean() {

    }

    public String getBeanName() {
        return beanName;
    }

    public Bean setBeanName(String beanName) {
        this.beanName = beanName;
        return this;
    }

    public Integer getBeanAge() {
        return beanAge;
    }

    public Bean setBeanAge(Integer beanAge) {
        this.beanAge = beanAge;
        return this;
    }

    public String getBeanForm() {
        return beanForm;
    }

    public Bean setBeanForm(String beanForm) {
        this.beanForm = beanForm;
        return this;
    }
}
