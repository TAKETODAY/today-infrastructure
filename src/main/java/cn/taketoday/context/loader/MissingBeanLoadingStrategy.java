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

package cn.taketoday.context.loader;

import java.util.Set;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author TODAY 2021/10/19 22:42
 * @see cn.taketoday.context.annotation.MissingBean
 * @see cn.taketoday.context.annotation.MissingComponent
 */
public class MissingBeanLoadingStrategy implements BeanDefinitionLoadingStrategy {
  private static final Logger log = LoggerFactory.getLogger(MissingBeanLoadingStrategy.class);
  private final MissingBeanRegistry missingBeanRegistry;

  public MissingBeanLoadingStrategy(MissingBeanRegistry missingBeanRegistry) {
    this.missingBeanRegistry = missingBeanRegistry;
  }

  @Override
  public Set<BeanDefinition> loadBeanDefinitions(
          MetadataReader metadata, DefinitionLoadingContext loadingContext) {
    if (metadata.getAnnotationMetadata().isAbstract()) {
      return null;
    }
    // just collect scanning missing-bean info
    AnnotationMetadata annotationMetadata = metadata.getAnnotationMetadata();
    AnnotationAttributes attributes = annotationMetadata.getAnnotations()
            .get(MissingBean.class).asAnnotationAttributes();
    if (attributes != null) {
      missingBeanRegistry.registerMissing(attributes, metadata);
    }
    return null;
  }

}
