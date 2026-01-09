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

package infra.context.annotation;

import org.jspecify.annotations.Nullable;

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
