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

package cn.taketoday.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;

import cn.taketoday.core.annotation.Order;
import cn.taketoday.lang.Nullable;

/**
 * Strategy interface for creating {@link BeanInfo} instances for Framework beans.
 * Can be used to plug in custom bean property resolution strategies (e.g. for other
 * languages on the JVM) or more efficient {@link BeanInfo} retrieval algorithms.
 *
 * <p>BeanInfoFactories are instantiated by the {@link CachedIntrospectionResults},
 * by using the {@link cn.taketoday.lang.TodayStrategies} utility class.
 *
 * When a {@link BeanInfo} is to be created, the {@code CachedIntrospectionResults}
 * will iterate through the discovered factories, calling {@link #getBeanInfo(Class)}
 * on each one. If {@code null} is returned, the next factory will be queried.
 * If none of the factories support the class, a standard {@link BeanInfo} will be
 * created as a default.
 *
 * <p>Note that the {@link cn.taketoday.lang.TodayStrategies} sorts the {@code
 * BeanInfoFactory} instances by {@link Order @Order}, so that ones with a
 * higher precedence come first.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CachedIntrospectionResults
 * @see cn.taketoday.lang.TodayStrategies
 * @since 4.0 2022/2/23 11:25
 */
public interface BeanInfoFactory {

  /**
   * Return the bean info for the given class, if supported.
   *
   * @param beanClass the bean class
   * @return the BeanInfo, or {@code null} if the given class is not supported
   * @throws IntrospectionException in case of exceptions
   */
  @Nullable
  BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException;

}

