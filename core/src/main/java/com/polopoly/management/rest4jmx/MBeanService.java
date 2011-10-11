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
import javax.management.OperationsException;
import javax.management.ReflectionException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.google.inject.Inject;
import com.sun.jersey.api.json.JSONWithPadding;

@Path("/")
@Produces({ MediaType.APPLICATION_JSON, "application/x-javascript" })
public class MBeanService
{
    private static final String INVOKE_ALLOWED_APPLICATION_PARAMETER_NAME = "invokeAllowedApplication";
    private static final String ALLOW_GLOBAL_INVOKES_INIT_PARAMETER_NAME = "allowCrossApplicationInvokes";

    private static final String OPERATION_RESPONSE_STATUS_PARAMETER_NAME = "status";
    private static final String OPERATION_PARAMETERS_PARAMETER_NAME = "params";

    private static final Logger LOG = Logger.getLogger(MBeanService.class.getName());
    private static final String CLAZZ = MBeanService.class.getName();

    @Inject MBeanServerInstance mbeanServer;
    @Context SecurityContext secContext;

    private boolean allowGlobalInvokes = true;
    private String invokeAllowedApplication = null;

    public MBeanService()
    {

    }

    public MBeanService(final @Context ServletContext servletContext,
                        final @Context ServletConfig servletConfig)
    {
        String allowGlobalInvokesString = servletConfig.getInitParameter(ALLOW_GLOBAL_INVOKES_INIT_PARAMETER_NAME);

        if (allowGlobalInvokesString != null) {
            allowGlobalInvokes = Boolean.valueOf(allowGlobalInvokesString);
        }

        invokeAllowedApplication = servletConfig.getInitParameter(INVOKE_ALLOWED_APPLICATION_PARAMETER_NAME);
    }

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
    public Response getMBeanNamesForDomain(@PathParam("domain") String domain,
        @QueryParam("callback") String callback) throws JSONException {
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.entering(CLAZZ, "getMBeanNamesForDomain", new String[]{domain, callback});
        }
        
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
        
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.entering(CLAZZ, "getMBean", new String[]{objectName, callback});
        }
        
        MBeanServer server = getMBeanServer();
        try {
            boolean isUserAWriter = secContext.getUserPrincipal() != null ? 
                                    secContext.isUserInRole("jmx_writer") :
                                    true;
            
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
                att.put("value", getValueAsJson(a.getValue()));
                att.put("writable", isUserAWriter ? ai.isWritable() : false);
                att.put("isBoolean", ai.isIs());

                attValues.put(n, att);
            }

            MBeanOperationInfo[] operations = info.getOperations();
            List<JSONObject> ops = new ArrayList<JSONObject>();

            if(isUserAWriter) {
                for (MBeanOperationInfo operation : operations) {
                    if (operationHasNoCollectionTypesInSignature(operation)) {
                        JSONObject op = new JSONObject();
                        String[] signatureTypes = getSignatureForOperation(operation);
                        String returnType = operation.getReturnType();

                        StringBuilder sb = new StringBuilder();
                        sb.append(returnType + " ");
                        sb.append(operation.getName() + "(");

                        for (String type : signatureTypes) {
                            sb.append(type).append(",");
                        }

                        if (signatureTypes.length > 0) {
                            sb.deleteCharAt(sb.length() - 1);
                        }

                        sb.append(")");

                        op.put("operation", sb.toString() );
                        op.put("name", operation.getName() );
                        op.put("returns", returnType);
                        op.put("params", getValueAsJson(signatureTypes));
                        ops.add(op);
                    }
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
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.entering(CLAZZ, "getAttribute", new String[]{objectName, attribute, callback});
        }
        
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
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.entering(CLAZZ, "setAttribute", new String[]{objectName, attribute, value, callback});
        }
        
        //System.err.println("DEBUG "+objectName + ":" + attribute +"="+value);
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
                                        final @QueryParam("callback") String callback,
                                        final String requestBody)
    {
        //System.err.println("DEBUG invoke with json " + requestBody);
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.entering(CLAZZ, "invokeOperationPost", new String[]{objectName, operationName, requestBody, callback});
        }
        
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
                assertOperationInvokeAllowedInApplication(name);

                Object oRet = doInvoke(server, info, name, operationName, paramsArray);
          //      System.err.println("DEBUG ret " + oRet);
                JSONObject ret = new JSONObject();
                ret.put("name", objectName);
                ret.put("operation", operationName); //XXX not perfect, hard do distinguish
                ret.put("return", getValueAsJson(oRet));
                return getResponse(ret, callback);

            } else {
                throw new OperationsException("No such method");            }
        } catch (MalformedObjectNameException me) {
            LOG.log(Level.WARNING, "Malformed object name!", me);
            throw new WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE).entity("Malformed mbean name '" + objectName + "'!").build());
        } catch (InstanceNotFoundException ne) {
            LOG.log(Level.WARNING, "Mbean not found!", ne);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("No such mbean '" + objectName + "'!").build());
        } catch(OperationsException ne) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).
                                              entity("No operation " + operationName + " with params " +
                                              requestBody     + " for " + objectName
                                               + ": " + ne).build());
        } catch (WebApplicationException wae) {
            LOG.log(Level.WARNING, "Error while invoking operation!", wae);
            throw wae;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error while invoking operation!", e);
            throw new WebApplicationException(Response.serverError().entity("Error while invoking operation '" + operationName + "'!").build());
        }

    }

    private void assertOperationInvokeAllowedInApplication(final ObjectName objectName)
    {
        if (!allowGlobalInvokes) {
            String applicationKeyProperty = objectName.getKeyProperty("application");

            if (applicationKeyProperty != null && !applicationKeyProperty.equals(invokeAllowedApplication)) {
                String message = "Operation invoke not allowed in application '" + applicationKeyProperty + "'!";
                Response response = Response.status(Response.Status.FORBIDDEN).entity(message).build();

                LOG.log(Level.WARNING, message);
                throw new WebApplicationException(response);
            }
        }
    }

    private Object doInvoke(final MBeanServer mbeanServer,
                          final MBeanInfo mbeanInfo,
                          final ObjectName objectName,
                          final String operationName,
                          final JSONArray paramsArray)
        throws InstanceNotFoundException,
               ReflectionException,
               MBeanException,
               JSONException,
               ClassNotFoundException,
               OperationsException
    {
        MBeanOperationInfo[] operations = mbeanInfo.getOperations();

        for (MBeanOperationInfo operation : operations) {
            if (operation.getName().equals(operationName) &&
                operationHasNoCollectionTypesInSignature(operation) &&
                // Match in length only
                signatureMatchParameters(operation, paramsArray)) {

                List params = fitParametersToOperation(operation, paramsArray);
                return mbeanServer.invoke(objectName, operationName, params.toArray(new Object[0]), 
                                   getSignatureForOperation(operation));
            }
        }
        throw new OperationsException("No method that fits params " + paramsArray);
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
                                             final JSONArray paramsArray) 
        throws JSONException,ClassNotFoundException
    {
        int paramsArrayLength = (paramsArray != null) ? paramsArray.length() : 0;
        int operationInfoLength = operationInfo.getSignature().length;

        if (paramsArrayLength != operationInfoLength) {
            return false;
        }
        return true;
    }
    
    private List fitParametersToOperation(final MBeanOperationInfo operationInfo,
        final JSONArray paramsArray) throws OperationsException  {
        List params = new ArrayList();
        try {
            int paramsArrayLength = (paramsArray != null) ? paramsArray.length() : 0;
            int operationInfoLength = operationInfo.getSignature().length;

            if (paramsArrayLength != operationInfoLength) {
                throw new OperationsException("Wrong signatur, parameters not same length, was " + 
                                              paramsArrayLength + " excpected " + operationInfoLength);
            }

            if (paramsArray != null && paramsArray.length() > 0) {
                for (int i = 0; i < paramsArray.length(); i++) {
                    Object param = paramsArray.get(i);
                    Class targetType = getClassForType(operationInfo.getSignature()[i].getType());
                    // Try type
                    if (targetType.equals(param.getClass())) {
                        params.add(param);
                    }
                    else {
                        // Try valueOf Marshalling
                        Object ret = marshallWithValueOf(targetType, param.toString());
                        params.add(ret);
                    }
                }
            }
            // Just for safety
            if(paramsArrayLength == params.size()) {
                return params;
            }
            throw getMarshallOperationsException(null, paramsArray, operationInfo);
        }
        catch(ReflectionException ex) {
            throw getMarshallOperationsException(ex, paramsArray, operationInfo);
        }
        catch(ClassNotFoundException ex) {
            throw getMarshallOperationsException(ex, paramsArray, operationInfo);
        }
        catch(JSONException ex) {
            throw getMarshallOperationsException(ex, paramsArray, operationInfo);
        }
    }
    private OperationsException getMarshallOperationsException(Exception cause, JSONArray paramsArray, 
                                                               MBeanOperationInfo operationInfo) {
        OperationsException o =  new OperationsException("Could not marshal " + paramsArray + " to " + operationInfo.getSignature() 
                                                         + ": " + cause.getMessage());
        if(cause != null) {
            o.initCause(cause);
        }
        return o;
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

    private JSONObject getAttributeAsJSON(ObjectName mbean, String attribute) throws JSONException {
        try {
            JSONObject att = new JSONObject();
            att.put("name", mbean.toString());
            att.put("attribute", attribute);
            att.put("value", getValueAsJson(getMBeanServer().getAttribute(mbean, attribute)));
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

    private Object getValueAsJson(Object a) {
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
        else if(a instanceof List) {
            return new JSONArray((List)a);
        }
        else if(a instanceof Map) {
            return new JSONObject((Map)a);
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
            // XXX handle RefEx better?
        } catch(ClassNotFoundException e) {
            throw new ReflectionException(e);
        }
    }


    
    private Object marshallWithValueOf(Class typeClass, String value) throws ReflectionException {
        try {
            Method valueOf = typeClass.getMethod("valueOf", new Class[]{String.class});
            return valueOf.invoke(typeClass, value);
        } catch(NoSuchMethodException e) {
            throw new ReflectionException(e);
        } catch (IllegalArgumentException e) {
            throw new ReflectionException(e);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e);
        } catch (InvocationTargetException e) {
            throw new ReflectionException(e);
        }
    }

    private Class getClassForType(String type)
        throws ClassNotFoundException
    {
        Class typeClass = getWrapperClassForPrimitiveType(type);

        if (typeClass == null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            try {
                typeClass = classLoader.loadClass(type);
            } catch (ClassNotFoundException e) {
                // JDK6 forbids loading of array types by ClassLoader.loadClass(...).
                // See http://bugs.sun.com/view_bug.do?bug_id=6434149

                typeClass = Class.forName(type, false, classLoader);
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

    private Response createOkResponse(final String callback)
    {
        JSONObject status = new JSONObject();
        String mediaType = (callback == null) ? MediaType.APPLICATION_JSON : "application/x-javascript";

        try {
            status.put(OPERATION_RESPONSE_STATUS_PARAMETER_NAME, "OK");
        } catch (JSONException e) {
            LOG.log(Level.WARNING, "Error while creating JSON response!", e);
            throw new WebApplicationException(Response.serverError().entity("Error while creating JSON response!").build());
        }

        return Response.status(Response.Status.NOT_FOUND).entity((new JSONWithPadding(status, mediaType))).build();
    }

    private Response createOperationNotFoundResponse(final String callback)
    {
        JSONObject status = new JSONObject();
        String mediaType = (callback == null) ? MediaType.APPLICATION_JSON : "application/x-javascript";

        try {
            status.put(OPERATION_RESPONSE_STATUS_PARAMETER_NAME, "OPERATION NOT FOUND");
        } catch (JSONException e) {
            LOG.log(Level.WARNING, "Error while creating JSON response!", e);
            throw new WebApplicationException(Response.serverError().entity("Error while creating JSON response!").build());
        }

        return Response.status(Response.Status.NOT_FOUND).entity((new JSONWithPadding(status, mediaType))).build();
    }
}
