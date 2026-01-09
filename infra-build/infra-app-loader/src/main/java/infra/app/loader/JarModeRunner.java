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

package infra.app.loader;

import java.lang.reflect.Constructor;
import java.util.List;

import infra.app.loader.jarmode.JarMode;
import org.jspecify.annotations.Nullable;
import infra.lang.TodayStrategies;
import infra.util.ClassUtils;
import infra.util.ExceptionUtils;

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
    Class<JarMode> jarModeClass = ClassUtils.load("infra.jarmode.layertools.LayerToolsJarMode");
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
