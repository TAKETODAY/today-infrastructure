/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.context.conversion.Converter;
import cn.taketoday.context.factory.AutowireCapableBeanFactory;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.DefaultMultiValueMap;
import cn.taketoday.context.utils.MultiValueMap;

/**
 * Strategies Detector
 * <p>
 * Get keyed strategies
 * </p>
 *
 * @author TODAY 2021/7/17 21:59
 * @since 3.1.0
 */
public class StrategiesDetector {
  private static final Logger log = LoggerFactory.getLogger(StrategiesDetector.class);
  public static final String DEFAULT_STRATEGIES_LOCATION = "classpath*:META-INF/today.strategies";

  /** strategies file location */
  private String strategiesLocation = DEFAULT_STRATEGIES_LOCATION;
  private StrategiesReader strategiesReader;

  private ClassLoader classLoader = ClassUtils.getClassLoader();
  private BeanFactory beanFactory;
  private boolean throwWhenClassNotFound = false;

  private final DefaultMultiValueMap<String, String> strategies = new DefaultMultiValueMap<>();

  public StrategiesDetector() {
    this(new DefaultStrategiesReader());
  }

  public StrategiesDetector(BeanFactory beanFactory) {
    this(new DefaultStrategiesReader());
    this.beanFactory = beanFactory;
  }

  public StrategiesDetector(StrategiesReader reader) {
    setStrategiesReader(reader);
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

  public <T> List<T> getStrategies(Class<T> strategyClass) {
    return getStrategies(strategyClass, beanFactory);
  }

  /**
   * get none repeatable strategies by given class
   * <p>
   * strategies must be instance of given strategy class
   * </p>
   *
   * @param strategyClass
   *         strategy class
   * @param beanFactory
   *         bean factory (supports constructor parameters injection)
   * @param <T>
   *         target type
   *
   * @return returns none repeatable strategies by given class
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> getStrategies(Class<T> strategyClass, BeanFactory beanFactory) {
    Assert.notNull(strategyClass, "strategy-class must not be null");
    // get class list by class full name
    final ArrayList<T> strategiesObject = new ArrayList<>();
    consumeTypes(strategyClass.getName(), strategy -> {
      if (strategyClass.isAssignableFrom(strategy)) {
        final Object instance = createInstance(strategy, beanFactory);
        strategiesObject.add((T) instance);
      }
    });
    return strategiesObject;
  }

  private static Object createInstance(Class<?> strategy, BeanFactory factory) {
    final Object instance = ClassUtils.newInstance(strategy, factory);
    if (factory instanceof AutowireCapableBeanFactory) {
      // autowire, dont apply bean post processor
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
      final Class<?> aClass = loadClass(classLoader, strategy);
      if (aClass != null) {
        consumer.accept(aClass);
      }
    });
  }

  /**
   * consume by strategy key
   *
   * @param strategyKey
   *         key
   * @param consumer
   *         string consumer
   */
  public void consumeStrategies(String strategyKey, Consumer<String> consumer) {
    final Collection<String> strategies = getStrategies(strategyKey);
    if (!CollectionUtils.isEmpty(strategies)) {
      for (final String strategy : strategies) {
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
      else {
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
   * @param strategyKey
   *         key
   *
   * @return list of objects
   */
  public List getObjects(String strategyKey) {
    return getObjects(strategyKey, beanFactory);
  }

  /**
   * get objects by key
   *
   * @param strategyKey
   *         key
   * @param beanFactory
   *         bean factory (supports constructor parameters injection)
   *
   * @return list of objects
   */
  public List getObjects(String strategyKey, BeanFactory beanFactory) {
    return getStrategies(strategyKey, strategy -> {
      final Class<?> aClass = loadClass(classLoader, strategy);
      if (aClass != null) {
        return createInstance(aClass, beanFactory);
      }
      return null;
    });
  }

  /**
   * get collection of strategies
   *
   * @param strategyKey
   *         key
   * @param converter
   *         converter to convert string to T
   * @param <T>
   *         return type
   *
   * @return collection of strategies
   */
  public <T> List<T> getStrategies(String strategyKey, Converter<String, T> converter) {
    Assert.notNull(converter, "converter must not be null");
    final Collection<String> strategies = getStrategies(strategyKey);
    final ArrayList<T> ret = new ArrayList<>(strategies.size());
    for (final String strategy : strategies) {
      final T convert = converter.convert(strategy);
      if (convert != null) {
        ret.add(convert);
      }
    }
    return ret;
  }

  /**
   * get list of strategies by key
   * <p>
   * filter repeat strategies
   * </p>
   *
   * @param strategyKey
   *         key
   *
   * @return list of strategies
   */
  public Collection<String> getStrategies(String strategyKey) {
    return getStrategies(strategyKey, true);
  }

  /**
   * @param filterRepeat
   *         filter repeat strategies
   */
  public Collection<String> getStrategies(String strategyKey, boolean filterRepeat) {
    Assert.notNull(strategyKey, "strategy-key must not be null");
    loadStrategies();
    final List<String> strategies = this.strategies.get(strategyKey);
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

  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  public BeanFactory getBeanFactory() {
    return beanFactory;
  }
}
