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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.servlet;

import java.io.Serial;
import java.io.Serializable;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.handler.DispatcherHandler;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * Central dispatcher for HTTP request handlers/controllers in Servlet
 *
 * @author TODAY 2018-06-25 19:47:14
 * @since 2.0
 */
public class DispatcherServlet
        extends DispatcherHandler implements Servlet, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private transient ServletConfig servletConfig;

  public DispatcherServlet() { }

  public DispatcherServlet(WebServletApplicationContext context) {
    super(context);
  }

  @Override
  public void service(final ServletRequest request,
                      final ServletResponse response) throws ServletException {
    RequestContext context = ServletUtils.getRequestContext(request, response);
    try {
      dispatch(context);
    }
    catch (final Throwable e) {
      throw new ServletException(e);
    }
    finally {
      RequestContextHolder.resetContext();
    }
  }

  @Override
  public void init(ServletConfig servletConfig) {
    this.servletConfig = servletConfig;
  }

  @Override
  public ServletConfig getServletConfig() {
    Assert.state(servletConfig != null, "DispatcherServlet has not been initialized");
    return servletConfig;
  }

  @Override
  public final String getServletInfo() {
    return "DispatcherServlet, Copyright © TODAY & 2017 - 2021 All Rights Reserved";
  }

  @Override
  public void destroy() {
    super.destroy();
  }

  @Override
  protected void log(String msg) {
    super.log(msg);
    getServletConfig().getServletContext().log(msg);
  }

}
