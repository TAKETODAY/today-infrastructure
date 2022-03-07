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

package cn.taketoday.context.annotation;

import cn.taketoday.beans.factory.support.AnnotatedBeanDefinition;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.lang.Nullable;

/**
 * Extension of the {@link AnnotatedBeanDefinition}
 * class, based on an ASM ClassReader, with support for annotation metadata exposed
 * through the {@link AnnotatedBeanDefinition} interface.
 *
 * <p>This class does <i>not</i> load the bean {@code Class} early.
 * It rather retrieves all relevant metadata from the ".class" file itself,
 * parsed with the ASM ClassReader. It is functionally equivalent to
 * {@link AnnotatedBeanDefinition#AnnotatedBeanDefinition(AnnotationMetadata)}
 * but distinguishes by type beans that have been <em>scanned</em> vs those that have
 * been otherwise registered or detected by other means.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see #getMetadata()
 * @see #getBeanClassName()
 * @see MetadataReaderFactory
 * @see AnnotatedBeanDefinition
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ScannedBeanDefinition extends AnnotatedBeanDefinition {

  /**
   * Create a new ScannedBeanDefinition for the class that the
   * given MetadataReader describes.
   *
   * @param metadataReader the MetadataReader for the scanned target class
   */
  public ScannedBeanDefinition(MetadataReader metadataReader) {
    super(metadataReader.getAnnotationMetadata());
    setSource(metadataReader.getResource());
    setResource(metadataReader.getResource());
  }

  @Override
  @Nullable
  public MethodMetadata getFactoryMethodMetadata() {
    return null;
  }

}
