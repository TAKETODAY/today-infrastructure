/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.test.io.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;

/**
 * Simple mock {@link TodayStrategies} implementation that can be used for testing
 * purposes.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class MockTodayStrategies extends TodayStrategies {

  private final AtomicInteger sequence = new AtomicInteger();

  private final Map<String, List<String>> factories;

  private final Map<String, Object> implementations = new HashMap<>();

  /**
   * Create a new {@link MockTodayStrategies} instance with the default
   * classloader.
   */
  public MockTodayStrategies() {
    this(null);
  }

  /**
   * Create a new {@link MockTodayStrategies} instance with the given classloader.
   *
   * @param classLoader the classloader to use
   */
  public MockTodayStrategies(@Nullable ClassLoader classLoader) {
    this(classLoader, new LinkedHashMap<>());
  }

  protected MockTodayStrategies(@Nullable ClassLoader classLoader,
          Map<String, List<String>> factories) {
    super(classLoader, factories);
    this.factories = factories;
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  protected <T> T instantiateStrategy(String implementationName, Class<T> type, Instantiator instantiator, FailureHandler failureHandler) {
    if (implementationName.startsWith("!")) {
      Object implementation = this.implementations.get(implementationName);
      if (implementation != null) {
        return (T) implementation;
      }
    }
    return super.instantiateStrategy(implementationName, type, instantiator, failureHandler);
  }

  /**
   * Add factory implementations to this instance.
   *
   * @param factoryType the factory type class
   * @param factoryImplementations the implementation classes
   */
  @SafeVarargs
  public final <T> void add(Class<T> factoryType, Class<? extends T>... factoryImplementations) {
    for (Class<? extends T> factoryImplementation : factoryImplementations) {
      add(factoryType.getName(), factoryImplementation.getName());
    }
  }

  /**
   * Add factory implementations to this instance.
   *
   * @param factoryType the factory type class name
   * @param factoryImplementations the implementation class names
   */
  public void add(String factoryType, String... factoryImplementations) {
    List<String> implementations = this.factories.computeIfAbsent(
            factoryType, key -> new ArrayList<>());
    Collections.addAll(implementations, factoryImplementations);
  }

  /**
   * Add factory instances to this instance.
   *
   * @param factoryType the factory type class
   * @param factoryInstances the implementation instances to add
   */
  @SuppressWarnings("unchecked")
  public <T> void addInstance(Class<T> factoryType, T... factoryInstances) {
    addInstance(factoryType.getName(), factoryInstances);
  }

  /**
   * Add factory instances to this instance.
   *
   * @param factoryType the factory type class name
   * @param factoryInstance the implementation instances to add
   */
  @SuppressWarnings("unchecked")
  public <T> void addInstance(String factoryType, T... factoryInstance) {
    List<String> implementations = this.factories.computeIfAbsent(factoryType, key -> new ArrayList<>());
    for (T factoryImplementation : factoryInstance) {
      String reference = "!" + factoryType + ":" + factoryImplementation.getClass().getName()
              + this.sequence.getAndIncrement();
      implementations.add(reference);
      this.implementations.put(reference, factoryImplementation);
    }
  }

}
