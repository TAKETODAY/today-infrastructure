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
package cn.taketoday.beans.factory;

import cn.taketoday.beans.BeansException;

/**
 * Interface to be implemented by beans that wish to be aware of their
 * owning {@link BeanFactory}.
 *
 * <p>For example, beans can look up collaborating (same factory) beans via the factory
 * (Dependency Lookup). Note that most beans will choose to receive references
 * to collaborating beans via corresponding bean properties or constructor
 * arguments (Dependency Injection).
 *
 * @author TODAY 2018-08-03 15:54
 */
public interface BeanFactoryAware extends Aware {

  /**
   * Callback that supplies the owning factory to a bean instance.
   * <p>Invoked after the population of normal bean properties
   * but before an initialization callback such as
   * {@link InitializingBean#afterPropertiesSet()} or a custom init-method.
   *
   * @param beanFactory owning BeanFactory (never {@code null}).
   * The bean can immediately call methods on the factory.
   * @throws BeansException in case of initialization errors
   * @see BeanInitializationException
   */
  void setBeanFactory(BeanFactory beanFactory) throws BeansException;
}
