/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.domain;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.ClassPathScanningCandidateComponentProvider;
import cn.taketoday.context.annotation.config.AutoConfigurationPackages;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * An entity scanner that searches the classpath from an {@link EntityScan @EntityScan}
 * specified packages.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Phillip Webb
 * @since 4.0
 */
public class EntityScanner {

  private final ApplicationContext context;

  /**
   * Create a new {@link EntityScanner} instance.
   *
   * @param context the source application context
   */
  public EntityScanner(ApplicationContext context) {
    Assert.notNull(context, "Context must not be null");
    this.context = context;
  }

  /**
   * Scan for entities with the specified annotations.
   *
   * @param annotationTypes the annotation types used on the entities
   * @return a set of entity classes
   * @throws ClassNotFoundException if an entity class cannot be loaded
   */
  @SafeVarargs
  public final Set<Class<?>> scan(Class<? extends Annotation>... annotationTypes) throws ClassNotFoundException {
    List<String> packages = getPackages();
    if (packages.isEmpty()) {
      return Collections.emptySet();
    }
    var scanner = createClassPathScanningCandidateComponentProvider(context);
    for (Class<? extends Annotation> annotationType : annotationTypes) {
      scanner.addIncludeFilter(new AnnotationTypeFilter(annotationType));
    }
    Set<Class<?>> entitySet = new HashSet<>();
    for (String basePackage : packages) {
      if (StringUtils.hasText(basePackage)) {
        for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
          entitySet.add(ClassUtils.forName(candidate.getBeanClassName(), context.getClassLoader()));
        }
      }
    }
    return entitySet;
  }

  /**
   * Create a {@link ClassPathScanningCandidateComponentProvider} to scan entities based
   * on the specified {@link ApplicationContext}.
   *
   * @param context the {@link ApplicationContext} to use
   * @return a {@link ClassPathScanningCandidateComponentProvider} suitable to scan
   * entities
   */
  protected ClassPathScanningCandidateComponentProvider createClassPathScanningCandidateComponentProvider(
          ApplicationContext context) {
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.setEnvironment(context.getEnvironment());
    scanner.setResourceLoader(context);
    return scanner;
  }

  private List<String> getPackages() {
    List<String> packages = EntityScanPackages.get(this.context).getPackageNames();
    if (packages.isEmpty() && AutoConfigurationPackages.has(this.context)) {
      packages = AutoConfigurationPackages.get(this.context);
    }
    return packages;
  }

}
