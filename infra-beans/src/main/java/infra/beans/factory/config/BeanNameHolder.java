/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
