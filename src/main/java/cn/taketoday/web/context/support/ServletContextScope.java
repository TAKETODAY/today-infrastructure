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

package cn.taketoday.web.context.support;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.Scope;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import freemarker.template.utility.ObjectFactory;
import jakarta.servlet.ServletContext;

/**
 * {@link Scope} wrapper for a ServletContext, i.e. for global web application attributes.
 *
 * <p>This differs from traditional Spring singletons in that it exposes attributes in the
 * ServletContext. Those attributes will get destroyed whenever the entire application
 * shuts down, which might be earlier or later than the shutdown of the containing Spring
 * ApplicationContext.
 *
 * <p>The associated destruction mechanism relies on a
 * {@link cn.taketoday.web.context.ContextCleanupListener} being registered in
 * {@code web.xml}. Note that {@link cn.taketoday.web.context.ContextLoaderListener}
 * includes ContextCleanupListener's functionality.
 *
 * <p>This scope is registered as default scope with key
 * {@link cn.taketoday.web.context.WebApplicationContext#SCOPE_APPLICATION "application"}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.web.context.ContextCleanupListener
 * @since 4.0 2022/2/20 17:17
 */
public class ServletContextScope implements Scope, DisposableBean {

  private final ServletContext servletContext;

  private final Map<String, Runnable> destructionCallbacks = new LinkedHashMap<>();

  /**
   * Create a new Scope wrapper for the given ServletContext.
   *
   * @param servletContext the ServletContext to wrap
   */
  public ServletContextScope(ServletContext servletContext) {
    Assert.notNull(servletContext, "ServletContext must not be null");
    this.servletContext = servletContext;
  }

  @Override
  public Object get(String name, ObjectFactory<?> objectFactory) {
    Object scopedObject = this.servletContext.getAttribute(name);
    if (scopedObject == null) {
      scopedObject = objectFactory.getObject();
      this.servletContext.setAttribute(name, scopedObject);
    }
    return scopedObject;
  }

  @Override
  @Nullable
  public Object remove(String name) {
    Object scopedObject = this.servletContext.getAttribute(name);
    if (scopedObject != null) {
      synchronized(this.destructionCallbacks) {
        this.destructionCallbacks.remove(name);
      }
      this.servletContext.removeAttribute(name);
      return scopedObject;
    }
    else {
      return null;
    }
  }

  @Override
  public void registerDestructionCallback(String name, Runnable callback) {
    synchronized(this.destructionCallbacks) {
      this.destructionCallbacks.put(name, callback);
    }
  }

  @Override
  @Nullable
  public Object resolveContextualObject(String key) {
    return null;
  }

  @Override
  @Nullable
  public String getConversationId() {
    return null;
  }

  /**
   * Invoke all registered destruction callbacks.
   * To be called on ServletContext shutdown.
   *
   * @see cn.taketoday.web.context.ContextCleanupListener
   */
  @Override
  public void destroy() {
    synchronized(this.destructionCallbacks) {
      for (Runnable runnable : this.destructionCallbacks.values()) {
        runnable.run();
      }
      this.destructionCallbacks.clear();
    }
  }

}
