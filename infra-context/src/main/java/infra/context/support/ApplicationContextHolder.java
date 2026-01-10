/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.context.support;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

import infra.context.ApplicationContext;

/**
 * {@link ApplicationContext} holder
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/10/12 22:39
 */
public abstract class ApplicationContextHolder {

  private static final LinkedHashMap<String, ApplicationContext> contextMap = new LinkedHashMap<>();

  @Nullable
  public static ApplicationContext get(String id) {
    return contextMap.get(id);
  }

  public static Optional<ApplicationContext> optional(String id) {
    return Optional.ofNullable(contextMap.get(id));
  }

  public static ApplicationContext obtain(String id) {
    ApplicationContext context = get(id);
    if (context == null) {
      throw new IllegalStateException("No ApplicationContext: '" + id + "'");
    }
    return context;
  }

  /**
   * @return Returns: the previous ApplicationContext associated with id,
   * or null if there was no application for id.
   */
  @Nullable
  public static ApplicationContext register(String id, ApplicationContext context) {
    return contextMap.put(id, context);
  }

  /**
   * @return Returns: the previous ApplicationContext associated with id,
   * or null if there was no application for id.
   */
  @Nullable
  public static ApplicationContext register(ApplicationContext context) {
    return contextMap.put(context.getId(), context);
  }

  @Nullable
  public static ApplicationContext remove(String id) {
    return contextMap.remove(id);
  }

  public static void remove(ApplicationContext context) {
    contextMap.remove(context.getId());
  }

  // getLastStartupContext

  @Nullable
  public static ApplicationContext getLastStartupContext() {
    if (contextMap.isEmpty()) {
      return null;
    }
    return contextMap.values().iterator().next();
  }

  /**
   * Get all ApplicationContexts in this JVM
   *
   * @return all ApplicationContexts in this JVM
   */
  public static HashMap<String, ApplicationContext> getAll() {
    return contextMap;
  }

}

