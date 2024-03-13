/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.expression.spel;

import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.core.CodeFlow;
import cn.taketoday.expression.PropertyAccessor;

/**
 * A compilable property accessor is able to generate bytecode that represents
 * the access operation, facilitating compilation to bytecode of expressions
 * that use the accessor.
 *
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface CompilablePropertyAccessor extends PropertyAccessor, Opcodes {

  /**
   * Return {@code true} if this property accessor is currently suitable for compilation.
   */
  boolean isCompilable();

  /**
   * Return the type of the accessed property - may only be known once an access has occurred.
   */
  Class<?> getPropertyType();

  /**
   * Generate the bytecode that performs the access operation into the specified
   * {@link MethodVisitor} using context information from the {@link CodeFlow}
   * where necessary.
   *
   * @param propertyName the name of the property
   * @param methodVisitor the ASM method visitor into which code should be generated
   * @param codeFlow the current state of the expression compiler
   */
  void generateCode(String propertyName, MethodVisitor methodVisitor, CodeFlow codeFlow);

}
