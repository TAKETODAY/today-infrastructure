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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;

import static cn.taketoday.javapoet.Util.checkNotNull;

public final class ArrayTypeName extends TypeName {
  public final TypeName componentType;

  private ArrayTypeName(TypeName componentType) {
    this(componentType, new ArrayList<>());
  }

  private ArrayTypeName(TypeName componentType, List<AnnotationSpec> annotations) {
    super(annotations);
    this.componentType = checkNotNull(componentType, "rawType == null");
  }

  @Override
  public ArrayTypeName annotated(List<AnnotationSpec> annotations) {
    return new ArrayTypeName(componentType, concatAnnotations(annotations));
  }

  @Override
  public TypeName withoutAnnotations() {
    return new ArrayTypeName(componentType);
  }

  @Override
  CodeWriter emit(CodeWriter out) throws IOException {
    return emit(out, false);
  }

  CodeWriter emit(CodeWriter out, boolean varargs) throws IOException {
    emitLeafType(out);
    return emitBrackets(out, varargs);
  }

  private CodeWriter emitLeafType(CodeWriter out) throws IOException {
    if (TypeName.asArray(componentType) != null) {
      return TypeName.asArray(componentType).emitLeafType(out);
    }
    return componentType.emit(out);
  }

  private CodeWriter emitBrackets(CodeWriter out, boolean varargs) throws IOException {
    if (isAnnotated()) {
      out.emit(" ");
      emitAnnotations(out);
    }

    if (TypeName.asArray(componentType) == null) {
      // Last bracket.
      return out.emit(varargs ? "..." : "[]");
    }
    out.emit("[]");
    return TypeName.asArray(componentType).emitBrackets(out, varargs);
  }

  /** Returns an array type whose elements are all instances of {@code componentType}. */
  public static ArrayTypeName of(TypeName componentType) {
    return new ArrayTypeName(componentType);
  }

  /** Returns an array type whose elements are all instances of {@code componentType}. */
  public static ArrayTypeName of(Type componentType) {
    return of(TypeName.get(componentType));
  }

  /** Returns an array type equivalent to {@code mirror}. */
  public static ArrayTypeName get(ArrayType mirror) {
    return get(mirror, new LinkedHashMap<>());
  }

  static ArrayTypeName get(
          ArrayType mirror, Map<TypeParameterElement, TypeVariableName> typeVariables) {
    return new ArrayTypeName(get(mirror.getComponentType(), typeVariables));
  }

  /** Returns an array type equivalent to {@code type}. */
  public static ArrayTypeName get(GenericArrayType type) {
    return get(type, new LinkedHashMap<>());
  }

  static ArrayTypeName get(GenericArrayType type, Map<Type, TypeVariableName> map) {
    return ArrayTypeName.of(get(type.getGenericComponentType(), map));
  }
}
