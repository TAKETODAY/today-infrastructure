/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.core.type.classreading;

import org.jspecify.annotations.Nullable;

import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.constant.ClassDesc;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import infra.core.annotation.AnnotationFilter;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.util.ClassUtils;

/**
 * Parse {@link RuntimeVisibleAnnotationsAttribute} into {@link MergedAnnotations}
 * instances.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
abstract class ClassFileAnnotationMetadata {

  static MergedAnnotations createMergedAnnotations(String className, RuntimeVisibleAnnotationsAttribute annotationAttribute, @Nullable ClassLoader classLoader) {
    Set<MergedAnnotation<?>> annotations = annotationAttribute.annotations()
            .stream()
            .map(ann -> createMergedAnnotation(className, ann, classLoader))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    return MergedAnnotations.valueOf(annotations);
  }

  @Nullable
  private static <A extends java.lang.annotation.Annotation> MergedAnnotation<A> createMergedAnnotation(String className, Annotation annotation, @Nullable ClassLoader classLoader) {
    String typeName = fromTypeDescriptor(annotation.className().stringValue());
    if (AnnotationFilter.PLAIN.matches(typeName)) {
      return null;
    }
    var attributes = new LinkedHashMap<String, Object>(4);
    try {
      for (AnnotationElement element : annotation.elements()) {
        Object annotationValue = readAnnotationValue(className, element.value(), classLoader);
        if (annotationValue != null) {
          attributes.put(element.name().stringValue(), annotationValue);
        }
      }
      Map<String, Object> compactedAttributes = (attributes.isEmpty() ? Collections.emptyMap() : attributes);
      Class<A> annotationType = ClassUtils.forName(typeName, classLoader);
      return MergedAnnotation.valueOf(classLoader, new Source(annotation), annotationType, compactedAttributes);
    }
    catch (ClassNotFoundException | LinkageError ex) {
      return null;
    }
  }

  @Nullable
  private static Object readAnnotationValue(String className, AnnotationValue elementValue, @Nullable ClassLoader classLoader) {
    switch (elementValue) {
      case AnnotationValue.OfConstant constantValue -> {
        return constantValue.resolvedValue();
      }
      case AnnotationValue.OfAnnotation annotationValue -> {
        return createMergedAnnotation(className, annotationValue.annotation(), classLoader);
      }
      case AnnotationValue.OfClass classValue -> {
        return fromTypeDescriptor(classValue.className().stringValue());
      }
      case AnnotationValue.OfEnum enumValue -> {
        return parseEnum(enumValue, classLoader);
      }
      case AnnotationValue.OfArray arrayValue -> {
        return parseArrayValue(className, classLoader, arrayValue);
      }
    }
  }

  private static String fromTypeDescriptor(String descriptor) {
    ClassDesc classDesc = ClassDesc.ofDescriptor(descriptor);
    return classDesc.isPrimitive() ? classDesc.displayName() :
            classDesc.packageName() + "." + classDesc.displayName();
  }

  private static Object parseArrayValue(String className, @Nullable ClassLoader classLoader, AnnotationValue.OfArray arrayValue) {
    if (arrayValue.values().isEmpty()) {
      return new Object[0];
    }
    Stream<AnnotationValue> stream = arrayValue.values().stream();
    switch (arrayValue.values().getFirst()) {
      case AnnotationValue.OfInt _ -> {
        return stream.map(AnnotationValue.OfInt.class::cast).mapToInt(AnnotationValue.OfInt::intValue).toArray();
      }
      case AnnotationValue.OfDouble _ -> {
        return stream.map(AnnotationValue.OfDouble.class::cast).mapToDouble(AnnotationValue.OfDouble::doubleValue).toArray();
      }
      case AnnotationValue.OfLong _ -> {
        return stream.map(AnnotationValue.OfLong.class::cast).mapToLong(AnnotationValue.OfLong::longValue).toArray();
      }
      default -> {
        Class<?> arrayElementType = resolveArrayElementType(arrayValue.values(), classLoader);
        return stream
                .map(rawValue -> readAnnotationValue(className, rawValue, classLoader))
                .toArray(s -> (Object[]) Array.newInstance(arrayElementType, s));
      }
    }
  }

  @Nullable
  private static <E extends Enum<E>> Enum<E> parseEnum(AnnotationValue.OfEnum enumValue, @Nullable ClassLoader classLoader) {
    String enumClassName = fromTypeDescriptor(enumValue.className().stringValue());
    try {
      Class<E> enumClass = ClassUtils.forName(enumClassName, classLoader);
      return Enum.valueOf(enumClass, enumValue.constantName().stringValue());
    }
    catch (ClassNotFoundException | LinkageError ex) {
      return null;
    }
  }

  private static Class<?> resolveArrayElementType(List<AnnotationValue> values, @Nullable ClassLoader classLoader) {
    AnnotationValue firstValue = values.getFirst();
    switch (firstValue) {
      case AnnotationValue.OfConstant constantValue -> {
        return constantValue.resolvedValue().getClass();
      }
      case AnnotationValue.OfAnnotation _ -> {
        return MergedAnnotation.class;
      }
      case AnnotationValue.OfClass _ -> {
        return String.class;
      }
      case AnnotationValue.OfEnum enumValue -> {
        return loadClass(enumValue.className().stringValue(), classLoader);
      }
      default -> {
        return Object.class;
      }
    }
  }

  private static Class<?> loadClass(String className, @Nullable ClassLoader classLoader) {
    String name = fromTypeDescriptor(className);
    return ClassUtils.resolveClassName(name, classLoader);
  }

  record Source(Annotation entryName) {

  }

}
