/**
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
package cn.taketoday.web.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.annotation.HandlesTypes;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.aware.AnnotationImportAware;
import cn.taketoday.context.loader.CandidateComponentScanner;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.servlet.initializer.ServletContextInitializer;

/**
 * @author TODAY <br>
 * 2020-03-30 21:38
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Import(ServletContainerInitializerConfig.class)
public @interface EnableServletContainerInitializer {

  /**
   * scan classpath classes For {@link HandlesTypes}
   *
   * @see HandlesTypes#value()
   */
  String[] scanPackages() default {};
}

class ServletContainerInitializerConfig
        extends WebApplicationContextSupport implements ServletContextInitializer, AnnotationImportAware<EnableServletContainerInitializer> {
  EnableServletContainerInitializer target;

  @Override
  @SuppressWarnings("unchecked")
  public void onStartup(final ServletContext servletContext) throws Throwable {
    final ApplicationContext context = getApplicationContext();
    CandidateComponentScanner componentScannerToUse = CandidateComponentScanner.getSharedInstance();
    if (target != null) {
      final String[] scanPackages = target.scanPackages();
      if (ObjectUtils.isNotEmpty(scanPackages)) {
        componentScannerToUse.scan(scanPackages);
      }
    }

    for (final ServletContainerInitializer initializer : context.getBeans(ServletContainerInitializer.class)) {
      final HandlesTypes handles = AnnotationUtils.getAnnotation(initializer, HandlesTypes.class);

      Set<Class<?>> c = null;
      if (handles != null) {
        c = new HashSet<>();
        for (final Class<?> handlesType : handles.value()) {
          if (handlesType.isAnnotation()) {
            c.addAll(componentScannerToUse.getAnnotatedClasses((Class<? extends Annotation>) handlesType));
          }
          else if (handlesType.isInterface()) {
            c.addAll(componentScannerToUse.getImplementationClasses(handlesType));
          }
          else {
            c.add(handlesType);
          }
        }
      }
      initializer.onStartup(c, servletContext);
    }
  }

  @Override
  public void setImportBeanDefinition(
          EnableServletContainerInitializer target, BeanDefinition importDef) {
    this.target = target;
  }
}
