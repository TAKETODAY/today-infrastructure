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

package cn.taketoday.beans.dependency;

import cn.taketoday.beans.factory.AbstractBeanFactory;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.Prototypes;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/18 22:55</a>
 * @since 4.0
 */
public class BeanReferenceRetriever {

  /** reference name */
  @Nullable
  private final String referenceName;
  /** property is required? **/
  private final boolean required;
  /** record reference type @since v2.1.2 */
  private final Class<?> referenceClass;
  /** record if property is prototype @since v2.1.6 */
  private boolean prototype = false;
  /** @since 3.0.2 */
  private BeanDefinition reference;

  public BeanReferenceRetriever(
          @Nullable String referenceName, boolean required, Class<?> referenceClass) {
    this.referenceName = referenceName;
    this.required = required;
    this.referenceClass = referenceClass;
  }

  public Object retrieve(BeanFactory beanFactory) {
    // fix: same name of bean
    Object value = resolveBeanReference(beanFactory);
    if (value == null) {
      if (required) {
        if (StringUtils.hasText(referenceName)) {
          throw new NoSuchBeanDefinitionException(referenceName, referenceClass);
        }
        throw new NoSuchBeanDefinitionException(referenceClass);
      }
      return null; // if reference bean is null, and it is not required ,do nothing,default value
    }
    return value;
  }

  /**
   * Resolve reference
   *
   * @return A bean or a proxy
   * @see BeanFactory#isFullLifecycle()
   * @see BeanFactory#isFullPrototype()
   */
  protected Object resolveBeanReference(BeanFactory beanFactory) {
    Class<?> type = getReferenceClass();
    if (!StringUtils.hasText(referenceName)) {
      // by-type
      return beanFactory.getBean(type);
    }

    // by-name
    if (reference != null) {
      if (prototype && beanFactory.isFullPrototype()) {
        return Prototypes.newProxyInstance(type, reference, beanFactory);
      }
      return beanFactory.getBean(reference);
    }

    if (prototype && beanFactory.isFullPrototype() && beanFactory.containsBeanDefinition(referenceName)) {
      return Prototypes.newProxyInstance(type, beanFactory.getBeanDefinition(referenceName), beanFactory);
    }

    // by-name
    return beanFactory.getBean(referenceName, type);
  }

  /** @since 3.0.2 */
  public void setReference(BeanDefinition reference, AbstractBeanFactory beanFactory) {
    this.reference = reference;
    if (beanFactory.isFullPrototype()) {
      setPrototype(reference.isPrototype());
    }
  }

  /** @since 3.0.2 */
  public boolean isRequired() {
    return required;
  }

  /** @since 3.0.2 */
  public Class<?> getReferenceClass() {
    return referenceClass;
  }

  public String getReferenceName() {
    return referenceName;
  }

  /** @since 3.0.2 */
  public boolean isPrototype() {
    return prototype;
  }

  /** @since 3.0.2 */
  public void setPrototype(boolean prototype) {
    this.prototype = prototype;
  }

  /** @since 3.0.2 */
  public BeanDefinition getReference() {
    return reference;
  }
}
