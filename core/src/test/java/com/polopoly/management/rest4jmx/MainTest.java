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

import com.polopoly.management.rest4jmx.JAXBContextResolver;
import com.polopoly.management.rest4jmx.Main;
import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
/**
 *       
 */
public class MainTest {

    
    private static final String DEFAULT = "default";
    private static final String TESTDOMAIN = "testdomain";
    private static final String TESTDOMAIN_NAME_TEST_BEAN = TESTDOMAIN + ":name=testBean";

    private SelectorThread server;
    
    private WebResource r;

    private static MBeanServer s;

    public MainTest() {}

    
    @Before
    public void setUp() throws Exception {
        server = Main.startServer();

        ClientConfig cc = new DefaultClientConfig();
        cc.getClasses().add(JAXBContextResolver.class);
        Client c = Client.create(cc);
        r = c.resource(Main.BASE_URI);
    }

    @After
    public void tearDown() throws Exception {
        server.stopEndpoint();
    }

    /**
     * Test checks that the application.wadl is reachable.
     */
    public void testApplicationWadl() {
        String applicationWadl = r.path("application.wadl").get(String.class);
        assertTrue("Something wrong. Returned wadl length is not > 0",
                applicationWadl.length() > 0);
    }

    /*
    @Test
    public void getTest(){
        GenericType<List<String>> gt = new GenericType<List<String>>() {};
        List<String> l = r.path("test").accept("application/json").get(gt);
        System.out.println("DEBUG " + l);
    
    }
    */
    @Test
    public void testGetDomainsAsJSON() throws JSONException {
            JSONArray ja = r.path("domains").accept("application/json").get(JSONArray.class);
            assertEquals("No domain in " + ja, TESTDOMAIN, ja.get(0));
    }
    
    @Test 
    public void testGetDomainsAsJSONP() throws JSONException {
            String js = r.path("domains").accept("application/x-javascript").get(String.class);
            assertTrue("Not JSONP, does not start with callback " + js, js.startsWith("callback"));
    }
    
    @Test 
    public void testGetDomainsAsJSONPWithParam() throws JSONException {
        assertCallback("domains");
    }
    
    private void assertCallback(String path) throws JSONException {
            String js = r.path(path).queryParam("callback", "callback")
                         .accept("*/*").get(String.class);
            assertTrue("Not JSONP, does not start with callback " + js, js.startsWith("callback"));
    }
    
    @Test
    public void testGetMBeansForDomain() throws JSONException {
        JSONObject jo = r.path("domains/" + TESTDOMAIN).accept("application/json").get(JSONObject.class);
        assertEquals("Not correct domain name in " + jo, TESTDOMAIN, jo.get("domain"));
        assertEquals("Does not contain mbean name", TESTDOMAIN_NAME_TEST_BEAN, jo.getJSONArray("mbeans").get(0));
    }
    
    @Test 
    public void testGetMBeansForDomainAsJSONPWithParam() throws JSONException {
        assertCallback("domains/" + TESTDOMAIN);
    }
    
    @Test
    public void getMBean() throws JSONException {
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN).accept("application/json").get(JSONObject.class);
        assertEquals("Not correct mbean name", jo.get("name"), TESTDOMAIN_NAME_TEST_BEAN);
        assertEquals("Not correct attribute value " + jo, DEFAULT, jo.getJSONObject("attributes").get("MyAttr"));
    }
    
    @Test 
    public void testGetMBeanAsJSONPWithParam() throws JSONException {
        assertCallback(TESTDOMAIN_NAME_TEST_BEAN);
    }
    
    @Test
    public void getAttribute() throws JSONException {
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/MyAttr").accept("application/json").get(JSONObject.class);
        assertEquals("Not correct mbean name", jo.get("name"), TESTDOMAIN_NAME_TEST_BEAN);
        assertEquals("Not correct attribute" + jo, "MyAttr", jo.get("attribute"));
        assertEquals("Not correct attribute value " + jo, DEFAULT, jo.get("value"));
    }
    
    @Test
    public void getBooleanAttribute() throws JSONException {
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/So").accept("application/json").get(JSONObject.class);
        assertEquals("Not correct mbean name", jo.get("name"), TESTDOMAIN_NAME_TEST_BEAN);
        assertEquals("Not correct attribute" + jo, "So", jo.get("attribute"));
        assertEquals("Not correct attribute value " + jo, false, jo.get("value"));
    }
    
    @Test 
    public void testGetAttributeAsJSONPWithParam() throws JSONException {
        assertCallback(TESTDOMAIN_NAME_TEST_BEAN + "/MyAttr");
    }
    
    @Test
    public void putStringAttribute() throws Exception{
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/MyAttr").type("text/plain").
            put(JSONObject.class, "newValue");
        assertEquals("Not correct attribute value " + jo, "newValue", jo.get("value"));
    }
    
    @Test
    public void putBooleanAttribute() throws Exception{
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/So").type("text/plain").
            put(JSONObject.class, "true");
        assertEquals("Not correct attribute value " + jo, true, jo.get("value"));
    }
    
    @Test
    public void putIntAttribute() throws Exception{
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/MyIntAttr").type("text/plain").
            put(JSONObject.class, "10");
        assertEquals("Not correct attribute value " + jo, 10, jo.get("value"));
    }
    
    @BeforeClass
    public static void setUpMBeanServer() throws JMException {
        MBeanServerInstance mb = new MBeanServerInstance();
        s = mb.getMBeanServer();
        if (s == null) {
            s = MBeanServerFactory.createMBeanServer();
        }
        StandardMBean bean = new StandardMBean(new TestMBean(), MyAttr.class);
        s.registerMBean(bean, new ObjectName(TESTDOMAIN_NAME_TEST_BEAN));
        
    }
    
    public interface MyAttr {
        public String getMyAttr();
        public void setMyAttr(String s);
        
        public int getMyIntAttr();
        public void setMyIntAttr(int i);
        
        public boolean isSo();
        public void setSo(boolean b);
    }
    
    public static class TestMBean  implements MyAttr {
        private String myAttr = DEFAULT;
        private int myIntAttr = 0;
        private boolean so = false;

        public String getMyAttr() {
            return myAttr;
        }
        
        public void setMyAttr(String myAttr) {
            this.myAttr = myAttr;
        }
        
        public int getMyIntAttr() {
            return myIntAttr;
        }
        
        public void setMyIntAttr(int myIntAttr) {
            this.myIntAttr = myIntAttr;
        }
        public boolean isSo() {
            return so;
        }
        public void setSo(boolean b) {
            so = b;
        }
        
    }
    
}