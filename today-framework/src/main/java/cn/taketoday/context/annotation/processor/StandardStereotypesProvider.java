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

import java.util.LinkedHashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * A {@link StereotypesProvider} that extracts a stereotype for each
 * {@code javax.*} annotation <i>present</i> on a class or interface.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
class StandardStereotypesProvider implements StereotypesProvider {

  private final TypeHelper typeHelper;

  StandardStereotypesProvider(TypeHelper typeHelper) {
    this.typeHelper = typeHelper;
  }

  @Override
  public Set<String> getStereotypes(Element element) {
    Set<String> stereotypes = new LinkedHashSet<>();
    ElementKind kind = element.getKind();
    if (kind != ElementKind.CLASS && kind != ElementKind.INTERFACE) {
      return stereotypes;
    }
    for (AnnotationMirror annotation : this.typeHelper.getAllAnnotationMirrors(element)) {
      String type = this.typeHelper.getType(annotation);
      if (type.startsWith("jakarta.")) {
        stereotypes.add(type);
      }
    }
    return stereotypes;
  }

}
