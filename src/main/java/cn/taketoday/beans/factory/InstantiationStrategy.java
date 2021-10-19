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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.beans.factory;

import cn.taketoday.beans.BeansException;
import cn.taketoday.lang.Nullable;

import java.lang.reflect.Constructor;

/**
 * Interface responsible for creating instances corresponding to a bean definition.
 *
 * <p>This is pulled out into a strategy as various approaches are possible,
 * including using CGLIB to create subclasses on the fly to support Method Injection.
 *
 * @author TODAY 2021/10/19 17:11
 * @since 4.0
 */
public interface InstantiationStrategy {

  /**
   * Return an instance of the bean with the given name in this factory.
   *
   * @param bd the bean definition
   * @param beanName the name of the bean when it is created in this context.
   * The name can be {@code null} if we are autowiring a bean which doesn't
   * belong to the factory.
   * @param owner the owning BeanFactory
   * @return a bean instance for this bean definition
   * @throws BeansException if the instantiation attempt failed
   */
  Object instantiate(BeanDefinition bd, @Nullable String beanName, BeanFactory owner)
          throws BeansException;

  /**
   * Return an instance of the bean with the given name in this factory,
   * creating it via the given constructor.
   *
   * @param bd the bean definition
   * @param beanName the name of the bean when it is created in this context.
   * The name can be {@code null} if we are autowiring a bean which doesn't
   * belong to the factory.
   * @param owner the owning BeanFactory
   * @param args the constructor arguments to apply
   * @return a bean instance for this bean definition
   * @throws BeansException if the instantiation attempt failed
   */
  Object instantiate(BeanDefinition bd, @Nullable String beanName, BeanFactory owner, Object... args) throws BeansException;

}
