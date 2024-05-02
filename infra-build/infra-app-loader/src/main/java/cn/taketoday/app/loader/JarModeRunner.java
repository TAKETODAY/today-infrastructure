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

package cn.taketoday.app.loader;

import java.lang.reflect.Constructor;
import java.util.List;

import cn.taketoday.app.loader.jarmode.JarMode;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ExceptionUtils;

/**
 * Delegate class used to run the nested jar in a specific mode.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class JarModeRunner {

  static final String DISABLE_SYSTEM_EXIT = JarModeRunner.class.getName() + ".DISABLE_SYSTEM_EXIT";

  private JarModeRunner() {

  }

  static void main(String[] args) {
    String mode = System.getProperty("jarmode");
    List<JarMode> candidates = TodayStrategies.find(JarMode.class, ClassUtils.getDefaultClassLoader());
    for (JarMode candidate : candidates) {
      if (tryRun(candidate, mode, args)) {
        return;
      }
    }

    JarMode jarMode = findDefault();
    if (jarMode != null && tryRun(jarMode, mode, args)) {
      return;
    }

    System.err.println("Unsupported jarmode '" + mode + "'");
    if (!Boolean.getBoolean(DISABLE_SYSTEM_EXIT)) {
      System.exit(1);
    }
  }

  private static boolean tryRun(JarMode candidate, String mode, String[] args) {
    if (candidate.accepts(mode)) {
      candidate.run(mode, args);
      return true;
    }
    return false;
  }

  @Nullable
  private static JarMode findDefault() {
    Class<JarMode> jarModeClass = ClassUtils.load("cn.taketoday.jarmode.layertools.LayerToolsJarMode");
    if (jarModeClass != null) {
      try {
        Constructor<JarMode> constructor = jarModeClass.getConstructor();
        return constructor.newInstance();
      }
      catch (Exception e) {
        throw ExceptionUtils.sneakyThrow(e);
      }
    }
    return null;
  }
}
