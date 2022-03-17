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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Type utilities.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
class TypeHelper {

  private final Types types;
  private final ProcessingEnvironment env;

  public TypeHelper(ProcessingEnvironment env) {
    this.env = env;
    this.types = env.getTypeUtils();
  }

  public String getType(Element element) {
    return getType(element != null ? element.asType() : null);
  }

  public String getType(AnnotationMirror annotation) {
    return getType(annotation != null ? annotation.getAnnotationType() : null);
  }

  public String getType(TypeMirror type) {
    if (type == null) {
      return null;
    }
    if (type instanceof DeclaredType declaredType) {
      Element enclosingElement = declaredType.asElement().getEnclosingElement();
      if (enclosingElement instanceof TypeElement) {
        return getQualifiedName(enclosingElement) + "$" + declaredType.asElement().getSimpleName().toString();
      }
      else {
        return getQualifiedName(declaredType.asElement());
      }
    }
    return type.toString();
  }

  private String getQualifiedName(Element element) {
    if (element instanceof QualifiedNameable) {
      return ((QualifiedNameable) element).getQualifiedName().toString();
    }
    return element.toString();
  }

  /**
   * Return the super class of the specified {@link Element} or null if this
   * {@code element} represents {@link Object}.
   */
  public Element getSuperClass(Element element) {
    List<? extends TypeMirror> superTypes = this.types.directSupertypes(element.asType());
    if (superTypes.isEmpty()) {
      return null;  // reached java.lang.Object
    }
    return this.types.asElement(superTypes.get(0));
  }

  /**
   * Return the interfaces that are <strong>directly</strong> implemented by the
   * specified {@link Element} or an empty list if this {@code element} does not
   * implement any interface.
   */
  public List<Element> getDirectInterfaces(Element element) {
    List<? extends TypeMirror> superTypes = this.types.directSupertypes(element.asType());
    List<Element> directInterfaces = new ArrayList<>();
    if (superTypes.size() > 1) { // index 0 is the super class
      for (int i = 1; i < superTypes.size(); i++) {
        Element e = this.types.asElement(superTypes.get(i));
        if (e != null) {
          directInterfaces.add(e);
        }
      }
    }
    return directInterfaces;
  }

  public List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e) {
    try {
      return this.env.getElementUtils().getAllAnnotationMirrors(e);
    }
    catch (Exception ex) {
      // This may fail if one of the annotations is not available.
      return Collections.emptyList();
    }
  }

}
