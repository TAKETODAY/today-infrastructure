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

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClassNameGenerator}.
 *
 * @author Phillip Webb
 */
class ClassNameGeneratorTests {

  private static final ClassName TEST_TARGET = ClassName.get("com.example", "Test");

  private final ClassNameGenerator generator = new ClassNameGenerator(TEST_TARGET);

  @Test
  void generateClassNameWhenTargetClassIsNullUsesMainTarget() {
    ClassName generated = this.generator.generateClassName("test", null);
    assertThat(generated).hasToString("com.example.Test__Test");
  }

  @Test
  void generateClassNameUseFeatureNamePrefix() {
    ClassName generated = new ClassNameGenerator(TEST_TARGET, "One")
            .generateClassName("test", ClassName.get(InputStream.class));
    assertThat(generated).hasToString("java.io.InputStream__OneTest");
  }

  @Test
  void generateClassNameWithNoTextFeatureNamePrefix() {
    ClassName generated = new ClassNameGenerator(TEST_TARGET, "  ")
            .generateClassName("test", ClassName.get(InputStream.class));
    assertThat(generated).hasToString("java.io.InputStream__Test");
  }

  @Test
  void generatedClassNameWhenFeatureIsEmptyThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.generator.generateClassName("", ClassName.get(InputStream.class)))
            .withMessage("'featureName' must not be empty");
  }

  @Test
  void generatedClassNameWhenFeatureIsNotAllLettersThrowsException() {
    assertThat(this.generator.generateClassName("name!", ClassName.get(InputStream.class)))
            .hasToString("java.io.InputStream__Name");
    assertThat(this.generator.generateClassName("1NameHere", ClassName.get(InputStream.class)))
            .hasToString("java.io.InputStream__NameHere");
    assertThat(this.generator.generateClassName("Y0pe", ClassName.get(InputStream.class)))
            .hasToString("java.io.InputStream__YPe");
  }

  @Test
  void generateClassNameWithClassWhenLowercaseFeatureNameGeneratesName() {
    ClassName generated = this.generator.generateClassName("bytes", ClassName.get(InputStream.class));
    assertThat(generated).hasToString("java.io.InputStream__Bytes");
  }

  @Test
  void generateClassNameWithClassWhenInnerClassGeneratesName() {
    ClassName innerBean = ClassName.get("com.example", "Test", "InnerBean");
    ClassName generated = this.generator.generateClassName("EventListener", innerBean);
    assertThat(generated)
            .hasToString("com.example.Test_InnerBean__EventListener");
  }

  @Test
  void generateClassWithClassWhenMultipleCallsGeneratesSequencedName() {
    ClassName generated1 = this.generator.generateClassName("bytes", ClassName.get(InputStream.class));
    ClassName generated2 = this.generator.generateClassName("bytes", ClassName.get(InputStream.class));
    ClassName generated3 = this.generator.generateClassName("bytes", ClassName.get(InputStream.class));
    assertThat(generated1).hasToString("java.io.InputStream__Bytes");
    assertThat(generated2).hasToString("java.io.InputStream__Bytes1");
    assertThat(generated3).hasToString("java.io.InputStream__Bytes2");
  }

}
