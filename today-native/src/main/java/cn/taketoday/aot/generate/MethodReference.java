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

package cn.taketoday.aot.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import java.util.function.Function;

import cn.taketoday.lang.Nullable;

/**
 * A reference to a method with convenient code generation for
 * referencing, or invoking it.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0
 */
public interface MethodReference {

  /**
   * Return this method reference as a {@link CodeBlock}. If the reference is
   * to an instance method then {@code this::<method name>} will be returned.
   *
   * @return a code block for the method reference.
   */
  CodeBlock toCodeBlock();

  /**
   * Return this method reference as a {@link CodeBlock} using the specified
   * {@link ArgumentCodeGenerator}.
   *
   * @param argumentCodeGenerator the argument code generator to use
   * @return a code block to invoke the method
   */
  default CodeBlock toInvokeCodeBlock(ArgumentCodeGenerator argumentCodeGenerator) {
    return toInvokeCodeBlock(argumentCodeGenerator, null);
  }

  /**
   * Return this method reference as a {@link CodeBlock} using the specified
   * {@link ArgumentCodeGenerator}. The {@code targetClassName} defines the
   * context in which the method invocation is added.
   * <p>If the caller has an instance of the type in which this method is
   * defined, it can hint that by specifying the type as a target class.
   *
   * @param argumentCodeGenerator the argument code generator to use
   * @param targetClassName the target class name
   * @return a code block to invoke the method
   */
  CodeBlock toInvokeCodeBlock(ArgumentCodeGenerator argumentCodeGenerator, @Nullable ClassName targetClassName);

  /**
   * Strategy for generating code for arguments based on their type.
   */
  interface ArgumentCodeGenerator {

    /**
     * Generate the code for the given argument type. If this type is
     * not supported, return {@code null}.
     *
     * @param argumentType the argument type
     * @return the code for this argument, or {@code null}
     */
    @Nullable
    CodeBlock generateCode(TypeName argumentType);

    /**
     * Factory method that returns an {@link ArgumentCodeGenerator} that
     * always returns {@code null}.
     *
     * @return a new {@link ArgumentCodeGenerator} instance
     */
    static ArgumentCodeGenerator none() {
      return from(type -> null);
    }

    /**
     * Factory method that can be used to create an {@link ArgumentCodeGenerator}
     * that support only the given argument type.
     *
     * @param argumentType the argument type
     * @param argumentCode the code for an argument of that type
     * @return a new {@link ArgumentCodeGenerator} instance
     */
    static ArgumentCodeGenerator of(Class<?> argumentType, String argumentCode) {
      return from(candidateType -> candidateType.equals(ClassName.get(argumentType)) ?
                                   CodeBlock.of(argumentCode) : null);
    }

    /**
     * Factory method that creates a new {@link ArgumentCodeGenerator} from
     * a lambda friendly function. The given function is provided with the
     * argument type and must provide the code to use or {@code null} if
     * the type is not supported.
     *
     * @param function the resolver function
     * @return a new {@link ArgumentCodeGenerator} instance backed by the function
     */
    static ArgumentCodeGenerator from(Function<TypeName, CodeBlock> function) {
      return function::apply;
    }

    /**
     * Create a new composed {@link ArgumentCodeGenerator} by combining this
     * generator with supporting the given argument type.
     *
     * @param argumentType the argument type
     * @param argumentCode the code for an argument of that type
     * @return a new composite {@link ArgumentCodeGenerator} instance
     */
    default ArgumentCodeGenerator and(Class<?> argumentType, String argumentCode) {
      return and(ArgumentCodeGenerator.of(argumentType, argumentCode));
    }

    /**
     * Create a new composed {@link ArgumentCodeGenerator} by combining this
     * generator with the given generator.
     *
     * @param argumentCodeGenerator the argument generator to add
     * @return a new composite {@link ArgumentCodeGenerator} instance
     */
    default ArgumentCodeGenerator and(ArgumentCodeGenerator argumentCodeGenerator) {
      return from(type -> {
        CodeBlock code = generateCode(type);
        return (code != null ? code : argumentCodeGenerator.generateCode(type));
      });
    }

  }

}
