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
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.TypeName;

import org.junit.jupiter.api.Test;

import javax.lang.model.element.Modifier;

import cn.taketoday.aot.generate.MethodReference.ArgumentCodeGenerator;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link DefaultMethodReference}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class DefaultMethodReferenceTests {

  private static final String EXPECTED_STATIC = "cn.taketoday.aot.generate.DefaultMethodReferenceTests::someMethod";

  private static final String EXPECTED_ANONYMOUS_INSTANCE = "<instance>::someMethod";

  private static final String EXPECTED_DECLARED_INSTANCE = "<cn.taketoday.aot.generate.DefaultMethodReferenceTests>::someMethod";

  private static final ClassName TEST_CLASS_NAME = ClassName.get("com.example", "Test");

  private static final ClassName INITIALIZER_CLASS_NAME = ClassName.get("com.example", "Initializer");

  @Test
  void createWithStringCreatesMethodReference() {
    MethodSpec method = createTestMethod("someMethod", new TypeName[0]);
    MethodReference reference = new DefaultMethodReference(method, null);
    assertThat(reference).hasToString(EXPECTED_ANONYMOUS_INSTANCE);
  }

  @Test
  void createWithClassNameAndStringCreateMethodReference() {
    ClassName declaringClass = ClassName.get(DefaultMethodReferenceTests.class);
    MethodReference reference = createMethodReference("someMethod", new TypeName[0], declaringClass);
    assertThat(reference).hasToString(EXPECTED_DECLARED_INSTANCE);
  }

  @Test
  void createWithStaticAndClassAndStringCreatesMethodReference() {
    ClassName declaringClass = ClassName.get(DefaultMethodReferenceTests.class);
    MethodReference reference = createStaticMethodReference("someMethod", declaringClass);
    assertThat(reference).hasToString(EXPECTED_STATIC);
  }

  @Test
  void toCodeBlock() {
    assertThat(createLocalMethodReference("methodName").toCodeBlock())
            .isEqualTo(CodeBlock.of("this::methodName"));
  }

  @Test
  void toCodeBlockWithStaticMethod() {
    assertThat(createStaticMethodReference("methodName", TEST_CLASS_NAME).toCodeBlock())
            .isEqualTo(CodeBlock.of("com.example.Test::methodName"));
  }

  @Test
  void toCodeBlockWithStaticMethodRequiresDeclaringClass() {
    MethodSpec method = createTestMethod("methodName", new TypeName[0], Modifier.STATIC);
    MethodReference methodReference = new DefaultMethodReference(method, null);
    assertThatIllegalStateException().isThrownBy(methodReference::toCodeBlock)
            .withMessage("static method reference must define a declaring class");
  }

  @Test
  void toInvokeCodeBlockWithNullDeclaringClassAndTargetClass() {
    MethodSpec method = createTestMethod("methodName", new TypeName[0]);
    MethodReference methodReference = new DefaultMethodReference(method, null);
    assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none(), TEST_CLASS_NAME))
            .isEqualTo(CodeBlock.of("methodName()"));
  }

  @Test
  void toInvokeCodeBlockWithNullDeclaringClassAndNullTargetClass() {
    MethodSpec method = createTestMethod("methodName", new TypeName[0]);
    MethodReference methodReference = new DefaultMethodReference(method, null);
    assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none()))
            .isEqualTo(CodeBlock.of("methodName()"));
  }

  @Test
  void toInvokeCodeBlockWithDeclaringClassAndNullTargetClass() {
    MethodSpec method = createTestMethod("methodName", new TypeName[0]);
    MethodReference methodReference = new DefaultMethodReference(method, TEST_CLASS_NAME);
    assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none()))
            .isEqualTo(CodeBlock.of("new com.example.Test().methodName()"));
  }

  @Test
  void toInvokeCodeBlockWithMatchingTargetClass() {
    MethodSpec method = createTestMethod("methodName", new TypeName[0]);
    MethodReference methodReference = new DefaultMethodReference(method, TEST_CLASS_NAME);
    CodeBlock invocation = methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none(), TEST_CLASS_NAME);
    // Assume com.example.Test is in a `test` variable.
    assertThat(CodeBlock.of("$L.$L", "test", invocation)).isEqualTo(CodeBlock.of("test.methodName()"));
  }

  @Test
  void toInvokeCodeBlockWithNonMatchingDeclaringClass() {
    MethodSpec method = createTestMethod("methodName", new TypeName[0]);
    MethodReference methodReference = new DefaultMethodReference(method, TEST_CLASS_NAME);
    assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none(), INITIALIZER_CLASS_NAME))
            .isEqualTo(CodeBlock.of("new com.example.Test().methodName()"));
  }

  @Test
  void toInvokeCodeBlockWithMatchingArg() {
    MethodReference methodReference = createLocalMethodReference("methodName", ClassName.get(String.class));
    ArgumentCodeGenerator argCodeGenerator = ArgumentCodeGenerator.of(String.class, "stringArg");
    assertThat(methodReference.toInvokeCodeBlock(argCodeGenerator))
            .isEqualTo(CodeBlock.of("methodName(stringArg)"));
  }

  @Test
  void toInvokeCodeBlockWithMatchingArgs() {
    MethodReference methodReference = createLocalMethodReference("methodName",
            ClassName.get(Integer.class), ClassName.get(String.class));
    ArgumentCodeGenerator argCodeGenerator = ArgumentCodeGenerator.of(String.class, "stringArg")
            .and(Integer.class, "integerArg");
    assertThat(methodReference.toInvokeCodeBlock(argCodeGenerator))
            .isEqualTo(CodeBlock.of("methodName(integerArg, stringArg)"));
  }

  @Test
  void toInvokeCodeBlockWithNonMatchingArg() {
    MethodReference methodReference = createLocalMethodReference("methodName",
            ClassName.get(Integer.class), ClassName.get(String.class));
    ArgumentCodeGenerator argCodeGenerator = ArgumentCodeGenerator.of(Integer.class, "integerArg");
    assertThatIllegalArgumentException().isThrownBy(() -> methodReference.toInvokeCodeBlock(argCodeGenerator))
            .withMessageContaining("parameter 1 of type java.lang.String is not supported");
  }

  @Test
  void toInvokeCodeBlockWithStaticMethodAndMatchingDeclaringClass() {
    MethodReference methodReference = createStaticMethodReference("methodName", TEST_CLASS_NAME);
    assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none(), TEST_CLASS_NAME))
            .isEqualTo(CodeBlock.of("methodName()"));
  }

  @Test
  void toInvokeCodeBlockWithStaticMethodAndSeparateDeclaringClass() {
    MethodReference methodReference = createStaticMethodReference("methodName", TEST_CLASS_NAME);
    assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none(), INITIALIZER_CLASS_NAME))
            .isEqualTo(CodeBlock.of("com.example.Test.methodName()"));
  }

  private MethodReference createLocalMethodReference(String name, TypeName... argumentTypes) {
    return createMethodReference(name, argumentTypes, null);
  }

  private MethodReference createMethodReference(String name, TypeName[] argumentTypes, @Nullable ClassName declaringClass) {
    MethodSpec method = createTestMethod(name, argumentTypes);
    return new DefaultMethodReference(method, declaringClass);
  }

  private MethodReference createStaticMethodReference(String name, ClassName declaringClass, TypeName... argumentTypes) {
    MethodSpec method = createTestMethod(name, argumentTypes, Modifier.STATIC);
    return new DefaultMethodReference(method, declaringClass);
  }

  private MethodSpec createTestMethod(String name, TypeName[] argumentTypes, Modifier... modifiers) {
    Builder method = MethodSpec.methodBuilder(name);
    for (int i = 0; i < argumentTypes.length; i++) {
      method.addParameter(argumentTypes[i], "args" + i);
    }
    method.addModifiers(modifiers);
    return method.build();
  }

}
