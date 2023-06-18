/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
package cn.taketoday.javapoet;

import java.io.IOException;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

import static cn.taketoday.javapoet.Util.checkArgument;

public final class WildcardTypeName extends TypeName {
  public final List<TypeName> upperBounds;
  public final List<TypeName> lowerBounds;

  private WildcardTypeName(List<TypeName> upperBounds, List<TypeName> lowerBounds) {
    this(upperBounds, lowerBounds, new ArrayList<>());
  }

  private WildcardTypeName(List<TypeName> upperBounds, List<TypeName> lowerBounds,
          List<AnnotationSpec> annotations) {
    super(annotations);
    this.upperBounds = Util.immutableList(upperBounds);
    this.lowerBounds = Util.immutableList(lowerBounds);

    checkArgument(this.upperBounds.size() == 1, "unexpected extends bounds: %s", upperBounds);
    for (TypeName upperBound : this.upperBounds) {
      checkArgument(!upperBound.isPrimitive() && upperBound != VOID,
              "invalid upper bound: %s", upperBound);
    }
    for (TypeName lowerBound : this.lowerBounds) {
      checkArgument(!lowerBound.isPrimitive() && lowerBound != VOID,
              "invalid lower bound: %s", lowerBound);
    }
  }

  @Override
  public WildcardTypeName annotated(List<AnnotationSpec> annotations) {
    return new WildcardTypeName(upperBounds, lowerBounds, concatAnnotations(annotations));
  }

  @Override
  public TypeName withoutAnnotations() {
    return new WildcardTypeName(upperBounds, lowerBounds);
  }

  @Override
  CodeWriter emit(CodeWriter out) throws IOException {
    if (lowerBounds.size() == 1) {
      return out.emit("? super $T", lowerBounds.get(0));
    }
    return upperBounds.get(0).equals(TypeName.OBJECT)
           ? out.emit("?")
           : out.emit("? extends $T", upperBounds.get(0));
  }

  /**
   * Returns a type that represents an unknown type that extends {@code bound}. For example, if
   * {@code bound} is {@code CharSequence.class}, this returns {@code ? extends CharSequence}. If
   * {@code bound} is {@code Object.class}, this returns {@code ?}, which is shorthand for {@code
   * ? extends Object}.
   */
  public static WildcardTypeName subtypeOf(TypeName upperBound) {
    return new WildcardTypeName(Collections.singletonList(upperBound), Collections.emptyList());
  }

  public static WildcardTypeName subtypeOf(Type upperBound) {
    return subtypeOf(TypeName.get(upperBound));
  }

  /**
   * Returns a type that represents an unknown supertype of {@code bound}. For example, if {@code
   * bound} is {@code String.class}, this returns {@code ? super String}.
   */
  public static WildcardTypeName supertypeOf(TypeName lowerBound) {
    return new WildcardTypeName(Collections.singletonList(OBJECT),
            Collections.singletonList(lowerBound));
  }

  public static WildcardTypeName supertypeOf(Type lowerBound) {
    return supertypeOf(TypeName.get(lowerBound));
  }

  public static TypeName get(javax.lang.model.type.WildcardType mirror) {
    return get(mirror, new LinkedHashMap<>());
  }

  static TypeName get(
          javax.lang.model.type.WildcardType mirror,
          Map<TypeParameterElement, TypeVariableName> typeVariables) {
    TypeMirror extendsBound = mirror.getExtendsBound();
    if (extendsBound == null) {
      TypeMirror superBound = mirror.getSuperBound();
      if (superBound == null) {
        return subtypeOf(Object.class);
      }
      else {
        return supertypeOf(TypeName.get(superBound, typeVariables));
      }
    }
    else {
      return subtypeOf(TypeName.get(extendsBound, typeVariables));
    }
  }

  public static TypeName get(WildcardType wildcardName) {
    return get(wildcardName, new LinkedHashMap<>());
  }

  static TypeName get(WildcardType wildcardName, Map<Type, TypeVariableName> map) {
    return new WildcardTypeName(
            list(wildcardName.getUpperBounds(), map),
            list(wildcardName.getLowerBounds(), map));
  }
}
