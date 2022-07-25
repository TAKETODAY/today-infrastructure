/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
   * Generate the bytecode the performs the access operation into the specified MethodVisitor
   * using context information from the codeflow where necessary.
   *
   * @param propertyName the name of the property
   * @param mv the Asm method visitor into which code should be generated
   * @param cf the current state of the expression compiler
   */
  void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf);

}
