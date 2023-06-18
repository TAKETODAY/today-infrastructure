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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Default {@link MethodReference} implementation based on a {@link MethodSpec}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0
 */
public class DefaultMethodReference implements MethodReference {

  private final MethodSpec method;

  @Nullable
  private final ClassName declaringClass;

  public DefaultMethodReference(MethodSpec method, @Nullable ClassName declaringClass) {
    this.method = method;
    this.declaringClass = declaringClass;
  }

  @Override
  public CodeBlock toCodeBlock() {
    String methodName = this.method.name;
    if (isStatic()) {
      Assert.state(this.declaringClass != null, "static method reference must define a declaring class");
      return CodeBlock.of("$T::$L", this.declaringClass, methodName);
    }
    else {
      return CodeBlock.of("this::$L", methodName);
    }
  }

  @Override
  public CodeBlock toInvokeCodeBlock(ArgumentCodeGenerator argumentCodeGenerator,
          @Nullable ClassName targetClassName) {
    String methodName = this.method.name;
    CodeBlock.Builder code = CodeBlock.builder();
    if (isStatic()) {
      Assert.state(this.declaringClass != null, "static method reference must define a declaring class");
      if (isSameDeclaringClass(targetClassName)) {
        code.add("$L", methodName);
      }
      else {
        code.add("$T.$L", this.declaringClass, methodName);
      }
    }
    else {
      if (!isSameDeclaringClass(targetClassName)) {
        code.add(instantiateDeclaringClass(this.declaringClass));
      }
      code.add("$L", methodName);
    }
    code.add("(");
    addArguments(code, argumentCodeGenerator);
    code.add(")");
    return code.build();
  }

  /**
   * Add the code for the method arguments using the specified
   * {@link ArgumentCodeGenerator} if necessary.
   *
   * @param code the code builder to use to add method arguments
   * @param argumentCodeGenerator the code generator to use
   */
  protected void addArguments(CodeBlock.Builder code, ArgumentCodeGenerator argumentCodeGenerator) {
    List<CodeBlock> arguments = new ArrayList<>();
    TypeName[] argumentTypes = this.method.parameters.stream()
            .map(parameter -> parameter.type).toArray(TypeName[]::new);
    for (int i = 0; i < argumentTypes.length; i++) {
      TypeName argumentType = argumentTypes[i];
      CodeBlock argumentCode = argumentCodeGenerator.generateCode(argumentType);
      if (argumentCode == null) {
        throw new IllegalArgumentException("Could not generate code for " + this
                + ": parameter " + i + " of type " + argumentType + " is not supported");
      }
      arguments.add(argumentCode);
    }
    code.add(CodeBlock.join(arguments, ", "));
  }

  protected CodeBlock instantiateDeclaringClass(ClassName declaringClass) {
    return CodeBlock.of("new $T().", declaringClass);
  }

  private boolean isStatic() {
    return this.method.modifiers.contains(Modifier.STATIC);
  }

  private boolean isSameDeclaringClass(ClassName declaringClass) {
    return this.declaringClass == null || this.declaringClass.equals(declaringClass);
  }

  @Override
  public String toString() {
    String methodName = this.method.name;
    if (isStatic()) {
      return this.declaringClass + "::" + methodName;
    }
    else {
      return ((this.declaringClass != null) ?
              "<" + this.declaringClass + ">" : "<instance>") + "::" + methodName;
    }
  }

}
