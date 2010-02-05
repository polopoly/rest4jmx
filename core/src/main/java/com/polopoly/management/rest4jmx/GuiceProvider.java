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

import java.lang.reflect.Type;

import javax.ws.rs.ext.Provider;

import com.polopoly.management.rest4jmx.ServerModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

/**
 * Guice Adapter for JAXRS.
 */
@Provider
public class GuiceProvider implements InjectableProvider<Inject, Type> {

	protected Injector injector;

	public GuiceProvider() {
		this(Guice.createInjector(new ServerModule()));
	}

	public GuiceProvider(Injector injector) {
		this.injector = injector;
	}

	
	public Injectable<?> getInjectable(ComponentContext context,
			Inject annotation, final Type targetType) {
		if (!(targetType instanceof Class<?>))
			return null;
		return new Injectable<Object>() {
			@SuppressWarnings("unchecked")
			
			public Object getValue() {
				return injector.getInstance((Class<Object>) targetType);
			}
		};
	}

	
	public ComponentScope getScope() {
		return ComponentScope.Singleton;
	}
}
