package br.com.nord_tool_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication(exclude = HibernateJpaAutoConfiguration.class)
public class NordToolBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(NordToolBackendApplication.class,args);
    }
}