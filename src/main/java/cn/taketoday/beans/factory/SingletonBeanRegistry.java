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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.beans.factory;

import java.util.Map;

import cn.taketoday.beans.BeanNameCreator;

/**
 * @author TODAY 2018-11-14 19:47
 * @since 2.0.1
 */
public interface SingletonBeanRegistry {

  /**
   * Register a singleton to context
   *
   * @param name
   *         bean name
   * @param bean
   *         bean instance
   */
  void registerSingleton(String name, Object bean);

  /**
   * Register a singleton to context user {@link BeanNameCreator} to create a name
   *
   * @param bean
   *         bean instance
   *
   * @since 2.1.2
   */
  void registerSingleton(Object bean);

  /**
   * Get all instances Map
   *
   * @return the map of singletons
   */
  Map<String, Object> getSingletons();

  /**
   * get bean instance, one {@link BeanDefinition} can have a lot of names, so
   * can't put instances in BeanDefinition.
   *
   * @param name
   *         bean name
   *
   * @return bean instance
   */
  Object getSingleton(String name);

  /**
   * Get singleton objects
   *
   * @param requiredType
   *         required type
   * @param <T>
   *         required type
   *
   * @return singleton object
   */
  <T> T getSingleton(Class<T> requiredType);

  /**
   * remove a singleton with given name
   *
   * @param name
   *         bean name
   */
  void removeSingleton(String name);

  /**
   * contains instance with given name?
   *
   * @param name
   *         bean name
   *
   * @return if contains singleton
   */
  boolean containsSingleton(String name);

}
