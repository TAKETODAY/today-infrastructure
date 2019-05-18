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
package cn.taketoday.web;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import cn.taketoday.context.AbstractApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.Aware;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.aware.BeanNameAware;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.bean.BeanReference;
import cn.taketoday.context.bean.DefaultBeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.factory.ObjectFactory;
import cn.taketoday.context.factory.StandardBeanFactory;
import cn.taketoday.web.listener.RequestContextHolder;

/**
 * @author TODAY <br>
 *         2019-03-23 14:59
 */
public class DefaultWebBeanFactory extends StandardBeanFactory {

    private final WebApplicationContext webApplicationContext;

    public DefaultWebBeanFactory(AbstractApplicationContext applicationContext) {
        super(applicationContext);
        this.webApplicationContext = (WebApplicationContext) applicationContext;
    }

    @Override
    protected void aware(Object bean, String name) {
        if (bean instanceof Aware) {
            if (bean instanceof ServletContextAware) {
                ((ServletContextAware) bean).setServletContext(webApplicationContext.getServletContext());
            }
            if (bean instanceof ApplicationContextAware) {
                ((ApplicationContextAware) bean).setApplicationContext(webApplicationContext);
            }
            if (bean instanceof WebApplicationContextAware) {
                ((WebApplicationContextAware) bean).setWebApplicationContext(webApplicationContext);
            }
            if (bean instanceof BeanNameAware) {
                ((BeanNameAware) bean).setBeanName(name);
            }
            if (bean instanceof BeanFactoryAware) {
                ((BeanFactoryAware) bean).setBeanFactory(this);
            }
            if (bean instanceof EnvironmentAware) {
                ((EnvironmentAware) bean).setEnvironment(webApplicationContext.getEnvironment());
            }
        }
    }

    @Override
    public void handleDependency() {

        final Map<Class<?>, ObjectFactory<?>> servletEnv = new HashMap<Class<?>, ObjectFactory<?>>();
        servletEnv.put(HttpServletRequest.class, RequestContextHolder::currentRequest);
        servletEnv.put(ServletContext.class, () -> webApplicationContext.getServletContext());
        servletEnv.put(HttpSession.class, () -> RequestContextHolder.currentRequest().getSession());

        boolean checked = false;
        for (final PropertyValue propertyValue : getDependencies()) {
            final Class<?> propertyType = propertyValue.getField().getType();
            if (servletEnv.containsKey(propertyType)) {

                if (!checked) {
                    webApplicationContext.getEnvironment().setProperty(Constant.ENABLE_REQUEST_CONTEXT, "true");
                    checked = true;
                }

                final String beanName = ((BeanReference) propertyValue.getValue()).getName();

                registerSingleton(beanName, Proxy.newProxyInstance(propertyType.getClassLoader(), new Class[] { propertyType }, //
                        new ObjectFactoryDelegatingHandler(servletEnv.get(propertyType))//
                ));
                registerBeanDefinition(//
                        beanName, //
                        new DefaultBeanDefinition()//
                                .setAbstract(true)//
                                .setName(beanName)//
                                .setBeanClass(propertyType)//
                );
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
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            try {
                return method.invoke(objectFactory.getObject(), args);
            }
            catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }

}
