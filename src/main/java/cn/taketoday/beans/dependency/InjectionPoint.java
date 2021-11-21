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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Required;

/**
 * Dependency InjectionPoint
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/16 21:29</a>
 * @since 4.0
 */
public abstract class InjectionPoint implements Serializable {

  /**
   * It shows that the value is not set
   */
  public static final Object DO_NOT_SET = new Object();

  protected Boolean required = null;
  protected MergedAnnotations annotations;

  @Nullable
  private transient ResolvableType resolvableType;

  @Nullable
  private transient TypeDescriptor typeDescriptor;

  public int nestingLevel;

  public void increaseNestingLevel() {
    nestingLevel++;
  }

  public void decreaseNestingLevel() {
    nestingLevel--;
  }

  public abstract Class<?> getDependencyType();

  /**
   * Return the wrapped annotated element.
   */
  public abstract AnnotatedElement getAnnotatedElement();

  public MergedAnnotations getAnnotations() {
    if (annotations == null) {
      annotations = doGetAnnotations();
    }
    return annotations;
  }

  protected abstract MergedAnnotations doGetAnnotations();

  public <A extends Annotation> boolean isAnnotationPresent(Class<A> annotationType) {
    return getAnnotations().isPresent(annotationType);
  }

  public <A extends Annotation> MergedAnnotation<A> getAnnotation(Class<A> annotationType) {
    return getAnnotations().get(annotationType);
  }

  public boolean isRequired() {
    if (required == null) {
      required = doGetRequiredStatus();
    }
    return required;
  }

  protected boolean doGetRequiredStatus() {
    return getAnnotations().isPresent(Required.class);
  }

  /**
   * Build a {@link ResolvableType} object for the wrapped parameter/field.
   */
  public ResolvableType getResolvableType() {
    if (resolvableType == null) {
      resolvableType = doGetResolvableType();
    }
    return resolvableType;
  }

  protected abstract ResolvableType doGetResolvableType();

  /**
   * Build a {@link TypeDescriptor} object for the wrapped parameter/field.
   */
  public TypeDescriptor getTypeDescriptor() {
    if (typeDescriptor == null) {
      typeDescriptor = doGetTypeDescriptor();
    }
    return typeDescriptor;
  }

  protected abstract TypeDescriptor doGetTypeDescriptor();

  public boolean isArray() {
    return getDependencyType().isArray();
  }

  public boolean isMap() {
    return Map.class.isAssignableFrom(getDependencyType());
  }

  public boolean dependencyIs(Class<?> type) {
    return type == getDependencyType();
  }

  public abstract Object getTarget();

  public boolean isProperty() {
    return false;
  }

}
