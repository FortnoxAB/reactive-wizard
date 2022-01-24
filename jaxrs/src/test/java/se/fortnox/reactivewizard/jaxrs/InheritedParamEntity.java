package se.fortnox.reactivewizard.jaxrs;

import javax.ws.rs.QueryParam;

public class InheritedParamEntity extends ParamEntity {

    @QueryParam("inherited")
    String inherited;

    public String getInherited() {
        return inherited;
    }

    public void setInherited(String inherited) {
        this.inherited = inherited;
    }
}
