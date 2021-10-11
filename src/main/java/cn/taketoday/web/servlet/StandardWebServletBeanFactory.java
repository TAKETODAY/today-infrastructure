/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
import java.util.function.Supplier;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.StandardWebBeanFactory;

/**
 * @author TODAY 2019-03-23 14:59
 */
public class StandardWebServletBeanFactory extends StandardWebBeanFactory {

  private ConfigurableWebServletApplicationContext context;

  public StandardWebServletBeanFactory(ConfigurableWebServletApplicationContext context) {
    this.context = context;
  }

  @Override
  protected Map<Class<?>, Object> createObjectFactories() {
    final Map<Class<?>, Object> servletEnv = super.createObjectFactories();
    // @since 3.0
    final class HttpSessionFactory implements Supplier<HttpSession> {
      @Override
      public HttpSession get() {
        return ServletUtils.getHttpSession(RequestContextHolder.currentContext());
      }
    }

    servletEnv.put(HttpSession.class, new HttpSessionFactory());
    servletEnv.put(HttpServletRequest.class, factory(RequestContextHolder::currentRequest));
    servletEnv.put(HttpServletResponse.class, factory(RequestContextHolder::currentResponse));

    servletEnv.put(ServletContext.class, factory(context::getServletContext));

    return servletEnv;
  }

}
