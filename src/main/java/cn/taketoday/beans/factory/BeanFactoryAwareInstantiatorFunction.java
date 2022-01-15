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

import java.util.function.Function;

import cn.taketoday.beans.factory.support.BeanFactoryAwareBeanInstantiator;
import cn.taketoday.lang.Assert;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/15 23:34
 */
public class BeanFactoryAwareInstantiatorFunction<T> implements Function<Class<T>, T> {
  private final BeanFactoryAwareBeanInstantiator instantiator;

  public BeanFactoryAwareInstantiatorFunction(BeanFactory beanFactory) {
    this.instantiator = BeanFactoryAwareBeanInstantiator.from(beanFactory);
  }

  public BeanFactoryAwareInstantiatorFunction(BeanFactoryAwareBeanInstantiator instantiator) {
    Assert.notNull(instantiator, "instantiator is required");
    this.instantiator = instantiator;
  }

  @Override
  public T apply(Class<T> strategyImpl) {
    return instantiator.instantiate(strategyImpl);
  }

}
