/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.factory.config;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;

import infra.beans.BeanMetadataAttributeAccessor;
import infra.beans.BeanMetadataElement;
import infra.beans.factory.BeanNameAware;
import infra.beans.factory.support.ChildBeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.lang.Assert;
import infra.util.ObjectUtils;
import infra.util.StringUtils;

/**
 * Holder for a BeanDefinition with name and aliases.
 * Can be registered as a placeholder for an inner bean.
 *
 * <p>Can also be used for programmatic registration of inner bean
 * definitions. If you don't care about BeanNameAware and the like,
 * registering RootBeanDefinition or ChildBeanDefinition is good enough.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanNameAware
 * @see RootBeanDefinition
 * @see ChildBeanDefinition
 * @since 4.0 2022/3/9 9:03
 */
public class BeanDefinitionHolder extends BeanNameHolder implements BeanMetadataElement {

  private final BeanDefinition beanDefinition;

  /**
   * Create a new BeanDefinitionHolder.
   *
   * @param beanDefinition the BeanDefinition to wrap
   * @param beanName the name of the bean, as specified for the bean definition
   */
  public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName) {
    this(beanDefinition, beanName, null);
  }

  /**
   * Create a new BeanDefinitionHolder.
   *
   * @param beanDefinition the BeanDefinition to wrap
   * @param beanName the name of the bean, as specified for the bean definition
   * @param aliases alias names for the bean, or {@code null} if none
   */
  public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName, String @Nullable [] aliases) {
    super(beanName, computeAliases(beanDefinition, aliases));
    Assert.notNull(beanDefinition, "BeanDefinition is required");
    this.beanDefinition = beanDefinition;
  }

  /**
   * Copy constructor: Create a new BeanDefinitionHolder with the
   * same contents as the given BeanDefinitionHolder instance.
   * <p>Note: The wrapped BeanDefinition reference is taken as-is;
   * it is {@code not} deeply copied.
   *
   * @param holder the BeanDefinitionHolder to copy
   * @throws NullPointerException if holder is null
   */
  public BeanDefinitionHolder(BeanDefinitionHolder holder) {
    super(holder.getBeanName(), holder.getAliases());
    this.beanDefinition = holder.getBeanDefinition();
  }

  /**
   * Create a new BeanDefinitionHolder.
   *
   * @param beanDefinition the BeanDefinition to wrap
   * @param holder the BeanNameHolder to copy
   * @throws NullPointerException if holder is null
   */
  public BeanDefinitionHolder(BeanDefinition beanDefinition, BeanNameHolder holder) {
    super(holder.getBeanName(), holder.getAliases());
    this.beanDefinition = beanDefinition;
  }

  /**
   * Return the wrapped BeanDefinition.
   */
  public BeanDefinition getBeanDefinition() {
    return this.beanDefinition;
  }

  /**
   * Expose the bean definition's source object.
   *
   * @see BeanDefinition#getSource()
   */
  @Override
  @Nullable
  public Object getSource() {
    return this.beanDefinition.getSource();
  }

  public void setSource(Object source) {
    if (beanDefinition instanceof BeanMetadataAttributeAccessor accessor) {
      accessor.setSource(source);
    }
  }

  /**
   * Return a friendly, short description for the bean, stating name and aliases.
   *
   * @see #getBeanName()
   * @see #getAliases()
   */
  public String getShortDescription() {
    if (aliases == null) {
      return "Bean definition with name '%s'".formatted(beanName);
    }
    return "Bean definition with name '%s' and aliases [%s]"
            .formatted(beanName, StringUtils.arrayToCommaDelimitedString(this.aliases));
  }

  /**
   * Return a long description for the bean, including name and aliases
   * as well as a description of the contained {@link BeanDefinition}.
   *
   * @see #getShortDescription()
   * @see #getBeanDefinition()
   */
  public String getLongDescription() {
    return getShortDescription() + ": " + beanDefinition;
  }

  /**
   * This implementation returns the long description. Can be overridden
   * to return the short description or any kind of custom description instead.
   *
   * @see #getLongDescription()
   * @see #getShortDescription()
   */
  @Override
  public String toString() {
    return getLongDescription();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BeanDefinitionHolder otherHolder)) {
      return false;
    }
    return beanName.equals(otherHolder.beanName)
            && Arrays.equals(aliases, otherHolder.aliases)
            && beanDefinition.equals(otherHolder.beanDefinition);
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHash(this.beanDefinition, this.beanName, this.aliases);
  }

  static String @Nullable [] computeAliases(BeanDefinition beanDefinition, String @Nullable [] aliases) {
    if (ObjectUtils.isNotEmpty(aliases)) {
      return aliases;
    }
    BeanNameHolder beanNameHolder = find(beanDefinition);
    if (beanNameHolder != null) {
      return beanNameHolder.getAliases();
    }
    return null;
  }

}
