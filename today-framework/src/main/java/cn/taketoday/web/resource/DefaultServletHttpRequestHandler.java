/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.resource;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletContextAware;
import cn.taketoday.web.servlet.ServletUtils;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * An {@link HttpRequestHandler} for serving static files using the Servlet container's "default" Servlet.
 *
 * <p>This handler is intended to be used with a "/*" mapping when the
 * {@link cn.taketoday.web.servlet.DispatcherServlet DispatcherServlet}
 * is mapped to "/", thus  overriding the Servlet container's default handling of static resources.
 * The mapping to this handler should generally be ordered as the last in the chain so that it will
 * only execute when no other more specific mappings (i.e., to controllers) can be matched.
 *
 * <p>Requests are handled by forwarding through the {@link RequestDispatcher} obtained via the
 * name specified through the {@link #setDefaultServletName "defaultServletName" property}.
 * In most cases, the {@code defaultServletName} does not need to be set explicitly, as the
 * handler checks at initialization time for the presence of the default Servlet of well-known
 * containers such as Tomcat, Jetty, Resin, WebLogic and WebSphere. However, when running in a
 * container where the default Servlet's name is not known, or where it has been customized
 * via server configuration, the  {@code defaultServletName} will need to be set explicitly.
 *
 * @author Jeremy Grelle
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DefaultServletHttpRequestHandler implements HttpRequestHandler, ServletContextAware {

  /** Default Servlet name used by Tomcat, Jetty, JBoss, and GlassFish. */
  private static final String COMMON_DEFAULT_SERVLET_NAME = "default";

  /** Default Servlet name used by Google App Engine. */
  private static final String GAE_DEFAULT_SERVLET_NAME = "_ah_default";

  /** Default Servlet name used by Resin. */
  private static final String RESIN_DEFAULT_SERVLET_NAME = "resin-file";

  /** Default Servlet name used by WebLogic. */
  private static final String WEBLOGIC_DEFAULT_SERVLET_NAME = "FileServlet";

  /** Default Servlet name used by WebSphere. */
  private static final String WEBSPHERE_DEFAULT_SERVLET_NAME = "SimpleFileServlet";

  @Nullable
  private String defaultServletName;

  @Nullable
  private ServletContext servletContext;

  /**
   * Set the name of the default Servlet to be forwarded to for static resource requests.
   */
  public void setDefaultServletName(@Nullable String defaultServletName) {
    this.defaultServletName = defaultServletName;
  }

  /**
   * If the {@code defaultServletName} property has not been explicitly set,
   * attempts to locate the default Servlet using the known common
   * container-specific names.
   */
  @Override
  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
    if (!StringUtils.hasText(this.defaultServletName)) {
      if (servletContext.getNamedDispatcher(COMMON_DEFAULT_SERVLET_NAME) != null) {
        this.defaultServletName = COMMON_DEFAULT_SERVLET_NAME;
      }
      else if (servletContext.getNamedDispatcher(GAE_DEFAULT_SERVLET_NAME) != null) {
        this.defaultServletName = GAE_DEFAULT_SERVLET_NAME;
      }
      else if (servletContext.getNamedDispatcher(RESIN_DEFAULT_SERVLET_NAME) != null) {
        this.defaultServletName = RESIN_DEFAULT_SERVLET_NAME;
      }
      else if (servletContext.getNamedDispatcher(WEBLOGIC_DEFAULT_SERVLET_NAME) != null) {
        this.defaultServletName = WEBLOGIC_DEFAULT_SERVLET_NAME;
      }
      else if (servletContext.getNamedDispatcher(WEBSPHERE_DEFAULT_SERVLET_NAME) != null) {
        this.defaultServletName = WEBSPHERE_DEFAULT_SERVLET_NAME;
      }
      else {
        throw new IllegalStateException("Unable to locate the default servlet for serving static content. " +
                "Please set the 'defaultServletName' property explicitly.");
      }
    }
  }

  @Override
  public Object handleRequest(RequestContext request) throws Throwable {
    Assert.state(this.servletContext != null, "No ServletContext set");
    RequestDispatcher rd = this.servletContext.getNamedDispatcher(this.defaultServletName);
    if (rd == null) {
      throw new IllegalStateException("A RequestDispatcher could not be located for the default servlet '" +
              this.defaultServletName + "'");
    }

    HttpServletRequest servletRequest = ServletUtils.getServletRequest(request);
    HttpServletResponse servletResponse = ServletUtils.getServletResponse(request);

    rd.forward(servletRequest, servletResponse);
    return NONE_RETURN_VALUE;
  }

}
