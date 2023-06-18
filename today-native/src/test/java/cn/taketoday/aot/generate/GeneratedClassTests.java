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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link GeneratedClass}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class GeneratedClassTests {

  private static final ClassName TEST_CLASS_NAME = ClassName.get("com.example", "Test");

  private static final Consumer<TypeSpec.Builder> emptyTypeCustomizer = type -> { };

  private static final Consumer<MethodSpec.Builder> emptyMethodCustomizer = method -> { };

  @Test
  void getEnclosingNameOnTopLevelClassReturnsNull() {
    GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
    assertThat(generatedClass.getEnclosingClass()).isNull();
  }

  @Test
  void getEnclosingNameOnInnerClassReturnsParent() {
    GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
    GeneratedClass innerGeneratedClass = generatedClass.getOrAdd("Test", emptyTypeCustomizer);
    assertThat(innerGeneratedClass.getEnclosingClass()).isEqualTo(generatedClass);
  }

  @Test
  void getNameReturnsName() {
    GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
    assertThat(generatedClass.getName()).isSameAs(TEST_CLASS_NAME);
  }

  @Test
  void reserveMethodNamesWhenNameUsedThrowsException() {
    GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
    generatedClass.getMethods().add("apply", emptyMethodCustomizer);
    assertThatIllegalStateException()
            .isThrownBy(() -> generatedClass.reserveMethodNames("apply"));
  }

  @Test
  void reserveMethodNamesReservesNames() {
    GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
    generatedClass.reserveMethodNames("apply");
    GeneratedMethod generatedMethod = generatedClass.getMethods().add("apply", emptyMethodCustomizer);
    assertThat(generatedMethod.getName()).isEqualTo("apply1");
  }

  @Test
  void generateMethodNameWhenAllEmptyPartsGeneratesSetName() {
    GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
    GeneratedMethod generatedMethod = generatedClass.getMethods().add("123", emptyMethodCustomizer);
    assertThat(generatedMethod.getName()).isEqualTo("$$aot");
  }

  @Test
  void getOrAddWhenRepeatReturnsSameGeneratedClass() {
    GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
    GeneratedClass innerGeneratedClass = generatedClass.getOrAdd("Inner", emptyTypeCustomizer);
    GeneratedClass innerGeneratedClass2 = generatedClass.getOrAdd("Inner", emptyTypeCustomizer);
    GeneratedClass innerGeneratedClass3 = generatedClass.getOrAdd("Inner", emptyTypeCustomizer);
    assertThat(innerGeneratedClass).isSameAs(innerGeneratedClass2).isSameAs(innerGeneratedClass3);
  }

  @Test
  void generateJavaFileIncludesGeneratedMethods() {
    GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
    generatedClass.getMethods().add("test", method -> method.addJavadoc("Test Method"));
    assertThat(generatedClass.generateJavaFile().toString()).contains("Test Method");
  }

  @Test
  void generateJavaFileIncludesDeclaredClasses() {
    GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
    generatedClass.getOrAdd("First", type -> type.modifiers.add(Modifier.STATIC));
    generatedClass.getOrAdd("Second", type -> type.modifiers.add(Modifier.PRIVATE));
    assertThat(generatedClass.generateJavaFile().toString())
            .contains("static class First").contains("private class Second");
  }

  @Test
  void generateJavaFileOnInnerClassThrowsException() {
    GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME)
            .getOrAdd("Inner", emptyTypeCustomizer);
    assertThatIllegalStateException().isThrownBy(generatedClass::generateJavaFile);
  }

  private static GeneratedClass createGeneratedClass(ClassName className) {
    return new GeneratedClass(className, emptyTypeCustomizer);
  }

}
