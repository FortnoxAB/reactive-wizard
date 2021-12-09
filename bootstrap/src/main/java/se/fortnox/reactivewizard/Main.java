package se.fortnox.reactivewizard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Just an entry point for a SpringBoot application
 */
@SpringBootApplication
@ComponentScan({"se.fortnox", "com.fortnox"})
public class Main {

    /**
     * no comment
     *
     * @param args description that explains
     */
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
