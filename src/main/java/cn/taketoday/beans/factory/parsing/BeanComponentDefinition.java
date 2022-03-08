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

package cn.taketoday.beans.factory.parsing;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanReference;
import cn.taketoday.beans.factory.support.GenericBeanDefinition;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * ComponentDefinition based on a standard BeanDefinition, exposing the given bean
 * definition as well as inner bean definitions and bean references for the given bean.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public class BeanComponentDefinition extends GenericBeanDefinition implements ComponentDefinition {

  private final BeanDefinition[] innerBeanDefinitions;

  private final BeanReference[] beanReferences;

  /**
   * Create a new BeanComponentDefinition for the given bean.
   *
   * @param definition the BeanDefinition
   * @param beanName the name of the bean
   */
  public BeanComponentDefinition(BeanDefinition definition, String beanName) {
    this(definition);
    setBeanName(beanName);
  }

  /**
   * Create a new BeanComponentDefinition for the given bean.
   *
   * @param definition the BeanDefinition
   * @param beanName the name of the bean
   * @param aliases alias names for the bean, or {@code null} if none
   */
  public BeanComponentDefinition(BeanDefinition definition, String beanName, @Nullable String[] aliases) {
    this(definition);
    setBeanName(beanName);
    setAliases(aliases);
  }

  /**
   * Create a new BeanComponentDefinition for the given bean.
   *
   * @param definition the BeanDefinition encapsulating
   * the bean definition as well as the name of the bean
   */
  public BeanComponentDefinition(BeanDefinition definition) {
    super(definition);
    List<BeanDefinition> innerBeans = new ArrayList<>();
    List<BeanReference> references = new ArrayList<>();
    PropertyValues propertyValues = definition.getPropertyValues();
    for (PropertyValue propertyValue : propertyValues.toArray()) {
      Object value = propertyValue.getValue();
      if (value instanceof BeanDefinition) {
        innerBeans.add(((BeanDefinition) value));
      }
      else if (value instanceof BeanReference) {
        references.add((BeanReference) value);
      }
    }
    this.innerBeanDefinitions = innerBeans.toArray(new BeanDefinition[0]);
    this.beanReferences = references.toArray(new BeanReference[0]);
  }

  @Override
  public String getName() {
    return getBeanName();
  }

  @Override
  public String getDescription() {
    return getShortDescription();
  }

  @Override
  public BeanDefinition[] getBeanDefinitions() {
    return new BeanDefinition[] { this };
  }

  @Override
  public BeanDefinition[] getInnerBeanDefinitions() {
    return this.innerBeanDefinitions;
  }

  @Override
  public BeanReference[] getBeanReferences() {
    return this.beanReferences;
  }

  /**
   * This implementation returns this ComponentDefinition's description.
   *
   * @see #getDescription()
   */
  @Override
  public String toString() {
    return getDescription();
  }

  /**
   * Return a friendly, short description for the bean, stating name and aliases.
   *
   * @see #getBeanName()
   * @see #getAliases()
   */
  public String getShortDescription() {
    if (getAliases() == null) {
      return "Bean definition with name '" + getBeanName() + "'";
    }
    return "Bean definition with name '" + getBeanName() + "' and aliases [" + StringUtils.arrayToCommaDelimitedString(getAliases()) + ']';
  }

  /**
   * Return a long description for the bean, including name and aliases
   * as well as a description of the contained {@link BeanDefinition}.
   *
   * @see #getShortDescription()
   */
  public String getLongDescription() {
    return getShortDescription() + ": " + this;
  }

  /**
   * This implementations expects the other object to be of type BeanComponentDefinition
   * as well, in addition to the superclass's equality requirements.
   */
  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof BeanComponentDefinition && super.equals(other)));
  }

}
