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

import java.util.function.Consumer;

import cn.taketoday.aot.generate.GeneratedFiles.Kind;
import cn.taketoday.aot.hint.RuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DefaultGenerationContext}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class DefaultGenerationContextTests {

  private static final ClassName SAMPLE_TARGET = ClassName.get("com.example", "SampleTarget");

  private static final Consumer<TypeSpec.Builder> typeSpecCustomizer = type -> { };

  private final GeneratedClasses generatedClasses = new GeneratedClasses(
          new ClassNameGenerator(SAMPLE_TARGET));

  private final InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();

  private final RuntimeHints runtimeHints = new RuntimeHints();

  @Test
  void createWithOnlyGeneratedFilesCreatesContext() {
    DefaultGenerationContext context = new DefaultGenerationContext(
            new ClassNameGenerator(SAMPLE_TARGET), this.generatedFiles);
    assertThat(context.getGeneratedFiles()).isSameAs(this.generatedFiles);
    assertThat(context.getRuntimeHints()).isInstanceOf(RuntimeHints.class);
  }

  @Test
  void createCreatesContext() {
    DefaultGenerationContext context = new DefaultGenerationContext(
            this.generatedClasses, this.generatedFiles, this.runtimeHints);
    assertThat(context.getGeneratedFiles()).isNotNull();
    assertThat(context.getRuntimeHints()).isNotNull();
  }

  @Test
  void createWhenGeneratedClassesIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new DefaultGenerationContext((GeneratedClasses) null,
                    this.generatedFiles, this.runtimeHints))
            .withMessage("'generatedClasses' must not be null");
  }

  @Test
  void createWhenGeneratedFilesIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new DefaultGenerationContext(this.generatedClasses,
                    null, this.runtimeHints))
            .withMessage("'generatedFiles' must not be null");
  }

  @Test
  void createWhenRuntimeHintsIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new DefaultGenerationContext(this.generatedClasses,
                    this.generatedFiles, null))
            .withMessage("'runtimeHints' must not be null");
  }

  @Test
  void getGeneratedClassesReturnsClassNameGenerator() {
    DefaultGenerationContext context = new DefaultGenerationContext(
            this.generatedClasses, this.generatedFiles, this.runtimeHints);
    assertThat(context.getGeneratedClasses()).isSameAs(this.generatedClasses);
  }

  @Test
  void getGeneratedFilesReturnsGeneratedFiles() {
    DefaultGenerationContext context = new DefaultGenerationContext(
            this.generatedClasses, this.generatedFiles, this.runtimeHints);
    assertThat(context.getGeneratedFiles()).isSameAs(this.generatedFiles);
  }

  @Test
  void getRuntimeHintsReturnsRuntimeHints() {
    DefaultGenerationContext context = new DefaultGenerationContext(
            this.generatedClasses, this.generatedFiles, this.runtimeHints);
    assertThat(context.getRuntimeHints()).isSameAs(this.runtimeHints);
  }

  @Test
  void withNameUpdateNamingConvention() {
    DefaultGenerationContext context = new DefaultGenerationContext(
            new ClassNameGenerator(SAMPLE_TARGET), this.generatedFiles);
    GenerationContext anotherContext = context.withName("Another");
    GeneratedClass generatedClass = anotherContext.getGeneratedClasses()
            .addForFeature("Test", typeSpecCustomizer);
    assertThat(generatedClass.getName().simpleName()).endsWith("__AnotherTest");
  }

  @Test
  void withNameKeepsTrackOfAllGeneratedFiles() {
    DefaultGenerationContext context = new DefaultGenerationContext(
            new ClassNameGenerator(SAMPLE_TARGET), this.generatedFiles);
    context.getGeneratedClasses().addForFeature("Test", typeSpecCustomizer);
    GenerationContext anotherContext = context.withName("Another");
    assertThat(anotherContext.getGeneratedClasses()).isNotSameAs(context.getGeneratedClasses());
    assertThat(anotherContext.getGeneratedFiles()).isSameAs(context.getGeneratedFiles());
    assertThat(anotherContext.getRuntimeHints()).isSameAs(context.getRuntimeHints());
    anotherContext.getGeneratedClasses().addForFeature("Test", typeSpecCustomizer);
    context.writeGeneratedContent();
    assertThat(this.generatedFiles.getGeneratedFiles(Kind.SOURCE)).hasSize(2);
  }

  @Test
  void withNameGeneratesUniqueName() {
    DefaultGenerationContext context = new DefaultGenerationContext(
            new ClassNameGenerator(SAMPLE_TARGET), this.generatedFiles);
    context.withName("Test").getGeneratedClasses()
            .addForFeature("Feature", typeSpecCustomizer);
    context.withName("Test").getGeneratedClasses()
            .addForFeature("Feature", typeSpecCustomizer);
    context.withName("Test").getGeneratedClasses()
            .addForFeature("Feature", typeSpecCustomizer);
    context.writeGeneratedContent();
    assertThat(this.generatedFiles.getGeneratedFiles(Kind.SOURCE)).containsOnlyKeys(
            "com/example/SampleTarget__TestFeature.java",
            "com/example/SampleTarget__Test1Feature.java",
            "com/example/SampleTarget__Test2Feature.java");
  }

}
