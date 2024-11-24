/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.beans.testfixture.beans.factory.aot;

import infra.beans.BeanUtils;
import infra.beans.factory.FactoryBean;
import infra.lang.Nullable;

/**
 * A public {@link FactoryBean} with a generic type.
 *
 * @author Stephane Nicoll
 */
public class GenericFactoryBean<T> implements FactoryBean<T> {

  private final Class<T> beanType;

  public GenericFactoryBean(Class<T> beanType) {
    this.beanType = beanType;
  }

  @Nullable
  @Override
  public T getObject() throws Exception {
    return BeanUtils.newInstance(this.beanType);
  }

  @Nullable
  @Override
  public Class<?> getObjectType() {
    return this.beanType;
  }
}
