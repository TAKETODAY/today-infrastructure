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

package cn.taketoday.web.config;

import java.util.Collections;

import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.registry.SimpleUrlHandlerRegistry;
import cn.taketoday.web.resource.DefaultServletHttpRequestHandler;
import cn.taketoday.web.servlet.DispatcherServlet;
import jakarta.servlet.ServletContext;

/**
 * Configures a request handler for serving static resources by forwarding
 * the request to the Servlet container's "default" Servlet. This is intended
 * to be used when the MVC {@link DispatcherServlet} is mapped to "/"
 * thus overriding the Servlet container's default handling of static resources.
 *
 * <p>Since this handler is configured at the lowest precedence, effectively
 * it allows all other handler mappings to handle the request, and if none
 * of them do, this handler can forward it to the "default" Servlet.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultServletHttpRequestHandler
 * @since 4.0 2022/2/15 17:10
 */
public class DefaultServletHandlerConfigurer {

  private final ServletContext servletContext;

  @Nullable
  private DefaultServletHttpRequestHandler handler;

  /**
   * Create a {@link DefaultServletHandlerConfigurer} instance.
   *
   * @param servletContext the ServletContext to use.
   */
  public DefaultServletHandlerConfigurer(ServletContext servletContext) {
    Assert.notNull(servletContext, "ServletContext is required");
    this.servletContext = servletContext;
  }

  /**
   * Enable forwarding to the "default" Servlet.
   * <p>When this method is used the {@link DefaultServletHttpRequestHandler}
   * will try to autodetect the "default" Servlet name. Alternatively, you can
   * specify the name of the default Servlet via {@link #enable(String)}.
   *
   * @see DefaultServletHttpRequestHandler
   */
  public void enable() {
    enable(null);
  }

  /**
   * Enable forwarding to the "default" Servlet identified by the given name.
   * <p>This is useful when the default Servlet cannot be autodetected,
   * for example when it has been manually configured.
   *
   * @see DefaultServletHttpRequestHandler
   */
  public void enable(@Nullable String defaultServletName) {
    this.handler = new DefaultServletHttpRequestHandler();
    if (defaultServletName != null) {
      this.handler.setDefaultServletName(defaultServletName);
    }
    this.handler.setServletContext(this.servletContext);
  }

  /**
   * Return a handler mapping instance ordered at {@link Ordered#LOWEST_PRECEDENCE}
   * containing the {@link DefaultServletHttpRequestHandler} instance mapped
   * to {@code "/**"}; or {@code null} if default servlet handling was not
   * been enabled.
   */
  @Nullable
  protected SimpleUrlHandlerRegistry buildHandlerMapping() {
    if (this.handler == null) {
      return null;
    }
    return new SimpleUrlHandlerRegistry(Collections.singletonMap("/**", this.handler),
            Ordered.LOWEST_PRECEDENCE);
  }

}

