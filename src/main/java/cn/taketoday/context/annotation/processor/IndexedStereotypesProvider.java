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

package cn.taketoday.context.annotation.processor;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * A {@link StereotypesProvider} implementation that extracts the stereotypes
 * flagged by the {@value #INDEXED_ANNOTATION} annotation. This implementation
 * honors stereotypes defined this way on meta-annotations.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
class IndexedStereotypesProvider implements StereotypesProvider {
  private static final String INDEXED_ANNOTATION = "cn.taketoday.lang.Indexed";

  private final TypeHelper typeHelper;

  public IndexedStereotypesProvider(TypeHelper typeHelper) {
    this.typeHelper = typeHelper;
  }

  @Override
  public Set<String> getStereotypes(Element element) {
    Set<String> stereotypes = new LinkedHashSet<>();
    ElementKind kind = element.getKind();
    if (!kind.isClass() && kind != ElementKind.INTERFACE) {
      return stereotypes;
    }
    Set<Element> seen = new HashSet<>();
    collectStereotypesOnAnnotations(seen, stereotypes, element);
    seen = new HashSet<>();
    collectStereotypesOnTypes(seen, stereotypes, element);
    return stereotypes;
  }

  private void collectStereotypesOnAnnotations(Set<Element> seen, Set<String> stereotypes, Element element) {
    for (AnnotationMirror annotation : this.typeHelper.getAllAnnotationMirrors(element)) {
      Element next = collectStereotypes(seen, stereotypes, element, annotation);
      if (next != null) {
        collectStereotypesOnAnnotations(seen, stereotypes, next);
      }
    }
  }

  private void collectStereotypesOnTypes(Set<Element> seen, Set<String> stereotypes, Element type) {
    if (!seen.contains(type)) {
      seen.add(type);
      if (isAnnotatedWithIndexed(type)) {
        stereotypes.add(this.typeHelper.getType(type));
      }
      Element superClass = this.typeHelper.getSuperClass(type);
      if (superClass != null) {
        collectStereotypesOnTypes(seen, stereotypes, superClass);
      }
      this.typeHelper.getDirectInterfaces(type)
              .forEach(i -> collectStereotypesOnTypes(seen, stereotypes, i));
    }
  }

  private Element collectStereotypes(
          Set<Element> seen, Set<String> stereotypes, Element element,
          AnnotationMirror annotation) {

    if (isIndexedAnnotation(annotation)) {
      stereotypes.add(this.typeHelper.getType(element));
    }
    return getCandidateAnnotationElement(seen, annotation);
  }

  private Element getCandidateAnnotationElement(Set<Element> seen, AnnotationMirror annotation) {
    Element element = annotation.getAnnotationType().asElement();
    if (seen.contains(element)) {
      return null;
    }
    // We need to visit all indexed annotations.
    if (!isIndexedAnnotation(annotation)) {
      seen.add(element);
    }
    return !element.toString().startsWith("java.lang") ? element : null;
  }

  private boolean isAnnotatedWithIndexed(Element type) {
    for (AnnotationMirror annotation : type.getAnnotationMirrors()) {
      if (isIndexedAnnotation(annotation)) {
        return true;
      }
    }
    return false;
  }

  private boolean isIndexedAnnotation(AnnotationMirror annotation) {
    return INDEXED_ANNOTATION.equals(annotation.getAnnotationType().toString());
  }

}
