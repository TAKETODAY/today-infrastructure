/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context.aot;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * Utilities for loading generated maps.
 *
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class GeneratedMapUtils {

  private GeneratedMapUtils() { }

  /**
   * Load a generated map.
   *
   * @param className the name of the class in which the static method resides
   * @param methodName the name of the static method to invoke
   * @return an unmodifiable map retrieved from a static method
   */
  @SuppressWarnings({ "rawtypes" })
  static Map loadMap(String className, String methodName) {
    try {
      Class<?> clazz = ClassUtils.forName(className, null);
      Method method = ReflectionUtils.findMethod(clazz, methodName);
      Assert.state(method != null, () -> "No %s() method found in %s".formatted(methodName, className));
      Map map = (Map) ReflectionUtils.invokeMethod(method, null);
      return Collections.unmodifiableMap(map);
    }
    catch (IllegalStateException ex) {
      throw ex;
    }
    catch (Exception ex) {
      throw new IllegalStateException("Failed to invoke %s() method on %s".formatted(methodName, className), ex);
    }
  }

}
