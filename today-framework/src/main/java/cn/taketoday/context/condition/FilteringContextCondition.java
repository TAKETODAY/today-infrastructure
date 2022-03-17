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

package cn.taketoday.context.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 16:07
 */
abstract class FilteringContextCondition extends ContextCondition {

  protected final List<String> filter(
          Collection<String> classNames, ClassNameFilter classNameFilter, ClassLoader classLoader) {
    if (CollectionUtils.isEmpty(classNames)) {
      return Collections.emptyList();
    }
    ArrayList<String> matches = new ArrayList<>(classNames.size());
    for (String candidate : classNames) {
      if (classNameFilter.matches(candidate, classLoader)) {
        matches.add(candidate);
      }
    }
    return matches;
  }

  /**
   * Slightly faster variant of {@link ClassUtils#forName(String, ClassLoader)} that
   * doesn't deal with primitives, arrays or inner types.
   *
   * @param className the class name to resolve
   * @param classLoader the class loader to use
   * @return a resolved class
   * @throws ClassNotFoundException if the class cannot be found
   */
  protected static Class<?> resolve(String className, ClassLoader classLoader) throws ClassNotFoundException {
    if (classLoader != null) {
      return Class.forName(className, false, classLoader);
    }
    return Class.forName(className);
  }

  protected enum ClassNameFilter {

    PRESENT {
      @Override
      public boolean matches(String className, ClassLoader classLoader) {
        return isPresent(className, classLoader);
      }

    },

    MISSING {
      @Override
      public boolean matches(String className, ClassLoader classLoader) {
        return !isPresent(className, classLoader);
      }

    };

    abstract boolean matches(String className, ClassLoader classLoader);

    static boolean isPresent(String className, ClassLoader classLoader) {
      if (classLoader == null) {
        classLoader = ClassUtils.getDefaultClassLoader();
      }
      try {
        resolve(className, classLoader);
        return true;
      }
      catch (Throwable ex) {
        return false;
      }
    }

  }
}
