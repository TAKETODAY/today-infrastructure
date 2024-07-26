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
import cn.taketoday.javapoet.TypeSpec;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.Consumer;

import cn.taketoday.aot.generate.GeneratedFiles.Kind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link GeneratedClasses}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class GeneratedClassesTests {

  private static final Consumer<TypeSpec.Builder> emptyTypeCustomizer = type -> { };

  private final GeneratedClasses generatedClasses = new GeneratedClasses(
          new ClassNameGenerator(ClassName.get("com.example", "Test")));

  @Test
  void createWhenClassNameGeneratorIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new GeneratedClasses(null))
            .withMessage("'classNameGenerator' is required");
  }

  @Test
  void addForFeatureComponentWhenFeatureNameIsEmptyThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.generatedClasses.addForFeatureComponent("",
                    TestComponent.class, emptyTypeCustomizer))
            .withMessage("'featureName' must not be empty");
  }

  @Test
  void addForFeatureWhenFeatureNameIsEmptyThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.generatedClasses.addForFeature("", emptyTypeCustomizer))
            .withMessage("'featureName' must not be empty");
  }

  @Test
  void addForFeatureComponentWhenTypeSpecCustomizerIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.generatedClasses
                    .addForFeatureComponent("test", TestComponent.class, null))
            .withMessage("'type' is required");
  }

  @Test
  void addForFeatureUsesDefaultTarget() {
    GeneratedClass generatedClass = this.generatedClasses.addForFeature("Test", emptyTypeCustomizer);
    assertThat(generatedClass.getName()).hasToString("com.example.Test__Test");
  }

  @Test
  void addForFeatureComponentUsesTarget() {
    GeneratedClass generatedClass = this.generatedClasses
            .addForFeatureComponent("Test", TestComponent.class, emptyTypeCustomizer);
    assertThat(generatedClass.getName().toString()).endsWith("TestComponent__Test");
  }

  @Test
  void addForFeatureComponentWithSameNameReturnsDifferentInstances() {
    GeneratedClass generatedClass1 = this.generatedClasses
            .addForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
    GeneratedClass generatedClass2 = this.generatedClasses
            .addForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
    assertThat(generatedClass1).isNotSameAs(generatedClass2);
    assertThat(generatedClass1.getName().simpleName()).endsWith("__One");
    assertThat(generatedClass2.getName().simpleName()).endsWith("__One1");
  }

  @Test
  void getOrAddForFeatureComponentWhenNewReturnsGeneratedClass() {
    GeneratedClass generatedClass1 = this.generatedClasses
            .getOrAddForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
    GeneratedClass generatedClass2 = this.generatedClasses
            .getOrAddForFeatureComponent("two", TestComponent.class, emptyTypeCustomizer);
    assertThat(generatedClass1).isNotNull().isNotEqualTo(generatedClass2);
    assertThat(generatedClass2).isNotNull();
  }

  @Test
  void getOrAddForFeatureWhenNewReturnsGeneratedClass() {
    GeneratedClass generatedClass1 = this.generatedClasses
            .getOrAddForFeature("one", emptyTypeCustomizer);
    GeneratedClass generatedClass2 = this.generatedClasses
            .getOrAddForFeature("two", emptyTypeCustomizer);
    assertThat(generatedClass1).isNotNull().isNotEqualTo(generatedClass2);
    assertThat(generatedClass2).isNotNull();
  }

  @Test
  void getOrAddForFeatureComponentWhenRepeatReturnsSameGeneratedClass() {
    GeneratedClass generatedClass1 = this.generatedClasses
            .getOrAddForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
    GeneratedClass generatedClass2 = this.generatedClasses
            .getOrAddForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
    GeneratedClass generatedClass3 = this.generatedClasses
            .getOrAddForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
    assertThat(generatedClass1).isNotNull().isSameAs(generatedClass2)
            .isSameAs(generatedClass3);
  }

  @Test
  void getOrAddForFeatureWhenRepeatReturnsSameGeneratedClass() {
    GeneratedClass generatedClass1 = this.generatedClasses
            .getOrAddForFeature("one", emptyTypeCustomizer);
    GeneratedClass generatedClass2 = this.generatedClasses
            .getOrAddForFeature("one", emptyTypeCustomizer);
    GeneratedClass generatedClass3 = this.generatedClasses
            .getOrAddForFeature("one", emptyTypeCustomizer);
    assertThat(generatedClass1).isNotNull().isSameAs(generatedClass2)
            .isSameAs(generatedClass3);
  }

  @Test
  void getOrAddForFeatureComponentWhenHasFeatureNamePrefix() {
    GeneratedClasses prefixed = this.generatedClasses.withFeatureNamePrefix("prefix");
    GeneratedClass generatedClass1 = this.generatedClasses.getOrAddForFeatureComponent(
            "one", TestComponent.class, emptyTypeCustomizer);
    GeneratedClass generatedClass2 = this.generatedClasses.getOrAddForFeatureComponent(
            "one", TestComponent.class, emptyTypeCustomizer);
    GeneratedClass generatedClass3 = prefixed.getOrAddForFeatureComponent
            ("one", TestComponent.class, emptyTypeCustomizer);
    GeneratedClass generatedClass4 = prefixed.getOrAddForFeatureComponent(
            "one", TestComponent.class, emptyTypeCustomizer);
    assertThat(generatedClass1).isSameAs(generatedClass2).isNotSameAs(generatedClass3);
    assertThat(generatedClass3).isSameAs(generatedClass4);
  }

  @Test
  @SuppressWarnings("unchecked")
  void writeToInvokeTypeSpecCustomizer() throws IOException {
    Consumer<TypeSpec.Builder> typeSpecCustomizer = mock();
    this.generatedClasses.addForFeatureComponent("one", TestComponent.class, typeSpecCustomizer);
    verifyNoInteractions(typeSpecCustomizer);
    InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
    this.generatedClasses.writeTo(generatedFiles);
    verify(typeSpecCustomizer).accept(any());
    assertThat(generatedFiles.getGeneratedFiles(Kind.SOURCE)).hasSize(1);
  }

  @Test
  void withNameUpdatesNamingConventions() {
    GeneratedClass generatedClass1 = this.generatedClasses
            .addForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
    GeneratedClass generatedClass2 = this.generatedClasses.withFeatureNamePrefix("Another")
            .addForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
    assertThat(generatedClass1.getName().toString()).endsWith("TestComponent__One");
    assertThat(generatedClass2.getName().toString()).endsWith("TestComponent__AnotherOne");
  }

  private static class TestComponent {

  }

}
