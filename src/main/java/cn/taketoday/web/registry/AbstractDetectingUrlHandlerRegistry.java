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

package cn.taketoday.web.registry;

import java.util.Set;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.util.ObjectUtils;

/**
 * Abstract implementation of the {@link HandlerRegistry} interface,
 * detecting URL mappings for handler beans through introspection of all
 * defined beans in the application context.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #determineUrlsForHandler
 * @since 4.0 2022/1/29 00:26
 */
public abstract class AbstractDetectingUrlHandlerRegistry extends AbstractUrlHandlerRegistry {

  private boolean detectHandlersInAncestorContexts = false;

  /**
   * Set whether to detect handler beans in ancestor ApplicationContexts.
   * <p>Default is "false": Only handler beans in the current ApplicationContext
   * will be detected, i.e. only in the context that this HandlerMapping itself
   * is defined in (typically the current DispatcherServlet's context).
   * <p>Switch this flag on to detect handler beans in ancestor contexts
   * (typically the Spring root WebApplicationContext) as well.
   */
  public void setDetectHandlersInAncestorContexts(boolean detectHandlersInAncestorContexts) {
    this.detectHandlersInAncestorContexts = detectHandlersInAncestorContexts;
  }

  /**
   * Calls the {@link #detectHandlers()} method in addition to the
   * superclass's initialization.
   */
  @Override
  public void initApplicationContext() throws ApplicationContextException {
    super.initApplicationContext();
    detectHandlers();
  }

  /**
   * Register all handlers found in the current ApplicationContext.
   * <p>The actual URL determination for a handler is up to the concrete
   * {@link #determineUrlsForHandler(String)} implementation. A bean for
   * which no such URLs could be determined is simply not considered a handler.
   *
   * @throws BeansException if the handler couldn't be registered
   * @see #determineUrlsForHandler(String)
   */
  protected void detectHandlers() throws BeansException {
    ApplicationContext applicationContext = obtainApplicationContext();
    Set<String> beanNames = this.detectHandlersInAncestorContexts
                            ? BeanFactoryUtils.beanNamesForTypeIncludingAncestors(applicationContext, Object.class)
                            : applicationContext.getBeanNamesForType(Object.class);

    // Take any bean name that we can determine URLs for.
    for (String beanName : beanNames) {
      String[] urls = determineUrlsForHandler(beanName);
      if (!ObjectUtils.isEmpty(urls)) {
        // URL paths found: Let's consider it a handler.
        registerHandler(urls, beanName);
      }
    }

    if (mappingsLogger.isDebugEnabled()) {
      mappingsLogger.debug("{}  {}", formatMappingName(), getHandlerMap());
    }
    else if ((log.isDebugEnabled() && !getHandlerMap().isEmpty()) || log.isTraceEnabled()) {
      log.debug("Detected {} mappings in {}", getHandlerMap().size(), formatMappingName());
    }
  }

  /**
   * Determine the URLs for the given handler bean.
   *
   * @param beanName the name of the candidate bean
   * @return the URLs determined for the bean, or an empty array if none
   */
  protected abstract String[] determineUrlsForHandler(String beanName);

}

