/*
 * Copyright 2010 Polopoly AB (publ).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.polopoly.management.rest4jmx;
import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *       
 */
public class MainTest extends RestIntegrationTest {

    private SelectorThread server;
    
    private static MBeanServer s;

    public MainTest() {}

    
    @Before
    public void setUp() throws Exception {
        server = MainTest.startServer();
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        server.stopEndpoint();
    }

    @BeforeClass
    public static void setUpMBeanServer() throws JMException {
        MBeanServerInstance mb = new MBeanServerInstance();
        s = mb.getMBeanServer();
        if (s == null) {
            s = MBeanServerFactory.createMBeanServer();
        }
        StandardMBean bean = new StandardMBean(new TestMBean(), MyMBean.class);
        s.registerMBean(bean, new ObjectName(TESTDOMAIN_NAME_TEST_BEAN));
        
    }
    private static int getPort(int defaultPort) {
        String port = System.getenv("JERSEY_HTTP_PORT");
        if (null != port) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
            }
        }
        return defaultPort;        
    } 
    
    private static URI getBaseURI() {
        return UriBuilder.fromUri("http://localhost/rest4jmx/").port(getPort(9998)).build();
    }

    public static final URI BASE_URI = getBaseURI();
   
    public static SelectorThread startServer() throws IOException{
        final Map<String, String> initParams = new HashMap<String, String>();

        initParams.put("com.sun.jersey.config.property.packages",
                "com.polopoly.management.rest4jmx");

        SelectorThread threadSelector = GrizzlyWebContainerFactory.create(BASE_URI, initParams);
        return threadSelector;
    }
    
    public static void main(String[] args) throws Exception {
        System.out.println("Starting grizzly...");
        SelectorThread threadSelector = startServer();
        setUpMBeanServer();
        System.out.println(String.format(
                "Jersey app started with WADL available at %sapplication.wadl\n" +
                "Hit enter to stop it...", BASE_URI));
        System.in.read();
        threadSelector.stopEndpoint();
    }
}