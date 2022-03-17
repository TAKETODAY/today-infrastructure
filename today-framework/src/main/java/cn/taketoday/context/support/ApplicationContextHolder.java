/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.support;

import java.util.HashMap;
import java.util.LinkedHashMap;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Nullable;

/**
 * @author TODAY 2021/10/12 22:39
 * @since 4.0
 */
public final class ApplicationContextHolder {
  private static final LinkedHashMap<String, ApplicationContext> contextMap = new LinkedHashMap<>();

  @Nullable
  public static ApplicationContext get(String applicationName) {
    return contextMap.get(applicationName);
  }

  /**
   * @return Returns: the previous ApplicationContext associated with name,
   * or null if there was no application for name.
   */
  public static ApplicationContext register(String applicationName, ApplicationContext context) {
    return contextMap.put(applicationName, context);
  }

  /**
   * @return Returns: the previous ApplicationContext associated with name,
   * or null if there was no application for name.
   */
  public static ApplicationContext register(ApplicationContext context) {
    return contextMap.put(context.getApplicationName(), context);
  }

  public static ApplicationContext remove(String applicationName) {
    return contextMap.remove(applicationName);
  }

  static void remove(ApplicationContext context) {
    contextMap.remove(context.getApplicationName());
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

