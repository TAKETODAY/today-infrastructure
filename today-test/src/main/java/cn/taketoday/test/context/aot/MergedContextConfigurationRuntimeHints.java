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
import java.util.Arrays;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.util.ClassUtils;

import static cn.taketoday.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static cn.taketoday.util.ResourceUtils.CLASSPATH_URL_PREFIX;

/**
 * {@code MergedContextConfigurationRuntimeHints} registers run-time hints for
 * standard functionality in the <em>Spring TestContext Framework</em> based on
 * {@link MergedContextConfiguration}.
 *
 * <p>This class interacts with {@code cn.taketoday.test.context.web.WebMergedContextConfiguration}
 * via reflection to avoid a package cycle.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class MergedContextConfigurationRuntimeHints {

  private static final String SLASH = "/";

  private static final String WEB_MERGED_CONTEXT_CONFIGURATION_CLASS_NAME =
          "cn.taketoday.test.context.web.WebMergedContextConfiguration";

  private static final String GET_RESOURCE_BASE_PATH_METHOD_NAME = "getResourceBasePath";

  private static final Class<?> webMergedContextConfigurationClass = loadWebMergedContextConfigurationClass();

  private static final Method getResourceBasePathMethod = loadGetResourceBasePathMethod();

  public void registerHints(RuntimeHints runtimeHints, MergedContextConfiguration mergedConfig, ClassLoader classLoader) {
    // @ContextConfiguration(loader = ...)
    ContextLoader contextLoader = mergedConfig.getContextLoader();
    if (contextLoader != null) {
      registerDeclaredConstructors(contextLoader.getClass(), runtimeHints);
    }

    // @ContextConfiguration(initializers = ...)
    mergedConfig.getContextInitializerClasses()
            .forEach(clazz -> registerDeclaredConstructors(clazz, runtimeHints));

    // @ContextConfiguration(locations = ...)
    registerClasspathResources(mergedConfig.getLocations(), runtimeHints, classLoader);

    // @TestPropertySource(locations = ... )
    registerClasspathResources(mergedConfig.getPropertySourceLocations(), runtimeHints, classLoader);

    // @WebAppConfiguration(value = ...)
    if (webMergedContextConfigurationClass.isInstance(mergedConfig)) {
      String resourceBasePath = null;
      try {
        resourceBasePath = (String) getResourceBasePathMethod.invoke(mergedConfig);
      }
      catch (Exception ex) {
        throw new IllegalStateException(
                "Failed to invoke WebMergedContextConfiguration#getResourceBasePath()", ex);
      }
      registerClasspathResourceDirectoryStructure(resourceBasePath, runtimeHints);
    }
  }

  private void registerDeclaredConstructors(Class<?> type, RuntimeHints runtimeHints) {
    runtimeHints.reflection().registerType(type, INVOKE_DECLARED_CONSTRUCTORS);
  }

  private void registerClasspathResources(String[] paths, RuntimeHints runtimeHints, ClassLoader classLoader) {
    DefaultResourceLoader resourceLoader = new DefaultResourceLoader(classLoader);
    Arrays.stream(paths)
            .filter(path -> path.startsWith(CLASSPATH_URL_PREFIX))
            .map(resourceLoader::getResource)
            .forEach(runtimeHints.resources()::registerResource);
  }

  private void registerClasspathResourceDirectoryStructure(String directory, RuntimeHints runtimeHints) {
    if (directory.startsWith(CLASSPATH_URL_PREFIX)) {
      String pattern = directory.substring(CLASSPATH_URL_PREFIX.length());
      if (pattern.startsWith(SLASH)) {
        pattern = pattern.substring(1);
      }
      if (!pattern.endsWith(SLASH)) {
        pattern += SLASH;
      }
      pattern += "*";
      runtimeHints.resources().registerPattern(pattern);
    }
  }

  private static Class<?> loadWebMergedContextConfigurationClass() {
    try {
      return ClassUtils.forName(WEB_MERGED_CONTEXT_CONFIGURATION_CLASS_NAME,
              MergedContextConfigurationRuntimeHints.class.getClassLoader());
    }
    catch (ClassNotFoundException | LinkageError ex) {
      throw new IllegalStateException(
              "Failed to load class " + WEB_MERGED_CONTEXT_CONFIGURATION_CLASS_NAME, ex);
    }
  }

  private static Method loadGetResourceBasePathMethod() {
    try {
      return webMergedContextConfigurationClass.getMethod(GET_RESOURCE_BASE_PATH_METHOD_NAME);
    }
    catch (Exception ex) {
      throw new IllegalStateException(
              "Failed to load method WebMergedContextConfiguration#getResourceBasePath()", ex);
    }
  }

}
