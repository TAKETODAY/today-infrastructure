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

import cn.taketoday.beans.BeanMetadataAttributeAccessor;
import cn.taketoday.beans.BeanMetadataElement;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Experimental;
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
@Experimental
public class BeanDefinitionHolder implements BeanMetadataElement {

  private final BeanDefinition beanDefinition;

  private final String beanName;

  @Nullable
  private final String[] aliases;

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
    Assert.notNull(beanDefinition, "BeanDefinition must not be null");
    Assert.notNull(beanName, "Bean name must not be null");
    this.beanDefinition = beanDefinition;
    this.beanName = beanName;
    this.aliases = aliases;
  }

  /**
   * Copy constructor: Create a new BeanDefinitionHolder with the
   * same contents as the given BeanDefinitionHolder instance.
   * <p>Note: The wrapped BeanDefinition reference is taken as-is;
   * it is {@code not} deeply copied.
   *
   * @param beanDefinitionHolder the BeanDefinitionHolder to copy
   */
  public BeanDefinitionHolder(BeanDefinitionHolder beanDefinitionHolder) {
    Assert.notNull(beanDefinitionHolder, "BeanDefinitionHolder must not be null");
    this.beanDefinition = beanDefinitionHolder.getBeanDefinition();
    this.beanName = beanDefinitionHolder.getBeanName();
    this.aliases = beanDefinitionHolder.getAliases();
  }

  /**
   * Return the wrapped BeanDefinition.
   */
  public BeanDefinition getBeanDefinition() {
    return this.beanDefinition;
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
   * Determine whether the given candidate name matches the bean name
   * or the aliases stored in this bean definition.
   */
  public boolean matchesName(@Nullable String candidateName) {
    return candidateName != null && (candidateName.equals(this.beanName) ||
            candidateName.equals(BeanFactoryUtils.transformedBeanName(this.beanName)) ||
            ObjectUtils.containsElement(this.aliases, candidateName));
  }

  /**
   * Return a friendly, short description for the bean, stating name and aliases.
   *
   * @see #getBeanName()
   * @see #getAliases()
   */
  public String getShortDescription() {
    if (this.aliases == null) {
      return "Bean definition with name '" + this.beanName + "'";
    }
    return "Bean definition with name '" + this.beanName + "' and aliases [" + StringUtils.arrayToCommaDelimitedString(this.aliases) + ']';
  }

  /**
   * Return a long description for the bean, including name and aliases
   * as well as a description of the contained {@link BeanDefinition}.
   *
   * @see #getShortDescription()
   * @see #getBeanDefinition()
   */
  public String getLongDescription() {
    return getShortDescription() + ": " + this.beanDefinition;
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
    return this.beanDefinition.equals(otherHolder.beanDefinition) &&
            this.beanName.equals(otherHolder.beanName) &&
            ObjectUtils.nullSafeEquals(this.aliases, otherHolder.aliases);
  }

  @Override
  public int hashCode() {
    int hashCode = this.beanDefinition.hashCode();
    hashCode = 29 * hashCode + this.beanName.hashCode();
    hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.aliases);
    return hashCode;
  }

}
