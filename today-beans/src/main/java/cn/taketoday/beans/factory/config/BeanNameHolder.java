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

import java.util.Arrays;
import java.util.Objects;

import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Holder for a bean with name and aliases.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/12 22:50
 */
public class BeanNameHolder {
  public static final String AttributeName = "beanNameHolder";

  protected final String beanName;

  @Nullable
  protected final String[] aliases;

  public BeanNameHolder(String beanName, @Nullable String[] aliases) {
    Assert.notNull(beanName, "Bean name is required");
    this.beanName = beanName;
    this.aliases = aliases;
  }

  /**
   * Return the primary name of the bean, as specified for the bean definition.
   */
  public String getBeanName() {
    return this.beanName;
  }

  /**
   * Return the alias names for the bean, as specified directly for the bean definition.
   *
   * @return the array of alias names, or {@code null} if none
   */
  @Nullable
  public String[] getAliases() {
    return this.aliases;
  }

  /**
   * Determine whether the given candidate name matches the bean name
   * or the aliases stored in this bean definition.
   */
  public boolean matchesName(@Nullable String candidateName) {
    return candidateName != null
            && (
            candidateName.equals(beanName)
                    || candidateName.equals(BeanFactoryUtils.transformedBeanName(beanName))
                    || ObjectUtils.containsElement(aliases, candidateName)
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof BeanNameHolder that))
      return false;
    return Objects.equals(beanName, that.beanName)
            && Arrays.equals(aliases, that.aliases);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(beanName);
    result = 31 * result + Arrays.hashCode(aliases);
    return result;
  }

  @Override
  public String toString() {
    if (aliases == null) {
      return "Bean name '" + beanName + "'";
    }
    return "Bean name '" + beanName + "' and aliases [" + StringUtils.arrayToCommaDelimitedString(aliases) + ']';
  }

  // static
  @Nullable
  public static BeanNameHolder find(AttributeAccessor accessor) {
    return (BeanNameHolder) accessor.getAttribute(AttributeName);
  }

}
