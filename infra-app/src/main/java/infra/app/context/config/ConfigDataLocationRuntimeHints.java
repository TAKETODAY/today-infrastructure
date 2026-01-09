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

package infra.app.context.config;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.support.FilePatternResourceHintsRegistrar;
import infra.app.env.PropertySourceLoader;
import infra.lang.TodayStrategies;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ResourceUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation for application configuration.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FilePatternResourceHintsRegistrar
 * @since 4.0 2023/7/3 22:21
 */
class ConfigDataLocationRuntimeHints implements RuntimeHintsRegistrar {

  private static final Logger logger = LoggerFactory.getLogger(ConfigDataLocationRuntimeHints.class);

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    List<String> fileNames = getFileNames(classLoader);
    List<String> locations = getLocations(classLoader);
    List<String> extensions = getExtensions(classLoader);
    if (logger.isDebugEnabled()) {
      logger.debug("Registering application configuration hints for {}({}) at {}", fileNames, extensions, locations);
    }
    FilePatternResourceHintsRegistrar.forClassPathLocations(locations)
            .withFileExtensions(extensions)
            .withFilePrefixes(fileNames)
            .registerHints(hints.resources(), classLoader);
  }

  /**
   * Get the application file names to consider.
   *
   * @param classLoader the classloader to use
   * @return the configuration file names
   */
  protected List<String> getFileNames(@Nullable ClassLoader classLoader) {
    return Arrays.asList(StandardConfigDataLocationResolver.DEFAULT_CONFIG_NAMES);
  }

  /**
   * Get the locations to consider. A location is a classpath location that may or may
   * not use the standard {@code classpath:} prefix.
   *
   * @param classLoader the classloader to use
   * @return the configuration file locations
   */
  protected List<String> getLocations(@Nullable ClassLoader classLoader) {
    var classpathLocations = new ArrayList<String>();
    for (ConfigDataLocation candidate : ConfigDataEnvironment.DEFAULT_SEARCH_LOCATIONS) {
      for (ConfigDataLocation configDataLocation : candidate.split()) {
        String location = configDataLocation.getValue();
        if (location.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
          classpathLocations.add(location);
        }
      }
    }
    return classpathLocations;
  }

  /**
   * Get the application file extensions to consider. A valid extension starts with a
   * dot.
   *
   * @param classLoader the classloader to use
   * @return the configuration file extensions
   */
  protected List<String> getExtensions(@Nullable ClassLoader classLoader) {
    List<String> extensions = new ArrayList<>();
    List<PropertySourceLoader> loaders = getLoaderStrategies(classLoader).load(PropertySourceLoader.class);
    for (PropertySourceLoader propertySourceLoader : loaders) {
      for (String fileExtension : propertySourceLoader.getFileExtensions()) {
        String candidate = "." + fileExtension;
        if (!extensions.contains(candidate)) {
          extensions.add(candidate);
        }
      }
    }
    return extensions;
  }

  protected TodayStrategies getLoaderStrategies(@Nullable ClassLoader classLoader) {
    return TodayStrategies.forDefaultResourceLocation(classLoader);
  }

}
