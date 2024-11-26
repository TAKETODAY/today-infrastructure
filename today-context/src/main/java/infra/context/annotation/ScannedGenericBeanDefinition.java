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

package infra.context.annotation;

import java.io.Serial;

import infra.beans.factory.annotation.AnnotatedBeanDefinition;
import infra.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.GenericBeanDefinition;
import infra.core.type.AnnotationMetadata;
import infra.core.type.MethodMetadata;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Extension of the {@link BeanDefinition} class, based on an ASM ClassReader,
 * with support for annotation metadata exposed through the
 * {@link AnnotatedBeanDefinition} interface.
 *
 * <p>This class does <i>not</i> load the bean {@code Class} early.
 * It rather retrieves all relevant metadata from the ".class" file itself,
 * parsed with the ASM ClassReader. It is functionally equivalent to
 * {@link AnnotatedGenericBeanDefinition#AnnotatedGenericBeanDefinition(AnnotationMetadata)}
 * but distinguishes by type beans that have been <em>scanned</em> vs those that have
 * been otherwise registered or detected by other means.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getMetadata()
 * @see #getBeanClassName()
 * @see MetadataReaderFactory
 * @see AnnotatedBeanDefinition
 * @since 4.0
 */
public class ScannedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition {

  @Serial
  private static final long serialVersionUID = 1L;

  private final AnnotationMetadata metadata;

  /**
   * Create a new ScannedGenericBeanDefinition for the class that the
   * given MetadataReader describes.
   *
   * @param metadataReader the MetadataReader for the scanned target class
   */
  public ScannedGenericBeanDefinition(MetadataReader metadataReader) {
    Assert.notNull(metadataReader, "MetadataReader is required");
    this.metadata = metadataReader.getAnnotationMetadata();
    setBeanClassName(this.metadata.getClassName());
    setResource(metadataReader.getResource());
  }

  @Override
  public final AnnotationMetadata getMetadata() {
    return this.metadata;
  }

  @Override
  @Nullable
  public MethodMetadata getFactoryMethodMetadata() {
    return null;
  }

}
