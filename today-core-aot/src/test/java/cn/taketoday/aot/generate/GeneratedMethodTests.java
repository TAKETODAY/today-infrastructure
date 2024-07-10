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

import cn.taketoday.javapoet.ClassName;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.MethodSpec;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import cn.taketoday.aot.generate.MethodReference.ArgumentCodeGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link GeneratedMethod}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class GeneratedMethodTests {

  private static final ClassName TEST_CLASS_NAME = ClassName.get("com.example", "Test");

  private static final Consumer<MethodSpec.Builder> emptyMethod = method -> { };

  private static final String NAME = "spring";

  @Test
  void getNameReturnsName() {
    GeneratedMethod generatedMethod = new GeneratedMethod(TEST_CLASS_NAME, NAME, emptyMethod);
    assertThat(generatedMethod.getName()).isSameAs(NAME);
  }

  @Test
  void generateMethodSpecReturnsMethodSpec() {
    GeneratedMethod generatedMethod = create(method -> method.addJavadoc("Test"));
    assertThat(generatedMethod.getMethodSpec().javadoc).asString().contains("Test");
  }

  @Test
  void generateMethodSpecWhenMethodNameIsChangedThrowsException() {
    assertThatIllegalStateException().isThrownBy(() ->
                    create(method -> method.setName("badname")).getMethodSpec())
            .withMessage("'method' consumer must not change the generated method name");
  }

  @Test
  void toMethodReferenceWithInstanceMethod() {
    GeneratedMethod generatedMethod = create(emptyMethod);
    MethodReference methodReference = generatedMethod.toMethodReference();
    assertThat(methodReference).isNotNull();
    assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none(), TEST_CLASS_NAME))
            .isEqualTo(CodeBlock.of("spring()"));
  }

  @Test
  void toMethodReferenceWithStaticMethod() {
    GeneratedMethod generatedMethod = create(method -> method.addModifiers(Modifier.STATIC));
    MethodReference methodReference = generatedMethod.toMethodReference();
    assertThat(methodReference).isNotNull();
    ClassName anotherDeclaringClass = ClassName.get("com.example", "Another");
    assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none(), anotherDeclaringClass))
            .isEqualTo(CodeBlock.of("com.example.Test.spring()"));
  }

  private GeneratedMethod create(Consumer<MethodSpec.Builder> method) {
    return new GeneratedMethod(TEST_CLASS_NAME, NAME, method);
  }

}
