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

package cn.taketoday.beans.dependency;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashSet;

import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Autowired;
import cn.taketoday.util.ClassUtils;

/**
 * Injectable annotations
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/20 22:10</a>
 * @see jakarta.inject.Inject
 * @see jakarta.annotation.Resource
 * @see Autowired
 * @since 4.0
 */
public class InjectableAnnotations {
  public static final InjectableAnnotations shared = new InjectableAnnotations();

  static {
    shared.initDefaultAnnotations();
  }

  private final LinkedHashSet<Class<? extends Annotation>> injectableAnnotations = new LinkedHashSet<>();

  public void addAnnotation(Class<? extends Annotation> injectableAnnotation) {
    Assert.notNull(injectableAnnotation, "'injectableAnnotation' is required");
    injectableAnnotations.add(injectableAnnotation);
  }

  public boolean isInjectable(AnnotatedElement element) {
    return isInjectable(MergedAnnotations.from(element));
  }

  public boolean isInjectable(DependencyInjectionPoint injectionPoint) {
    return isInjectable(injectionPoint.getAnnotations());
  }

  public boolean isInjectable(MergedAnnotations annotations) {
    for (Class<? extends Annotation> injectableAnnotation : injectableAnnotations) {
      if (annotations.isPresent(injectableAnnotation)) {
        return true;
      }
    }
    return false;
  }

  public void initDefaultAnnotations() {
    try { // @formatter:off
      addAnnotation(ClassUtils.forName("jakarta.inject.Inject"));
    }
    catch (Exception ignored) {}
    try {
      addAnnotation(ClassUtils.forName("jakarta.annotation.Resource"));
    }
    catch (Exception ignored) {}
    // @formatter:on
    addAnnotation(Autowired.class);
  }

}
