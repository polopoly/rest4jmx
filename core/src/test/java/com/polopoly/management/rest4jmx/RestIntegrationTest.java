package com.polopoly.management.rest4jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;


public class RestIntegrationTest {
    static final String DEFAULT = "default";
    static final String TESTDOMAIN = "testdomain";
    static final String TESTDOMAIN_NAME_TEST_BEAN = TESTDOMAIN + ":name=testBean";
    
    private WebResource r;
    
    @Before
    public void setUp() throws Exception {
        ClientConfig cc = new DefaultClientConfig();
        cc.getClasses().add(JAXBContextResolver.class);
        Client c = Client.create(cc);
        r = c.resource(MainTest.BASE_URI);
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/MyAttr").type("text/plain").
        put(JSONObject.class, DEFAULT);
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
            assertEquals("No domain in " + ja, TESTDOMAIN, ja.get(0));
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
        JSONObject jo = r.path("domains/" + TESTDOMAIN).accept("application/json").get(JSONObject.class);
        assertEquals("Not correct domain name in " + jo, TESTDOMAIN, jo.get("domain"));
        assertEquals("Does not contain mbean name", TESTDOMAIN_NAME_TEST_BEAN, jo.getJSONArray("mbeans").get(0));
    }

    @Test
    public void testGetMBeansForDomainAsJSONPWithParam()
        throws JSONException
    {
        assertCallback("domains/" + TESTDOMAIN);
    }

    
    
    @Test
    public void getMBean() throws JSONException
    {
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN).accept("application/json").get(JSONObject.class);
        assertEquals("Not correct mbean name", jo.get("name"), TESTDOMAIN_NAME_TEST_BEAN);
        assertEquals("Not correct attribute value " + jo, DEFAULT, jo.getJSONObject("attributes").getJSONObject("MyAttr").get("value"));
        boolean isWritable = ((Boolean)jo.getJSONObject("attributes").getJSONObject("MyAttr").get("writable")).booleanValue();
        assertTrue( "Attrubute should be writable " + jo, isWritable);
        assertEquals("Should contain simpleMethod " + jo, "simpleMethod", jo.getJSONArray("operations").getJSONObject(0).get("name"));
    
        
    }

    @Test
    public void testGetMBeanAsJSONPWithParam() throws JSONException
    {
        assertCallback(TESTDOMAIN_NAME_TEST_BEAN);
    }

    @Test
    public void getAttribute() throws JSONException
    {
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/MyAttr").accept("application/json").get(JSONObject.class);
        assertEquals("Not correct mbean name", jo.get("name"), TESTDOMAIN_NAME_TEST_BEAN);
        assertEquals("Not correct attribute" + jo, "MyAttr", jo.get("attribute"));
        assertEquals("Not correct attribute value " + jo, DEFAULT, jo.get("value"));
    }

    @Test
    public void getBooleanAttribute() throws JSONException
    {
        r.path(TESTDOMAIN_NAME_TEST_BEAN + "/So").type("text/plain").
        put(JSONObject.class, "false");
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/So").accept("application/json").get(JSONObject.class);
        assertEquals("Not correct mbean name", jo.get("name"), TESTDOMAIN_NAME_TEST_BEAN);
        assertEquals("Not correct attribute" + jo, "So", jo.get("attribute"));
        assertEquals("Not correct attribute value " + jo, false, jo.get("value"));
    }

    @Test
    public void testGetAttributeAsJSONPWithParam() throws JSONException
    {
        assertCallback(TESTDOMAIN_NAME_TEST_BEAN + "/MyAttr");
    }

    @Test
    public void putStringAttribute() throws Exception
    {
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/MyAttr").type("text/plain").
            put(JSONObject.class, "newValue");
        assertEquals("Not correct attribute value " + jo, "newValue", jo.get("value"));
    }

    @Test
    public void putBooleanAttribute() throws Exception
    {
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/So").type("text/plain").
            put(JSONObject.class, "true");
        assertEquals("Not correct attribute value " + jo, true, jo.get("value"));
    }

    @Test
    public void putIntAttribute() throws Exception
    {
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/MyIntAttr").type("text/plain").
            put(JSONObject.class, "10");
        assertEquals("Not correct attribute value " + jo, 10, jo.get("value"));
    }

    @Test
    public void invokeSimpleMethod() throws Exception
    {
        r.path(TESTDOMAIN_NAME_TEST_BEAN + "/ops/simpleMethod").type("application/json").post();
    }
    
    @Test
    public void invokeMethodWithParams() throws Exception
    {
        JSONObject o = new JSONObject();
        JSONArray params = new JSONArray(Arrays.asList(1,2));
        o.put("params", params);
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/ops/methodWithParams").type("application/json").post(JSONObject.class, o);
        assertEquals("No correct return value " + jo, 0, jo.getInt("return"));
    }
    
    @Test
    public void invokeMethodWithIntParamsWithString() throws Exception
    {
        JSONObject o = new JSONObject();
        JSONArray params = new JSONArray(Arrays.asList("1","2"));
        o.put("params", params);
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/ops/methodWithParams").type("application/json").post(JSONObject.class, o);
        assertEquals("No correct return value " + jo, 0, jo.getInt("return"));
    }
    
    @Test
    public void invokeMethodReturnsAList() throws Exception
    {
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/ops/methodReturnList").type("application/json").post(JSONObject.class);
        assertEquals("No correct return value " + jo, "foo", jo.getJSONArray("return").getString(0));
    }
    
    
    
    @Test
    public void invokeMethodReturnMap() throws Exception
    {
        JSONObject o = new JSONObject();
        JSONArray params = new JSONArray(Arrays.asList("foo"));
        o.put("params", params);
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/ops/methodReturnMap").type("application/json").post(JSONObject.class, o);
        assertEquals("No correct return value " + jo, "value", jo.getJSONObject("return").getString("key"));
    }
    
    @Test
    public void invokeNonSerializableMethod() throws Exception
    {
        // XXX Hm, don't know yet how to handle this
        JSONObject jo = r.path(TESTDOMAIN_NAME_TEST_BEAN + "/ops/nonSerializableMethod").type("application/json").post(JSONObject.class);
    }

    public interface MyMBean {
        public String getMyAttr();
        public void setMyAttr(String s);
        
        public int getMyIntAttr();
        public void setMyIntAttr(int i);
        
        public boolean isSo();
        public void setSo(boolean b);
        
        public List getMyListAttr();
        public void setMyListAttr(List attr);
        
        public String[] getMyArrayAttr();
        public void setMyArrayAttr(String[] a);
        
        public void simpleMethod();
        
        public int methodWithParams(int i, int j);
        
        public List<String> methodReturnList();
        
        public Map<String, String> methodReturnMap(String key);
        
        public Map<String, CustomType> nonSerializableMethod();
    }

    public static class TestMBean  implements MyMBean {
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
    
        public String[] getMyArrayAttr()
        {
            // TODO Auto-generated method stub
            return null;
        }
    
        public List getMyListAttr()
        {
            // TODO Auto-generated method stub
            return null;
        }
    
        public Map<String, String> methodReturnMap(String key)
        {
            Map m = new HashMap();
            m.put("key", "value");
            return m;
        }
    
        public List<String> methodReturnList()
        {
            return Arrays.asList("foo", "bar");
        }
    
        public int methodWithParams(int i, int j)
        {
            // TODO Auto-generated method stub
            return 0;
        }
    
        public Map<String, CustomType> nonSerializableMethod()
        {
            Map m = new HashMap();
            m.put("key", new CustomType());
            return m;
        }
    
        public void setMyArrayAttr(String[] a)
        {
            // TODO Auto-generated method stub
            
        }
    
        public void setMyListAttr(List attr)
        {
            // TODO Auto-generated method stub
            
        }
    
        public void simpleMethod()
        {
            // TODO Auto-generated method stub
            
        }
        
    }
    static public class CustomType {

    }
}
