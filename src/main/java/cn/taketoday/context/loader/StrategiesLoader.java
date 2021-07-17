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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.DefaultMultiValueMap;
import cn.taketoday.context.utils.MultiValueMap;

/**
 * Strategies Loader
 *
 * @author TODAY 2021/7/17 21:59
 * @since 3.0.6
 */
public class StrategiesLoader {
  public static final Logger log = LoggerFactory.getLogger(StrategiesLoader.class);

  /** strategies file location */
  private String strategiesLocation = "classpath:META-INF/today.strategies";
  private StrategiesReader strategiesReader = new DefaultStrategiesReader();

  private ClassLoader classLoader = ClassUtils.getClassLoader();
  private BeanFactory beanFactory;
  private boolean throwWhenClassNotFound = false;

  private final DefaultMultiValueMap<String, String> strategies = new DefaultMultiValueMap<>();

  public void loadStrategies() {
    if (strategies.isEmpty()) {
      loadStrategies(strategiesLocation);
    }
  }

  public void loadStrategies(String strategiesLocation) {
    strategiesReader.read(strategiesLocation, strategies);
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> getStrategies(Class<T> strategyClass) {
    Assert.notNull(strategyClass, "strategy-class must not be nul");
    // get class list by class full name
    final ArrayList<T> strategiesObject = new ArrayList<>();
    consumeStrategyClasses(strategyClass.getName(), strategy -> {
      final Object instance = ClassUtils.newInstance(strategy, beanFactory);
      if (strategyClass.isInstance(instance)) {
        strategiesObject.add((T) instance);
      }
    });
    return strategiesObject;
  }

  public Set<Class<?>> getStrategyClasses(String strategyKey) {
    // get class list by class full name
    final LinkedHashSet<Class<?>> strategies = new LinkedHashSet<>();
    consumeStrategyClasses(strategyKey, strategies::add);
    return strategies;
  }

  public void consumeStrategyClasses(String strategyKey, Consumer<Class<?>> consumer) {
    Assert.notNull(strategyKey, "strategy-key must not be nul");
    consumeStrategies(strategyKey, strategy -> {
      final Class<?> aClass = loadClass(classLoader, strategy);
      if (aClass != null) {
        consumer.accept(aClass);
      }
    });
  }

  public void consumeStrategies(String strategyKey, Consumer<String> consumer) {
    Assert.notNull(strategyKey, "strategy-key must not be nul");
    // get class list by class full name
    final List<String> strategies = getStrategies(strategyKey);
    if (!CollectionUtils.isEmpty(strategies)) {
      for (final String strategy : strategies) {
        consumer.accept(strategy);
      }
    }
  }

  public List<?> consumeStrategies(String strategyKey) {
    Assert.notNull(strategyKey, "strategy-key must not be nul");
    // get class list by class full name
    final List<String> strategies = getStrategies(strategyKey);
    final BeanFactory beanFactory = getBeanFactory();
    final ClassLoader classLoader = getClassLoader();
    final ArrayList<Object> strategiesObject = new ArrayList<>();

    for (final String strategy : strategies) {
      final Class<?> aClass = loadClass(classLoader, strategy);
      if (aClass != null) {
        final Object instance = ClassUtils.newInstance(aClass, beanFactory);
        strategiesObject.add(instance);
      }
    }
    return strategiesObject;
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

  public List<String> getStrategies(String key) {
    loadStrategies();
    return strategies.get(key);
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
