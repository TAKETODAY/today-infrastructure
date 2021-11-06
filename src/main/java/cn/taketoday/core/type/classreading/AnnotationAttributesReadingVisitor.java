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

package cn.taketoday.core.type.classreading;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * ASM visitor which looks for annotations defined on a class or method,
 * including meta-annotations.
 *
 * <p>This visitor is fully recursive, taking into account any nested
 * annotations or nested annotation arrays.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @deprecated this class and related classes in this
 * package have been replaced by {@link SimpleAnnotationMetadataReadingVisitor}
 * and related classes for internal use within the framework.
 */
@Deprecated
final class AnnotationAttributesReadingVisitor extends RecursiveAnnotationAttributesVisitor {

  private final MultiValueMap<String, AnnotationAttributes> attributesMap;

  private final Map<String, Set<String>> metaAnnotationMap;

  public AnnotationAttributesReadingVisitor(
          String annotationType,
          MultiValueMap<String, AnnotationAttributes> attributesMap,
          Map<String, Set<String>> metaAnnotationMap,
          @Nullable ClassLoader classLoader) {

    super(annotationType, new AnnotationAttributes(annotationType, classLoader), classLoader);
    this.attributesMap = attributesMap;
    this.metaAnnotationMap = metaAnnotationMap;
  }

  @Override
  public void visitEnd() {
    super.visitEnd();

    Class<? extends Annotation> annotationClass = this.attributes.annotationType();
    if (annotationClass != null) {
      List<AnnotationAttributes> attributeList = this.attributesMap.get(this.annotationType);
      if (attributeList == null) {
        this.attributesMap.add(this.annotationType, this.attributes);
      }
      else {
        attributeList.add(0, this.attributes);
      }
      if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotationClass.getName())) {
        try {
          Annotation[] metaAnnotations = annotationClass.getAnnotations();
          if (ObjectUtils.isNotEmpty(metaAnnotations)) {
            Set<Annotation> visited = new LinkedHashSet<>();
            for (Annotation metaAnnotation : metaAnnotations) {
              recursivelyCollectMetaAnnotations(visited, metaAnnotation);
            }
            if (!visited.isEmpty()) {
              Set<String> metaAnnotationTypeNames = new LinkedHashSet<>(visited.size());
              for (Annotation ann : visited) {
                metaAnnotationTypeNames.add(ann.annotationType().getName());
              }
              this.metaAnnotationMap.put(annotationClass.getName(), metaAnnotationTypeNames);
            }
          }
        }
        catch (Throwable ex) {
          if (logger.isDebugEnabled()) {
            logger.debug("Failed to introspect meta-annotations on {}: {}", annotationClass, ex, ex);
          }
        }
      }
    }
  }

  private void recursivelyCollectMetaAnnotations(Set<Annotation> visited, Annotation annotation) {
    Class<? extends Annotation> annotationType = annotation.annotationType();
    String annotationName = annotationType.getName();
    if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotationName) && visited.add(annotation)) {
      try {
        // Only do attribute scanning for public annotations.
        if (Modifier.isPublic(annotationType.getModifiers())) {
          this.attributesMap.add(annotationName,
                  AnnotationUtils.getAnnotationAttributes(annotation, false, true));
        }
        for (Annotation metaMetaAnnotation : annotationType.getAnnotations()) {
          recursivelyCollectMetaAnnotations(visited, metaMetaAnnotation);
        }
      }
      catch (Throwable ex) {
        if (logger.isDebugEnabled()) {
          logger.debug("Failed to introspect meta-annotations on " + annotation + ": " + ex);
        }
      }
    }
  }

}
