/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.lang;

import cn.taketoday.core.DefaultStrategiesReader;
import cn.taketoday.core.StrategiesDetector;
import cn.taketoday.core.StrategiesReader;
import cn.taketoday.core.YamlStrategiesReader;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * today-framework Strategies
 * <p>General purpose factory loading mechanism for internal use within the framework.
 * <p>Reads a {@code today.strategies} file from the root of the library classpath,
 * and also allows for programmatically setting properties through {@link #setProperty}.
 * When checking a property, local entries are being checked first, then falling back
 * to JVM-level system properties through a {@link System#getProperty} check.
 *
 * @author TODAY 2021/9/5 13:57
 * @since 4.0
 */
public final class TodayStrategies extends StrategiesDetector {

  public static final String STRATEGIES_LOCATION = "classpath*:META-INF/today.strategies";
  public static final String KEY_STRATEGIES_LOCATION = "strategies.file.location";
  public static final String KEY_STRATEGIES_FILE_TYPE = "strategies.file.type";

  private static final TodayStrategies todayStrategies;

  static {
    final String strategiesFileType = System.getProperty(KEY_STRATEGIES_FILE_TYPE, Constant.DEFAULT);// default,yaml
    final String strategiesLocation = System.getProperty(KEY_STRATEGIES_LOCATION, STRATEGIES_LOCATION);
    StrategiesReader strategiesReader;
    if (Constant.DEFAULT.equals(strategiesFileType)) {
      strategiesReader = new DefaultStrategiesReader();
    }
    else if ("yaml".equals(strategiesFileType) || "yml".equals(strategiesFileType)) {
      strategiesReader = new YamlStrategiesReader();// org.yaml.snakeyaml.Yaml must present
    }
    else {
      try {
        strategiesReader = ReflectionUtils.newInstance(strategiesFileType);
      }
      catch (ClassNotFoundException e) {
        throw new UnsupportedOperationException("Unsupported strategies file type", e);
      }
    }
    todayStrategies = new TodayStrategies(strategiesReader, strategiesLocation);
  }

  private TodayStrategies(StrategiesReader reader, String strategiesLocation) {
    super(reader, strategiesLocation);
  }

  public static TodayStrategies getDetector() {
    return todayStrategies;
  }

  /**
   * Retrieve the flag for the given property key.
   *
   * @param key the property key
   * @return {@code true} if the property is set to "true", {@code} false
   * otherwise
   */
  public static boolean getFlag(String key) {
    String property = getProperty(key);
    return Boolean.parseBoolean(property);
  }

  /**
   * Retrieve the flag for the given property key.
   * <p>
   * If there isn't a key returns defaultFlag
   * </p>
   *
   * @param key the property key
   * @return {@code true} if the property is set to "true", {@code} false
   * otherwise ,If there isn't a key returns defaultFlag
   */
  public static boolean getFlag(String key, boolean defaultFlag) {
    String property = getProperty(key);
    return StringUtils.isEmpty(property) ? defaultFlag : Boolean.parseBoolean(property);
  }

  /**
   * Programmatically set a local flag to "true", overriding an
   * entry in the {@link #STRATEGIES_LOCATION} file (if any).
   *
   * @param key the property key
   */
  public static void setFlag(String key) {
    getDetector().getStrategies().set(key, Boolean.TRUE.toString());
  }

  /**
   * Programmatically set a local property, overriding an entry in the
   * {@link #STRATEGIES_LOCATION} file (if any).
   *
   * @param key the property key
   * @param value the associated property value, or {@code null} to reset it
   */
  public static void setProperty(String key, @Nullable String value) {
    if (value != null) {
      getDetector().getStrategies().set(key, value);
    }
    else {
      getDetector().getStrategies().remove(key);
    }
  }

  /**
   * Retrieve the property value for the given key, checking local
   * properties first and falling back to JVM-level system properties.
   *
   * @param key the property key
   * @return the associated property value, or {@code null} if none found
   */
  @Nullable
  public static String getProperty(String key) {
    String value = getDetector().getFirst(key);

    if (value == null) {
      try {
        value = System.getProperty(key);
      }
      catch (Throwable ex) {
        System.err.println("Could not retrieve system property '" + key + "': " + ex);
      }
    }

    return value;
  }

  /**
   * Retrieve the property value for the given key, checking local
   * properties first and falling back to JVM-level system properties.
   *
   * @param key the name of the system property.
   * @param def a default value.
   * @return the string value of the system property,
   * or the default value if there is no property with that key.
   * @see #setProperty
   * @see java.lang.System#getProperties()
   */
  public static String getProperty(String key, String def) {
    String property = getProperty(key);
    if (property == null) {
      property = def;
    }
    return property;
  }

  /**
   * Determines the integer value of the property with the specified name.
   *
   * <p>The first argument is treated as the name of a system property.
   * System properties are accessible through the {@link
   * java.lang.System#getProperty(java.lang.String)} method. The
   * string value of this property is then interpreted as an integer
   * value using the grammar supported by {@link Integer#decode decode} and
   * an {@code Integer} object representing this value is returned.
   *
   * <p>If there is no property with the specified name, if the
   * specified name is empty or {@code null}, or if the property
   * does not have the correct numeric format, then {@code null} is
   * returned.
   *
   * <p>In other words, this method returns an {@code Integer}
   * object equal to the value of:
   *
   * <blockquote>
   * {@code getInteger(nm, null)}
   * </blockquote>
   *
   * @param key property name.
   * @return the {@code Integer} value of the property.
   * @throws SecurityException for the same reasons as
   * {@link System#getProperty(String) System.getProperty}
   * @see java.lang.System#getProperty(java.lang.String)
   * @see java.lang.System#getProperty(java.lang.String, java.lang.String)
   */
  @Nullable
  public static Integer getInteger(String key) {
    return getInteger(key, null);
  }

  /**
   * Determines the integer value of the system property with the
   * specified name.
   *
   * <p>The first argument is treated as the name of a system
   * property.  System properties are accessible through the {@link
   * java.lang.System#getProperty(java.lang.String)} method. The
   * string value of this property is then interpreted as an integer
   * value using the grammar supported by {@link Integer#decode decode} and
   * an {@code Integer} object representing this value is returned.
   *
   * <p>The second argument is the default value. An {@code Integer} object
   * that represents the value of the second argument is returned if there
   * is no property of the specified name, if the property does not have
   * the correct numeric format, or if the specified name is empty or
   * {@code null}.
   *
   * <p>In other words, this method returns an {@code Integer} object
   * equal to the value of:
   *
   * <blockquote>
   * {@code getInteger(nm, new Integer(val))}
   * </blockquote>
   *
   * but in practice it may be implemented in a manner such as:
   *
   * <blockquote><pre>
   * Integer result = getInteger(nm, null);
   * return (result == null) ? new Integer(val) : result;
   * </pre></blockquote>
   *
   * to avoid the unnecessary allocation of an {@code Integer}
   * object when the default value is not needed.
   *
   * @param key property name.
   * @param val default value.
   * @return the {@code Integer} value of the property.
   * @throws SecurityException for the same reasons as
   * {@link System#getProperty(String) System.getProperty}
   * @see java.lang.System#getProperty(java.lang.String)
   * @see java.lang.System#getProperty(java.lang.String, java.lang.String)
   */
  public static int getInt(String key, int val) {
    Integer result = getInteger(key, null);
    return (result == null) ? Integer.valueOf(val) : result;
  }

  /**
   * Returns the integer value of the property with the
   * specified name.  The first argument is treated as the name of a
   * system property.  System properties are accessible through the
   * {@link java.lang.System#getProperty(java.lang.String)} method.
   * The string value of this property is then interpreted as an
   * integer value, as per the {@link Integer#decode decode} method,
   * and an {@code Integer} object representing this value is
   * returned; in summary:
   *
   * <ul><li>If the property value begins with the two ASCII characters
   *         {@code 0x} or the ASCII character {@code #}, not
   *      followed by a minus sign, then the rest of it is parsed as a
   *      hexadecimal integer exactly as by the method
   *      {@link Integer#valueOf(java.lang.String, int)} with radix 16.
   * <li>If the property value begins with the ASCII character
   *     {@code 0} followed by another character, it is parsed as an
   *     octal integer exactly as by the method
   *     {@link Integer#valueOf(java.lang.String, int)} with radix 8.
   * <li>Otherwise, the property value is parsed as a decimal integer
   * exactly as by the method {@link Integer#valueOf(java.lang.String, int)}
   * with radix 10.
   * </ul>
   *
   * <p>The second argument is the default value. The default value is
   * returned if there is no property of the specified name, if the
   * property does not have the correct numeric format, or if the
   * specified name is empty or {@code null}.
   *
   * @param key property name.
   * @param val default value.
   * @return the {@code Integer} value of the property.
   * {@link System#getProperty(String) System.getProperty}
   * @see System#getProperty(java.lang.String)
   * @see System#getProperty(java.lang.String, java.lang.String)
   * @see Integer#decode(String)
   */
  @Nullable
  public static Integer getInteger(String key, @Nullable Integer val) {
    String v = null;
    try {
      v = getProperty(key);
    }
    catch (IllegalArgumentException | NullPointerException ignored) { }
    if (v != null) {
      try {
        return Integer.decode(v);
      }
      catch (NumberFormatException ignored) { }
    }
    return val;
  }

}
