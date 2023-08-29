/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.annotation.config.processor;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * Utilities for dealing with {@link Element} classes.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Phillip Webb
 * @since 4.0
 */
final class Elements {

  static String getQualifiedName(Element element) {
    if (element != null) {
      TypeElement enclosingElement = getEnclosingTypeElement(element.asType());
      if (enclosingElement != null) {
        return getQualifiedName(enclosingElement) + "$"
                + ((DeclaredType) element.asType()).asElement().getSimpleName().toString();
      }
      if (element instanceof TypeElement typeElement) {
        return typeElement.getQualifiedName().toString();
      }
    }
    return null;
  }

  private static TypeElement getEnclosingTypeElement(TypeMirror type) {
    if (type instanceof DeclaredType declaredType) {
      Element enclosingElement = declaredType.asElement().getEnclosingElement();
      if (enclosingElement instanceof TypeElement typeElement) {
        return typeElement;
      }
    }
    return null;
  }

}
