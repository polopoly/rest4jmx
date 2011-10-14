package com.polopoly.management.rest4jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.Arrays;

import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.apache.ApacheHttpClient;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.junit.Before;


public class RestIntegrationTest {
    protected WebResource r;
    
    protected static int getPort(int defaultPort)
    {    
        String port = System.getenv("JERSEY_HTTP_PORT");
        if (null != port) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
            }
        }
        return defaultPort;        
    }

    protected static URI getBaseURI()
    {
        return UriBuilder.fromUri("http://localhost/rest4jmx/").port(getPort(9998)).build();
    }
    
    protected URI getTestURI() {
        return getBaseURI();
    }
    
    protected ClientConfig getConfig() {
     return  new DefaultClientConfig();
    }
    
    @Before
    public void setUp() throws Exception {
        ClientConfig cc = getConfig();
        cc.getClasses().add(JAXBContextResolver.class);
        // Only way to get auth working
        Client c = ApacheHttpClient.create(cc);
        r = c.resource(getTestURI());
        JSONObject jo = r.path(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN + "/MyAttr").type("text/plain").
        put(JSONObject.class, MBeanServerSetup.DEFAULT);
    }
    /**
     * Test checks that the application.wadl is reachable.
     */
    public void testApplicationWadl()
    {
        String applicationWadl = r.path("application.wadl").get(String.class);
        assertTrue("Something wrong. Returned wadl length is not > 0",
                applicationWadl.length() > 0);
    }

    @Test
    public void testGetDomainsAsJSON() throws JSONException
    {
        JSONArray ja = r.path("domains").accept("application/json").get(JSONArray.class);
        assertEquals("No domain in " + ja, MBeanServerSetup.TESTDOMAIN, ja.get(0));
    }

    @Test
    public void testGetDomainsAsJSONP() throws JSONException
    {
            String js = r.path("domains").accept("application/x-javascript").get(String.class);
            assertTrue("Not JSONP, does not start with callback " + js, js.startsWith("callback"));
    }

    @Test
    public void testGetDomainsAsJSONPWithParam() throws JSONException
    {
        assertCallback("domains");
    }

    private void assertCallback(String path) throws JSONException
    {
            String js = r.path(path).queryParam("callback", "callback")
                         .accept("*/*").get(String.class);
            assertTrue("Not JSONP, does not start with callback " + js, js.startsWith("callback"));
    }

    @Test
    public void testGetMBeansForDomain() throws JSONException
    {
        JSONObject jo = r.path("domains/" + MBeanServerSetup.TESTDOMAIN).accept("application/json").get(JSONObject.class);
        assertEquals("Not correct domain name in " + jo, MBeanServerSetup.TESTDOMAIN, jo.get("domain"));
        assertEquals("Does not contain mbean name", MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN, jo.getJSONArray("mbeans").get(0));
    }

    @Test
    public void testGetMBeansForDomainAsJSONPWithParam()
        throws JSONException
    {
        assertCallback("domains/" + MBeanServerSetup.TESTDOMAIN);
    }

    
    
    @Test
    public void getMBean() throws JSONException
    {
        JSONObject jo = r.path(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN).accept("application/json").get(JSONObject.class);
        assertEquals("Not correct mbean name", jo.get("name"), MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN);
        assertEquals("Not correct attribute value " + jo, MBeanServerSetup.DEFAULT, jo.getJSONObject("attributes").getJSONObject("MyAttr").get("value"));
        boolean isWritable = ((Boolean)jo.getJSONObject("attributes").getJSONObject("MyAttr").get("writable")).booleanValue();
        assertTrue( "Attrubute should be writable " + jo, isWritable);
        JSONArray operations = jo.getJSONArray("operations");
        for (int i = 0; i < operations.length(); i++) {
            JSONObject op = operations.getJSONObject(i);
            if(op.getString("name").equals("simpleMethod")) {
                return;
            }
        }
        fail("Should contain simpleMethod " + jo);
    
        
    }

    @Test
    public void testGetMBeanAsJSONPWithParam() throws JSONException
    {
        assertCallback(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN);
    }

    @Test
    public void getAttribute() throws JSONException
    {
        JSONObject jo = r.path(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN + "/MyAttr").accept("application/json").get(JSONObject.class);
        assertEquals("Not correct mbean name", jo.get("name"), MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN);
        assertEquals("Not correct attribute" + jo, "MyAttr", jo.get("attribute"));
        assertEquals("Not correct attribute value " + jo, MBeanServerSetup.DEFAULT, jo.get("value"));
    }

    @Test
    public void getBooleanAttribute() throws JSONException
    {
        r.path(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN + "/So").type("text/plain").
        put(JSONObject.class, "false");
        JSONObject jo = r.path(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN + "/So").accept("application/json").get(JSONObject.class);
        assertEquals("Not correct mbean name", jo.get("name"), MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN);
        assertEquals("Not correct attribute" + jo, "So", jo.get("attribute"));
        assertEquals("Not correct attribute value " + jo, false, jo.get("value"));
    }

    @Test
    public void testGetAttributeAsJSONPWithParam() throws JSONException
    {
        assertCallback(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN + "/MyAttr");
    }

    @Test
    public void putStringAttribute() throws Exception
    {
        JSONObject jo = r.path(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN + "/MyAttr").type("text/plain").
            put(JSONObject.class, "newValue");
        assertEquals("Not correct attribute value " + jo, "newValue", jo.get("value"));
    }

    @Test
    public void putBooleanAttribute() throws Exception
    {
        JSONObject jo = r.path(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN + "/So").type("text/plain").
            put(JSONObject.class, "true");
        assertEquals("Not correct attribute value " + jo, true, jo.get("value"));
    }

    @Test
    public void putIntAttribute() throws Exception
    {
        JSONObject jo = r.path(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN + "/MyIntAttr").type("text/plain").
            put(JSONObject.class, "10");
        assertEquals("Not correct attribute value " + jo, 10, jo.get("value"));
    }

    @Test
    public void invokeSimpleMethod() throws Exception
    {
        r.path(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN + "/ops/simpleMethod").type("application/json").post();
    }
    
    @Test
    public void invokeMethodWithParams() throws Exception
    {
        JSONObject o = new JSONObject();
        JSONArray params = new JSONArray(Arrays.asList(1,2));
        o.put("params", params);
        JSONObject jo = r.path(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN + "/ops/methodWithParams").type("application/json").post(JSONObject.class, o);
        assertEquals("No correct return value " + jo, 0, jo.getInt("return"));
    }
    
    @Test
    public void invokeMethodWithIntParamsWithString() throws Exception
    {
        JSONObject o = new JSONObject();
        JSONArray params = new JSONArray(Arrays.asList("1","2"));
        o.put("params", params);
        JSONObject jo = r.path(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN + "/ops/methodWithParams").type("application/json").post(JSONObject.class, o);
        assertEquals("No correct return value " + jo, 0, jo.getInt("return"));
    }
    
    @Test
    public void invokeMethodReturnsAList() throws Exception
    {
        JSONObject jo = r.path(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN + "/ops/methodReturnList").type("application/json").post(JSONObject.class);
        assertEquals("No correct return value " + jo, "foo", jo.getJSONArray("return").getString(0));
    }
    
    
    
    @Test
    public void invokeMethodReturnMap() throws Exception
    {
        JSONObject o = new JSONObject();
        JSONArray params = new JSONArray(Arrays.asList("foo"));
        o.put("params", params);
        JSONObject jo = r.path(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN + "/ops/methodReturnMap").type("application/json").post(JSONObject.class, o);
        assertEquals("No correct return value " + jo, "value", jo.getJSONObject("return").getString("key"));
    }
    
    @Test
    public void invokeNonSerializableMethod() throws Exception
    {
        // XXX Hm, don't know yet how to handle this
        JSONObject jo = r.path(MBeanServerSetup.TESTDOMAIN_NAME_TEST_BEAN + "/ops/nonSerializableMethod").type("application/json").post(JSONObject.class);
    }

    
}
