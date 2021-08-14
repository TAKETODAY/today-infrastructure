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
package cn.taketoday.beans;

import cn.taketoday.beans.factory.BeanFactory;

/**
 * Interface to be implemented by objects used within a {@link BeanFactory}
 * which are themselves factories for individual objects. If a bean implements
 * this interface, it is used as a factory for an object to expose, not directly
 * as a bean instance that will be exposed itself.
 *
 * <p>
 * <b>NB: A bean that implements this interface cannot be used as a normal
 * bean.</b> A FactoryBean is defined in a bean style, but the object exposed
 * for bean references ({@link #getBean()}) is always the object that it creates
 * ,and initialization by this factory.
 *
 * <p>
 * The implementation of FactoryBean is a factory; its instance will cached in
 * {@link BeanFactory} as a singleton bean.
 *
 * @author TODAY <br>
 * 2018-08-03 17:38
 */
public interface FactoryBean<T> {

  /**
   * Get the bean instance
   *
   * @return bean instance
   */
  T getBean();

  /**
   * Get the bean class
   *
   * @return bean class
   *
   * @since 2.1.2
   */
  Class<T> getBeanClass();

}
