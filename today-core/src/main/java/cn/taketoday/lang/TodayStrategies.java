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

package cn.taketoday.lang;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * today-framework Strategies
 * <p>General purpose strategy loading mechanism for internal use within the framework.
 * <p>Reads a {@code META-INF/today.strategies} file from the root of the library classpath,
 * and also allows for programmatically setting properties through {@link #setProperty}.
 * When checking a property, local entries are being checked first, then falling back
 * to JVM-level system properties through a {@link System#getProperty} check.
 * <p>
 * Get keyed strategies
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/9/5 13:57
 */
public class TodayStrategies {
  private static final Logger log = LoggerFactory.getLogger(TodayStrategies.class);

  public static final String STRATEGIES_LOCATION = "META-INF/today.strategies";

  static final ConcurrentReferenceHashMap<ClassLoader, Map<String, TodayStrategies>>
          strategiesCache = new ConcurrentReferenceHashMap<>();

  private static final String PROPERTIES_RESOURCE_LOCATION = "today.properties";

  // local application properties file
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
   * <p>The first argument is treated as the name of a today.properties or
   * system property. properties are accessible through the {@link #getProperty(java.lang.String)}
   * method. The string value of this property is then interpreted as an integer
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
   * <p>The first argument is treated as the name of a today.properties or
   * system property. properties are accessible through the {@link #getProperty(java.lang.String)}
   * method. The string value of this property is then interpreted as an integer
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
    return result == null ? val : result;
  }

  /**
   * Returns the integer value of the property with the
   * specified name.
   * <p>The first argument is treated as the name of a today.properties or
   * system property. properties are accessible through the {@link #getProperty(java.lang.String)}
   * method. The string value of this property is then interpreted as an integer
   * value using the grammar supported by {@link Integer#decode decode} and
   * an {@code Integer} object representing this value is returned.
   *
   * in summary:
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
    try {
      String v = getProperty(key);
      if (v != null) {
        try {
          return Integer.decode(v);
        }
        catch (NumberFormatException ignored) { }
      }
    }
    catch (IllegalArgumentException | NullPointerException ignored) { }
    return val;
  }

  /**
   * Determines the {@code long} value of the system property
   * with the specified name.
   *
   * <p>The first argument is treated as the name of a today.properties or
   * system property. properties are accessible through the {@link #getProperty(java.lang.String)}
   * method. The string value of this property is then interpreted as an integer
   * value using the grammar supported by {@link Integer#decode decode} and
   * an {@code Integer} object representing this value is returned.
   *
   * <p>If there is no property with the specified name, if the
   * specified name is empty or {@code null}, or if the property
   * does not have the correct numeric format, then {@code null} is
   * returned.
   *
   * <p>In other words, this method returns a {@code Long} object
   * equal to the value of:
   *
   * <blockquote>
   * {@code getLong(nm, null)}
   * </blockquote>
   *
   * @param nm property name.
   * @return the {@code Long} value of the property.
   * @throws SecurityException for the same reasons as
   * {@link System#getProperty(String) System.getProperty}
   * @see java.lang.System#getProperty(java.lang.String)
   * @see java.lang.System#getProperty(java.lang.String, java.lang.String)
   */
  @Nullable
  public static Long getLong(String nm) {
    return getLong(nm, null);
  }

  /**
   * Determines the {@code long} value of the system property
   * with the specified name.
   *
   * <p>The first argument is treated as the name of a today.properties or
   * system property. properties are accessible through the {@link #getProperty(java.lang.String)}
   * method. The string value of this property is then interpreted as an integer
   * value using the grammar supported by {@link Integer#decode decode} and
   * an {@code Integer} object representing this value is returned.
   *
   * <p>The second argument is the default value. A {@code Long} object
   * that represents the value of the second argument is returned if there
   * is no property of the specified name, if the property does not have
   * the correct numeric format, or if the specified name is empty or null.
   *
   * <p>In other words, this method returns a {@code Long} object equal
   * to the value of:
   *
   * <blockquote>
   * {@code getLong(nm, new Long(val))}
   * </blockquote>
   *
   * but in practice it may be implemented in a manner such as:
   *
   * <blockquote><pre>
   * Long result = getLong(nm, null);
   * return (result == null) ? new Long(val) : result;
   * </pre></blockquote>
   *
   * to avoid the unnecessary allocation of a {@code Long} object when
   * the default value is not needed.
   *
   * @param key property name.
   * @param val default value.
   * @return the {@code Long} value of the property.
   * @throws SecurityException for the same reasons as
   * {@link System#getProperty(String) System.getProperty}
   * @see java.lang.System#getProperty(java.lang.String)
   * @see java.lang.System#getProperty(java.lang.String, java.lang.String)
   */
  public static long getLong(String key, long val) {
    Long result = getLong(key, null);
    return result == null ? val : result;
  }

  /**
   * Returns the {@code long} value of the system property with the specified name.
   * <p>The first argument is treated as the name of a today.properties or
   * system property. properties are accessible through the {@link #getProperty(java.lang.String)}
   * method. The string value of this property is then interpreted as an integer
   * value using the grammar supported by {@link Integer#decode decode} and
   * an {@code Integer} object representing this value is returned.
   *
   * in summary:
   *
   * <ul>
   * <li>If the property value begins with the two ASCII characters
   * {@code 0x} or the ASCII character {@code #}, not followed by
   * a minus sign, then the rest of it is parsed as a hexadecimal integer
   * exactly as for the method {@link Long#valueOf(java.lang.String, int)}
   * with radix 16.
   * <li>If the property value begins with the ASCII character
   * {@code 0} followed by another character, it is parsed as
   * an octal integer exactly as by the method {@link
   * Long#valueOf(java.lang.String, int)} with radix 8.
   * <li>Otherwise the property value is parsed as a decimal
   * integer exactly as by the method
   * {@link Long#valueOf(java.lang.String, int)} with radix 10.
   * </ul>
   *
   * <p>Note that, in every case, neither {@code L}
   * ({@code '\u005Cu004C'}) nor {@code l}
   * ({@code '\u005Cu006C'}) is permitted to appear at the end
   * of the property value as a type indicator, as would be
   * permitted in Java programming language source code.
   *
   * <p>The second argument is the default value. The default value is
   * returned if there is no property of the specified name, if the
   * property does not have the correct numeric format, or if the
   * specified name is empty or {@code null}.
   *
   * @param key property name.
   * @param val default value.
   * @return the {@code Long} value of the property.
   * @throws SecurityException for the same reasons as
   * {@link System#getProperty(String) System.getProperty}
   * @see System#getProperty(java.lang.String)
   * @see System#getProperty(java.lang.String, java.lang.String)
   */
  @Nullable
  public static Long getLong(String key, Long val) {
    try {
      String v = getProperty(key);
      if (v != null) {
        try {
          return Long.decode(v);
        }
        catch (NumberFormatException ignored) { }
      }
    }
    catch (IllegalArgumentException | NullPointerException ignored) { }

    return val;
  }

  //---------------------------------------------------------------------
  // Strategies
  //---------------------------------------------------------------------

  private final ClassLoader classLoader;
  private final Map<String, List<String>> strategies;

  /**
   * Create a new {@link TodayStrategies} instance.
   *
   * @param classLoader the classloader used to instantiate the factories
   * @param strategies a map of strategy class name to implementation class names
   */
  protected TodayStrategies(@Nullable ClassLoader classLoader, Map<String, List<String>> strategies) {
    this.classLoader = classLoader;
    this.strategies = strategies;
  }

  /**
   * @param strategies output
   * @param properties input
   */
  public static void readStrategies(MultiValueMap<String, String> strategies, Properties properties) {
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      Object key = entry.getKey();
      Object value = entry.getValue();
      if (key != null && value != null) {
        // split as string list
        List<String> strategyValues = StringUtils.splitAsList(value.toString());
        for (String strategyValue : strategyValues) {
          strategyValue = strategyValue.trim(); // trim whitespace
          if (StringUtils.isNotEmpty(strategyValue)) {
            strategies.add(key.toString(), strategyValue);
          }
        }
      }
    }
  }

  /**
   * Load and instantiate the strategy implementations of the given type from
   * {@value #STRATEGIES_LOCATION}, using the configured class loader
   * and a default argument resolver that expects a no-arg constructor.
   * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
   * <p>If a custom instantiation strategy is required, use {@code load(...)}
   * with a custom {@link ArgumentResolver ArgumentResolver} and/or
   * {@link FailureHandler FailureHandler}.
   * <p> if duplicate implementation class names are
   * discovered for a given strategy type, only one instance of the duplicated
   * implementation type will be instantiated.
   *
   * @param strategyType the interface or abstract class representing the strategy
   * @return Returns strategy object list that can be modified
   * @throws IllegalArgumentException if any strategy implementation class cannot
   * be loaded or if an error occurs while instantiating any strategy
   */
  public <T> List<T> load(Class<T> strategyType) {
    return load(strategyType, (ArgumentResolver) null, null);
  }

  /**
   * Load and instantiate the strategy implementations of the given type from
   * {@value #STRATEGIES_LOCATION}, using the configured class loader
   * and the given argument resolver.
   * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
   * <p>If duplicate implementation class names are
   * discovered for a given strategy type, only one instance of the duplicated
   * implementation type will be instantiated.
   *
   * @param strategyType the interface or abstract class representing the strategy
   * @param argumentResolver strategy used to resolve constructor arguments by their type
   * @return Returns strategy object list that can be modified
   * @throws IllegalArgumentException if any strategy implementation class cannot
   * be loaded or if an error occurs while instantiating any strategy
   */
  public <T> List<T> load(Class<T> strategyType, @Nullable ArgumentResolver argumentResolver) {
    return load(strategyType, argumentResolver, null);
  }

  /**
   * Load and instantiate the strategy implementations of the given type from
   * {@value #STRATEGIES_LOCATION}, using the configured class loader
   * and the given argument resolver.
   * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
   * <p>If duplicate implementation class names are
   * discovered for a given strategy type, only one instance of the duplicated
   * implementation type will be instantiated.
   *
   * @param strategyType the interface or abstract class representing the strategy
   * @param instantiator strategy used it to instantiate instance
   * @return Returns strategy object list that can be modified
   * @throws IllegalArgumentException if any strategy implementation class cannot
   * be loaded or if an error occurs while instantiating any strategy
   */
  public <T> List<T> load(Class<T> strategyType, ClassInstantiator instantiator) {
    return load(strategyType, instantiator, null);
  }

  /**
   * Load and instantiate the strategy implementations of the given type from
   * {@value #STRATEGIES_LOCATION}, using the configured class loader
   * with custom failure handling provided by the given failure handler.
   * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
   * <p>If duplicate implementation class names are
   * discovered for a given strategy type, only one instance of the duplicated
   * implementation type will be instantiated.
   * <p>For any strategy implementation class that cannot be loaded or error that
   * occurs while instantiating it, the given failure handler is called.
   *
   * @param strategyType the interface or abstract class representing the strategy
   * @param failureHandler strategy used to handle strategy instantiation failures
   * @return Returns strategy object list that can be modified
   */
  public <T> List<T> load(Class<T> strategyType, @Nullable FailureHandler failureHandler) {
    return load(strategyType, (ArgumentResolver) null, failureHandler);
  }

  /**
   * Load and instantiate the strategy implementations of the given type from
   * {@value #STRATEGIES_LOCATION}, using the configured class loader,
   * the given argument resolver, and custom failure handling provided by the given
   * failure handler.
   * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
   * <p>If duplicate implementation class names are
   * discovered for a given strategy type, only one instance of the duplicated
   * implementation type will be instantiated.
   * <p>For any strategy implementation class that cannot be loaded or error that
   * occurs while instantiating it, the given failure handler is called.
   *
   * @param strategyType the interface or abstract class representing the strategy
   * @param resolver strategy used to resolve constructor arguments by their type
   * @param failureHandler strategy used to handle strategy instantiation failures
   * @return Returns strategy object list that can be modified
   */
  public <T> List<T> load(Class<T> strategyType, @Nullable ArgumentResolver resolver, @Nullable FailureHandler failureHandler) {
    var instantiator = new DefaultInstantiator(resolver);
    return load(strategyType, instantiator, failureHandler);
  }

  /**
   * Load and instantiate the strategy implementations of the given type from
   * {@value #STRATEGIES_LOCATION}, using the configured class loader,
   * the given argument resolver, and custom failure handling provided by the given
   * failure handler.
   * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
   * <p>If duplicate implementation class names are
   * discovered for a given strategy type, only one instance of the duplicated
   * implementation type will be instantiated.
   * <p>For any strategy implementation class that cannot be loaded or error that
   * occurs while instantiating it, the given failure handler is called.
   *
   * @param strategyType the interface or abstract class representing the strategy
   * @param instantiator strategy used it to instantiate instance
   * @param failureHandler strategy used to handle strategy instantiation failures
   * @return Returns strategy object list that can be modified
   */
  public <T> List<T> load(Class<T> strategyType, ClassInstantiator instantiator, @Nullable FailureHandler failureHandler) {
    Assert.notNull(strategyType, "'strategyType' is required");
    Assert.notNull(instantiator, "'instantiator' is required");

    List<String> implementationNames = getStrategyNames(strategyType);
    if (log.isTraceEnabled()) {
      log.trace("Loaded [{}] names: {}", strategyType.getName(), implementationNames);
    }

    var result = new ArrayList<T>(implementationNames.size());
    if (failureHandler == null) {
      failureHandler = FailureHandler.throwing();
    }
    for (String implementationName : implementationNames) {
      T strategy = instantiateStrategy(implementationName, strategyType, instantiator, failureHandler);
      if (strategy != null) {
        result.add(strategy);
      }
    }
    AnnotationAwareOrderComparator.sort(result);
    return result;
  }

  // private stuff

  private List<String> getStrategyNames(String strategyKey) {
    return strategies.getOrDefault(strategyKey, Collections.emptyList());
  }

  private List<String> getStrategyNames(Class<?> strategyType) {
    return getStrategyNames(strategyType.getName());
  }

  @Nullable
  protected <T> T instantiateStrategy(String implementationName, Class<T> type,
          ClassInstantiator instantiator, FailureHandler failureHandler) {
    try {
      Class<T> implementation = ClassUtils.forName(implementationName, classLoader);
      if (!type.isAssignableFrom(implementation)) {
        throw new IllegalArgumentException(
                "Class [%s] is not assignable to strategy type [%s]".formatted(implementationName, type.getName()));
      }

      return instantiator.instantiate(implementation);
    }
    catch (Throwable ex) {
      failureHandler.handleFailure(type, implementationName, ex);
      return null;
    }
  }

  // Static

  /**
   * get first strategy
   *
   * @see #find(Class)
   */
  @Nullable
  public static String findFirst(String strategyKey) {
    return CollectionUtils.firstElement(findNames(strategyKey, null));
  }

  /**
   * get first strategy
   *
   * @see #find(Class)
   */
  @Nullable
  public static <T> T findFirst(Class<T> strategyClass, @Nullable Supplier<T> defaultValue) {
    T first = CollectionUtils.firstElement(find(strategyClass, (ClassLoader) null));
    if (first == null && defaultValue != null) {
      return defaultValue.get();
    }
    return first;
  }

  public static <T> Optional<T> findFirst(Class<T> strategyClass) {
    T first = CollectionUtils.firstElement(find(strategyClass, (ClassLoader) null));
    return Optional.ofNullable(first);
  }

  /**
   * get none repeatable strategies by given class
   *
   * @param strategyClass strategy class
   * @param <T> target type
   * @return returns none repeatable strategies by given class
   */
  public static <T> List<T> find(Class<T> strategyClass) {
    return find(strategyClass, (ClassLoader) null);
  }

  /**
   * Load and instantiate the strategy implementations of the given type from
   * {@value #STRATEGIES_LOCATION}, using the given class loader.
   * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
   * <p> if duplicate implementation class names are
   * discovered for a given strategy type, only one instance of the duplicated
   * implementation type will be instantiated.
   * <p>For more advanced strategy loading with {@link ArgumentResolver} or
   * {@link FailureHandler} support use {@link #forDefaultResourceLocation(ClassLoader)}
   * to obtain a {@link TodayStrategies} instance.
   *
   * @param strategyType the interface or abstract class representing the strategy
   * @param classLoader the ClassLoader to use for loading (can be {@code null}
   * to use the default)
   * @throws IllegalArgumentException if any strategy implementation class cannot
   * be loaded or if an error occurs while instantiating any strategy
   */
  public static <T> List<T> find(Class<T> strategyType, @Nullable ClassLoader classLoader) {
    return forDefaultResourceLocation(classLoader).load(strategyType);
  }

  /**
   * Load and instantiate the strategy implementations of the given type from
   * {@value #STRATEGIES_LOCATION}, using the given class loader.
   * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
   * <p> if duplicate implementation class names are
   * discovered for a given strategy type, only one instance of the duplicated
   * implementation type will be instantiated.
   * <p>For more advanced strategy loading with {@link ArgumentResolver} or
   * {@link FailureHandler} support use {@link #forDefaultResourceLocation(ClassLoader)}
   * to obtain a {@link TodayStrategies} instance.
   *
   * @param strategyType the interface or abstract class representing the strategy
   * @param classLoader the ClassLoader to use for loading (can be {@code null}
   * to use the default)
   * @throws IllegalArgumentException if any strategy implementation class cannot
   * be loaded or if an error occurs while instantiating any strategy
   */
  public static <T> List<T> find(Class<T> strategyType, @Nullable ClassLoader classLoader, ClassInstantiator instantiator) {
    return forDefaultResourceLocation(classLoader).load(strategyType, instantiator);
  }

  /**
   * Load and instantiate the strategy implementations of the given type from
   * {@value #STRATEGIES_LOCATION}, using the given class loader.
   * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
   * <p> if duplicate implementation class names are
   * discovered for a given strategy type, only one instance of the duplicated
   * implementation type will be instantiated.
   * <p>For more advanced strategy loading with {@link ArgumentResolver} or
   * {@link FailureHandler} support use {@link #forDefaultResourceLocation(ClassLoader)}
   * to obtain a {@link TodayStrategies} instance.
   *
   * @param strategyType the interface or abstract class representing the strategy
   * @throws IllegalArgumentException if any strategy implementation class cannot
   * be loaded or if an error occurs while instantiating any strategy
   */
  public static <T> List<T> find(Class<T> strategyType, ClassInstantiator instantiator) {
    return forDefaultResourceLocation().load(strategyType, instantiator);
  }

  // names

  /**
   * Load the fully qualified class names of strategy implementations of the
   * given type from {@value #STRATEGIES_LOCATION}, using the given
   * class loader.
   * <p> if a particular implementation class name
   * is discovered more than once for the given strategy type, duplicates will
   * be ignored.
   *
   * @param strategyType the interface or abstract class representing the strategy
   * @throws IllegalArgumentException if an error occurs while loading strategy names
   * @see #findNames
   */
  public static List<String> findNames(Class<?> strategyType) {
    return findNames(strategyType, null);
  }

  /**
   * Load the fully qualified class names of strategy implementations of the
   * given type from {@value #STRATEGIES_LOCATION}, using the given
   * class loader.
   * <p> if a particular implementation class name
   * is discovered more than once for the given strategy type, duplicates will
   * be ignored.
   *
   * @param strategyType the interface or abstract class representing the strategy
   * @param classLoader the ClassLoader to use for loading resources; can be
   * {@code null} to use the default
   * @throws IllegalArgumentException if an error occurs while loading strategy names
   * @see #findNames
   */
  public static List<String> findNames(Class<?> strategyType, @Nullable ClassLoader classLoader) {
    return forDefaultResourceLocation(classLoader).getStrategyNames(strategyType);
  }

  /**
   * Load the fully qualified class names of strategy implementations of the
   * given type from {@value #STRATEGIES_LOCATION}, using the given
   * class loader.
   * <p> if a particular implementation class name
   * is discovered more than once for the given strategy type, duplicates will
   * be ignored.
   *
   * @param strategyKey the key representing the strategy
   * @throws IllegalArgumentException if an error occurs while loading strategy names
   * @see #findNames
   */
  public static List<String> findNames(String strategyKey) {
    return findNames(strategyKey, null);
  }

  /**
   * Load the fully qualified class names of strategy implementations of the
   * given type from {@value #STRATEGIES_LOCATION}, using the given
   * class loader.
   * <p> if a particular implementation class name
   * is discovered more than once for the given strategy type, duplicates will
   * be ignored.
   *
   * @param strategyKey the key representing the strategy
   * @param classLoader the ClassLoader to use for loading resources; can be
   * {@code null} to use the default
   * @throws IllegalArgumentException if an error occurs while loading strategy names
   * @see #findNames
   */
  public static List<String> findNames(String strategyKey, @Nullable ClassLoader classLoader) {
    return forDefaultResourceLocation(classLoader).getStrategyNames(strategyKey);
  }

  /**
   * Create a {@link TodayStrategies} instance that will load and
   * instantiate the strategy implementations from
   * {@value #STRATEGIES_LOCATION}, using the default class loader.
   *
   * @return a {@link TodayStrategies} instance
   * @see #forDefaultResourceLocation(ClassLoader)
   */
  public static TodayStrategies forDefaultResourceLocation() {
    return forDefaultResourceLocation(null);
  }

  /**
   * Create a {@link TodayStrategies} instance that will load and
   * instantiate the strategy implementations from
   * {@value #STRATEGIES_LOCATION}, using the given class loader.
   *
   * @param classLoader the ClassLoader to use for loading resources; can be
   * {@code null} to use the default
   * @return a {@link TodayStrategies} instance
   * @see #forDefaultResourceLocation()
   */
  public static TodayStrategies forDefaultResourceLocation(@Nullable ClassLoader classLoader) {
    return forLocation(STRATEGIES_LOCATION, classLoader);
  }

  /**
   * Create a {@link TodayStrategies} instance that will load and
   * instantiate the strategy implementations from the given location, using
   * the default class loader.
   *
   * @param resourceLocation the resource location to look for factories
   * @return a {@link TodayStrategies} instance
   * @see #forLocation(String, ClassLoader)
   */
  public static TodayStrategies forLocation(String resourceLocation) {
    return forLocation(resourceLocation, null);
  }

  /**
   * Create a {@link TodayStrategies} instance that will load and
   * instantiate the strategy implementations from the given location, using
   * the given class loader.
   *
   * @param resourceLocation the resource location to look for factories
   * @param classLoader the ClassLoader to use for loading resources; can be
   * {@code null} to use the default
   * @return a {@link TodayStrategies} instance
   * @see #forLocation(String)
   */
  public static TodayStrategies forLocation(String resourceLocation, @Nullable ClassLoader classLoader) {
    Assert.hasText(resourceLocation, "'resourceLocation' must not be empty");

    if (classLoader == null) {
      classLoader = TodayStrategies.class.getClassLoader();
    }

    Map<String, TodayStrategies> loaders = strategiesCache.get(classLoader);
    if (loaders == null) {
      synchronized(strategiesCache) {
        loaders = strategiesCache.get(classLoader);
        if (loaders == null) {
          loaders = new ConcurrentReferenceHashMap<>();
          strategiesCache.put(classLoader, loaders);
        }
      }
    }

    TodayStrategies todayStrategies = loaders.get(resourceLocation);
    if (todayStrategies == null) {
      synchronized(loaders) {
        todayStrategies = loaders.get(resourceLocation);
        if (todayStrategies == null) {
          todayStrategies = new TodayStrategies(classLoader, loadResource(classLoader, resourceLocation));
          loaders.put(resourceLocation, todayStrategies);
        }
      }
    }

    return todayStrategies;
  }

  protected static Map<String, List<String>> loadResource(ClassLoader classLoader, String resourceLocation) {
    MultiValueMap<String, String> strategies = MultiValueMap.forLinkedHashMap();
    try {
      log.debug("Detecting strategies location '{}'", resourceLocation);
      Enumeration<URL> urls = classLoader.getResources(resourceLocation);
      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();
        Properties properties = new Properties();

        try (InputStream inputStream = url.openStream()) {
          properties.load(inputStream);
        }

        log.debug("Reading strategies file '{}'", url);
        readStrategies(strategies, properties);
      }

      strategies.replaceAll(TodayStrategies::toDistinctUnmodifiableList);
    }
    catch (IOException ex) {
      throw new IllegalArgumentException(
              "Unable to load strategies from location [" + resourceLocation + "]", ex);
    }
    return Collections.unmodifiableMap(strategies);
  }

  private static List<String> toDistinctUnmodifiableList(String strategyType, List<String> implementations) {
    return implementations.stream().distinct().toList();
  }

  /**
   * Internal instantiator used to create the strategy instance.
   */
  static final class DefaultInstantiator implements ClassInstantiator {

    @Nullable
    private final ArgumentResolver argumentResolver;

    DefaultInstantiator(@Nullable ArgumentResolver argumentResolver) {
      this.argumentResolver = argumentResolver;
    }

    /**
     * @param <T> the instance implementation type
     * @param implementation strategy implementation
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T instantiate(Class<T> implementation) throws Exception {
      Constructor<?> constructor = findConstructor(implementation);
      if (constructor == null) {
        throw new IllegalStateException(
                "Class [%s] has no suitable constructor".formatted(implementation.getName()));
      }
      Class<?>[] types = constructor.getParameterTypes();
      Object[] args = new Object[types.length];
      if (argumentResolver != null) {
        int i = 0;
        for (Class<?> type : types) {
          args[i++] = argumentResolver.resolve(type);
        }
      }
      ReflectionUtils.makeAccessible(constructor);
      return (T) constructor.newInstance(args);
    }

    @Nullable
    private static Constructor<?> findConstructor(Class<?> implementationClass) {
      Constructor<?> constructor = findSingleConstructor(implementationClass.getConstructors());
      if (constructor == null) {
        constructor = findSingleConstructor(implementationClass.getDeclaredConstructors());
        if (constructor == null) {
          constructor = findDeclaredConstructor(implementationClass);
        }
      }
      return constructor;
    }

    @Nullable
    private static Constructor<?> findSingleConstructor(Constructor<?>[] constructors) {
      return (constructors.length == 1 ? constructors[0] : null);
    }

    @Nullable
    private static Constructor<?> findDeclaredConstructor(Class<?> implementationClass) {
      try {
        return implementationClass.getDeclaredConstructor();
      }
      catch (NoSuchMethodException ex) {
        return null;
      }
    }

  }

  /**
   * Strategy for resolving constructor arguments based on their type.
   *
   * @see ArgumentResolver#of(Class, Object)
   * @see ArgumentResolver#ofSupplied(Class, Supplier)
   * @see ArgumentResolver#from(Function)
   */
  @FunctionalInterface
  public interface ArgumentResolver {

    /**
     * Resolve the given argument if possible.
     *
     * @param <T> the argument type
     * @param type the argument type
     * @return the resolved argument value or {@code null}
     */
    @Nullable
    <T> T resolve(Class<T> type);

    /**
     * Create a new composed {@link ArgumentResolver} by combining this resolver
     * with the given type and value.
     *
     * @param <T> the argument type
     * @param type the argument type
     * @param value the argument value
     * @return a new composite {@link ArgumentResolver} instance
     */
    default <T> ArgumentResolver and(Class<T> type, T value) {
      return and(ArgumentResolver.of(type, value));
    }

    /**
     * Create a new composed {@link ArgumentResolver} by combining this resolver
     * with the given type and value.
     *
     * @param <T> the argument type
     * @param type the argument type
     * @param valueSupplier the argument value supplier
     * @return a new composite {@link ArgumentResolver} instance
     */
    default <T> ArgumentResolver andSupplied(Class<T> type, Supplier<T> valueSupplier) {
      return and(ArgumentResolver.ofSupplied(type, valueSupplier));
    }

    /**
     * Create a new composed {@link ArgumentResolver} by combining this resolver
     * with the given resolver.
     *
     * @param argumentResolver the argument resolver to add
     * @return a new composite {@link ArgumentResolver} instance
     */
    default ArgumentResolver and(ArgumentResolver argumentResolver) {
      return from(type -> {
        Object resolved = resolve(type);
        return (resolved != null ? resolved : argumentResolver.resolve(type));
      });
    }

    /**
     * Factory method that returns an {@link ArgumentResolver} that always
     * returns {@code null}.
     *
     * @return a new {@link ArgumentResolver} instance
     */
    static ArgumentResolver none() {
      return from(type -> null);
    }

    /**
     * Factory method that can be used to create an {@link ArgumentResolver}
     * that resolves only the given type.
     *
     * @param <T> the argument type
     * @param type the argument type
     * @param value the argument value
     * @return a new {@link ArgumentResolver} instance
     */
    static <T> ArgumentResolver of(Class<T> type, T value) {
      return ofSupplied(type, () -> value);
    }

    /**
     * Factory method that can be used to create an {@link ArgumentResolver}
     * that resolves only the given type.
     *
     * @param <T> the argument type
     * @param type the argument type
     * @param valueSupplier the argument value supplier
     * @return a new {@link ArgumentResolver} instance
     */
    static <T> ArgumentResolver ofSupplied(Class<T> type, Supplier<T> valueSupplier) {
      return from(candidateType -> (candidateType.equals(type) ? valueSupplier.get() : null));
    }

    /**
     * Factory method that creates a new {@link ArgumentResolver} from a
     * lambda friendly function. The given function is provided with the
     * argument type and must provide an instance of that type or {@code null}.
     *
     * @param function the resolver function
     * @return a new {@link ArgumentResolver} instance backed by the function
     */
    static ArgumentResolver from(Function<Class<?>, Object> function) {
      return new ArgumentResolver() {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T resolve(Class<T> type) {
          return (T) function.apply(type);
        }

      };
    }

  }

  /**
   * Strategy for handling a failure that occurs when instantiating a strategy.
   *
   * @see FailureHandler#throwing()
   * @see FailureHandler#logging(Logger)
   */
  @FunctionalInterface
  public interface FailureHandler {

    /**
     * Handle the {@code failure} that occurred when instantiating the
     * {@code strategyImplementationName} that was expected to be of the
     * given {@code strategyType}.
     *
     * @param strategyType the type of the strategy
     * @param strategyImplementationName the name of the strategy implementation
     * @param failure the failure that occurred
     * @see #throwing()
     * @see #logging
     */
    void handleFailure(Class<?> strategyType, String strategyImplementationName, Throwable failure);

    /**
     * Create a new {@link FailureHandler} that handles errors by throwing an
     * {@link IllegalArgumentException}.
     *
     * @return a new {@link FailureHandler} instance
     * @see #throwing(BiFunction)
     */
    static FailureHandler throwing() {
      return throwing(IllegalArgumentException::new);
    }

    /**
     * Create a new {@link FailureHandler} that handles errors by throwing an
     * exception.
     *
     * @param exceptionFactory strategy used to create the exception
     * @return a new {@link FailureHandler} instance
     */
    static FailureHandler throwing(BiFunction<String, Throwable, ? extends RuntimeException> exceptionFactory) {
      return handleMessage((messageSupplier, failure) -> {
        throw exceptionFactory.apply(messageSupplier.get(), failure);
      });
    }

    /**
     * Create a new {@link FailureHandler} that handles errors by logging trace
     * messages.
     *
     * @param logger the logger used to log messages
     * @return a new {@link FailureHandler} instance
     */
    static FailureHandler logging(Logger logger) {
      return handleMessage((messageSupplier, failure) ->
              logger.trace(LogMessage.from(messageSupplier), failure));
    }

    /**
     * Create a new {@link FailureHandler} that handles errors using a standard
     * formatted message.
     *
     * @param messageHandler the message handler used to handle the problem
     * @return a new {@link FailureHandler} instance
     */
    static FailureHandler handleMessage(BiConsumer<Supplier<String>, Throwable> messageHandler) {
      return (strategyType, implementationName, failure) -> {
        Supplier<String> messageSupplier = () -> "Unable to instantiate strategy class [%s] for strategy type [%s]"
                .formatted(implementationName, strategyType.getName());
        messageHandler.accept(messageSupplier, failure);
      };
    }

  }

}
