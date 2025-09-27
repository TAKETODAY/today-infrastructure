/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans.factory.config;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

import infra.beans.factory.BeanFactoryUtils;
import infra.core.AttributeAccessor;
import infra.lang.Assert;
import infra.util.ObjectUtils;
import infra.util.StringUtils;

/**
 * Holder for a bean with name and aliases.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/12 22:50
 */
public class BeanNameHolder {

  public static final String AttributeName = "beanNameHolder";

  protected final String beanName;

  protected final String @Nullable [] aliases;

  public BeanNameHolder(String beanName, String @Nullable [] aliases) {
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
  public String @Nullable [] getAliases() {
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
      return "Bean name '%s'".formatted(beanName);
    }
    return "Bean name '%s' and aliases [%s]".formatted(beanName, StringUtils.arrayToCommaDelimitedString(aliases));
  }

  // static
  @Nullable
  public static BeanNameHolder find(AttributeAccessor accessor) {
    return (BeanNameHolder) accessor.getAttribute(AttributeName);
  }

}
