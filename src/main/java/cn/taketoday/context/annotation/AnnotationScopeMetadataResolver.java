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

import java.lang.annotation.Annotation;

import cn.taketoday.beans.factory.support.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.loader.ScopeMetadata;
import cn.taketoday.context.loader.ScopeMetadataResolver;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.lang.Assert;

/**
 * @author TODAY 2021/10/26 15:57
 * @since 4.0
 */
public class AnnotationScopeMetadataResolver implements ScopeMetadataResolver {
  protected Class<? extends Annotation> scopeAnnotationType = Scope.class;

  /**
   * Set the type of annotation that is checked for by this
   * {@code AnnotationScopeMetadataResolver}.
   *
   * @param scopeAnnotationType the target annotation type
   */
  public void setScopeAnnotationType(Class<? extends Annotation> scopeAnnotationType) {
    Assert.notNull(scopeAnnotationType, "'scopeAnnotationType' must not be null");
    this.scopeAnnotationType = scopeAnnotationType;
  }

  @Override
  public ScopeMetadata resolveScopeMetadata(BeanDefinition definition) {
    ScopeMetadata metadata = new ScopeMetadata();
    if (definition instanceof AnnotatedBeanDefinition annDef) {
      MergedAnnotation<? extends Annotation> annotation = annDef.getMetadata().getAnnotations().get(scopeAnnotationType);
      if (annotation.isPresent()) {
        metadata.setScopeName(annotation.getStringValue());
      }
    }
    return metadata;
  }

}
