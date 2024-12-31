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

package infra.test.context.aot;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import infra.aot.hint.ResourceHints;
import infra.aot.hint.RuntimeHints;
import infra.core.io.ClassPathResource;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.PropertySourceDescriptor;
import infra.core.io.Resource;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.context.ContextLoader;
import infra.test.context.MergedContextConfiguration;
import infra.util.ClassUtils;

import static infra.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static infra.core.io.PatternResourceLoader.CLASSPATH_ALL_URL_PREFIX;
import static infra.util.ResourceUtils.CLASSPATH_URL_PREFIX;

/**
 * {@code MergedContextConfigurationRuntimeHints} registers run-time hints for
 * standard functionality in the <em>Infra TestContext Framework</em> based on
 * {@link MergedContextConfiguration}.
 *
 * <p>This class interacts with {@code infra.test.context.web.WebMergedContextConfiguration}
 * via reflection to avoid a package cycle.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class MergedContextConfigurationRuntimeHints {

  private static final String SLASH = "/";

  private static final String WEB_MERGED_CONTEXT_CONFIGURATION_CLASS_NAME =
          "infra.test.context.web.WebMergedContextConfiguration";

  private static final String GET_RESOURCE_BASE_PATH_METHOD_NAME = "getResourceBasePath";

  private static final Class<?> webMergedContextConfigurationClass = loadWebMergedContextConfigurationClass();

  private static final Method getResourceBasePathMethod = loadGetResourceBasePathMethod();

  private final Logger logger = LoggerFactory.getLogger(getClass());

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
    registerClasspathResources("@ContextConfiguration", mergedConfig.getLocations(), runtimeHints, classLoader);

    for (PropertySourceDescriptor descriptor : mergedConfig.getPropertySourceDescriptors()) {
      // @TestPropertySource(locations = ...)
      registerClasspathResources("@TestPropertySource", descriptor.locations(), runtimeHints, classLoader);

      // @TestPropertySource(factory = ...)
      Class<?> factoryClass = descriptor.propertySourceFactory();
      if (factoryClass != null) {
        registerDeclaredConstructors(factoryClass, runtimeHints);
      }
    }

    // @WebAppConfiguration(value = ...)
    if (webMergedContextConfigurationClass.isInstance(mergedConfig)) {
      String resourceBasePath;
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

  private void registerClasspathResources(String annotation, String[] locations, RuntimeHints runtimeHints, ClassLoader classLoader) {
    registerClasspathResources(annotation, Arrays.asList(locations), runtimeHints, classLoader);
  }

  private void registerClasspathResources(String annotation, List<String> locations, RuntimeHints runtimeHints, ClassLoader classLoader) {
    DefaultResourceLoader resourceLoader = new DefaultResourceLoader(classLoader);
    ResourceHints resourceHints = runtimeHints.resources();
    for (String location : locations) {
      if (location.startsWith(CLASSPATH_ALL_URL_PREFIX) ||
              (location.startsWith(CLASSPATH_URL_PREFIX) && (location.contains("*") || location.contains("?")))) {

        if (logger.isWarnEnabled()) {
          logger.warn("""
                  Runtime hint registration is not supported for the 'classpath*:' \
                  prefix or wildcards in %s locations. Please manually register a \
                  resource hint for each location represented by '%s'."""
                  .formatted(annotation, location));
        }
      }
      else {
        Resource resource = resourceLoader.getResource(location);
        if (resource instanceof ClassPathResource classPathResource && classPathResource.exists()) {
          resourceHints.registerPattern(classPathResource.getPath());
        }
      }
    }
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
      pattern += "**";
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
