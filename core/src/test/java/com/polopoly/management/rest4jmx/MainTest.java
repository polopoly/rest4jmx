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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.MBeanServer;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *       
 */
public class MainTest extends RestIntegrationTest {
    static final Logger LOG = Logger.getLogger(MainTest.class.getName());

    private SelectorThread server;
    
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
        MBeanServerSetup.setupMBeanServer();
      
    }


    public static SelectorThread startServer() throws IOException{
        final Map<String, String> initParams = new HashMap<String, String>();

        initParams.put("com.sun.jersey.config.property.packages",
                "com.polopoly.management.rest4jmx");

        SelectorThread threadSelector = GrizzlyWebContainerFactory.create(getBaseURI(), initParams);
        return threadSelector;
    }
    
    public static void main(String[] args) throws Exception {
        System.out.println("Starting grizzly...");
        SelectorThread threadSelector = startServer();
        setUpMBeanServer();
        System.out.println(String.format(
                "Jersey app started with WADL available at %sapplication.wadl\n" +
                "Hit enter to stop it...", getBaseURI()));
        System.in.read();
        threadSelector.stopEndpoint();
    }
}