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

import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.Modifier;

/**
 * Reflection based access to {@code com.sun.source.tree.VariableTree}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class VariableTree extends ReflectionWrapper {

  VariableTree(Object instance) {
    super("com.sun.source.tree.VariableTree", instance);
  }

  String getName() throws Exception {
    return findMethod("getName").invoke(getInstance()).toString();
  }

  String getType() throws Exception {
    return findMethod("getType").invoke(getInstance()).toString();
  }

  ExpressionTree getInitializer() throws Exception {
    Object instance = findMethod("getInitializer").invoke(getInstance());
    return (instance != null) ? new ExpressionTree(instance) : null;
  }

  @SuppressWarnings("unchecked")
  Set<Modifier> getModifierFlags() throws Exception {
    Object modifiers = findMethod("getModifiers").invoke(getInstance());
    if (modifiers == null) {
      return Collections.emptySet();
    }
    return (Set<Modifier>) findMethod(findClass("com.sun.source.tree.ModifiersTree"), "getFlags").invoke(modifiers);
  }

}
