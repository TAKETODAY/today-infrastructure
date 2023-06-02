/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.socket;

import java.lang.reflect.Field;
import java.util.Map;

import cn.taketoday.web.servlet.ContextLoader;
import cn.taketoday.web.servlet.WebApplicationContext;

/**
 * General test utilities for manipulating the {@link ContextLoader}.
 *
 * @author Phillip Webb
 */
public class ContextLoaderTestUtils {

  private static Map<ClassLoader, WebApplicationContext> currentContextPerThread =
          getCurrentContextPerThreadFromContextLoader();

  public static void setCurrentWebApplicationContext(WebApplicationContext applicationContext) {
    setCurrentWebApplicationContext(Thread.currentThread().getContextClassLoader(), applicationContext);
  }

  public static void setCurrentWebApplicationContext(ClassLoader classLoader,
          WebApplicationContext applicationContext) {

    if (applicationContext != null) {
      currentContextPerThread.put(classLoader, applicationContext);
    }
    else {
      currentContextPerThread.remove(classLoader);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<ClassLoader, WebApplicationContext> getCurrentContextPerThreadFromContextLoader() {
    try {
      Field field = ContextLoader.class.getDeclaredField("currentContextPerThread");
      field.setAccessible(true);
      return (Map<ClassLoader, WebApplicationContext>) field.get(null);
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

}
