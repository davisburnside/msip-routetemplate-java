package com.msip.Supporting;


import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class HelperFunctions {
    
	@Value("${routeKeystore}")
	private String routeKeystore;

	@Value("${routeKeystorePassword}")
	private String routeKeystorePass;

	@Value("${authenticationServiceUrl}")
	private String authenticationServiceUrl;

	private final static Logger logger = LoggerFactory.getLogger(HelperFunctions.class);
    
    public void configureLDAPSSLConnection(CamelContext context) {

        logger.info("Importing certificate " + routeKeystore);
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource(routeKeystore);
        ksp.setPassword(routeKeystorePass);

        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyStore(ksp);
        kmp.setKeyPassword(routeKeystorePass);

        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        trustManagersParameters.setKeyStore(ksp);

        SSLContextParameters scp = new SSLContextParameters();
        scp.setKeyManagers(kmp);
        scp.setTrustManagers(trustManagersParameters);

        HttpComponent httpComponent = context.getComponent("https", HttpComponent.class);
        httpComponent.setSslContextParameters(scp);
    }

    public void authCheckLDAP(Exchange exchange) throws IOException {

        // Send a request to the external URL
        ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate();
        Exchange response = producerTemplate.send(authenticationServiceUrl + "?bridgeEndpoint=true&throwExceptionOnFailure=false", exchangeCopy -> {
            exchangeCopy.getIn().setHeaders(exchange.getIn().getHeaders());
            exchangeCopy.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
        });


        // InputStream inputStream = response.getMessage().getBody(InputStream.class);
        // String result = IOUtils.toString(inputStream, "UTF-8");
        // System.out.println(result);

        Number responseCodeNumber = response.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Number.class);
        int responseCode = responseCodeNumber != null ? responseCodeNumber.intValue() : 500; // Default to 500 if response code is null
        if (responseCode == 200) {
            logger.info("Authenticated user");
            
        } else if (responseCode == 401) {
            logger.info("Invalid credentials");
            exchange.getMessage().setBody("Invalid credentials");
            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 401);
            exchange.setRouteStop(true);
        } else {
            logger.info("Unexpected error when authenticating request inside MSIP Route");
            exchange.getMessage().setBody("Unexpected error when authenticating request inside MSIP Route");
            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
            exchange.setRouteStop(true);
        }
    }

}
