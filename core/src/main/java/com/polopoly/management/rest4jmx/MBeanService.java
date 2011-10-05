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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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

import com.google.inject.Inject;
import com.sun.jersey.api.json.JSONWithPadding;

@Path("/")
@Produces({"application/json", "application/x-javascript"})
public class MBeanService
{
    private static final String OPERATION_RESPONSE_STATUS_PARAMETER_NAME = "status";
    private static final String OPERATION_PARAMETERS_PARAMETER_NAME = "params";

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
                String n = a.getName();
                MBeanAttributeInfo ai = getAttributeInfo(name, n);
                JSONObject att = new JSONObject();
                att.put("name", n);
                att.put("value", getAttributeValueAsJson(a.getValue()));
                att.put("writable", ai.isWritable());
                att.put("isBoolean", ai.isIs());

                attValues.put(n, att);
            }

            MBeanOperationInfo[] operations = info.getOperations();
            List<String> ops = new ArrayList<String>();

            for (MBeanOperationInfo operation : operations) {
                if (operationHasNoCollectionTypesInSignature(operation)) {
                    String[] signatureTypes = getSignatureForOperation(operation);

                    StringBuilder sb = new StringBuilder();
                    sb.append(operation.getName() + "(");

                    for (String type : signatureTypes) {
                        sb.append(type).append(",");
                    }

                    if (signatureTypes.length > 0) {
                        sb.deleteCharAt(sb.length() - 1);
                    }

                    sb.append(")");

                    ops.add(sb.toString());
                }
            }

            JSONObject mbean = new JSONObject();
            mbean.put("name", objectName);
            mbean.put("attributes", attValues);
            mbean.put("operations", ops);

            //return new JSONWithPadding(mbean, callback);
            return getResponse(mbean, callback);

        } catch(MalformedObjectNameException me) {
            throw new
                WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE).
                                        entity("Wrong mbean name " + objectName).build());
        } catch(InstanceNotFoundException ne) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).
                                    entity("No mbean " + objectName).build());
        } catch(Exception je) {
            throw new WebApplicationException(je, 500);
        }
    }

    @GET
    @Path("/{objectName}/{attribute}")
    public Response getAttribute(@PathParam("objectName") String objectName,
                                        @PathParam("attribute") String attribute,
                                        @QueryParam("callback") String callback) throws JSONException {
        try {
            ObjectName name = new ObjectName(objectName);
            return getResponse(getAttributeAsJSON(name, attribute), callback);

        } catch(MalformedObjectNameException me) {
            throw new
                WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE).
                                        entity("Wrong mbean name " + objectName).build());
        }
    }

    @PUT
    @Path("/{objectName}/{attribute}")
    @Consumes({"text/plain"})
    public Response setAttribute(@PathParam("objectName") String objectName,
                                 @PathParam("attribute") String attribute,
                                 @QueryParam("callback") String callback,
                                 String value) throws JSONException {
        System.err.println("DEBUG "+objectName + ":" + attribute +"="+value);
        MBeanServer server = getMBeanServer();
        try {
            value = value.trim();
            ObjectName name = new ObjectName(objectName);
            Attribute att = new Attribute(attribute, fitToAttributeType(name, attribute, value));
            server.setAttribute(name, att);
            return getResponse(getAttributeAsJSON(name, attribute), callback);
        } catch(MalformedObjectNameException me) {
            throw new
                WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE).
                                        entity("Wrong mbean name " + objectName).build());
        } catch (InstanceNotFoundException e) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).
                                              entity("No mbean " + objectName).build());
        } catch(AttributeNotFoundException ne) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).
                                              entity("No attribute " + attribute + " for " + objectName
                                               + ": " + ne).build());
        } catch (InvalidAttributeValueException e) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE).
                                              entity("Invalid type " + value + ": " + e).build());
        } catch (MBeanException e) {
            throw new WebApplicationException(e, 500);
        } catch (ReflectionException e) {
            throw new WebApplicationException(e, 500);
        } catch (IntrospectionException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    private boolean operationHasNoCollectionTypesInSignature(final MBeanOperationInfo operation)
        throws ClassNotFoundException
    {
        for (MBeanParameterInfo pInfo : operation.getSignature()) {
            Class clazz = getClassForType(pInfo.getType());

            if (clazz == null || clazz.isArray() || clazz.isAssignableFrom(Map.class)) {
                return false;
            }
        }

        return true;
    }

    @POST
    @Path("/{objectName}/ops/{operationName}")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response invokeOperationPost(final @PathParam("objectName") String objectName,
                                        final @PathParam("operationName") String operationName,
                                        final String requestBody)
    {
        try {
            ObjectName name = new ObjectName(objectName);
            JSONArray paramsArray = new JSONArray();

            if (requestBody != null && requestBody.length() > 0) {
                JSONObject paramObject = new JSONObject(requestBody);
                paramsArray = (JSONArray) paramObject.getJSONArray(OPERATION_PARAMETERS_PARAMETER_NAME);
            }

            MBeanServer server = getMBeanServer();
            MBeanInfo info = server.getMBeanInfo(name);

            if (operationExists(info, operationName)) {
                doInvoke(server, info, name, operationName, paramsArray);
                return createOkResponse();
            }
        } catch(MalformedObjectNameException me) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE).entity("Wrong mbean name " + objectName).build());
        } catch(InstanceNotFoundException ne) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("No mbean " + objectName).build());
        } catch(Exception e) {
            throw new WebApplicationException(e, 500);
        }

        return createOperationNotFoundResponse();
    }

    private void doInvoke(final MBeanServer mbeanServer,
                          final MBeanInfo mbeanInfo,
                          final ObjectName objectName,
                          final String operationName,
                          final JSONArray paramsArray)
        throws InstanceNotFoundException,
               ReflectionException,
               MBeanException,
               JSONException,
               ClassNotFoundException
    {
        MBeanOperationInfo[] operations = mbeanInfo.getOperations();

        for (MBeanOperationInfo operation : operations) {
            if (operation.getName().equals(operationName) &&
                operationHasNoCollectionTypesInSignature(operation) &&
                signatureMatchParameters(operation, paramsArray)) {

                List<Object> arguments = new ArrayList<Object>();

                if (paramsArray != null) {
                    for (int i = 0; i < paramsArray.length(); i++) {
                        arguments.add(paramsArray.get(i));
                    }
                }

                mbeanServer.invoke(objectName, operationName, arguments.toArray(new Object[0]), getSignatureForOperation(operation));
            }
        }
    }

    private String[] getSignatureForOperation(final MBeanOperationInfo operationInfo)
    {
        List<String> arguments = new ArrayList<String>();

        for (MBeanParameterInfo pInfo : operationInfo.getSignature()) {
            arguments.add(pInfo.getType());
        }

        return arguments.toArray(new String[0]);
    }

    private boolean signatureMatchParameters(final MBeanOperationInfo operationInfo,
                                             final JSONArray paramsArray) throws JSONException
    {
        int paramsArrayLength = (paramsArray != null) ? paramsArray.length() : 0;
        int operationInfoLength = operationInfo.getSignature().length;

        if (paramsArrayLength != operationInfoLength) {
            return false;
        }

        if (paramsArray != null && paramsArray.length() > 0) {
            for (int i = 0; i < paramsArray.length(); i++) {
                if (!operationInfo.getSignature()[i].getType().equals(paramsArray.get(i).getClass().getCanonicalName())) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean operationExists(final MBeanInfo mbeanInfo,
                                    final String operationName)
    {
        MBeanOperationInfo[] operations = mbeanInfo.getOperations();
        for (MBeanOperationInfo operation : operations) {
            if (operation.getName().equals(operationName)) {
                return true;
            }
        }

        return false;
    }

    private Response getInvokeResponse(String callback)
        throws JSONException
    {
        String media = MediaType.APPLICATION_JSON;

        JSONObject json = new JSONObject();
        json.put(OPERATION_RESPONSE_STATUS_PARAMETER_NAME, "OK");

        if (callback != null) {
            media = "application/x-javascript";
            return Response.ok(new JSONWithPadding(json, callback), media).build();
         }

        // Use default media
        return Response.ok(new JSONWithPadding(json, callback)).build();
    }

    private JSONObject getAttributeAsJSON(ObjectName mbean, String attribute) throws JSONException {
        try {
            JSONObject att = new JSONObject();
            att.put("name", mbean.toString());
            att.put("attribute", attribute);
            att.put("value", getAttributeValueAsJson(getMBeanServer().getAttribute(mbean, attribute)));
            return att;
        } catch(InstanceNotFoundException ne) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).
                                              entity("No mbean " + mbean).build());
        } catch(AttributeNotFoundException ne) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).
                                              entity("No attribute " + attribute + " for " + mbean).build());
        }  catch(JMException je) {
            throw new WebApplicationException(je, 500);
        }
    }

    private Object getAttributeValueAsJson(Object a) {
        if(a == null)
                return null;
        if (a.getClass().isArray()) {
           int l = Array.getLength(a);
           List<Object> list = new ArrayList<Object>();
           for (int i = 0; i < l; i++) {
               list.add(Array.get(a, i));
           }
            return new JSONArray(list);
        }
        return a;
    }

    private Object fitToAttributeType(ObjectName name, String attributeName, String value)
    throws InvalidAttributeValueException, AttributeNotFoundException, IntrospectionException, InstanceNotFoundException, ReflectionException, WebApplicationException {
        String type = getAttributeType(name, attributeName);
        try {

            // Most easy first, skip the rest
            if (type.equals("java.lang.String")) {
                return value;
            }

            Class typeClass = getClassForType(type);
            Object ret = marshallWithValueOf(typeClass, value);
            if (ret != null) {
                return ret;
            }

            throw new InvalidAttributeValueException("Could not marshall " + value + " to type " + type);
        } catch(ClassNotFoundException e) {
            throw new ReflectionException(e);
        }
    }


    private Object marshallWithValueOf(Class typeClass, String value) throws ReflectionException {
        try {
            Method valueOf = typeClass.getMethod("valueOf", new Class[]{String.class});
            return valueOf.invoke(typeClass, value);
        } catch(NoSuchMethodException ignore) {
        } catch (IllegalArgumentException e) {
            throw new ReflectionException(e);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e);
        } catch (InvocationTargetException e) {
            throw new ReflectionException(e);
        }
        return null;
    }

    private Class getClassForType(String type) throws ClassNotFoundException  {
        Class typeClass = getWrapperClassForPrimitiveType(type);
        if(typeClass == null) {
                try {
                    typeClass = Thread.currentThread().getContextClassLoader().loadClass(type);
                } catch (ClassNotFoundException e) {
                    return Class.forName(type);
                }
        }
        return typeClass;
    }

    /**
     * *
     * @param type
     * @return primitive type class or null of not a primitive
     */
    private Class getWrapperClassForPrimitiveType(String type) {
        if ("int".equals(type)) {
            return Integer.class;
        } else if ("long".equals(type)) {
            return Long.class;
        } else if ("double".equals(type)) {
            return Double.class;
        } else if ("boolean".equals(type)) {
            return Boolean.class;
        } else if ("float".equals(type)) {
            return Float.class;
        } else if ("byte".equals(type)) {
            return Byte.class;
        } else if ("char".equals(type)) {
            return Character.class;
        }
        return null;
    }

    private String getAttributeType (ObjectName name, String attributeName)
    throws AttributeNotFoundException, IntrospectionException, InstanceNotFoundException, ReflectionException, WebApplicationException {
        MBeanAttributeInfo attributeInfo =
            getAttributeInfo(name, attributeName);


        if (!attributeInfo.isWritable()) {
            throw new AttributeNotFoundException("Attribute " + attributeName + " is not writable");
        }

        return attributeInfo.getType();
    }

    private MBeanAttributeInfo getAttributeInfo(ObjectName name,
        String attributeName) throws AttributeNotFoundException,
        ReflectionException, IntrospectionException, InstanceNotFoundException
    {
        if (attributeName == null)
             throw new AttributeNotFoundException("Attribute name was null");

        MBeanInfo info = getMBeanServer().getMBeanInfo(name);
        MBeanAttributeInfo[] attributeInfos = info.getAttributes();
        MBeanAttributeInfo attributeInfo = null;
        for(MBeanAttributeInfo mai: attributeInfos ) {
            if(attributeName.equals(mai.getName())) {
                attributeInfo = mai;
                break;
            }
        }
        if (attributeInfo == null) {
            throw new AttributeNotFoundException("Attribute " + attributeName + " not found");
        }

        return attributeInfo;
    }

    private Response createOkResponse()
    {
        JSONObject okJSONStatus = new JSONObject();

        try {
            okJSONStatus.put(OPERATION_RESPONSE_STATUS_PARAMETER_NAME, "OK");
        } catch (JSONException e) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error while creating JSON response!", e);
            throw new WebApplicationException(Response.serverError().entity("Error while creating JSON response!").build());
        }

        return Response.status(HttpServletResponse.SC_OK).entity(okJSONStatus).build();
    }

    private Response createOperationNotFoundResponse()
    {
        JSONObject operationNotFoundJSONStatus = new JSONObject();

        try {
            operationNotFoundJSONStatus.put(OPERATION_RESPONSE_STATUS_PARAMETER_NAME, "OPERATION NOT FOUND");
        } catch (JSONException e) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error while creating JSON response!", e);
            throw new WebApplicationException(Response.serverError().entity("Error while creating JSON response!").build());
        }

        return Response.status(HttpServletResponse.SC_NOT_FOUND).entity(operationNotFoundJSONStatus).build();
    }
}
