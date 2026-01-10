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

package infra.context.annotation.config;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import infra.core.io.PropertiesUtils;
import infra.core.io.UrlResource;
import infra.util.StringUtils;

/**
 * Provides access to meta-data written by the auto-configure annotation processor.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 02:48
 */
public class AutoConfigurationMetadata {

  protected static final String PATH = "META-INF/infra-autoconfigure-metadata.properties";

  private final Properties properties;

  AutoConfigurationMetadata(Properties properties) {
    this.properties = properties;
  }

  /**
   * Return {@code true} if the specified class name was processed by the annotation
   * processor.
   *
   * @param className the source class
   * @return if the class was processed
   */
  public boolean wasProcessed(String className) {
    return this.properties.containsKey(className);
  }

  /**
   * Get an {@link Integer} value from the meta-data.
   *
   * @param className the source class
   * @param key the meta-data key
   * @return the meta-data value or {@code null}
   */
  public @Nullable Integer getInteger(String className, String key) {
    return getInteger(className, key, null);
  }

  /**
   * Get an {@link Integer} value from the meta-data.
   *
   * @param className the source class
   * @param key the meta-data key
   * @param defaultValue the default value
   * @return the meta-data value or {@code defaultValue}
   */
  public @Nullable Integer getInteger(String className, String key, @Nullable Integer defaultValue) {
    String value = get(className, key);
    return value != null ? Integer.valueOf(value) : defaultValue;
  }

  /**
   * Get a {@link Set} value from the meta-data.
   *
   * @param className the source class
   * @param key the meta-data key
   * @return the meta-data value or {@code null}
   */
  public @Nullable Set<String> getSet(String className, String key) {
    return getSet(className, key, null);
  }

  /**
   * Get a {@link Set} value from the meta-data.
   *
   * @param className the source class
   * @param key the meta-data key
   * @param defaultValue the default value
   * @return the meta-data value or {@code defaultValue}
   */
  public @Nullable Set<String> getSet(String className, String key, @Nullable Set<String> defaultValue) {
    String value = get(className, key);
    return value != null ? StringUtils.commaDelimitedListToSet(value) : defaultValue;
  }

  /**
   * Get an {@link String} value from the meta-data.
   *
   * @param className the source class
   * @param key the meta-data key
   * @return the meta-data value or {@code null}
   */
  public @Nullable String get(String className, String key) {
    return get(className, key, null);
  }

  /**
   * Get an {@link String} value from the meta-data.
   *
   * @param className the source class
   * @param key the meta-data key
   * @param defaultValue the default value
   * @return the meta-data value or {@code defaultValue}
   */
  public @Nullable String get(String className, String key, @Nullable String defaultValue) {
    String value = this.properties.getProperty(className + "." + key);
    return value != null ? value : defaultValue;
  }

  // static

  public static AutoConfigurationMetadata load(@Nullable ClassLoader classLoader) {
    return load(classLoader, PATH);
  }

  public static AutoConfigurationMetadata load(@Nullable ClassLoader classLoader, String path) {
    try {
      Enumeration<URL> urls = classLoader != null
              ? classLoader.getResources(path)
              : ClassLoader.getSystemResources(path);
      Properties properties = new Properties();
      while (urls.hasMoreElements()) {
        properties.putAll(PropertiesUtils.loadProperties(new UrlResource(urls.nextElement())));
      }
      return valueOf(properties);
    }
    catch (IOException ex) {
      throw new IllegalArgumentException("Unable to load @ConditionalOnClass location [%s]".formatted(path), ex);
    }
  }

  public static AutoConfigurationMetadata valueOf(Properties properties) {
    return new AutoConfigurationMetadata(properties);
  }

}
