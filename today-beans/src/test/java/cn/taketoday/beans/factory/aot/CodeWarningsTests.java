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

package cn.taketoday.beans.factory.aot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.testfixture.beans.GenericBean;
import cn.taketoday.beans.testfixture.beans.factory.aot.DeferredTypeBuilder;
import cn.taketoday.beans.testfixture.beans.factory.generator.deprecation.DeprecatedBean;
import cn.taketoday.beans.testfixture.beans.factory.generator.deprecation.DeprecatedForRemovalBean;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.test.tools.Compiled;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.javapoet.MethodSpec;
import cn.taketoday.javapoet.MethodSpec.Builder;

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
  void registerNoWarningDoesNotIncludeAnnotation() {
    compile(method -> {
      this.codeWarnings.suppress(method);
      method.addStatement("$T bean = $S", String.class, "Hello");
    }, compiled -> assertThat(compiled.getSourceFile()).doesNotContain("@SuppressWarnings"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void registerWarningSuppressesIt() {
    this.codeWarnings.register("deprecation");
    compile(method -> {
      this.codeWarnings.suppress(method);
      method.addStatement("$T bean = new $T()", DeprecatedBean.class, DeprecatedBean.class);
    }, compiled -> assertThat(compiled.getSourceFile()).contains("@SuppressWarnings(\"deprecation\")"));
  }

  @Test
  @SuppressWarnings({ "deprecation", "removal" })
  void registerSeveralWarningsSuppressesThem() {
    this.codeWarnings.register("deprecation");
    this.codeWarnings.register("removal");
    compile(method -> {
      this.codeWarnings.suppress(method);
      method.addStatement("$T bean = new $T()", DeprecatedBean.class, DeprecatedBean.class);
      method.addStatement("$T another = new $T()", DeprecatedForRemovalBean.class, DeprecatedForRemovalBean.class);
    }, compiled -> assertThat(compiled.getSourceFile()).contains("@SuppressWarnings({ \"deprecation\", \"removal\" })"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void detectDeprecationOnAnnotatedElementWithDeprecated() {
    this.codeWarnings.detectDeprecation(DeprecatedBean.class);
    assertThat(this.codeWarnings.getWarnings()).containsOnly("deprecation");
  }

  @Test
  @SuppressWarnings("removal")
  void detectDeprecationOnAnnotatedElementWithDeprecatedForRemoval() {
    this.codeWarnings.detectDeprecation(DeprecatedForRemovalBean.class);
    assertThat(this.codeWarnings.getWarnings()).containsOnly("removal");
  }

  @ParameterizedTest
  @MethodSource("resolvableTypesWithDeprecated")
  void detectDeprecationOnResolvableTypeWithDeprecated(ResolvableType resolvableType) {
    this.codeWarnings.detectDeprecation(resolvableType);
    assertThat(this.codeWarnings.getWarnings()).containsExactly("deprecation");
  }

  @SuppressWarnings("deprecation")
  static Stream<Arguments> resolvableTypesWithDeprecated() {
    return Stream.of(
            Arguments.of(ResolvableType.forClass(DeprecatedBean.class)),
            Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class, DeprecatedBean.class)),
            Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class,
                    ResolvableType.forClassWithGenerics(GenericBean.class, DeprecatedBean.class)))
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
    return Stream.of(
            Arguments.of(ResolvableType.forClass(DeprecatedForRemovalBean.class)),
            Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class, DeprecatedForRemovalBean.class)),
            Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class,
                    ResolvableType.forClassWithGenerics(GenericBean.class, DeprecatedForRemovalBean.class)))
    );
  }

  @Test
  void toStringIncludesWarnings() {
    this.codeWarnings.register("deprecation");
    this.codeWarnings.register("rawtypes");
    assertThat(this.codeWarnings).hasToString("CodeWarnings[deprecation, rawtypes]");
  }

  private void compile(Consumer<Builder> method, Consumer<Compiled> result) {
    DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();
    this.generationContext.getGeneratedClasses().addForFeature("TestCode", typeBuilder);
    typeBuilder.set(type -> {
      type.addModifiers(Modifier.PUBLIC);
      Builder methodBuilder = MethodSpec.methodBuilder("apply")
              .addModifiers(Modifier.PUBLIC);
      method.accept(methodBuilder);
      type.addMethod(methodBuilder.build());
    });
    this.generationContext.writeGeneratedContent();
    TEST_COMPILER.with(this.generationContext).compile(result);
  }

}