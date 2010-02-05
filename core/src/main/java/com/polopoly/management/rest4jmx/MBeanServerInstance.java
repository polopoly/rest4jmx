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

import java.util.ArrayList;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

/**
 * 
 */
public class MBeanServerInstance {
    MBeanServer mbeanServer;
    /**
     * Creates a new <code>MBeanServerInstance</code> instance.
     *
     */
    public MBeanServerInstance() {
        ArrayList<MBeanServer> s = MBeanServerFactory.findMBeanServer(null);
        if (s != null && !s.isEmpty()) {
            mbeanServer = s.get(0);
        }
    }
    
    MBeanServer getMBeanServer() {
        return mbeanServer;
    }

}
