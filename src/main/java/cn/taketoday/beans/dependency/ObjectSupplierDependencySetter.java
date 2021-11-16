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

package cn.taketoday.beans.dependency;

import java.util.Objects;

import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.core.ResolvableType;

/**
 * {@link cn.taketoday.beans.factory.ObjectSupplier} property value
 *
 * @author TODAY 2021/11/15 23:04
 * @since 3.0
 */
public class ObjectSupplierDependencySetter
        extends AbstractDependencySetter implements DependencySetter {

  final ResolvableType generic;

  public ObjectSupplierDependencySetter(BeanProperty property, ResolvableType generic) {
    super(property);
    this.generic = generic;
  }

  @Override
  protected Object resolveValue(ConfigurableBeanFactory beanFactory) {
    return beanFactory.getObjectSupplier(generic);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ObjectSupplierDependencySetter that))
      return false;
    if (!super.equals(o))
      return false;
    return Objects.equals(generic, that.generic);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), generic);
  }

  @Override
  public String toString() {
    return "ObjectSupplierPropertyValue{" +
            "generic=" + generic +
            '}';
  }
}

