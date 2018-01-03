package se.fortnox.reactivewizard.helloworld;

import com.fasterxml.jackson.annotation.JsonProperty;
import se.fortnox.reactivewizard.config.Config;

@Config("server")
public class HelloWorldConfig {
    @JsonProperty("port")
    private int port = 8080;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
