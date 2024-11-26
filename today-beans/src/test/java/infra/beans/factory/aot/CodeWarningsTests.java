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

package infra.beans.factory.aot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import infra.aot.test.generate.TestGenerationContext;
import infra.beans.testfixture.beans.GenericBean;
import infra.beans.testfixture.beans.factory.aot.DeferredTypeBuilder;
import infra.beans.testfixture.beans.factory.generator.deprecation.DeprecatedBean;
import infra.beans.testfixture.beans.factory.generator.deprecation.DeprecatedForRemovalBean;
import infra.core.ResolvableType;
import infra.core.test.tools.Compiled;
import infra.core.test.tools.TestCompiler;
import infra.javapoet.FieldSpec;
import infra.javapoet.MethodSpec;
import infra.javapoet.MethodSpec.Builder;
import infra.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/15 11:16
 */
class CodeWarningsTests {

  private static final TestCompiler TEST_COMPILER = TestCompiler.forSystem()
          .withCompilerOptions("-Xlint:all", "-Werror");

  private final CodeWarnings codeWarnings = new CodeWarnings();

  private final TestGenerationContext generationContext = new TestGenerationContext();

  @Test
  void registerNoWarningDoesNotIncludeAnnotationOnMethod() {
    compileWithMethod(method -> {
      this.codeWarnings.suppress(method);
      method.addStatement("$T bean = $S", String.class, "Hello");
    }, compiled -> assertThat(compiled.getSourceFile()).doesNotContain("@SuppressWarnings"));
  }

  @Test
  void registerNoWarningDoesNotIncludeAnnotationOnType() {
    compile(type -> {
      this.codeWarnings.suppress(type);
      type.addField(FieldSpec.builder(String.class, "type").build());
    }, compiled -> assertThat(compiled.getSourceFile()).doesNotContain("@SuppressWarnings"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void registerWarningSuppressesItOnMethod() {
    this.codeWarnings.register("deprecation");
    compileWithMethod(method -> {
      this.codeWarnings.suppress(method);
      method.addStatement("$T bean = new $T()", DeprecatedBean.class, DeprecatedBean.class);
    }, compiled -> assertThat(compiled.getSourceFile()).contains("@SuppressWarnings(\"deprecation\")"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void registerWarningSuppressesItOnType() {
    this.codeWarnings.register("deprecation");
    compile(type -> {
      this.codeWarnings.suppress(type);
      type.addField(FieldSpec.builder(DeprecatedBean.class, "bean").build());
    }, compiled -> assertThat(compiled.getSourceFile())
            .contains("@SuppressWarnings(\"deprecation\")"));
  }

  @Test
  @SuppressWarnings({ "deprecation", "removal" })
  void registerSeveralWarningsSuppressesThemOnMethod() {
    this.codeWarnings.register("deprecation");
    this.codeWarnings.register("removal");
    compileWithMethod(method -> {
      this.codeWarnings.suppress(method);
      method.addStatement("$T bean = new $T()", DeprecatedBean.class, DeprecatedBean.class);
      method.addStatement("$T another = new $T()", DeprecatedForRemovalBean.class, DeprecatedForRemovalBean.class);
    }, compiled -> assertThat(compiled.getSourceFile()).contains("@SuppressWarnings({ \"deprecation\", \"removal\" })"));
  }

  @Test
  @SuppressWarnings({ "deprecation", "removal" })
  void registerSeveralWarningsSuppressesThemOnType() {
    this.codeWarnings.register("deprecation");
    this.codeWarnings.register("removal");
    compile(type -> {
      this.codeWarnings.suppress(type);
      type.addField(FieldSpec.builder(DeprecatedBean.class, "bean").build());
      type.addField(FieldSpec.builder(DeprecatedForRemovalBean.class, "another").build());
    }, compiled -> assertThat(compiled.getSourceFile())
            .contains("@SuppressWarnings({ \"deprecation\", \"removal\" })"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void detectDeprecationOnAnnotatedElementWithDeprecated() {
    this.codeWarnings.detectDeprecation(DeprecatedBean.class);
    assertThat(this.codeWarnings.getWarnings()).containsOnly("deprecation");
  }

  @Test
  @SuppressWarnings("deprecation")
  void detectDeprecationOnAnnotatedElementWhoseEnclosingElementIsDeprecated() {
    this.codeWarnings.detectDeprecation(DeprecatedBean.Nested.class);
    assertThat(this.codeWarnings.getWarnings()).containsExactly("deprecation");
  }

  @Test
  @SuppressWarnings("removal")
  void detectDeprecationOnAnnotatedElementWithDeprecatedForRemoval() {
    this.codeWarnings.detectDeprecation(DeprecatedForRemovalBean.class);
    assertThat(this.codeWarnings.getWarnings()).containsOnly("removal");
  }

  @Test
  @SuppressWarnings("removal")
  void detectDeprecationOnAnnotatedElementWhoseEnclosingElementIsDeprecatedForRemoval() {
    this.codeWarnings.detectDeprecation(DeprecatedForRemovalBean.Nested.class);
    assertThat(this.codeWarnings.getWarnings()).containsExactly("removal");
  }

  @ParameterizedTest
  @MethodSource("resolvableTypesWithDeprecated")
  void detectDeprecationOnResolvableTypeWithDeprecated(ResolvableType resolvableType) {
    this.codeWarnings.detectDeprecation(resolvableType);
    assertThat(this.codeWarnings.getWarnings()).containsExactly("deprecation");
  }

  @SuppressWarnings("deprecation")
  static Stream<Arguments> resolvableTypesWithDeprecated() {
    Class<?> deprecatedBean = DeprecatedBean.class;
    Class<?> nested = DeprecatedBean.Nested.class;
    return Stream.of(
            Arguments.of(ResolvableType.forClass(deprecatedBean)),
            Arguments.of(ResolvableType.forClass(nested)),
            Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class, deprecatedBean)),
            Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class, nested)),
            Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class,
                    ResolvableType.forClassWithGenerics(GenericBean.class, deprecatedBean))),
            Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class,
                    ResolvableType.forClassWithGenerics(GenericBean.class, nested)))
    );
  }

  @ParameterizedTest
  @MethodSource("resolvableTypesWithDeprecatedForRemoval")
  void detectDeprecationOnResolvableTypeWithDeprecatedForRemoval(ResolvableType resolvableType) {
    this.codeWarnings.detectDeprecation(resolvableType);
    assertThat(this.codeWarnings.getWarnings()).containsExactly("removal");
  }

  @SuppressWarnings("removal")
  static Stream<Arguments> resolvableTypesWithDeprecatedForRemoval() {
    Class<?> deprecatedBean = DeprecatedForRemovalBean.class;
    Class<?> nested = DeprecatedForRemovalBean.Nested.class;
    return Stream.of(
            Arguments.of(ResolvableType.forClass(deprecatedBean)),
            Arguments.of(ResolvableType.forClass(nested)),
            Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class, deprecatedBean)),
            Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class, nested)),
            Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class,
                    ResolvableType.forClassWithGenerics(GenericBean.class, deprecatedBean))),
            Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class,
                    ResolvableType.forClassWithGenerics(GenericBean.class, nested)))
    );
  }

  @Test
  void toStringIncludesWarnings() {
    this.codeWarnings.register("deprecation");
    this.codeWarnings.register("rawtypes");
    assertThat(this.codeWarnings).hasToString("CodeWarnings[deprecation, rawtypes]");
  }

  private void compileWithMethod(Consumer<Builder> method, Consumer<Compiled> result) {
    compile(type -> {
      type.addModifiers(Modifier.PUBLIC);
      Builder methodBuilder = MethodSpec.methodBuilder("apply")
              .addModifiers(Modifier.PUBLIC);
      method.accept(methodBuilder);
      type.addMethod(methodBuilder.build());
    }, result);
  }

  private void compile(Consumer<TypeSpec.Builder> type, Consumer<Compiled> result) {
    DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();
    this.generationContext.getGeneratedClasses().addForFeature("TestCode", typeBuilder);
    typeBuilder.set(type);
    this.generationContext.writeGeneratedContent();
    TEST_COMPILER.with(this.generationContext).compile(result);
  }

}