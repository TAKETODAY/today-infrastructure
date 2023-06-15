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

package cn.taketoday.test.context.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Collection of utilities for working with {@link cn.taketoday.lang.TodayStrategies} within
 * the <em>Infra TestContext Framework</em>.
 *
 * <p>Primarily intended for use within the TestContext framework.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/15 21:14
 */
public abstract class TestContextFactoriesUtils {

  private static final Logger logger = LoggerFactory.getLogger(TestContextFactoriesUtils.class);

  /**
   * Load factory implementations of the given type via the
   * {@link cn.taketoday.lang.TodayStrategies} mechanism.
   * <p>This method utilizes a custom {@link TodayStrategies.FailureHandler} and DEBUG/TRACE logging
   * that are specific to the needs of the <em>Infra TestContext Framework</em>.
   * <p>Specifically, this method looks up and instantiates all {@code factoryType}
   * entries configured in all {@code META-INF/today-strategies.properties} files on the classpath.
   * <p>If a particular factory implementation cannot be loaded due to a {@link LinkageError}
   * or {@link ClassNotFoundException}, a {@code DEBUG} message will be logged,
   * but the associated exception will not be rethrown. A {@link RuntimeException}
   * or any other {@link Error} will be rethrown. Any other exception will be
   * thrown wrapped in an {@link IllegalStateException}.
   *
   * @param <T> the factory type
   * @param factoryType the interface or abstract class representing the factory
   * @return an unmodifiable list of factory implementations
   * @see TodayStrategies#forDefaultResourceLocation(ClassLoader)
   * @see TodayStrategies#load(Class, TodayStrategies.FailureHandler)
   */
  public static <T> List<T> loadFactoryImplementations(Class<T> factoryType) {
    TodayStrategies loader = TodayStrategies.forDefaultResourceLocation(
            TestContextFactoriesUtils.class.getClassLoader());
    List<T> implementations = loader.load(factoryType, new TestContextFailureHandler());
    if (logger.isTraceEnabled()) {
      logger.trace("Loaded %s implementations from location [%s]: %s"
              .formatted(factoryType.getSimpleName(), TodayStrategies.STRATEGIES_LOCATION, classNames(implementations)));
    }
    return Collections.unmodifiableList(implementations);
  }

  private static List<String> classNames(Collection<?> components) {
    return components.stream().map(Object::getClass).map(Class::getName).toList();
  }

}
