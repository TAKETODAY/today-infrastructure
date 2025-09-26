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

package infra.expression.spel.ast;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.StringJoiner;

import infra.core.TypeDescriptor;
import infra.util.ClassUtils;

/**
 * Utility methods (formatters etc) used during parsing and evaluation.
 *
 * @author Andy Clement
 */
abstract class FormatHelper {

  /**
   * Produce a readable representation for a given method name with specified arguments.
   *
   * @param name the name of the method
   * @param argumentTypes the types of the arguments to the method
   * @return a nicely formatted representation, e.g. {@code foo(String,int)}
   */
  public static String formatMethodForMessage(String name, List<TypeDescriptor> argumentTypes) {
    StringJoiner sj = new StringJoiner(",", "(", ")");
    for (TypeDescriptor typeDescriptor : argumentTypes) {
      if (typeDescriptor != null) {
        sj.add(formatClassNameForMessage(typeDescriptor.getType()));
      }
      else {
        sj.add(formatClassNameForMessage(null));
      }
    }
    return name + sj;
  }

  /**
   * Determine a readable name for a given Class object.
   * <p>A String array will have the formatted name "java.lang.String[]".
   *
   * @param clazz the Class whose name is to be formatted
   * @return a formatted String suitable for message inclusion
   * @see ClassUtils#getQualifiedName(Class)
   */
  public static String formatClassNameForMessage(@Nullable Class<?> clazz) {
    return (clazz != null ? ClassUtils.getQualifiedName(clazz) : "null");
  }

}
