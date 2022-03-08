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

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.loader.ScopeMetadata;
import cn.taketoday.context.loader.ScopeMetadataResolver;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.lang.Assert;

/**
 * A {@link ScopeMetadataResolver} implementation that by default checks for
 * the presence of Spring's {@link Scope @Scope} annotation on the bean class.
 *
 * <p>The exact type of annotation that is checked for is configurable via
 * {@link #setScopeAnnotationType(Class)}.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author TODAY 2021/10/26 15:57
 * @see Scope
 * @since 4.0
 */
public class AnnotationScopeMetadataResolver implements ScopeMetadataResolver {

  private final ScopedProxyMode defaultProxyMode;
  protected Class<? extends Annotation> scopeAnnotationType = Scope.class;

  /**
   * Construct a new {@code AnnotationScopeMetadataResolver}.
   *
   * @see #AnnotationScopeMetadataResolver(ScopedProxyMode)
   * @see ScopedProxyMode#NO
   */
  public AnnotationScopeMetadataResolver() {
    this.defaultProxyMode = ScopedProxyMode.NO;
  }

  /**
   * Construct a new {@code AnnotationScopeMetadataResolver} using the
   * supplied default {@link ScopedProxyMode}.
   *
   * @param defaultProxyMode the default scoped-proxy mode
   */
  public AnnotationScopeMetadataResolver(ScopedProxyMode defaultProxyMode) {
    Assert.notNull(defaultProxyMode, "'defaultProxyMode' must not be null");
    this.defaultProxyMode = defaultProxyMode;
  }

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
      MethodMetadata factoryMethodMetadata = annDef.getFactoryMethodMetadata();
      MergedAnnotation<? extends Annotation> annotation;
      if (factoryMethodMetadata != null) {
        annotation = factoryMethodMetadata.getAnnotation(scopeAnnotationType);
      }
      else {
        annotation = annDef.getMetadata().getAnnotation(scopeAnnotationType);
      }
      if (annotation.isPresent()) {
        ScopedProxyMode proxyMode = annotation.getEnum("proxyMode", ScopedProxyMode.class);
        if (proxyMode == ScopedProxyMode.DEFAULT) {
          proxyMode = this.defaultProxyMode;
        }
        metadata.setScopedProxyMode(proxyMode);
        metadata.setScopeName(annotation.getStringValue());
      }
    }
    return metadata;
  }

}
