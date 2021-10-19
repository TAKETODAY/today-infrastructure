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

package cn.taketoday.beans.factory;

import java.lang.reflect.Field;
import java.util.Objects;

import cn.taketoday.lang.Assert;

/**
 * Use BeanReference to resolve value
 *
 * @author TODAY 2021/3/6 15:18
 * @since 3.0
 */
public class BeanReferencePropertySetter extends AbstractPropertySetter {
  /** reference name */
  private final String referenceName;
  /** property is required? **/
  private final boolean required;
  /** record reference type @since v2.1.2 */
  private final Class<?> referenceClass;
  /** record if property is prototype @since v2.1.6 */
  private boolean prototype = false;
  /** @since 3.0.2 */
  private BeanDefinition reference;

  /** @since 3.0.2 */
  public BeanReferencePropertySetter(String referenceName, boolean required, Field field) {
    super(field);
    Assert.notNull(referenceName, "Bean name can't be null");
    this.referenceName = referenceName;
    this.required = required;
    this.referenceClass = field.getType();
  }

  @Override
  protected Object resolveValue(AbstractBeanFactory beanFactory) {
    // fix: same name of bean
    final Object value = resolveBeanReference(beanFactory);
    if (value == null) {
      if (required) {
        throw new NoSuchBeanDefinitionException(reference.getName(), referenceClass);
      }
      return DO_NOT_SET; // if reference bean is null, and it is not required ,do nothing,default value
    }
    return value;
  }

  /**
   * Resolve reference {@link PropertySetter}
   *
   * @return A {@link PropertySetter} bean or a proxy
   * @see ConfigurableBeanFactory#isFullLifecycle()
   * @see ConfigurableBeanFactory#isFullPrototype()
   */
  protected Object resolveBeanReference(AbstractBeanFactory beanFactory) {
    final String name = referenceName;
    final Class<?> type = getReferenceClass();

    if (beanFactory.isFullPrototype() && prototype && beanFactory.containsBeanDefinition(name)) {
      return Prototypes.newProxyInstance(type, beanFactory.getBeanDefinition(name), beanFactory);
    }
    final BeanDefinition reference = getReference();
    if (reference != null) {
      return beanFactory.getBean(reference);
    }
    final Object bean = beanFactory.getBean(name, type);
    return bean != null ? bean : beanFactory.doGetBeanForType(type);
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
  public void applyPrototype() {
    this.prototype = true;
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

  /** @since 3.0.2 */
  public void setReference(BeanDefinition reference) {
    this.reference = reference;
  }

  //

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof BeanReferencePropertySetter))
      return false;
    if (!super.equals(o))
      return false;
    final BeanReferencePropertySetter that = (BeanReferencePropertySetter) o;
    return required == that.required
            && Objects.equals(referenceName, that.referenceName)
            && Objects.equals(referenceClass, that.referenceClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), referenceName, required, referenceClass);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("{\n\t\"referenceName\":\"");
    builder.append(referenceName);
    builder.append("\",\n\t\"required\":\"");
    builder.append(required);
    builder.append("\",\n\t\"referenceClass\":\"");
    builder.append(referenceClass);
    builder.append("\",\n\t\"field\":\"");
    builder.append(getField());
    builder.append("\",\n\t\"prototype\":\"");
    builder.append(isPrototype());
    builder.append("\"\n}");
    return builder.toString();
  }

}
