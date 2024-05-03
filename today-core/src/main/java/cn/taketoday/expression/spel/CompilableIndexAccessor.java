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
import cn.taketoday.expression.IndexAccessor;

/**
 * A compilable {@link IndexAccessor} is able to generate bytecode that represents
 * the operation for reading the index, facilitating compilation to bytecode of
 * expressions that use the accessor.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public interface CompilableIndexAccessor extends IndexAccessor, Opcodes {

  /**
   * Determine if this {@code IndexAccessor} is currently suitable for compilation.
   * <p>May only be known once the index has been read.
   *
   * @see #read(cn.taketoday.expression.EvaluationContext, Object, Object)
   */
  boolean isCompilable();

  /**
   * Get the type of the indexed value.
   * <p>For example, given the expression {@code book.authors[0]}, the indexed
   * value type represents the result of {@code authors[0]} which may be an
   * {@code Author} object, a {@code String} representing the author's name, etc.
   * <p>May only be known once the index has been read.
   *
   * @see #read(cn.taketoday.expression.EvaluationContext, Object, Object)
   */
  Class<?> getIndexedValueType();

  /**
   * Generate bytecode that performs the operation for reading the index.
   * <p>Bytecode should be generated into the supplied {@link MethodVisitor}
   * using context information from the {@link CodeFlow} where necessary.
   * <p>The supplied {@code indexNode} should be used to generate the
   * appropriate bytecode to load the index onto the stack. For example, given
   * the expression {@code book.authors[0]}, invoking
   * {@code codeFlow.generateCodeForArgument(methodVisitor, indexNode, int.class)}
   * will ensure that the index ({@code 0}) is available on the stack as a
   * primitive {@code int}.
   * <p>Will only be invoked if {@link #isCompilable()} returns {@code true}.
   *
   * @param indexNode the {@link SpelNode} that represents the index being
   * accessed
   * @param methodVisitor the ASM {@link MethodVisitor} into which code should
   * be generated
   * @param codeFlow the current state of the expression compiler
   */
  void generateCode(SpelNode indexNode, MethodVisitor methodVisitor, CodeFlow codeFlow);

}
