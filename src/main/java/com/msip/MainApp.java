package com.msip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import javax.servlet.Servlet;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;

@SpringBootApplication
public class MainApp {

    public static void main(String[] argv) {
        SpringApplication.run(MainApp.class, argv);
    }

    @Bean
    public ServletRegistrationBean<Servlet> servletRegistrationBean() {
        System.out.println("Executing ServletRegistrationBean");
        ServletRegistrationBean<Servlet> registration = new ServletRegistrationBean<>(new CamelHttpTransportServlet());
        registration.setName("CamelServlet");
        return registration;
    }

}
