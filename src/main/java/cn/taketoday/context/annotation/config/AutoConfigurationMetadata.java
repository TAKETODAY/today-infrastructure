/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.annotation.config;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.core.io.UrlBasedResource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Provides access to meta-data written by the auto-configure annotation processor.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 02:48
 */
public class AutoConfigurationMetadata {
  protected static final String PATH = "META-INF/autoconfigure-metadata.properties";

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
  public Integer getInteger(String className, String key) {
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
  public Integer getInteger(String className, String key, Integer defaultValue) {
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
  public Set<String> getSet(String className, String key) {
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
  public Set<String> getSet(String className, String key, Set<String> defaultValue) {
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
  public String get(String className, String key) {
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
  public String get(String className, String key, String defaultValue) {
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
        properties.putAll(PropertiesUtils.loadProperties(new UrlBasedResource(urls.nextElement())));
      }
      return valueOf(properties);
    }
    catch (IOException ex) {
      throw new IllegalArgumentException("Unable to load @ConditionalOnClass location [" + path + "]", ex);
    }
  }

  public static AutoConfigurationMetadata valueOf(Properties properties) {
    return new AutoConfigurationMetadata(properties);
  }

}
