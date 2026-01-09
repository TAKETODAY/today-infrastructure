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

import java.lang.annotation.Annotation;

import infra.beans.factory.annotation.AnnotatedBeanDefinition;
import infra.beans.factory.config.BeanDefinition;
import infra.core.annotation.MergedAnnotation;
import infra.core.type.MethodMetadata;
import infra.lang.Assert;

/**
 * A {@link ScopeMetadataResolver} implementation that by default checks for
 * the presence of Framework's {@link Scope @Scope} annotation on the bean class.
 *
 * <p>The exact type of annotation that is checked for is configurable via
 * {@link #setScopeAnnotationType(Class)}.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Scope
 * @since 4.0 2021/10/26 15:57
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
    Assert.notNull(defaultProxyMode, "'defaultProxyMode' is required");
    this.defaultProxyMode = defaultProxyMode;
  }

  /**
   * Set the type of annotation that is checked for by this
   * {@code AnnotationScopeMetadataResolver}.
   *
   * @param scopeAnnotationType the target annotation type
   */
  public void setScopeAnnotationType(Class<? extends Annotation> scopeAnnotationType) {
    Assert.notNull(scopeAnnotationType, "'scopeAnnotationType' is required");
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
        ScopedProxyMode proxyMode = annotation.getValue("proxyMode", ScopedProxyMode.class);
        if (proxyMode != null) {
          if (proxyMode == ScopedProxyMode.DEFAULT) {
            proxyMode = this.defaultProxyMode;
          }
          metadata.setScopedProxyMode(proxyMode);
        }

        metadata.setScopeName(annotation.getStringValue());
      }
    }
    return metadata;
  }

}
