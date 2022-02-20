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

package cn.taketoday.framework.web.embedded.tomcat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;

import cn.taketoday.framework.web.servlet.ServletContextInitializer;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * {@link ServletContainerInitializer} used to trigger {@link ServletContextInitializer
 * ServletContextInitializers} and track startup errors.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class TomcatStarter implements ServletContainerInitializer {

  private static final Logger logger = LoggerFactory.getLogger(TomcatStarter.class);

  private final ServletContextInitializer[] initializers;

  private volatile Exception startUpException;

  TomcatStarter(ServletContextInitializer[] initializers) {
    this.initializers = initializers;
  }

  @Override
  public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
    try {
      for (ServletContextInitializer initializer : this.initializers) {
        initializer.onStartup(servletContext);
      }
    }
    catch (Exception ex) {
      this.startUpException = ex;
      // Prevent Tomcat from logging and re-throwing when we know we can
      // deal with it in the main thread, but log for information here.
      if (logger.isErrorEnabled()) {
        logger.error("Error starting Tomcat context. Exception: " + ex.getClass().getName() + ". Message: "
                + ex.getMessage());
      }
    }
  }

  Exception getStartUpException() {
    return this.startUpException;
  }

}
