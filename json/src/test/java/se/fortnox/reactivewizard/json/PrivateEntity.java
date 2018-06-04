package se.fortnox.reactivewizard.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

class PrivateEntity {
    private String protectedProp;
    private String publicPropInPrivateClass;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String fieldProp;

    @JsonProperty()
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String getProtectedProp() {
        return protectedProp;
    }

    public void setProtectedProp(String myprop) {
        this.protectedProp = myprop;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getPublicPropInPrivateClass() {
        return publicPropInPrivateClass;
    }

    public void setPublicPropInPrivateClass(String publicPropInPrivateClass) {
        this.publicPropInPrivateClass = publicPropInPrivateClass;
    }
}
