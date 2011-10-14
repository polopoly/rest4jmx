package com.polopoly.management.rest4jmx;

import javax.management.JMException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class MBeanServerContextListener implements ServletContextListener {
    
    public void contextDestroyed(ServletContextEvent arg0)
    {
        // TODO Auto-generated method stub

    }

    public void contextInitialized(ServletContextEvent sce)
    {
        try {
            MBeanServerSetup.setupMBeanServer();
        } catch(JMException ex) {
            throw new Error("Could not setup MBean server "+ ex, ex);
        }
    }

}
