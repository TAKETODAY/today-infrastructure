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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * today-framework Strategies
 * <p>General purpose factory loading mechanism for internal use within the framework.
 * <p>Reads a {@code META-INF/today-strategies.properties} file from the root of the library classpath,
 * and also allows for programmatically setting properties through {@link #setProperty}.
 * When checking a property, local entries are being checked first, then falling back
 * to JVM-level system properties through a {@link System#getProperty} check.
 * <p>
 * Get keyed strategies
 * <p>
 * Like Framework's SpringFactoriesLoader
 *
 * @author TODAY 2021/9/5 13:57
 * @since 4.0
 */
public final class TodayStrategies {
  private static final Logger log = LoggerFactory.getLogger(TodayStrategies.class);

  public static final String STRATEGIES_LOCATION = "META-INF/today-strategies.properties";

  static final ConcurrentReferenceHashMap<ClassLoader, MultiValueMap<String, String>>
          strategiesCache = new ConcurrentReferenceHashMap<>();

  private static final String PROPERTIES_RESOURCE_LOCATION = "today.properties";

  private static final Properties localProperties = new Properties();

  static {
    try {
      ClassLoader cl = TodayStrategies.class.getClassLoader();
      URL url = (cl != null ? cl.getResource(PROPERTIES_RESOURCE_LOCATION) :
                 ClassLoader.getSystemResource(PROPERTIES_RESOURCE_LOCATION));
      if (url != null) {
        try (InputStream is = url.openStream()) {
          localProperties.load(is);
        }
      }
    }
    catch (IOException ex) {
      System.err.println("Could not load 'today.properties' file from local classpath: " + ex);
    }
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
   * entry in the {@link #PROPERTIES_RESOURCE_LOCATION} file (if any).
   *
   * @param key the property key
   */
  public static void setFlag(String key) {
    localProperties.put(key, Boolean.TRUE.toString());
  }

  /**
   * Programmatically set a local property, overriding an entry in the
   * {@link #PROPERTIES_RESOURCE_LOCATION} file (if any).
   *
   * @param key the property key
   * @param value the associated property value, or {@code null} to reset it
   */
  public static void setProperty(String key, @Nullable String value) {
    if (value != null) {
      localProperties.setProperty(key, value);
    }
    else {
      localProperties.remove(key);
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
    String value = localProperties.getProperty(key);

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

  //---------------------------------------------------------------------
  // Strategies
  //---------------------------------------------------------------------

  /**
   * get first strategy
   *
   * @see #get(Class)
   */
  @Nullable
  public static String getFirst(String strategyKey) {
    return CollectionUtils.firstElement(get(strategyKey, null));
  }

  /**
   * get first strategy
   *
   * @see #get(Class)
   */
  public static <T> T getFirst(Class<T> strategyClass, @Nullable Supplier<T> defaultValue) {
    T first = CollectionUtils.firstElement(get(strategyClass, (ClassLoader) null));
    if (first == null && defaultValue != null) {
      return defaultValue.get();
    }
    return first;
  }

  /**
   * get none repeatable strategies by given class
   *
   * @param strategyClass strategy class
   * @param <T> target type
   * @return returns none repeatable strategies by given class
   */
  public static <T> List<T> get(Class<T> strategyClass) {
    return get(strategyClass, (ClassLoader) null);
  }

  /**
   * get none repeatable strategies by given class
   * <p>
   * strategies must be an instance of given strategy class
   * </p>
   *
   * @param strategyClass strategy class
   * @param <T> target type
   * @return returns none repeatable strategies by given class
   */
  public static <T> List<T> get(
          Class<T> strategyClass, @Nullable ClassLoader classLoader) {
    Assert.notNull(strategyClass, "strategy-class must not be null");
    return get(strategyClass, classLoader, strategy -> {
      try {
        return ReflectionUtils.accessibleConstructor(strategy).newInstance();
      }
      catch (Throwable ex) {
        throw new IllegalArgumentException(
                "Unable to instantiate factory class [" + strategyClass.getName()
                        + "] for factory type [" + strategy.getName() + "]", ex);
      }
    });
  }

  /**
   * get collection of strategies
   *
   * @param strategyClass strategy super class
   * @param converter converter to convert strategyImpl to T
   * @param <T> return type
   * @return collection of strategies
   */
  public static <T> List<T> get(Class<T> strategyClass, Function<Class<T>, T> converter) {
    return get(strategyClass, null, converter);
  }

  /**
   * get collection of strategies
   *
   * @param strategyClass strategy super class
   * @param converter converter to convert strategyImpl to T
   * @param <T> return type
   * @return collection of strategies
   */
  public static <T> List<T> get(
          Class<T> strategyClass, @Nullable ClassLoader classLoader, Function<Class<T>, T> converter) {
    Assert.notNull(converter, "converter is required");
    Assert.notNull(strategyClass, "strategy-class is required");
    if (classLoader == null) {
      classLoader = TodayStrategies.class.getClassLoader();
    }
    // get class list by class full name
    List<String> strategies = get(strategyClass.getName(), classLoader);
    if (log.isTraceEnabled()) {
      log.trace("Loaded [{}] strategies: {}", strategyClass.getName(), strategies);
    }

    if (strategies.isEmpty()) {
      return new ArrayList<>();
    }

    ArrayList<T> ret = new ArrayList<>(strategies.size());
    for (String strategy : strategies) {
      Class<T> strategyImpl = ClassUtils.resolveClassName(strategy, classLoader);
      if (strategyImpl.isAssignableFrom(strategyImpl)) {
        T converted = converter.apply(strategyImpl);
        if (converted != null) {
          ret.add(converted);
        }
      }
      else {
        throw new IllegalArgumentException("Class [" + strategy +
                "] is not assignable to strategy type [" + strategyClass.getName() + "]");
      }
    }

    AnnotationAwareOrderComparator.sort(ret);
    return ret;
  }

  /**
   * get all strategies by a strategyKey, use {@code TodayStrategies.class.getClassLoader()}
   *
   * @param strategyKey key
   * @return list of strategies
   */
  public static List<String> get(String strategyKey) {
    return get(strategyKey, null);
  }

  /**
   * get all strategies by a strategyKey
   *
   * @param strategyKey key
   * @param classLoader classLoader to load
   * @return list of strategies
   */
  public static List<String> get(
          String strategyKey, @Nullable ClassLoader classLoader) {
    Assert.notNull(strategyKey, "strategy-key must not be null");
    if (classLoader == null) {
      classLoader = TodayStrategies.class.getClassLoader();
    }
    List<String> strategies = loadStrategies(classLoader).get(strategyKey);
    if (strategies == null) {
      return new ArrayList<>();
    }
    return strategies;
  }

  /**
   * get all strategies by a strategyClass
   *
   * @param strategyClass strategy-class
   * @param classLoader classLoader to load
   * @return list of strategies
   */
  public static List<String> getStrategiesNames(
          Class<?> strategyClass, @Nullable ClassLoader classLoader) {
    Assert.notNull(strategyClass, "strategy-class must not be null");
    return get(strategyClass.getName(), classLoader);
  }

  private static MultiValueMap<String, String> loadStrategies(ClassLoader classLoader) {
    MultiValueMap<String, String> strategies = strategiesCache.get(classLoader);
    if (strategies != null) {
      return strategies;
    }
    synchronized(strategiesCache) {
      strategies = strategiesCache.get(classLoader);
      if (strategies == null) {
        log.debug("Detecting strategies location '{}'", STRATEGIES_LOCATION);
        strategies = MultiValueMap.fromLinkedHashMap();
        try {
          Enumeration<URL> urls = classLoader.getResources(STRATEGIES_LOCATION);
          while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            Properties properties = new Properties();

            try (InputStream inputStream = url.openStream()) {
              properties.load(inputStream);
            }
            log.debug("Reading strategies file '{}'", url);

            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
              Object key = entry.getKey();
              Object value = entry.getValue();
              if (key != null && value != null) {
                String strategyKey = key.toString();
                // split as string list
                List<String> strategyValues = StringUtils.splitAsList(value.toString());
                for (String strategyValue : strategyValues) {
                  strategyValue = strategyValue.trim(); // trim whitespace
                  if (StringUtils.isNotEmpty(strategyValue)) {
                    strategies.add(strategyKey, strategyValue);
                  }
                }
              }
            }
          }

          strategiesCache.put(classLoader, strategies);
        }
        catch (IOException ex) {
          throw new IllegalArgumentException(
                  "Unable to load strategies from location [" + STRATEGIES_LOCATION + "]", ex);
        }
      }
    }
    return strategies;
  }

}
