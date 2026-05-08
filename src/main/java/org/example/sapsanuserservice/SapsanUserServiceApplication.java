package org.example.sapsanuserservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableMethodSecurity
@SpringBootApplication
public class SapsanUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SapsanUserServiceApplication.class, args);
    }

}
