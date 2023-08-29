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

package cn.taketoday.context.properties.processor.fieldvalues.javac;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Reflection based access to {@code com.sun.source.tree.ExpressionTree}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ExpressionTree extends ReflectionWrapper {

  private final Class<?> literalTreeType = findClass("com.sun.source.tree.LiteralTree");

  private final Method literalValueMethod = findMethod(this.literalTreeType, "getValue");

  private final Class<?> methodInvocationTreeType = findClass("com.sun.source.tree.MethodInvocationTree");

  private final Method methodInvocationArgumentsMethod = findMethod(this.methodInvocationTreeType, "getArguments");

  private final Class<?> newArrayTreeType = findClass("com.sun.source.tree.NewArrayTree");

  private final Method arrayValueMethod = findMethod(this.newArrayTreeType, "getInitializers");

  ExpressionTree(Object instance) {
    super("com.sun.source.tree.ExpressionTree", instance);
  }

  String getKind() throws Exception {
    return findMethod("getKind").invoke(getInstance()).toString();
  }

  Object getLiteralValue() throws Exception {
    if (this.literalTreeType.isAssignableFrom(getInstance().getClass())) {
      return this.literalValueMethod.invoke(getInstance());
    }
    return null;
  }

  Object getFactoryValue() throws Exception {
    if (this.methodInvocationTreeType.isAssignableFrom(getInstance().getClass())) {
      List<?> arguments = (List<?>) this.methodInvocationArgumentsMethod.invoke(getInstance());
      if (arguments.size() == 1) {
        return new ExpressionTree(arguments.get(0)).getLiteralValue();
      }
    }
    return null;
  }

  List<? extends ExpressionTree> getArrayExpression() throws Exception {
    if (this.newArrayTreeType.isAssignableFrom(getInstance().getClass())) {
      List<?> elements = (List<?>) this.arrayValueMethod.invoke(getInstance());
      List<ExpressionTree> result = new ArrayList<>();
      if (elements == null) {
        return result;
      }
      for (Object element : elements) {
        result.add(new ExpressionTree(element));
      }
      return result;
    }
    return null;
  }

}
