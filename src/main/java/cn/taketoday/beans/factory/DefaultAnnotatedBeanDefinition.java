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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.beans.factory;

import java.util.Objects;

import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.core.type.StandardAnnotationMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Extension of the {@link DefaultBeanDefinition} class, adding support
 * for annotation metadata exposed through the {@link AnnotatedBeanDefinition} interface.
 *
 * <p>This DefaultBeanDefinition variant is mainly useful for testing code that expects
 * to operate on an AnnotatedBeanDefinition
 *
 * @author TODAY 2021/10/25 17:37
 * @since 4.0
 */
public class DefaultAnnotatedBeanDefinition extends DefaultBeanDefinition implements AnnotatedBeanDefinition {

  private final AnnotationMetadata metadata;

  @Nullable
  private MethodMetadata factoryMethodMetadata;

  /**
   * Create a new AnnotatedGenericBeanDefinition for the given bean class.
   *
   * @param beanClass the loaded bean class
   */
  public DefaultAnnotatedBeanDefinition(Class<?> beanClass) {
    setBeanClass(beanClass);
    this.metadata = AnnotationMetadata.introspect(beanClass);
  }

  /**
   * Create a new AnnotatedGenericBeanDefinition for the given annotation metadata,
   * allowing for ASM-based processing and avoidance of early loading of the bean class.
   *
   * @param metadata the annotation metadata for the bean class in question
   */
  public DefaultAnnotatedBeanDefinition(AnnotationMetadata metadata) {
    Assert.notNull(metadata, "AnnotationMetadata must not be null");
    if (metadata instanceof StandardAnnotationMetadata) {
      setBeanClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass());
    }
    else {
      setBeanClassName(metadata.getClassName());
    }
    this.metadata = metadata;
  }

  /**
   * Create a new AnnotatedGenericBeanDefinition for the given annotation metadata,
   * based on an annotated class and a factory method on that class.
   *
   * @param metadata the annotation metadata for the bean class in question
   * @param factoryMethodMetadata metadata for the selected factory method
   */
  public DefaultAnnotatedBeanDefinition(AnnotationMetadata metadata, @Nullable MethodMetadata factoryMethodMetadata) {
    this(metadata);
    Assert.notNull(factoryMethodMetadata, "MethodMetadata must not be null");
    this.factoryMethodMetadata = factoryMethodMetadata;
  }

  @Override
  public final AnnotationMetadata getMetadata() {
    return this.metadata;
  }

  @Override
  @Nullable
  public final MethodMetadata getFactoryMethodMetadata() {
    return this.factoryMethodMetadata;
  }

  @Override
  protected BeanInstantiator createConstructor(BeanFactory factory) {
    MethodMetadata factoryMethodMetadata = getFactoryMethodMetadata();
    if (factoryMethodMetadata != null) {
      String methodName = factoryMethodMetadata.getMethodName();
      Class<?> beanClass = getBeanClass();



    }

    return super.createConstructor(factory);
  }

  @Override
  public BeanDefinition cloneDefinition() {
    DefaultAnnotatedBeanDefinition definition = new DefaultAnnotatedBeanDefinition(metadata);
    definition.copyFrom(this);
    return definition;
  }

  @Override
  public void copyFrom(BeanDefinition from) {
    super.copyFrom(from);
    if (from instanceof AnnotatedBeanDefinition) {
      this.factoryMethodMetadata = ((AnnotatedBeanDefinition) from).getFactoryMethodMetadata();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    DefaultAnnotatedBeanDefinition that = (DefaultAnnotatedBeanDefinition) o;
    return Objects.equals(metadata, that.metadata) && Objects.equals(factoryMethodMetadata, that.factoryMethodMetadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), metadata, factoryMethodMetadata);
  }
}
