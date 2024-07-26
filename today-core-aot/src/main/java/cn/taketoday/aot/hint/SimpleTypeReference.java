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

package cn.taketoday.aot.hint;

import java.util.List;

import javax.lang.model.SourceVersion;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A {@link TypeReference} based on fully qualified name.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
final class SimpleTypeReference extends AbstractTypeReference {

  private static final List<String> PRIMITIVE_NAMES = List.of(
          "boolean", "byte", "short", "int", "long", "char", "float", "double", "void");

  @Nullable
  private String canonicalName;

  SimpleTypeReference(String packageName, String simpleName, @Nullable TypeReference enclosingType) {
    super(packageName, simpleName, enclosingType);
  }

  static SimpleTypeReference of(String className) {
    Assert.notNull(className, "'className' is required");
    if (!isValidClassName(className)) {
      throw new IllegalStateException("Invalid class name '" + className + "'");
    }
    if (!className.contains("$")) {
      return createTypeReference(className);
    }
    String[] elements = className.split("(?<!\\$)\\$(?!\\$)");
    SimpleTypeReference typeReference = createTypeReference(elements[0]);
    for (int i = 1; i < elements.length; i++) {
      typeReference = new SimpleTypeReference(typeReference.getPackageName(), elements[i], typeReference);
    }
    return typeReference;
  }

  private static boolean isValidClassName(String className) {
    for (String s : className.split("\\.", -1)) {
      String candidate = s.replace("[", "").replace("]", "");
      if (!SourceVersion.isIdentifier(candidate)) {
        return false;
      }
    }
    return true;
  }

  private static SimpleTypeReference createTypeReference(String className) {
    int i = className.lastIndexOf('.');
    if (i != -1) {
      return new SimpleTypeReference(className.substring(0, i), className.substring(i + 1), null);
    }
    else {
      String packageName = isPrimitive(className) ? "java.lang" : "";
      return new SimpleTypeReference(packageName, className, null);
    }
  }

  @Override
  public String getCanonicalName() {
    if (this.canonicalName == null) {
      StringBuilder names = new StringBuilder();
      buildName(this, names);
      this.canonicalName = addPackageIfNecessary(names.toString());
    }
    return this.canonicalName;
  }

  @Override
  protected boolean isPrimitive() {
    return isPrimitive(getSimpleName());
  }

  private static boolean isPrimitive(String name) {
    return PRIMITIVE_NAMES.stream().anyMatch(name::startsWith);
  }

  private static void buildName(@Nullable TypeReference type, StringBuilder sb) {
    if (type == null) {
      return;
    }
    String typeName = (type.getEnclosingType() != null) ? "." + type.getSimpleName() : type.getSimpleName();
    sb.insert(0, typeName);
    buildName(type.getEnclosingType(), sb);
  }

}
