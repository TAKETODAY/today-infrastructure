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

package cn.taketoday.context.aot;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.aot.hint.annotation.Reflective;
import cn.taketoday.aot.hint.annotation.ReflectiveProcessor;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotContribution;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.context.annotation.ReflectiveScan;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * AOT {@code BeanFactoryInitializationAotProcessor} that detects the presence
 * of {@link Reflective @Reflective} on annotated elements of all registered
 * beans and invokes the underlying {@link ReflectiveProcessor} implementations.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
class ReflectiveProcessorBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

  @Override
  @Nullable
  public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
    Class<?>[] beanClasses = Arrays.stream(beanFactory.getBeanDefinitionNames())
            .map(beanName -> RegisteredBean.of(beanFactory, beanName).getBeanClass())
            .toArray(Class<?>[]::new);
    String[] packagesToScan = findBasePackagesToScan(beanClasses);
    return new ReflectiveProcessorAotContributionBuilder().withClasses(beanClasses)
            .scan(beanFactory.getBeanClassLoader(), packagesToScan).build();
  }

  protected String[] findBasePackagesToScan(Class<?>[] beanClasses) {
    Set<String> basePackages = new LinkedHashSet<>();
    for (Class<?> beanClass : beanClasses) {
      ReflectiveScan reflectiveScan = AnnotatedElementUtils.getMergedAnnotation(beanClass, ReflectiveScan.class);
      if (reflectiveScan != null) {
        basePackages.addAll(extractBasePackages(reflectiveScan, beanClass));
      }
    }
    return basePackages.toArray(new String[0]);
  }

  private Set<String> extractBasePackages(ReflectiveScan annotation, Class<?> declaringClass) {
    Set<String> basePackages = new LinkedHashSet<>();
    Collections.addAll(basePackages, annotation.basePackages());
    for (Class<?> clazz : annotation.basePackageClasses()) {
      basePackages.add(ClassUtils.getPackageName(clazz));
    }
    if (basePackages.isEmpty()) {
      basePackages.add(ClassUtils.getPackageName(declaringClass));
    }
    return basePackages;
  }

}
