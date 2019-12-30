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

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.ObjectFactory;
import cn.taketoday.context.factory.StandardBeanFactory;
import cn.taketoday.web.servlet.RequestContextHolder;

/**
 * @author TODAY <br>
 *         2019-11-20 21:47
 */
public class StandardWebBeanFactory extends StandardBeanFactory {

    public StandardWebBeanFactory(ConfigurableApplicationContext applicationContext) {
        super(applicationContext);
        if (applicationContext instanceof ConfigurableWebApplicationContext == false) {
            throw new ConfigurationException("application context must be 'ConfigurableWebApplicationContext'");
        }
    }

    @Override
    protected void awareInternal(Object bean, String name) {

        super.awareInternal(bean, name);

        if (bean instanceof WebApplicationContextAware) {
            ((WebApplicationContextAware) bean).setWebApplicationContext(getApplicationContext());
        }
    }

    @Override
    protected Map<Class<?>, Object> createObjectFactories() {

        final Map<Class<?>, Object> env = new HashMap<>();

        env.put(RequestContext.class, factory(RequestContextHolder::currentContext));

        return env;
    }

    protected <T> ObjectFactory<T> factory(ObjectFactory<T> objectFactory) {
        return objectFactory;
    }

    @Override
    public ConfigurableWebApplicationContext getApplicationContext() {
        return (ConfigurableWebApplicationContext) super.getApplicationContext();
    }

}
