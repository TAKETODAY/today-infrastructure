/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.NonNull;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * Java Reflect AnnotationMetaReader
 *
 * @author TODAY 2021/8/15 22:34
 * @since 4.0
 */
public class ReflectiveAnnotationMetaReader extends AnnotationMetaReader {

  @Override
  protected <T extends Annotation> AnnotationAttributes[] createAttributesArray(@NonNull AnnotationKey<T> key) {
    final Annotation[] annotations = key.element.getAnnotations();
    if (ObjectUtils.isEmpty(annotations)) {
      return EMPTY_ANNOTATION_ATTRIBUTES;
    }
    else {
      final Class<T> annotationClass = key.annotationClass;
      final ArrayList<AnnotationAttributes> result = new ArrayList<>(); // for the order
      for (final Annotation annotation : annotations) {
        final List<AnnotationAttributes> attr = getAttributes(annotation, annotationClass);
        if (!attr.isEmpty()) {
          result.addAll(attr);
        }
      }
      return result.isEmpty()
             ? EMPTY_ANNOTATION_ATTRIBUTES
             : result.toArray(new AnnotationAttributes[result.size()]);
    }
  }

  /**
   * Get target {@link AnnotationAttributes} on input annotation
   *
   * @param target
   *         The annotation class
   * @param annotation
   *         The annotation instance
   *
   * @return {@link AnnotationAttributes} list never be null.
   *
   * @since 2.1.7
   */
  public <T extends Annotation> List<AnnotationAttributes> getAttributes(
          final Annotation annotation, final Class<T> target
  ) {
    if (annotation == null) {
      return Collections.emptyList();
    }
    final Class<? extends Annotation> annotationType = annotation.annotationType();
    if (annotationType == target) {
      // 如果等于对象注解就直接添加
      return Collections.singletonList(getAttributes(annotationType, annotation));
    }
    // filter some annotation classes
    // -----------------------------------------
    if (IGNORE_ANNOTATION_CLASS.contains(annotationType)) {
      return Collections.emptyList();
    }
    // find the default value of annotation
    // -----------------------------------------
    return doSearch(annotation, target, annotationType);
  }

  @Override
  protected <T extends Annotation> List<AnnotationAttributes> doSearch(
          AnnotationAttributes annotation, Class<T> target, String nameToFind) {
    Class<? extends Annotation> annotationType = annotation.annotationType();
    Annotation annotationProxy = getAnnotationProxy(annotationType, annotation);
    ArrayList<AnnotationAttributes> ret = new ArrayList<>();
    findTargetAttributes(
            annotationType, target, ret,
            new TransformTarget(annotationProxy, annotationType), IGNORE_ANNOTATION_CLASS);
    return ret;
  }

  @Override
  protected <T extends Annotation> ArrayList<AnnotationAttributes> doSearch(
          Annotation annotation, Class<T> target, Class<? extends Annotation> annotationType) {
    ArrayList<AnnotationAttributes> ret = new ArrayList<>();

    findTargetAttributes(annotationType, target, ret,
                         new TransformTarget(annotation, annotationType), IGNORE_ANNOTATION_CLASS);
    return ret;
  }

  /**
   * Use recursive to find the All target {@link AnnotationAttributes} instance
   *
   * @param targetType
   *         Target {@link Annotation} class to find
   * @param source
   *         {@link Annotation} source
   * @param attributes
   *         All suitable {@link AnnotationAttributes}
   * @param ignoreAnnotation
   *         Ignore {@link Annotation}s
   *
   * @since 2.1.7
   */
  <T extends Annotation> void findTargetAttributes(
          final Class<?> source,
          final Class<T> targetType,
          final ArrayList<AnnotationAttributes> attributes,
          final AnnotationAttributesTransformer transformer,
          final Set<Class<? extends Annotation>> ignoreAnnotation
  ) {
    for (final Annotation current : source.getAnnotations()) {
      final Class<? extends Annotation> candidateType = current.annotationType();
      if (candidateType == source || ignoreAnnotation.contains(candidateType)) {
        continue;
      }
      if (candidateType == targetType) {
        // found target annotation
        attributes.add(getAttributes(current, candidateType, transformer)); // found it
      }
      else {
        findTargetAttributes(candidateType, targetType, attributes, transformer, ignoreAnnotation);
      }
    }
  }

  public AnnotationAttributes getAttributes(
          final Annotation current,
          final Class<? extends Annotation> candidateType,
          final AnnotationAttributesTransformer transformer
  ) {
    final Method[] declaredMethods = ReflectionUtils.getDeclaredMethods(candidateType);
    final AnnotationAttributes target = new AnnotationAttributes(candidateType, declaredMethods.length);
    for (final Method method : declaredMethods) {
      Object value = transformer.get(method);
      if (value == null) {
        value = ReflectionUtils.invokeMethod(method, current);
      }
      target.put(method.getName(), value);
    }
    return target;
  }

  interface AnnotationAttributesTransformer {

    Object get(Method method);

    void transform(AnnotationAttributes attributes);
  }

  static final class TransformTarget implements AnnotationAttributesTransformer {

    private Method[] declaredMethods;
    private final Annotation annotation;
    private final Class<?> annotationType;

    public TransformTarget(Annotation annotation, Class<?> annotationType) {
      this.annotation = annotation;
      this.annotationType = annotationType;
    }

    @Override
    public void transform(final AnnotationAttributes target) {
      // found it and override same properties
      // -------------------------------------
      final Annotation annotation = this.annotation;
      for (final Method method : getDeclaredMethods()) {
        final Object value = target.get(method.getName());
        if (value == null || eq(method.getReturnType(), value.getClass())) {
          target.put(method.getName(), ReflectionUtils.invokeMethod(method, annotation)); // override
        }
      }
    }

    protected Method[] getDeclaredMethods() {
      Method[] ret = this.declaredMethods;
      if (ret == null) {
        ret = ReflectionUtils.getDeclaredMethods(annotationType);
        this.declaredMethods = ret;
      }
      return ret;
    }

    @Override
    public Object get(final Method targetMethod) {
      final String name = targetMethod.getName();
      final Annotation annotation = this.annotation;
      // In general there isn't lots of Annotation Attributes
      for (final Method method : getDeclaredMethods()) {
        if (method.getName().equals(name)
                && eq(method.getReturnType(), targetMethod.getReturnType())) {
          return ReflectionUtils.invokeMethod(method, annotation);
        }
      }
      return null;
    }

    private static boolean eq(Class<?> returnType, Class<?> clazz) {
      if (returnType == clazz) {
        return true;
      }
      if (returnType.isPrimitive()) {
        switch (returnType.getName()) {//@off
          case "int" :    return clazz == Integer.class;
          case "long" :   return clazz == Long.class;
          case "byte" :   return clazz == Byte.class;
          case "char" :   return clazz == Character.class;
          case "float" :  return clazz == Float.class;
          case "double" : return clazz == Double.class;
          case "short" :  return clazz == Short.class;
          case "boolean" :return clazz == Boolean.class;
          default:        return false;
        } //@on
      }
      return false;
    }
  }

}
