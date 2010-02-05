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
import com.google.inject.Inject;
import com.sun.jersey.api.json.JSONWithPadding;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
@Path("/")
@Produces({"application/json", "application/x-javascript"})
public class MBeanService {
    @Inject MBeanServerInstance mbeanServer;
    
    private MBeanServer getMBeanServer() throws WebApplicationException {
        MBeanServer server = mbeanServer.getMBeanServer();
        if (server == null) {
            throw new WebApplicationException(Response.serverError().entity("No mbean server").build());
        }
        return server;
    }

    private Response getResponse(Object o, String callback) {
        String media = MediaType.APPLICATION_JSON;
        if(callback != null) {
            media = "application/x-javascript";
            return Response.ok(new JSONWithPadding(o, callback), media).build();
         }
        // Use default media
        return Response.ok(new JSONWithPadding(o, callback)).build();
    }
    
    /*
    @GET
    @Path("/test")
    public GenericEntity<List<String>> getList() {
        return new GenericEntity<List<String>>(new ArrayList<String>(  Arrays.asList("foo", "bar"))){};
    }
    */
    
    @GET
    @Path("/domains")
    public Response getDomains( 
         @QueryParam("callback") String callback) throws JSONException {
        
        MBeanServer server = getMBeanServer();
        String[] domains = server.getDomains();
         List<String> l =  Arrays.asList(domains);
         JSONArray arr = new JSONArray(l);        
         return getResponse(arr, callback);
    }
    
    @GET
    @Path("/domains/{domain}")
    public Response getMBeanNameForDomain(@PathParam("domain") String domain,
        @QueryParam("callback") String callback) throws JSONException {
        MBeanServer server = getMBeanServer();
        try {
            Set<ObjectName> names =server.queryNames(new ObjectName(domain + ":*"), null);
            JSONObject dom = new JSONObject();
            dom.put("domain", domain);
            JSONArray arr = new JSONArray(names);
            dom.put("mbeans", arr);
            return getResponse(dom, callback);
        
        } catch(MalformedObjectNameException me) {
            throw new 
                WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE).
                                        entity("Wrong domain name " + domain).build());
        }
    }
    
    @GET
    @Path("/{objectName}")
    public Response getMBean(@PathParam("objectName") String objectName, 
                                    @QueryParam("callback") String callback) throws JSONException {
        MBeanServer server = getMBeanServer();
        try {
            ObjectName name = new ObjectName(objectName);
            MBeanInfo info = server.getMBeanInfo(name);
            MBeanAttributeInfo[] attrInfos =  info.getAttributes();
            String[] attributeNames = new String[attrInfos.length];
            for(int i = 0; i < attrInfos.length;i++) {
                attributeNames[i] =  attrInfos[i].getName();
            }
            AttributeList values = server.getAttributes(name, attributeNames);
        
            JSONObject attValues = new JSONObject();
            for(Attribute a: values.asList()) {
                attValues.put(a.getName(), getAttributeValueAsJson(a.getValue()));           
            }
            
            JSONObject mbean = new JSONObject();
            mbean.put("name", objectName);
            mbean.put("attributes", attValues);
           
            //return new JSONWithPadding(mbean, callback);
            return getResponse(mbean, callback);
        
        } catch(MalformedObjectNameException me) {
            throw new 
                WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE).
                                        entity("Wrong mbean name " + objectName).build());
        } catch(InstanceNotFoundException ne) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).
                                    entity("No mbean " + objectName).build());
        } catch(JMException je) {
            throw new WebApplicationException(je, 500);
        }
    }
    
    @GET
    @Path("/{objectName}/{attribute}")
    public Response getAttribute(@PathParam("objectName") String objectName, 
                                        @PathParam("attribute") String attribute,
                                        @QueryParam("callback") String callback) throws JSONException {
        MBeanServer server = getMBeanServer();
        try {
            ObjectName name = new ObjectName(objectName);
            JSONObject att = new JSONObject();
            att.put("name", objectName);
            att.put("attribute", attribute);
            att.put("value", getAttributeValueAsJson(server.getAttribute(name, attribute)));
            return getResponse(att, callback);
            
        } catch(MalformedObjectNameException me) {
            throw new 
                WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE).
                                        entity("Wrong mbean name " + objectName).build());
        } catch(InstanceNotFoundException ne) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).
                                              entity("No mbean " + objectName).build());
        } catch(AttributeNotFoundException ne) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).
                                              entity("No attribute " + attribute + " for " + objectName).build());
        }  catch(JMException je) {
            throw new WebApplicationException(je, 500);
        }
        
    }
    
    private Object getAttributeValueAsJson(Object a) {
        if(a == null)
                return null;
        if (a.getClass().isArray()) {
            return new JSONArray(Arrays.asList((Object[])a));
        }
        return a;
    }
}
