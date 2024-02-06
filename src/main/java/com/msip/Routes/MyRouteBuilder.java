package com.msip.Routes;

import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.msip.Supporting.HelperFunctions;

@Component
@PropertySource("${envConfigFile}")
public class MyRouteBuilder extends RouteBuilder{

	@Autowired
	HelperFunctions helperFunctions;

	@Value("${routeKeystore}")
	private String routeKeystore;

	@Value("${routeKeystorePassword}")
	private String routeKeystorePass;

	@Value("${authenticationServiceUrl}")
	private String authenticationServiceUrl;

	@Value("${sampleAPIUrl}")
	private String sampleAPIUrl;

	@Value("${notificationAPIURL}")
	private String notificationAPIURL;
	
	@Value("${server.port}")
	private int port;

	String appJson = "application/json; charset=UTF-8";

	private final static Logger logger = LoggerFactory.getLogger(MyRouteBuilder.class);

	@Override
    public void configure() {

		// Import certificate to allow HTTPS connection to Honda URLs
		helperFunctions.configureLDAPSSLConnection(this.getContext());

		// Define Camel servlet and REST endpoints
		restConfiguration()
			.component("servlet")
			.port(port)
			.bindingMode(RestBindingMode.json)
            .dataFormatProperty("prettyPrint", "true");
		rest()
			.get("/exampleAPI-HealthCheck")
			.id("HealthCheck")
			.description("An HealthCheck API")
			.produces(appJson)
			.to("direct:HealthCheck");
		rest()
			.get("/exampleAPI-Authenticate")
			.id("Authenticate")
			.description("A secured API")
			.to("direct:Authenticate");
		rest()
			.get("/exampleAPI-ForwardRequest")
			.id("ForwardRequest")
			.description("Forwarding request to another MSIP service")
			.to("direct:ForwardRequest");
		rest()
			.post("/exampleAPI-TestNotification")
			.id("TestNotification")
			.description("Test Notification MSIP Service")
			.to("direct:TestNotification");

		from("direct:HealthCheck")
		.log(LoggingLevel.INFO, logger, "Calling HealthCheck API")
		.process(exchange -> {  
            exchange.getMessage().setBody("Service is running");  
        });

		from("direct:Authenticate")
		.log(LoggingLevel.INFO, logger, "Calling authenticated API")
		.process(helperFunctions::authCheckLDAP)
		.process(exchange -> {  
            exchange.getMessage().setBody("Authentication Successful");  
        });

		from("direct:TestNotification")
		.log(LoggingLevel.INFO, logger, "Calling Notification API")
		.process(exchange -> {  
				String email = exchange.getIn().getHeader("email", String.class);
				email = email.replace("\"", "");
				Map<String, Object> body = new HashMap<>();
				body.put("to", email);
				body.put("message", "Testing MSIP Email Notifications");
				exchange.getIn().setBody(body);
			})
		.marshal().json(JsonLibrary.Jackson) // Convert the Map to JSON
		.setHeader("Content-Type", constant("application/json"))
		.setHeader(Exchange.HTTP_METHOD, constant("POST"))
		.to(notificationAPIURL + "?bridgeEndpoint=true&throwExceptionOnFailure=false")
		.unmarshal().json(JsonLibrary.Jackson, String.class);

		from("direct:ForwardRequest")
		.log(LoggingLevel.INFO, logger, "Forwarding request to another MSIP service")
		.to(sampleAPIUrl + "?bridgeEndpoint=true&throwExceptionOnFailure=false");
    }
}
