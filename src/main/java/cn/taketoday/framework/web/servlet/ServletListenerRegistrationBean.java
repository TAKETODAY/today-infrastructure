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

package cn.taketoday.framework.web.servlet;

import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextAttributeListener;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRequestAttributeListener;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;

/**
 * A {@link ServletContextInitializer} to register {@link EventListener}s in a Servlet
 * 3.0+ container. Similar to the {@link ServletContext#addListener(EventListener)
 * registration} features provided by {@link ServletContext} but with a Spring Bean
 * friendly design.
 *
 * This bean can be used to register the following types of listener:
 * <ul>
 * <li>{@link ServletContextAttributeListener}</li>
 * <li>{@link ServletRequestListener}</li>
 * <li>{@link ServletRequestAttributeListener}</li>
 * <li>{@link HttpSessionAttributeListener}</li>
 * <li>{@link HttpSessionIdListener}</li>
 * <li>{@link HttpSessionListener}</li>
 * <li>{@link ServletContextListener}</li>
 * </ul>
 *
 * @param <T> the type of listener
 * @author Dave Syer
 * @author Phillip Webb
 * @since 4.0
 */
public class ServletListenerRegistrationBean<T extends EventListener> extends RegistrationBean {

  private static final Set<Class<?>> SUPPORTED_TYPES;

  static {
    Set<Class<?>> types = new HashSet<>();
    types.add(ServletContextAttributeListener.class);
    types.add(ServletRequestListener.class);
    types.add(ServletRequestAttributeListener.class);
    types.add(HttpSessionAttributeListener.class);
    types.add(HttpSessionIdListener.class);
    types.add(HttpSessionListener.class);
    types.add(ServletContextListener.class);
    SUPPORTED_TYPES = Collections.unmodifiableSet(types);
  }

  private T listener;

  /**
   * Create a new {@link ServletListenerRegistrationBean} instance.
   */
  public ServletListenerRegistrationBean() {
  }

  /**
   * Create a new {@link ServletListenerRegistrationBean} instance.
   *
   * @param listener the listener to register
   */
  public ServletListenerRegistrationBean(T listener) {
    Assert.notNull(listener, "Listener must not be null");
    Assert.isTrue(isSupportedType(listener), "Listener is not of a supported type");
    this.listener = listener;
  }

  /**
   * Set the listener that will be registered.
   *
   * @param listener the listener to register
   */
  public void setListener(T listener) {
    Assert.notNull(listener, "Listener must not be null");
    Assert.isTrue(isSupportedType(listener), "Listener is not of a supported type");
    this.listener = listener;
  }

  /**
   * Return the listener to be registered.
   *
   * @return the listener to be registered
   */
  public T getListener() {
    return this.listener;
  }

  @Override
  protected String getDescription() {
    Assert.notNull(this.listener, "Listener must not be null");
    return "listener " + this.listener;
  }

  @Override
  protected void register(String description, ServletContext servletContext) {
    try {
      servletContext.addListener(this.listener);
    }
    catch (RuntimeException ex) {
      throw new IllegalStateException("Failed to add listener '" + this.listener + "' to servlet context", ex);
    }
  }

  /**
   * Returns {@code true} if the specified listener is one of the supported types.
   *
   * @param listener the listener to test
   * @return if the listener is of a supported type
   */
  public static boolean isSupportedType(EventListener listener) {
    for (Class<?> type : SUPPORTED_TYPES) {
      if (ClassUtils.isAssignableValue(type, listener)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return the supported types for this registration.
   *
   * @return the supported types
   */
  public static Set<Class<?>> getSupportedTypes() {
    return SUPPORTED_TYPES;
  }

}
