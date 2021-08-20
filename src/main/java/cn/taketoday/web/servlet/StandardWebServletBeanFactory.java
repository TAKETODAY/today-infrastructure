/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import cn.taketoday.beans.ObjectFactory;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.ServletContextAware;
import cn.taketoday.web.StandardWebBeanFactory;

/**
 * @author TODAY 2019-03-23 14:59
 */
public class StandardWebServletBeanFactory extends StandardWebBeanFactory {

  public StandardWebServletBeanFactory(ConfigurableWebServletApplicationContext context) {
    super(context);
  }

  @Override
  protected void awareInternal(final Object bean, final BeanDefinition def) {
    super.awareInternal(bean, def);
    if (bean instanceof ServletContextAware) {
      ((ServletContextAware) bean).setServletContext(getApplicationContext().getServletContext());
    }
    if (bean instanceof WebServletApplicationContextAware) {
      ((WebServletApplicationContextAware) bean).setWebServletApplicationContext(getApplicationContext());
    }
  }

  @Override
  protected Map<Class<?>, Object> createObjectFactories() {
    final Map<Class<?>, Object> servletEnv = super.createObjectFactories();
    // @since 3.0
    final class HttpSessionFactory implements ObjectFactory<HttpSession> {
      @Override
      public HttpSession getObject() {
        return ServletUtils.getHttpSession(RequestContextHolder.currentContext());
      }
    }

    servletEnv.put(HttpSession.class, new HttpSessionFactory());
    servletEnv.put(HttpServletRequest.class, factory(RequestContextHolder::currentRequest));
    servletEnv.put(HttpServletResponse.class, factory(RequestContextHolder::currentResponse));

    final WebServletApplicationContext context = getApplicationContext();
    servletEnv.put(ServletContext.class, factory(context::getServletContext));

    return servletEnv;
  }

  @Override
  public ConfigurableWebServletApplicationContext getApplicationContext() {
    return (ConfigurableWebServletApplicationContext) super.getApplicationContext();
  }

}
