/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans.factory.support;

import java.util.Objects;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.lang.Nullable;

/**
 * GenericBeanDefinition is a one-stop shop for declarative bean definition purposes.
 * Like all common bean definitions, it allows for specifying a class plus optionally
 * constructor argument values and property values. Additionally, deriving from a
 * parent bean definition can be flexibly configured through the "parentName" property.
 *
 * <p>In general, use this {@code GenericBeanDefinition} class for the purpose of
 * registering declarative bean definitions (e.g. XML definitions which a bean
 * post-processor might operate on, potentially even reconfiguring the parent name).
 * Use {@code RootBeanDefinition}/{@code ChildBeanDefinition} where parent/child
 * relationships happen to be pre-determined, and prefer {@link RootBeanDefinition}
 * specifically for programmatic definitions derived from factory methods/suppliers.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setParentName
 * @see RootBeanDefinition
 * @see ChildBeanDefinition
 * @since 4.0 2022/3/8 21:11
 */
public class GenericBeanDefinition extends AbstractBeanDefinition {

  @Nullable
  private String parentName;

  /**
   * Create a new GenericBeanDefinition, to be configured through its bean
   * properties and configuration methods.
   *
   * @see #setBeanClass
   * @see #setScope
   * @see #setConstructorArgumentValues
   * @see #setPropertyValues
   */
  public GenericBeanDefinition() {
    super();
  }

  /**
   * Create a new GenericBeanDefinition as deep copy of the given
   * bean definition.
   *
   * @param original the original bean definition to copy from
   */
  public GenericBeanDefinition(BeanDefinition original) {
    super(original);
  }

  @Override
  public void setParentName(@Nullable String parentName) {
    this.parentName = parentName;
  }

  @Override
  @Nullable
  public String getParentName() {
    return this.parentName;
  }

  @Override
  public AbstractBeanDefinition cloneBeanDefinition() {
    return new GenericBeanDefinition(this);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof GenericBeanDefinition that)) {
      return false;
    }
    return Objects.equals(this.parentName, that.parentName)
            && super.equals(other);
  }

  @Override
  public String toString() {
    if (this.parentName != null) {
      return "Generic bean with parent '" + this.parentName + "': " + super.toString();
    }
    return "Generic bean: " + super.toString();
  }

}
