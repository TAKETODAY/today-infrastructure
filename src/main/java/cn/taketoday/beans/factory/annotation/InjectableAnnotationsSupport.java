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

package cn.taketoday.beans.factory.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashSet;

import cn.taketoday.beans.factory.dependency.InjectionPoint;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/21 17:38</a>
 * @since 4.0
 */
public class InjectableAnnotationsSupport {
  protected final Logger log = LoggerFactory.getLogger(getClass());
  protected final LinkedHashSet<Class<? extends Annotation>> injectableAnnotations = new LinkedHashSet<>();

  public void initInjectableAnnotations() {
    addInjectableAnnotation(Autowired.class);
    ClassLoader classLoader = getClass().getClassLoader();
    // @formatter:off
    try {
      // Resource ?
      addInjectableAnnotation(ClassUtils.forName("jakarta.annotation.Resource",classLoader));
    }
    catch (Exception ignored) {}
    try {
      addInjectableAnnotation(
              ClassUtils.forName("jakarta.inject.Inject", classLoader));
      log.trace("'jakarta.inject.Inject' annotation found and supported for autowiring");
    }
    catch (ClassNotFoundException ex) {
      // jakarta.inject API not available - simply skip.
    }
    try {
      addInjectableAnnotation(
              ClassUtils.forName("javax.inject.Inject", classLoader));
      log.trace("'javax.inject.Inject' annotation found and supported for autowiring");
    }
    catch (ClassNotFoundException ex) {
      // javax.inject API not available - simply skip.
    }
    // @formatter:on
  }

  public void addInjectableAnnotation(Class<? extends Annotation> injectableAnnotation) {
    Assert.notNull(injectableAnnotation, "'injectableAnnotation' is required");
    injectableAnnotations.add(injectableAnnotation);
  }

  public boolean isInjectable(AnnotatedElement element) {
    return isInjectable(MergedAnnotations.from(element));
  }

  public boolean isInjectable(InjectionPoint injectionPoint) {
    return isInjectable(MergedAnnotations.from(injectionPoint.getAnnotations()));
  }

  public boolean isInjectable(MergedAnnotations annotations) {
    for (Class<? extends Annotation> injectableAnnotation : injectableAnnotations) {
      if (annotations.isPresent(injectableAnnotation)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  protected MergedAnnotation<?> findAutowiredAnnotation(AccessibleObject ao) {
    MergedAnnotations annotations = MergedAnnotations.from(ao);
    for (Class<? extends Annotation> type : injectableAnnotations) {
      MergedAnnotation<?> annotation = annotations.get(type);
      if (annotation.isPresent()) {
        return annotation;
      }
    }
    return null;
  }

}
