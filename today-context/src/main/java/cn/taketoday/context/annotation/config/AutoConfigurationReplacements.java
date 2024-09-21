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

package cn.taketoday.context.annotation.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.core.io.UrlResource;
import cn.taketoday.lang.Assert;

/**
 * Contains auto-configuration replacements used to handle deprecated or moved
 * auto-configurations which may still be referenced by
 * {@link AutoConfigureBefore @AutoConfigureBefore},
 * {@link AutoConfigureAfter @AutoConfigureAfter} or exclusions.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
final class AutoConfigurationReplacements {

  private static final String LOCATION = "META-INF/config/%s.replacements";

  private final Map<String, String> replacements;

  private AutoConfigurationReplacements(Map<String, String> replacements) {
    this.replacements = Map.copyOf(replacements);
  }

  Set<String> replaceAll(Set<String> classNames) {
    Set<String> replaced = new LinkedHashSet<>(classNames.size());
    for (String className : classNames) {
      replaced.add(replace(className));
    }
    return replaced;
  }

  String replace(String className) {
    return this.replacements.getOrDefault(className, className);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return this.replacements.equals(((AutoConfigurationReplacements) obj).replacements);
  }

  @Override
  public int hashCode() {
    return this.replacements.hashCode();
  }

  /**
   * Loads the relocations from the classpath. Relactions are stored in files named
   * {@code META-INF/config/full-qualified-annotation-name.replacements} on the
   * classpath. The file is loaded using {@link Properties#load(java.io.InputStream)}
   * with each entry containing an auto-configuration class name as the key and the
   * replacement class name as the value.
   *
   * @param annotation annotation to load
   * @param classLoader class loader to use for loading
   * @return list of names of annotated classes
   */
  static AutoConfigurationReplacements load(Class<?> annotation, ClassLoader classLoader) {
    Assert.notNull(annotation, "'annotation' is required");
    ClassLoader classLoaderToUse = decideClassloader(classLoader);
    String location = String.format(LOCATION, annotation.getName());
    Enumeration<URL> urls = findUrlsInClasspath(classLoaderToUse, location);
    Map<String, String> replacements = new HashMap<>();
    while (urls.hasMoreElements()) {
      URL url = urls.nextElement();
      replacements.putAll(readReplacements(url));
    }
    return new AutoConfigurationReplacements(replacements);
  }

  private static ClassLoader decideClassloader(ClassLoader classLoader) {
    if (classLoader == null) {
      return ImportCandidates.class.getClassLoader();
    }
    return classLoader;
  }

  private static Enumeration<URL> findUrlsInClasspath(ClassLoader classLoader, String location) {
    try {
      return classLoader.getResources(location);
    }
    catch (IOException ex) {
      throw new IllegalArgumentException("Failed to load configurations from location [%s]".formatted(location), ex);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static Map<String, String> readReplacements(URL url) {
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new UrlResource(url).getInputStream(), StandardCharsets.UTF_8))) {
      Properties properties = new Properties();
      properties.load(reader);
      return (Map) properties;
    }
    catch (IOException ex) {
      throw new IllegalArgumentException("Unable to load replacements from location [%s]".formatted(url), ex);
    }
  }

}
