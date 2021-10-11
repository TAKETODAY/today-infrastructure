/**
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

import java.util.function.Supplier;

import cn.taketoday.beans.FactoryBean;
import cn.taketoday.lang.Assert;

/**
 * {@link FactoryBean} {@link Supplier}
 *
 * @author TODAY <br>
 * 2019-12-11 22:32
 */
public class FactoryBeanSupplier<T> implements Supplier<FactoryBean<T>> {

  private FactoryBean<T> factoryBean;
  private final BeanDefinition factoryDef;
  private final AbstractBeanFactory beanFactory;

  public FactoryBeanSupplier(BeanDefinition factoryDef, AbstractBeanFactory beanFactory) {
    Assert.notNull(beanFactory, "beanFactory must not be null");
    Assert.notNull(factoryDef, "factory BeanDefinition must not be null");
    Assert.isAssignable(FactoryBean.class, factoryDef.getBeanClass(),
                        "Target bean class must be 'cn.taketoday.beans.FactoryBean'");
    this.beanFactory = beanFactory;
    this.factoryDef = factoryDef instanceof FactoryBeanDefinition
                      ? ((FactoryBeanDefinition<?>) factoryDef).getFactoryDefinition()
                      : factoryDef;
  }

  @Override
  public FactoryBean<T> get() {
    final FactoryBean<T> factoryBean = this.factoryBean;
    if (factoryBean == null) {
      return this.factoryBean = beanFactory.getFactoryBean(factoryDef);
    }
    return factoryBean;
  }

}
