/*
 * Copyright 2017 - 2023 the original author or authors.
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

import cn.taketoday.beans.BeanMetadataAttributeAccessor;
import cn.taketoday.beans.BeanMetadataElement;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

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
 * @see cn.taketoday.beans.factory.BeanNameAware
 * @see cn.taketoday.beans.factory.support.RootBeanDefinition
 * @see cn.taketoday.beans.factory.support.ChildBeanDefinition
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
  public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName, @Nullable String[] aliases) {
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
      return "Bean definition with name '" + beanName + "'";
    }
    return "Bean definition with name '" + beanName + "' and aliases [" + StringUtils.arrayToCommaDelimitedString(this.aliases) + ']';
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

  @Nullable
  static String[] computeAliases(BeanDefinition beanDefinition, @Nullable String[] aliases) {
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
