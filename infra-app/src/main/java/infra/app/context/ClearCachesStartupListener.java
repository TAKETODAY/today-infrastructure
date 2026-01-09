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
