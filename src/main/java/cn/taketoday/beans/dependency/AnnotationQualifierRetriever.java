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

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.lang.Assert;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/20 22:25</a>
 * @see jakarta.inject.Named
 * @since 4.0
 */
public class AnnotationQualifierRetriever implements QualifierRetriever {

  private String attributeName = MergedAnnotation.VALUE;
  private final Class<? extends Annotation> annotationType;

  public AnnotationQualifierRetriever(Class<? extends Annotation> annotationType) {
    Assert.notNull(annotationType, "'annotationType' is required");
    this.annotationType = annotationType;
  }

  public AnnotationQualifierRetriever(Class<? extends Annotation> annotationType, String attributeName) {
    Assert.notNull(annotationType, "'annotationType' is required");
    setAttributeName(attributeName);
    this.annotationType = annotationType;
  }

  @Override
  public String retrieve(InjectionPoint injectionPoint) {
    MergedAnnotation<? extends Annotation> annotation = injectionPoint.getAnnotation(annotationType);
    return annotation.getValue(attributeName, String.class).orElse(null);
  }

  public void setAttributeName(String attributeName) {
    Assert.notNull(attributeName, "'attributeName' is required");
    this.attributeName = attributeName;
  }

  public String getAttributeName() {
    return attributeName;
  }

}
