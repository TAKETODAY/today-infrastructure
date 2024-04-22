/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */


package cn.taketoday.web.servlet;

import java.util.Enumeration;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * Web application listener that cleans up remaining disposable attributes
 * in the ServletContext, i.e. attributes which implement {@link DisposableBean}
 * and haven't been removed before. This is typically used for destroying objects
 * in "application" scope, for which the lifecycle implies destruction at the
 * very end of the web application's shutdown phase.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ContextLoaderListener
 * @since 4.0 2022/2/20 17:20
 */
public class ContextCleanupListener implements ServletContextListener {

  private static final Logger logger = LoggerFactory.getLogger(ContextCleanupListener.class);

  @Override
  public void contextInitialized(ServletContextEvent event) { }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    cleanupAttributes(event.getServletContext());
  }

  /**
   * Find all Frameworkinternal ServletContext attributes which implement
   * {@link DisposableBean} and invoke the destroy method on them.
   *
   * @param servletContext the ServletContext to check
   * @see DisposableBean#destroy()
   */
  static void cleanupAttributes(ServletContext servletContext) {
    Enumeration<String> attrNames = servletContext.getAttributeNames();
    while (attrNames.hasMoreElements()) {
      String attrName = attrNames.nextElement();
      if (attrName.startsWith("cn.taketoday.")) {
        Object attrValue = servletContext.getAttribute(attrName);
        if (attrValue instanceof DisposableBean) {
          try {
            ((DisposableBean) attrValue).destroy();
          }
          catch (Throwable ex) {
            if (logger.isWarnEnabled()) {
              logger.warn("Invocation of destroy method failed on ServletContext " +
                      "attribute with name '{}'", attrName, ex);
            }
          }
        }
      }
    }
  }

}

