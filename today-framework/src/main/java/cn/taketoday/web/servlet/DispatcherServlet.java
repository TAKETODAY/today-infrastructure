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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.handler.DispatcherHandler;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
  public void service(ServletRequest request, ServletResponse response) throws ServletException {
    HttpServletRequest servletRequest = (HttpServletRequest) request;
    if (isDebugEnabled) {
      logRequest(servletRequest);
    }
    RequestContext context = RequestContextHolder.get();

    boolean reset = false;
    if (context == null) {
      ApplicationContext ctx = getApplicationContext();
      context = new ServletRequestContext(ctx, servletRequest, (HttpServletResponse) response);
      RequestContextHolder.set(context);
      reset = true;
    }

    try {
      dispatch(context);
    }
    catch (final Throwable e) {
      throw new ServletException(e);
    }
    finally {
      if (reset) {
        RequestContextHolder.remove();
      }
    }
  }

  @Override
  public void init(ServletConfig servletConfig) {
    this.servletConfig = servletConfig;
    init();
  }

  @Override
  public ServletConfig getServletConfig() {
    Assert.state(servletConfig != null, "DispatcherServlet has not been initialized");
    return servletConfig;
  }

  @Override
  public final String getServletInfo() {
    return "DispatcherServlet, Copyright © TODAY & 2017 - 2022 All Rights Reserved";
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

  // @since 4.0
  private void logRequest(HttpServletRequest request) {
    String params;
    if (StringUtils.startsWithIgnoreCase(request.getContentType(), "multipart/")) {
      params = "multipart";
    }
    else if (isEnableLoggingRequestDetails()) {
      params = request.getParameterMap().entrySet().stream()
              .map(entry -> entry.getKey() + ":" + Arrays.toString(entry.getValue()))
              .collect(Collectors.joining(", "));
    }
    else {
      params = request.getParameterMap().isEmpty() ? "" : "masked";
    }

    String queryString = request.getQueryString();
    String queryClause = StringUtils.isNotEmpty(queryString) ? "?" + queryString : "";
    String dispatchType = !DispatcherType.REQUEST.equals(request.getDispatcherType())
                          ? "\"" + request.getDispatcherType() + "\" dispatch for "
                          : "";
    String message = dispatchType + request.getMethod() + " " +
            request.getRequestURL() + queryClause + ", parameters={" + params + "}";

    if (log.isTraceEnabled()) {
      List<String> values = Collections.list(request.getHeaderNames());
      String headers = values.size() > 0 ? "masked" : "";
      if (isEnableLoggingRequestDetails()) {
        headers = values.stream().map(name -> name + ":" + Collections.list(request.getHeaders(name)))
                .collect(Collectors.joining(", "));
      }
      log.trace(message + ", headers={" + headers + "} in DispatcherServlet '" +
              getServletConfig().getServletName() + "'");
    }
    else {
      log.debug(message);
    }
  }

}
