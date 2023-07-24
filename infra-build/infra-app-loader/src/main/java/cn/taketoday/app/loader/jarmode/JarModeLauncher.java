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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.app.loader.jarmode;

import java.util.List;

import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.ClassUtils;

/**
 * Delegate class used to launch the fat jar in a specific mode.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class JarModeLauncher {

  static final String DISABLE_SYSTEM_EXIT = JarModeLauncher.class.getName() + ".DISABLE_SYSTEM_EXIT";

  public static void main(String[] args) {
    String mode = System.getProperty("jarmode");
    List<JarMode> candidates = TodayStrategies.find(JarMode.class, ClassUtils.getDefaultClassLoader());
    for (JarMode candidate : candidates) {
      if (candidate.accepts(mode)) {
        candidate.run(mode, args);
        return;
      }
    }
    System.err.println("Unsupported jarmode '" + mode + "'");
    if (!Boolean.getBoolean(DISABLE_SYSTEM_EXIT)) {
      System.exit(1);
    }
  }

}
