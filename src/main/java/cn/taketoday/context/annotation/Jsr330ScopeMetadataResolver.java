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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.factory.support.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.loader.ClassPathBeanDefinitionScanner;
import cn.taketoday.context.loader.ScopeMetadata;
import cn.taketoday.context.loader.ScopeMetadataResolver;
import cn.taketoday.lang.Nullable;

/**
 * Simple {@link ScopeMetadataResolver} implementation that follows JSR-330 scoping rules:
 * defaulting to prototype scope unless {@link jakarta.inject.Singleton} is present.
 *
 * <p>This scope resolver can be used with {@link ClassPathBeanDefinitionScanner} and
 * {@link AnnotatedBeanDefinitionReader} for standard JSR-330 compliance. However,
 * in practice, you will typically use Framework's rich default scoping instead - or extend
 * this resolver with custom scoping annotations that point to extended Framework scopes.
 *
 * @author Juergen Hoeller
 * @see #registerScope
 * @see #resolveScopeName
 * @see ClassPathBeanDefinitionScanner#setScopeMetadataResolver
 * @see AnnotatedBeanDefinitionReader#setScopeMetadataResolver
 * @since 4.0
 */
public class Jsr330ScopeMetadataResolver implements ScopeMetadataResolver {

  private final Map<String, String> scopeMap = new HashMap<>();

  public Jsr330ScopeMetadataResolver() {
    registerScope("jakarta.inject.Singleton", BeanDefinition.SCOPE_SINGLETON);
  }

  /**
   * Register an extended JSR-330 scope annotation, mapping it onto a
   * specific Framework scope by name.
   *
   * @param annotationType the JSR-330 annotation type as a Class
   * @param scopeName the Framework scope name
   */
  public final void registerScope(Class<?> annotationType, String scopeName) {
    this.scopeMap.put(annotationType.getName(), scopeName);
  }

  /**
   * Register an extended JSR-330 scope annotation, mapping it onto a
   * specific Framework scope by name.
   *
   * @param annotationType the JSR-330 annotation type by name
   * @param scopeName the Framework scope name
   */
  public final void registerScope(String annotationType, String scopeName) {
    this.scopeMap.put(annotationType, scopeName);
  }

  /**
   * Resolve the given annotation type into a named Framework scope.
   * <p>The default implementation simply checks against registered scopes.
   * Can be overridden for custom mapping rules, e.g. naming conventions.
   *
   * @param annotationType the JSR-330 annotation type
   * @return the Framework scope name
   */
  @Nullable
  protected String resolveScopeName(String annotationType) {
    return this.scopeMap.get(annotationType);
  }

  @Override
  public ScopeMetadata resolveScopeMetadata(BeanDefinition definition) {
    ScopeMetadata metadata = new ScopeMetadata();
    metadata.setScopeName(BeanDefinition.SCOPE_PROTOTYPE);
    if (definition instanceof AnnotatedBeanDefinition annDef) {
      Set<String> annTypes = annDef.getMetadata().getAnnotationTypes();
      String found = null;
      for (String annType : annTypes) {
        Set<String> metaAnns = annDef.getMetadata().getMetaAnnotationTypes(annType);
        if (metaAnns.contains("jakarta.inject.Scope")) {
          if (found != null) {
            throw new IllegalStateException("Found ambiguous scope annotations on bean class [" +
                    definition.getBeanClassName() + "]: " + found + ", " + annType);
          }
          found = annType;
          String scopeName = resolveScopeName(annType);
          if (scopeName == null) {
            throw new IllegalStateException(
                    "Unsupported scope annotation - not mapped onto Framework scope name: " + annType);
          }
          metadata.setScopeName(scopeName);
        }
      }
    }
    return metadata;
  }

}
