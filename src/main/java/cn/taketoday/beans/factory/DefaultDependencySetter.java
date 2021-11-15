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

import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.core.style.ToStringBuilder;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Default implementation of {@link DependencySetter}
 *
 * @author TODAY 2021/3/6 14:55
 * @since 3.0
 */
public class DefaultDependencySetter extends AbstractDependencySetter {

  /** property value */
  private final Object value;

  public DefaultDependencySetter(Object value, Field property) {
    super(property);
    this.value = value;
  }

  public DefaultDependencySetter(Object value, BeanProperty property) {
    super(property);
    this.value = value;
  }

  public Object getValue() {
    return value;
  }

  @Override
  protected Object resolveValue(ConfigurableBeanFactory beanFactory) {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final DefaultDependencySetter that))
      return false;
    if (!super.equals(o))
      return false;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("value", value)
            .append("property", property.getName())
            .append("propertyClass", property.getType())
            .append("beanClass", property.getDeclaringClass())
            .toString();
  }

}
