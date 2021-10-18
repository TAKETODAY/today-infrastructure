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

package cn.taketoday.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Strategies Detector
 * <p>
 * Get keyed strategies
 * </p>
 *
 * @author TODAY 2021/7/17 21:59
 * @since 4.0
 */
@SuppressWarnings("rawtypes")
public class StrategiesDetector {
  private static final Logger log = LoggerFactory.getLogger(StrategiesDetector.class);

  /** strategies file location */
  private String strategiesLocation;
  private StrategiesReader strategiesReader;

  private ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

  @Nullable
  private BeanFactory beanFactory;
  private boolean throwWhenClassNotFound = false;
  // do log when class not found
  private boolean logWhenClassNotFound = false;

  private final DefaultMultiValueMap<String, String> strategies = new DefaultMultiValueMap<>();

//  private final DefaultSingletonBeanRegistry singletonBeanRegistry = new DefaultSingletonBeanRegistry();

  public StrategiesDetector() {
    this(new DefaultStrategiesReader());
  }

  public StrategiesDetector(@Nullable BeanFactory beanFactory) {
    this(new DefaultStrategiesReader());
    this.beanFactory = beanFactory;
  }

  public StrategiesDetector(StrategiesReader reader) {
    setStrategiesReader(reader);
  }

  public StrategiesDetector(StrategiesReader reader, String strategiesLocation) {
    setStrategiesReader(reader);
    setStrategiesLocation(strategiesLocation);
  }

  /**
   * load if strategies is empty
   */
  public void loadStrategies() {
    if (strategies.isEmpty()) {
      log.debug("Loading strategies files");
      loadStrategies(strategiesLocation);
    }
  }

  /**
   * load strategies with given location
   */
  public void loadStrategies(String strategiesLocation) {
    strategiesReader.read(strategiesLocation, strategies);
  }

  @Nullable
  public <T> T getFirst(Class<T> strategyClass) {
    return getFirst(strategyClass, beanFactory);
  }

  public <T> T getFirst(Class<T> strategyClass, Supplier<T> defaultValue) {
    T first = getFirst(strategyClass);
    if (first == null) {
      return defaultValue.get();
    }
    return first;
  }

  @Nullable
  public <T> T getFirst(Class<T> strategyClass, BeanFactory beanFactory) {
    return CollectionUtils.firstElement(getStrategies(strategyClass, beanFactory));
  }

  @Nullable
  public String getFirst(String strategyKey) {
    return CollectionUtils.firstElement(getStrategies(strategyKey));
  }

  public String getFirst(String strategyKey, Supplier<String> defaultValue) {
    String first = getFirst(strategyKey);
    if (first == null) {
      return defaultValue.get();
    }
    return first;
  }

  /**
   * Retrieve the flag for the given property key.
   *
   * @param strategyKey the property key
   * @return {@code true} if the property is set to "true", {@code} false
   * otherwise
   */
  public boolean getBoolean(String strategyKey) {
    return Boolean.parseBoolean(getFirst(strategyKey));
  }

  /**
   * Retrieve the flag for the given property key.
   * <p>
   * If there isn't a key returns defaultFlag
   * </p>
   *
   * @param strategyKey the property key
   * @return {@code true} if the property is set to "true", {@code} false
   * otherwise ,If there isn't a key returns defaultFlag
   */
  public boolean getBoolean(String strategyKey, boolean defaultFlag) {
    String property = getFirst(strategyKey);
    return StringUtils.isEmpty(property) ? defaultFlag : Boolean.parseBoolean(property);
  }

  /**
   * get none repeatable strategies by given class
   * <p>
   * strategies must be instance-of given strategy class
   * use default {@link #beanFactory}
   *
   * @param strategyClass strategy class
   * @param <T> target type
   * @return returns none repeatable strategies by given class
   */
  public <T> List<T> getStrategies(Class<T> strategyClass) {
    return getStrategies(strategyClass, beanFactory);
  }

  /**
   * get none repeatable strategies by given class
   * <p>
   * strategies must be an instance of given strategy class
   * </p>
   *
   * @param strategyClass strategy class
   * @param beanFactory bean factory (supports constructor parameters injection)
   * @param <T> target type
   * @return returns none repeatable strategies by given class
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> getStrategies(Class<T> strategyClass, @Nullable BeanFactory beanFactory) {
    Assert.notNull(strategyClass, "strategy-class must not be null");
    // get class list by class full name
    ArrayList<T> strategiesObject = new ArrayList<>();
    consumeTypes(strategyClass.getName(), strategy -> {
      if (strategyClass.isAssignableFrom(strategy)) {
        Object instance = createInstance(strategy, beanFactory);
        strategiesObject.add((T) instance);
      }
    });
    // sort
    AnnotationAwareOrderComparator.sort(strategiesObject);
    return strategiesObject;
  }

  @SuppressWarnings("unchecked")
  public <T> void consumeStrategies(
          Class<T> strategyClass, @Nullable BeanFactory beanFactory, Consumer<T> consumer) {
    Assert.notNull(strategyClass, "strategy-class must not be null");
    // get class list by class full name
    consumeTypes(strategyClass.getName(), strategy -> {
      if (strategyClass.isAssignableFrom(strategy)) {
        Object instance = createInstance(strategy, beanFactory);
        consumer.accept((T) instance);
      }
    });
  }

  private static Object createInstance(Class<?> strategy, BeanFactory factory) {
    Object instance = BeanUtils.newInstance(strategy, factory);
    if (factory instanceof AutowireCapableBeanFactory) {
      // autowire, don't apply bean post processor
      ((AutowireCapableBeanFactory) factory).autowireBean(strategy);
    }
    return instance;
  }

  public void consumeTypes(Class<?> strategyClass, Consumer<Class<?>> consumer) {
    Assert.notNull(strategyClass, "strategy-class must not be null");
    consumeTypes(strategyClass.getName(), consumer);
  }

  public void consumeTypes(String strategyKey, Consumer<Class<?>> consumer) {
    consumeStrategies(strategyKey, strategy -> {
      Class<?> aClass = loadClass(classLoader, strategy);
      if (aClass != null) {
        consumer.accept(aClass);
      }
    });
  }

  /**
   * consume by strategy key
   *
   * @param strategyKey key
   * @param consumer string consumer
   */
  public void consumeStrategies(String strategyKey, Consumer<String> consumer) {
    Collection<String> strategies = getStrategies(strategyKey);
    if (CollectionUtils.isNotEmpty(strategies)) {
      for (String strategy : strategies) {
        consumer.accept(strategy);
      }
    }
  }

  private Class<?> loadClass(ClassLoader classLoader, String className) {
    try {
      return classLoader.loadClass(className);
    }
    catch (ClassNotFoundException e) {
      if (throwWhenClassNotFound) {
        throw new IllegalStateException("class '" + className + "' not found", e);
      }
      else if (logWhenClassNotFound) {
        log.warn("class '{}' not found", className, e);
      }
    }
    return null;
  }

  public List<Class<?>> getTypes(Class<?> strategyClass) {
    Assert.notNull(strategyClass, "strategy-class must not be null");
    return getStrategies(strategyClass.getName(), strategy -> loadClass(classLoader, strategy));
  }

  public List<Class<?>> getTypes(String strategyKey) {
    return getStrategies(strategyKey, strategy -> loadClass(classLoader, strategy));
  }

  public List getObjects(Class<?> strategyClass) {
    Assert.notNull(strategyClass, "strategy-class must not be null");
    return getObjects(strategyClass.getName(), beanFactory);
  }

  /**
   * get objects by key
   * <p>
   * use default factory
   * </p>
   *
   * @param strategyKey key
   * @return list of objects
   */
  public List getObjects(String strategyKey) {
    return getObjects(strategyKey, beanFactory);
  }

  /**
   * get objects by key
   *
   * @param strategyKey key
   * @param beanFactory bean factory (supports constructor parameters injection)
   * @return list of objects
   */
  public List getObjects(String strategyKey, @Nullable BeanFactory beanFactory) {
    return getStrategies(strategyKey, strategy -> {
      Class<?> aClass = loadClass(classLoader, strategy);
      if (aClass != null) {
        return createInstance(aClass, beanFactory);
      }
      return null;
    });
  }

  /**
   * get collection of strategies
   *
   * @param strategyKey key
   * @param converter converter to convert string to T
   * @param <T> return type
   * @return collection of strategies
   */
  public <T> List<T> getStrategies(String strategyKey, Converter<String, T> converter) {
    Assert.notNull(converter, "converter must not be null");
//    return singletonBeanRegistry.getSingleton(strategyKey, () -> {
    Collection<String> strategies = getStrategies(strategyKey);
    ArrayList<T> ret = new ArrayList<>(strategies.size());
    for (String strategy : strategies) {
      T convert = converter.convert(strategy);
      if (convert != null) {
        ret.add(convert);
      }
    }
    AnnotationAwareOrderComparator.sort(ret);
    return ret;
//    });
  }

  /**
   * get list of strategies by key
   * <p>
   * filter repeat strategies
   * </p>
   *
   * @param strategyKey key
   * @return list of strategies
   */
  public Collection<String> getStrategies(String strategyKey) {
    return getStrategies(strategyKey, true);
  }

  /**
   * @param filterRepeat filter repeat strategies
   */
  public Collection<String> getStrategies(String strategyKey, boolean filterRepeat) {
    Assert.notNull(strategyKey, "strategy-key must not be null");
    loadStrategies();
    List<String> strategies = this.strategies.get(strategyKey);
    if (strategies == null) {
      return Collections.emptyList();
    }
    if (filterRepeat) {
      return new LinkedHashSet<>(strategies);
    }
    return strategies;
  }

  /**
   * clear strategies
   */
  public void clearStrategies() {
    strategies.clear();
  }

  public MultiValueMap<String, String> getStrategies() {
    return strategies;
  }

  public void setStrategiesLocation(String strategiesLocation) {
    Assert.notNull(classLoader, "strategiesLocation must not be null");
    this.strategiesLocation = strategiesLocation;
  }

  public String getStrategiesLocation() {
    return strategiesLocation;
  }

  public void setStrategiesReader(StrategiesReader strategiesReader) {
    Assert.notNull(strategiesReader, "strategiesReader must not be null");
    this.strategiesReader = strategiesReader;
  }

  public StrategiesReader getStrategiesReader() {
    return strategiesReader;
  }

  public void setClassLoader(ClassLoader classLoader) {
    Assert.notNull(classLoader, "classloader must not be null");
    this.classLoader = classLoader;
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public void setThrowWhenClassNotFound(boolean throwWhenClassNotFound) {
    this.throwWhenClassNotFound = throwWhenClassNotFound;
  }

  public boolean isThrowWhenClassNotFound() {
    return throwWhenClassNotFound;
  }

  public void setBeanFactory(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Nullable
  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public void setLogWhenClassNotFound(boolean logWhenClassNotFound) {
    this.logWhenClassNotFound = logWhenClassNotFound;
  }

  public boolean isLogWhenClassNotFound() {
    return logWhenClassNotFound;
  }

}
