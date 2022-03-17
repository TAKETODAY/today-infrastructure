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

package cn.taketoday.test.web.servlet.setup;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.context.WebApplicationContext;
import cn.taketoday.web.context.support.WebApplicationContextUtils;
import jakarta.servlet.ServletContext;

/**
 * A concrete implementation of {@link AbstractMockMvcBuilder} that provides
 * the {@link WebApplicationContext} supplied to it as a constructor argument.
 *
 * <p>In addition, if the {@link ServletContext} in the supplied
 * {@code WebApplicationContext} does not contain an entry for the
 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}
 * key, the root {@code WebApplicationContext} will be detected and stored
 * in the {@code ServletContext} under the
 * {@code ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} key.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.0
 */
public class DefaultMockMvcBuilder extends AbstractMockMvcBuilder<DefaultMockMvcBuilder> {

  private final WebApplicationContext webAppContext;

  /**
   * Protected constructor. Not intended for direct instantiation.
   *
   * @see MockMvcBuilders#webAppContextSetup(WebApplicationContext)
   */
  protected DefaultMockMvcBuilder(WebApplicationContext webAppContext) {
    Assert.notNull(webAppContext, "WebApplicationContext is required");
    Assert.notNull(webAppContext.getServletContext(), "WebApplicationContext must have a ServletContext");
    this.webAppContext = webAppContext;
  }

  @Override
  protected WebApplicationContext initWebAppContext() {
    ServletContext servletContext = this.webAppContext.getServletContext();
    Assert.state(servletContext != null, "No ServletContext");
    ApplicationContext rootWac = WebApplicationContextUtils.getWebApplicationContext(servletContext);

    if (rootWac == null) {
      rootWac = this.webAppContext;
      ApplicationContext parent = this.webAppContext.getParent();
      while (parent != null) {
        if (parent instanceof WebApplicationContext && !(parent.getParent() instanceof WebApplicationContext)) {
          rootWac = parent;
          break;
        }
        parent = parent.getParent();
      }
      servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, rootWac);
    }

    return this.webAppContext;
  }

}
