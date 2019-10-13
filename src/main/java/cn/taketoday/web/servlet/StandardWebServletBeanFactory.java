/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *   
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.servlet;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import cn.taketoday.context.AbstractApplicationContext;
import cn.taketoday.context.bean.BeanReference;
import cn.taketoday.context.bean.DefaultBeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.factory.ObjectFactory;
import cn.taketoday.context.factory.StandardBeanFactory;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletContextAware;
import cn.taketoday.web.WebApplicationContextAware;

/**
 * @author TODAY <br>
 *         2019-03-23 14:59
 */
public class StandardWebServletBeanFactory extends StandardBeanFactory {

    private final WebServletApplicationContext webApplicationContext;

    public StandardWebServletBeanFactory(AbstractApplicationContext applicationContext) {
        super(applicationContext);

        if (applicationContext instanceof WebServletApplicationContext) {
            this.webApplicationContext = (WebServletApplicationContext) applicationContext;
        }
        else {
            throw ExceptionUtils.newConfigurationException(null, "application context must be  'WebServletApplicationContext'");
        }
    }

    @Override
    protected void awareInternal(Object bean, String name) {

        super.awareInternal(bean, name);

        if (bean instanceof WebApplicationContextAware) {
            ((WebApplicationContextAware) bean).setWebApplicationContext(webApplicationContext);
        }
        if (bean instanceof ServletContextAware) {

            final ServletContext servletContext = webApplicationContext.getServletContext();
            if (servletContext == null) {

            }
            else {
                ((ServletContextAware) bean).setServletContext(servletContext);
            }
        }
        if (bean instanceof WebServletApplicationContextAware) {
            ((WebServletApplicationContextAware) bean).setWebServletApplicationContext(webApplicationContext);
        }
    }

    @Override
    public void handleDependency() {

        final Map<Class<?>, ObjectFactory<?>> servletEnv = new HashMap<Class<?>, ObjectFactory<?>>();

        servletEnv.put(HttpSession.class, RequestContextHolder::currentSession);
        servletEnv.put(HttpServletRequest.class, RequestContextHolder::currentRequest);
        servletEnv.put(ServletContext.class, webApplicationContext::getServletContext);
        // @since 2.3.7
        servletEnv.put(RequestContext.class, RequestContextHolder::currentContext);
        servletEnv.put(HttpServletResponse.class, RequestContextHolder::currentResponse);

        for (final PropertyValue propertyValue : getDependencies()) {
            final Class<?> propertyType = propertyValue.getField().getType();
            if (servletEnv.containsKey(propertyType)) {

                final String beanName = ((BeanReference) propertyValue.getValue()).getName();
                // @off
                registerSingleton(beanName, Proxy.newProxyInstance(propertyType.getClassLoader(), new Class[] { propertyType }, //
                     new ObjectFactoryDelegatingHandler(servletEnv.get(propertyType))//@on
                ));

                registerBeanDefinition(beanName, new DefaultBeanDefinition(beanName, propertyType));
            }
        }
        super.handleDependency();
    }

    /**
     * Reflective InvocationHandler for lazy access to the current target object.
     */
    @SuppressWarnings("serial")
    private static class ObjectFactoryDelegatingHandler implements InvocationHandler, Serializable {

        private final ObjectFactory<?> objectFactory;

        public ObjectFactoryDelegatingHandler(ObjectFactory<?> objectFactory) {
            this.objectFactory = objectFactory;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

            try {
                return method.invoke(objectFactory.getObject(), args);
            }
            catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }

}
