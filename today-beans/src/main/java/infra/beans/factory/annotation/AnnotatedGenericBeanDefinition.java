/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.beans.factory.annotation;

import java.io.Serial;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.GenericBeanDefinition;
import infra.core.type.AnnotationMetadata;
import infra.core.type.MethodMetadata;
import infra.core.type.StandardAnnotationMetadata;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Extension of the {@link BeanDefinition}
 * class, adding support for annotation metadata exposed through the
 * {@link AnnotatedBeanDefinition} interface.
 *
 * <p>This GenericBeanDefinition variant is mainly useful for testing code that expects
 * to operate on an AnnotatedBeanDefinition, for example strategy implementations
 * in Framework's component scanning support (where the default definition class is
 * {@link infra.context.annotation.ScannedGenericBeanDefinition},
 * which also implements the AnnotatedBeanDefinition interface).
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AnnotatedBeanDefinition#getMetadata()
 * @see StandardAnnotationMetadata
 * @since 4.0 2022/3/8 21:01
 */
public class AnnotatedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition {

  @Serial
  private static final long serialVersionUID = 1L;

  private final AnnotationMetadata metadata;

  @Nullable
  private MethodMetadata factoryMethodMetadata;

  /**
   * Create a new AnnotatedGenericBeanDefinition for the given bean class.
   *
   * @param beanClass the loaded bean class
   */
  public AnnotatedGenericBeanDefinition(Class<?> beanClass) {
    setBeanClass(beanClass);
    this.metadata = AnnotationMetadata.introspect(beanClass);
  }

  /**
   * Create a new AnnotatedGenericBeanDefinition for the given annotation metadata,
   * allowing for ASM-based processing and avoidance of early loading of the bean class.
   * Note that this constructor is functionally equivalent to
   * {@link infra.context.annotation.ScannedGenericBeanDefinition
   * ScannedGenericBeanDefinition}, however the semantics of the latter indicate that a
   * bean was discovered specifically via component-scanning as opposed to other means.
   *
   * @param metadata the annotation metadata for the bean class in question
   */
  public AnnotatedGenericBeanDefinition(AnnotationMetadata metadata) {
    Assert.notNull(metadata, "AnnotationMetadata is required");
    if (metadata instanceof StandardAnnotationMetadata sam) {
      setBeanClass(sam.getIntrospectedClass());
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
  public AnnotatedGenericBeanDefinition(AnnotationMetadata metadata, MethodMetadata factoryMethodMetadata) {
    this(metadata);
    Assert.notNull(factoryMethodMetadata, "MethodMetadata is required");
    setFactoryMethodName(factoryMethodMetadata.getMethodName());
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

}
