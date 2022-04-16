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

package cn.taketoday.framework.web.embedded.jetty;

import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

import cn.taketoday.framework.web.servlet.ServletContextInitializer;
import cn.taketoday.lang.Assert;
import jakarta.servlet.ServletException;

/**
 * Jetty {@link Configuration} that calls {@link ServletContextInitializer}s.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ServletContextInitializerConfiguration extends AbstractConfiguration {

  private final ServletContextInitializer[] initializers;

  /**
   * Create a new {@link ServletContextInitializerConfiguration}.
   *
   * @param initializers the initializers that should be invoked
   */
  public ServletContextInitializerConfiguration(ServletContextInitializer... initializers) {
    Assert.notNull(initializers, "Initializers must not be null");
    this.initializers = initializers;
  }

  @Override
  public void configure(WebAppContext context) throws Exception {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(context.getClassLoader());
    try {
      callInitializers(context);
    }
    finally {
      Thread.currentThread().setContextClassLoader(classLoader);
    }
  }

  private void callInitializers(WebAppContext context) throws ServletException {
    try {
      setExtendedListenerTypes(context, true);
      for (ServletContextInitializer initializer : initializers) {
        initializer.onStartup(context.getServletContext());
      }
    }
    finally {
      setExtendedListenerTypes(context, false);
    }
  }

  private void setExtendedListenerTypes(WebAppContext context, boolean extended) {
    try {
      context.getServletContext().setExtendedListenerTypes(extended);
    }
    catch (NoSuchMethodError ex) {
      // Not available on Jetty 8
    }
  }

}
