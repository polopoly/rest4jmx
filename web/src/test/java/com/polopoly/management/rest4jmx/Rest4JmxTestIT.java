package com.polopoly.management.rest4jmx;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.junit.Before;

import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;

public class Rest4JmxTestIT extends  RestIntegrationTest {
    
    @Override
    protected ClientConfig getConfig() {
        DefaultApacheHttpClientConfig config = new DefaultApacheHttpClientConfig();
        config.getState().setCredentials("JMX Realm", "localhost", getPort(8080), "admin", "admin");
        return config;
    }
    
    protected URI getTestURI() {
        return UriBuilder.fromUri("http://localhost/rest4jmx-web/mbeans/").port(getPort(8080)).build();
    }
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }
}
