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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.NonNull;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * @author TODAY 2021/8/15 22:43
 * @since 4.0
 */
public class ClassReadingAnnotationMetaReader extends AnnotationMetaReader {

  @Override
  protected <T extends Annotation> AnnotationAttributes[] createAttributesArray(@NonNull AnnotationKey<T> key) {
    AnnotationAttributes[] annotations = ClassMetaReader.readAnnotations(key.element);
    if (ObjectUtils.isEmpty(annotations)) {
      return EMPTY_ANNOTATION_ATTRIBUTES;
    }
    else {
      // scan target annotation on annotationClass
      final Class<T> annotationClass = key.annotationClass;
      final ArrayList<AnnotationAttributes> result = new ArrayList<>(); // for the order
      for (final AnnotationAttributes annotation : annotations) {
        final List<AnnotationAttributes> attr = searchAttributes(annotation, annotationClass);
        if (!attr.isEmpty()) {
          result.addAll(attr);
        }
      }
      return result.isEmpty()
             ? EMPTY_ANNOTATION_ATTRIBUTES
             : result.toArray(new AnnotationAttributes[result.size()]);
    }
  }

  @Override
  protected <T extends Annotation> List<AnnotationAttributes> doSearch(
          AnnotationAttributes annotation, Class<T> target, String nameToFind) {
    ArrayList<AnnotationAttributes> ret = new ArrayList<>();
    findTargetAttributes(nameToFind, target, annotation, ret);
    return ret;
  }

  @Override
  protected <T extends Annotation> List<AnnotationAttributes> doSearch(
          Annotation annotation, Class<T> target, Class<? extends Annotation> annotationType) {
    ArrayList<AnnotationAttributes> ret = new ArrayList<>();

    String nameToFind = annotationType.getName();
    findTargetAttributes(nameToFind, target, getAttributes(annotation), ret);
    return null;
  }

  <T extends Annotation> void findTargetAttributes(
          final String nameToFind,
          final Class<T> targetType,
          final AnnotationAttributes annotation,
          final ArrayList<AnnotationAttributes> attributes
  ) {
    AnnotationAttributes[] sourceAttributes = ClassMetaReader.readAnnotations(nameToFind);
    for (final AnnotationAttributes current : sourceAttributes) {
      if (ignorable(current, nameToFind)) {
        continue;
      }
      if (current.isTarget(targetType)) {
        // found target annotation
        AnnotationAttributes found = getAttributes(current, annotation, targetType);
        attributes.add(found); // found it
      }
      else { // next
        findTargetAttributes(current.annotationName(), targetType, annotation, attributes);
        if (!attributes.isEmpty()) {
          AnnotationAttributes override = getAttributes(current, annotation, targetType);
          for (final AnnotationAttributes attribute : attributes) {
            if (attribute.isTarget(targetType)) {
              attribute.putAll(override);
            }
          }
        }
      }
    }
  }

  AnnotationAttributes getAttributes(
          final AnnotationAttributes current,// 当前的
          final AnnotationAttributes annotation, // 最初的
//          final String candidateType // 就是要找的 type
          final Class<?> classToFind // 就是要找的 type
  ) {
    AnnotationDescriptor descriptor = ClassMetaReader.readDefault(classToFind);
    Map<String, String> annotationTypes = descriptor.annotationTypes; // method-name to return-type string
    AnnotationAttributes found = new AnnotationAttributes(descriptor.defaultAttributes, classToFind);

    AnnotationDescriptor candidateDescriptor = ClassMetaReader.readDefault(current.annotationName());
    Map<String, String> candidateAnnotationTypes = candidateDescriptor.annotationTypes;
    for (final Map.Entry<String, String> entry : annotationTypes.entrySet()) {
      String method = entry.getKey(); // attribute-name
      String returnType = entry.getValue();// return-type
      Object value;
      if (Objects.equals(returnType, candidateAnnotationTypes.get(method))) {
        value = current.getAttribute(method, getExpectedType(returnType));
      }
      else {
        value = annotation.getAttribute(method, getExpectedType(returnType));
      }
      if (value != null) {
        found.put(method, value);
      }
    }
    return found;
  }

  private static Class<?> getExpectedType(String returnType) {
    try {
      return ClassUtils.forName(returnType);
    }
    catch (ClassNotFoundException e) {
      return null;
    }
  }

}
