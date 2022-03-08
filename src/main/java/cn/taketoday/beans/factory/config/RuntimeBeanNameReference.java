/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans.factory.config;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Immutable placeholder class used for a property value object when it's a
 * reference to another bean name in the factory, to be resolved at runtime.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RuntimeBeanReference
 * @see BeanDefinition#getPropertyValues()
 * @see BeanFactory#getBean
 * @since 4.0 2022/1/9 14:39
 */
public class RuntimeBeanNameReference implements BeanReference {

  private final String beanName;

  @Nullable
  private Object source;

  /**
   * Create a new RuntimeBeanNameReference to the given bean name.
   *
   * @param beanName name of the target bean
   */
  public RuntimeBeanNameReference(String beanName) {
    Assert.hasText(beanName, "'beanName' must not be empty");
    this.beanName = beanName;
  }

  @Override
  public String getBeanName() {
    return this.beanName;
  }

  /**
   * Set the configuration source {@code Object} for this metadata element.
   * <p>The exact type of the object will depend on the configuration mechanism used.
   */
  public void setSource(@Nullable Object source) {
    this.source = source;
  }

  @Override
  @Nullable
  public Object getSource() {
    return this.source;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof RuntimeBeanNameReference that)) {
      return false;
    }
    return this.beanName.equals(that.beanName);
  }

  @Override
  public int hashCode() {
    return this.beanName.hashCode();
  }

  @Override
  public String toString() {
    return '<' + getBeanName() + '>';
  }

}
