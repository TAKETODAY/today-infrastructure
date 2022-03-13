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

package cn.taketoday.web.context;

import cn.taketoday.web.WebApplicationContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * Bootstrap listener to start up and shut down Framework's root {@link WebApplicationContext}.
 * Simply delegates to {@link ContextLoader} as well as to {@link ContextCleanupListener}.
 *
 * <p> {@code ContextLoaderListener} supports injecting the root web
 * application context via the {@link #ContextLoaderListener(WebApplicationContext)}
 * constructor, allowing for programmatic configuration in Servlet initializers.
 * See {@link cn.taketoday.web.WebApplicationInitializer} for usage examples.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see #setContextInitializers
 * @see cn.taketoday.web.WebApplicationInitializer
 * @since 17.02.2003
 */
public class ContextLoaderListener extends ContextLoader implements ServletContextListener {

  /**
   * Create a new {@code ContextLoaderListener} that will create a web application
   * context based on the "contextClass" and "contextConfigLocation" servlet
   * context-params. See {@link ContextLoader} superclass documentation for details on
   * default values for each.
   * <p>This constructor is typically used when declaring {@code ContextLoaderListener}
   * as a {@code <listener>} within {@code web.xml}, where a no-arg constructor is
   * required.
   * <p>The created application context will be registered into the ServletContext under
   * the attribute name {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}
   * and the Framework application context will be closed when the {@link #contextDestroyed}
   * lifecycle method is invoked on this listener.
   *
   * @see ContextLoader
   * @see #ContextLoaderListener(WebApplicationContext)
   * @see #contextInitialized(ServletContextEvent)
   * @see #contextDestroyed(ServletContextEvent)
   */
  public ContextLoaderListener() {
  }

  /**
   * Create a new {@code ContextLoaderListener} with the given application context. This
   * constructor is useful in Servlet initializers where instance-based registration of
   * listeners is possible through the {@link jakarta.servlet.ServletContext#addListener} API.
   * <p>The context may or may not yet be {@linkplain
   * cn.taketoday.context.ConfigurableApplicationContext#refresh() refreshed}. If it
   * (a) is an implementation of {@link ConfigurableWebServletApplicationContext} and
   * (b) has <strong>not</strong> already been refreshed (the recommended approach),
   * then the following will occur:
   * <ul>
   * <li>If the given context has not already been assigned an {@linkplain
   * cn.taketoday.context.ConfigurableApplicationContext#setId id}, one will be assigned to it</li>
   * <li>{@code ServletContext} and {@code ServletConfig} objects will be delegated to
   * the application context</li>
   * <li>{@link #customizeContext} will be called</li>
   * <li>Any {@link cn.taketoday.context.ApplicationContextInitializer ApplicationContextInitializer cn.taketoday.context.ApplicationContextInitializer ApplicationContextInitializers}
   * specified through the "contextInitializerClasses" init-param will be applied.</li>
   * <li>{@link cn.taketoday.context.ConfigurableApplicationContext#refresh refresh()} will be called</li>
   * </ul>
   * If the context has already been refreshed or does not implement
   * {@code ConfigurableWebApplicationContext}, none of the above will occur under the
   * assumption that the user has performed these actions (or not) per his or her
   * specific needs.
   * <p>See {@link cn.taketoday.web.WebApplicationInitializer} for usage examples.
   * <p>In any case, the given application context will be registered into the
   * ServletContext under the attribute name {@link
   * WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} and the Framework
   * application context will be closed when the {@link #contextDestroyed} lifecycle
   * method is invoked on this listener.
   *
   * @param context the application context to manage
   * @see #contextInitialized(ServletContextEvent)
   * @see #contextDestroyed(ServletContextEvent)
   */
  public ContextLoaderListener(WebApplicationContext context) {
    super(context);
  }

  /**
   * Initialize the root web application context.
   */
  @Override
  public void contextInitialized(ServletContextEvent event) {
    initWebApplicationContext(event.getServletContext());
  }

  /**
   * Close the root web application context.
   */
  @Override
  public void contextDestroyed(ServletContextEvent event) {
    closeWebApplicationContext(event.getServletContext());
    ContextCleanupListener.cleanupAttributes(event.getServletContext());
  }

}
