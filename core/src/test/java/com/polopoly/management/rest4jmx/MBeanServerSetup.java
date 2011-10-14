package com.polopoly.management.rest4jmx;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

public class MBeanServerSetup {
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

    static void setupMBeanServer() throws NotCompliantMBeanException,
        InstanceAlreadyExistsException, MBeanRegistrationException,
        MalformedObjectNameException
    {
        MBeanServerInstance mb = new MBeanServerInstance();
        s = mb.getMBeanServer();
        if (s == null) {
            s = MBeanServerFactory.createMBeanServer();
        }
        StandardMBean bean = new StandardMBean(new TestMBean(), MyMBean.class);
        s.registerMBean(bean, new ObjectName(TESTDOMAIN_NAME_TEST_BEAN));
        MainTest.LOG.info("Registered Test MBean " + TESTDOMAIN_NAME_TEST_BEAN);
    }

    static final String TESTDOMAIN_NAME_TEST_BEAN = MBeanServerSetup.TESTDOMAIN + ":name=testBean";
    static final String TESTDOMAIN = "testdomain";
    static final String DEFAULT = "default";
    private static MBeanServer s;
}
