package sn.sunuagri;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SunuAgriApplication {

    public static void main(String[] args) {
        SpringApplication.run(SunuAgriApplication.class, args);
    }
}
