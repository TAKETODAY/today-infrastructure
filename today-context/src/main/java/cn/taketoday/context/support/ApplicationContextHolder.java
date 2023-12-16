/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.support;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Nullable;

/**
 * {@link ApplicationContext} holder
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/10/12 22:39
 */
public final class ApplicationContextHolder {

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

