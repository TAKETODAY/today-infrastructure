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

package infra.app.context.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.support.FilePatternResourceHintsRegistrar;
import infra.app.env.PropertySourceLoader;
import infra.lang.Nullable;
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
