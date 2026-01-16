/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.server.context;

import org.jspecify.annotations.Nullable;

import infra.context.ApplicationContext;
import infra.util.ObjectUtils;
import infra.web.server.WebServer;

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
    return context instanceof WebServerApplicationContext serverCtx
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
  @Nullable
  static String getServerNamespace(ApplicationContext context) {
    return context instanceof WebServerApplicationContext serverCtx
            ? serverCtx.getServerNamespace() : null;
  }

}
