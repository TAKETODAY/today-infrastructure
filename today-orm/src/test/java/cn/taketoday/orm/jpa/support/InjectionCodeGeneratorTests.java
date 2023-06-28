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

package cn.taketoday.orm.jpa.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.beans.testfixture.beans.TestBeanWithPackagePrivateField;
import cn.taketoday.beans.testfixture.beans.TestBeanWithPackagePrivateMethod;
import cn.taketoday.beans.testfixture.beans.TestBeanWithPrivateMethod;
import cn.taketoday.beans.testfixture.beans.TestBeanWithPublicField;
import cn.taketoday.core.test.tools.CompileWithForkedClassLoader;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.core.test.tools.Compiled;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.JavaFile;
import cn.taketoday.javapoet.MethodSpec;
import cn.taketoday.javapoet.ParameterizedTypeName;
import cn.taketoday.javapoet.TypeSpec;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/28 21:54
 */
class InjectionCodeGeneratorTests {

  private static final String INSTANCE_VARIABLE = "instance";

  private static final ClassName TEST_TARGET = ClassName.get("com.example", "Test");

  private final RuntimeHints hints = new RuntimeHints();

  @Test
  void generateCodeWhenPublicFieldInjectsValue() {
    TestBeanWithPublicField bean = new TestBeanWithPublicField();
    Field field = ReflectionUtils.findField(bean.getClass(), "age");
    ClassName targetClassName = TEST_TARGET;
    CodeBlock generatedCode = createGenerator(targetClassName).generateInjectionCode(
            field, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
    testCompiledResult(targetClassName, generatedCode, TestBeanWithPublicField.class, (actual, compiled) -> {
      TestBeanWithPublicField instance = new TestBeanWithPublicField();
      actual.accept(instance);
      assertThat(instance).extracting("age").isEqualTo(123);
      assertThat(compiled.getSourceFile()).contains("instance.age = 123");
    });
  }

  @Test
  @CompileWithForkedClassLoader
  void generateCodeWhenPackagePrivateFieldInTargetPackageInjectsValue() {
    TestBeanWithPackagePrivateField bean = new TestBeanWithPackagePrivateField();
    Field field = ReflectionUtils.findField(bean.getClass(), "age");
    ClassName targetClassName = ClassName.get(TestBeanWithPackagePrivateField.class.getPackageName(), "Test");
    CodeBlock generatedCode = createGenerator(targetClassName).generateInjectionCode(
            field, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
    testCompiledResult(targetClassName, generatedCode, TestBeanWithPackagePrivateField.class, (actual, compiled) -> {
      TestBeanWithPackagePrivateField instance = new TestBeanWithPackagePrivateField();
      actual.accept(instance);
      assertThat(instance).extracting("age").isEqualTo(123);
      assertThat(compiled.getSourceFile()).contains("instance.age = 123");
    });
  }

  @Test
  void generateCodeWhenPackagePrivateFieldInAnotherPackageUsesReflection() {
    TestBeanWithPackagePrivateField bean = new TestBeanWithPackagePrivateField();
    Field field = ReflectionUtils.findField(bean.getClass(), "age");
    ClassName targetClassName = TEST_TARGET;
    CodeBlock generatedCode = createGenerator(targetClassName).generateInjectionCode(
            field, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
    testCompiledResult(targetClassName, generatedCode, TestBeanWithPackagePrivateField.class, (actual, compiled) -> {
      TestBeanWithPackagePrivateField instance = new TestBeanWithPackagePrivateField();
      actual.accept(instance);
      assertThat(instance).extracting("age").isEqualTo(123);
      assertThat(compiled.getSourceFile()).contains("setField(");
    });
  }

  @Test
  void generateCodeWhenPrivateFieldInjectsValueUsingReflection() {
    TestBean bean = new TestBean();
    Field field = ReflectionUtils.findField(bean.getClass(), "age");
    ClassName targetClassName = ClassName.get(TestBean.class);
    CodeBlock generatedCode = createGenerator(targetClassName).generateInjectionCode(
            field, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
    testCompiledResult(targetClassName, generatedCode, TestBean.class, (actual, compiled) -> {
      TestBean instance = new TestBean();
      actual.accept(instance);
      assertThat(instance).extracting("age").isEqualTo(123);
      assertThat(compiled.getSourceFile()).contains("setField(");
    });
  }

  @Test
  void generateCodeWhenPrivateFieldAddsHint() {
    TestBean bean = new TestBean();
    Field field = ReflectionUtils.findField(bean.getClass(), "age");
    createGenerator(TEST_TARGET).generateInjectionCode(
            field, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
    assertThat(RuntimeHintsPredicates.reflection().onField(TestBean.class, "age"))
            .accepts(this.hints);
  }

  @Test
  void generateCodeWhenPublicMethodInjectsValue() {
    TestBean bean = new TestBean();
    Method method = ReflectionUtils.findMethod(bean.getClass(), "setAge", int.class);
    ClassName targetClassName = TEST_TARGET;
    CodeBlock generatedCode = createGenerator(targetClassName).generateInjectionCode(
            method, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
    testCompiledResult(targetClassName, generatedCode, TestBean.class, (actual, compiled) -> {
      TestBean instance = new TestBean();
      actual.accept(instance);
      assertThat(instance).extracting("age").isEqualTo(123);
      assertThat(compiled.getSourceFile()).contains("instance.setAge(");
    });
  }

  @Test
  @CompileWithForkedClassLoader
  void generateCodeWhenPackagePrivateMethodInTargetPackageInjectsValue() {
    TestBeanWithPackagePrivateMethod bean = new TestBeanWithPackagePrivateMethod();
    Method method = ReflectionUtils.findMethod(bean.getClass(), "setAge", int.class);
    ClassName targetClassName = ClassName.get(TestBeanWithPackagePrivateMethod.class);
    CodeBlock generatedCode = createGenerator(targetClassName).generateInjectionCode(
            method, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
    testCompiledResult(targetClassName, generatedCode, TestBeanWithPackagePrivateMethod.class, (actual, compiled) -> {
      TestBeanWithPackagePrivateMethod instance = new TestBeanWithPackagePrivateMethod();
      actual.accept(instance);
      assertThat(instance).extracting("age").isEqualTo(123);
      assertThat(compiled.getSourceFile()).contains("instance.setAge(");
    });
  }

  @Test
  void generateCodeWhenPackagePrivateMethodInAnotherPackageUsesReflection() {
    TestBeanWithPackagePrivateMethod bean = new TestBeanWithPackagePrivateMethod();
    Method method = ReflectionUtils.findMethod(bean.getClass(), "setAge", int.class);
    ClassName targetClassName = TEST_TARGET;
    CodeBlock generatedCode = createGenerator(targetClassName).generateInjectionCode(
            method, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
    testCompiledResult(targetClassName, generatedCode, TestBeanWithPackagePrivateMethod.class, (actual, compiled) -> {
      TestBeanWithPackagePrivateMethod instance = new TestBeanWithPackagePrivateMethod();
      actual.accept(instance);
      assertThat(instance).extracting("age").isEqualTo(123);
      assertThat(compiled.getSourceFile()).contains("invokeMethod(");
    });
  }

  @Test
  void generateCodeWhenPrivateMethodInjectsValueUsingReflection() {
    TestBeanWithPrivateMethod bean = new TestBeanWithPrivateMethod();
    Method method = ReflectionUtils.findMethod(bean.getClass(), "setAge", int.class);
    ClassName targetClassName = ClassName.get(TestBeanWithPrivateMethod.class);
    CodeBlock generatedCode = createGenerator(targetClassName)
            .generateInjectionCode(method, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
    testCompiledResult(targetClassName, generatedCode, TestBeanWithPrivateMethod.class, (actual, compiled) -> {
      TestBeanWithPrivateMethod instance = new TestBeanWithPrivateMethod();
      actual.accept(instance);
      assertThat(instance).extracting("age").isEqualTo(123);
      assertThat(compiled.getSourceFile()).contains("invokeMethod(");
    });
  }

  @Test
  void generateCodeWhenPrivateMethodAddsHint() {
    TestBeanWithPrivateMethod bean = new TestBeanWithPrivateMethod();
    Method method = ReflectionUtils.findMethod(bean.getClass(), "setAge", int.class);
    createGenerator(TEST_TARGET).generateInjectionCode(
            method, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
    assertThat(RuntimeHintsPredicates.reflection()
            .onMethod(TestBeanWithPrivateMethod.class, "setAge").invoke()).accepts(this.hints);
  }

  private InjectionCodeGenerator createGenerator(ClassName target) {
    return new InjectionCodeGenerator(target, this.hints);
  }

  @SuppressWarnings("unchecked")
  private <T> void testCompiledResult(ClassName generatedClasName, CodeBlock generatedCode, Class<T> target,
          BiConsumer<Consumer<T>, Compiled> result) {
    JavaFile javaFile = createJavaFile(generatedClasName, generatedCode, target);
    TestCompiler.forSystem().compile(javaFile::writeTo,
            compiled -> result.accept(compiled.getInstance(Consumer.class), compiled));
  }

  private JavaFile createJavaFile(ClassName generatedClasName, CodeBlock generatedCode, Class<?> target) {
    TypeSpec.Builder builder = TypeSpec.classBuilder(generatedClasName.simpleName() + "__Injector");
    builder.addModifiers(Modifier.PUBLIC);
    builder.addSuperinterface(ParameterizedTypeName.get(Consumer.class, target));
    builder.addMethod(MethodSpec.methodBuilder("accept").addModifiers(Modifier.PUBLIC)
            .addParameter(target, INSTANCE_VARIABLE).addCode(generatedCode).build());
    return JavaFile.builder(generatedClasName.packageName(), builder.build()).build();
  }

}