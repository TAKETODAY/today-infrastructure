/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.bind.resolver;

import java.lang.annotation.Annotation;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * @author TODAY 2021/4/8 22:38
 * @since 3.0
 */
public abstract class AnnotationParameterResolver<A extends Annotation> implements ParameterResolvingStrategy {
  private final Class<A> annotationClass;

  public AnnotationParameterResolver(Class<A> annotationClass) {
    Assert.notNull(annotationClass, "annotationClass must not be null");
    this.annotationClass = annotationClass;
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return resolvable.hasParameterAnnotation(annotationClass);
  }

  @Override
  public Object resolveParameter(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    final A target = resolvable.getParameterAnnotation(annotationClass);
    return resolveInternal(target, context, resolvable);
  }

  protected Object resolveInternal(A target, RequestContext context, ResolvableMethodParameter parameter) {
    return null;
  }
}
