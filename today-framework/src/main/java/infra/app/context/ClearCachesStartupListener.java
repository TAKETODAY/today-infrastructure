/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.context;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.time.Duration;

import infra.app.ApplicationStartupListener;
import infra.context.ConfigurableApplicationContext;
import infra.util.ReflectionUtils;

/**
 * Cleanup caches once the context is loaded.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/11/26 12:38
 */
class ClearCachesStartupListener implements ApplicationStartupListener {

  @Override
  public void ready(ConfigurableApplicationContext context, @Nullable Duration timeTaken) {
    ReflectionUtils.clearCache();
    clearClassLoaderCaches(Thread.currentThread().getContextClassLoader());
  }

  private void clearClassLoaderCaches(ClassLoader classLoader) {
    if (classLoader == null) {
      return;
    }
    try {
      Method clearCacheMethod = classLoader.getClass().getDeclaredMethod("clearCache");
      clearCacheMethod.invoke(classLoader);
    }
    catch (Exception ex) {
      // Ignore
    }
    clearClassLoaderCaches(classLoader.getParent());
  }

}
