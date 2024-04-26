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

package cn.taketoday.web.server.context;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.server.WebServer;

/**
 * Interface to be implemented by {@link ApplicationContext application contexts} that
 * create and manage the lifecycle of an embedded {@link WebServer}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface WebServerApplicationContext extends ApplicationContext {

  /**
   * Returns the {@link WebServer} that was created by the context or {@code null} if
   * the server has not yet been created.
   *
   * @return the web server
   */
  @Nullable
  WebServer getWebServer();

  /**
   * Returns the namespace of the web server application context or {@code null} if no
   * namespace has been set. Used for disambiguation when multiple web servers are
   * running in the same application (for example a management context running on a
   * different port).
   *
   * @return the server namespace
   */
  @Nullable
  String getServerNamespace();

  /**
   * Returns {@code true} if the specified context is a
   * {@link WebServerApplicationContext} with a matching server namespace.
   *
   * @param context the context to check
   * @param serverNamespace the server namespace to match against
   * @return {@code true} if the server namespace of the context matches
   */
  static boolean hasServerNamespace(ApplicationContext context, String serverNamespace) {
    return (context instanceof WebServerApplicationContext serverCtx)
            && ObjectUtils.nullSafeEquals(serverCtx.getServerNamespace(), serverNamespace);
  }

  /**
   * Returns the server namespace if the specified context is a
   * {@link WebServerApplicationContext}.
   *
   * @param context the context
   * @return the server namespace or {@code null} if the context is not a
   * {@link WebServerApplicationContext}
   */
  static String getServerNamespace(ApplicationContext context) {
    return (context instanceof WebServerApplicationContext serverCtx)
            ? serverCtx.getServerNamespace() : null;

  }

}
