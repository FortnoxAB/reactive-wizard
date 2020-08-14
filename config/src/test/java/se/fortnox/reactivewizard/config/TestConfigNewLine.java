package se.fortnox.reactivewizard.config;

import java.util.HashMap;
import java.util.List;

@Config("myTestConfigNewLine")
public class TestConfigNewLine {
    private HashMap<String, List<String>> clients;

    public HashMap<String, List<String>> getClients() {
        return clients;
    }

    public TestConfigNewLine setClients(HashMap<String, List<String>> clients) {
        this.clients = clients;
        return this;
    }
}
