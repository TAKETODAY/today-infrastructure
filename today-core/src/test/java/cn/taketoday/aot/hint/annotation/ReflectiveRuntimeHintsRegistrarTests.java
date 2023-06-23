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

package cn.taketoday.aot.hint.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeHint;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.core.annotation.AliasFor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link ReflectiveRuntimeHintsRegistrar}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class ReflectiveRuntimeHintsRegistrarTests {

  private final ReflectiveRuntimeHintsRegistrar registrar = new ReflectiveRuntimeHintsRegistrar();

  private final RuntimeHints runtimeHints = new RuntimeHints();

  @Test
  void shouldIgnoreNonAnnotatedType() {
    RuntimeHints mock = mock();
    this.registrar.registerRuntimeHints(mock, String.class);
    verifyNoInteractions(mock);
  }

  @Test
  void shouldProcessAnnotationOnType() {
    process(SampleTypeAnnotatedBean.class);
    assertThat(this.runtimeHints.reflection().getTypeHint(SampleTypeAnnotatedBean.class))
            .isNotNull();
  }

  @Test
  void shouldProcessWithMultipleProcessorsWithAnnotationOnType() {
    process(SampleMultipleCustomProcessors.class);
    TypeHint typeHint = this.runtimeHints.reflection().getTypeHint(SampleMultipleCustomProcessors.class);
    assertThat(typeHint).isNotNull();
    assertThat(typeHint.getMemberCategories()).containsExactly(MemberCategory.INVOKE_DECLARED_METHODS);
  }

  @Test
  void shouldProcessAnnotationOnConstructor() {
    process(SampleConstructorAnnotatedBean.class);
    assertThat(this.runtimeHints.reflection().getTypeHint(SampleConstructorAnnotatedBean.class))
            .satisfies(typeHint -> assertThat(typeHint.constructors()).singleElement()
                    .satisfies(constructorHint -> assertThat(constructorHint.getParameterTypes())
                            .containsExactly(TypeReference.of(String.class))));
  }

  @Test
  void shouldProcessAnnotationOnField() {
    process(SampleFieldAnnotatedBean.class);
    assertThat(this.runtimeHints.reflection().getTypeHint(SampleFieldAnnotatedBean.class))
            .satisfies(typeHint -> assertThat(typeHint.fields()).singleElement()
                    .satisfies(fieldHint -> assertThat(fieldHint.getName()).isEqualTo("managed")));
  }

  @Test
  void shouldProcessAnnotationOnMethod() {
    process(SampleMethodAnnotatedBean.class);
    assertThat(this.runtimeHints.reflection().getTypeHint(SampleMethodAnnotatedBean.class))
            .satisfies(typeHint -> assertThat(typeHint.methods()).singleElement()
                    .satisfies(methodHint -> assertThat(methodHint.getName()).isEqualTo("managed")));
  }

  @Test
  void shouldProcessAnnotationOnInterface() {
    process(SampleMethodAnnotatedBeanWithInterface.class);
    assertThat(this.runtimeHints.reflection().getTypeHint(SampleInterface.class))
            .satisfies(typeHint -> assertThat(typeHint.methods()).singleElement()
                    .satisfies(methodHint -> assertThat(methodHint.getName()).isEqualTo("managed")));
    assertThat(this.runtimeHints.reflection().getTypeHint(SampleMethodAnnotatedBeanWithInterface.class))
            .satisfies(typeHint -> assertThat(typeHint.methods()).singleElement()
                    .satisfies(methodHint -> assertThat(methodHint.getName()).isEqualTo("managed")));
  }

  @Test
  void shouldProcessAnnotationOnInheritedClass() {
    process(SampleMethodAnnotatedBeanWithInheritance.class);
    assertThat(this.runtimeHints.reflection().getTypeHint(SampleInheritedClass.class))
            .satisfies(typeHint -> assertThat(typeHint.methods()).singleElement()
                    .satisfies(methodHint -> assertThat(methodHint.getName()).isEqualTo("managed")));
    assertThat(this.runtimeHints.reflection().getTypeHint(SampleMethodAnnotatedBeanWithInheritance.class))
            .satisfies(typeHint -> assertThat(typeHint.methods()).singleElement()
                    .satisfies(methodHint -> assertThat(methodHint.getName()).isEqualTo("managed")));
  }

  @Test
  void shouldInvokeCustomProcessor() {
    process(SampleCustomProcessor.class);
    assertThat(RuntimeHintsPredicates.reflection()
            .onMethod(SampleCustomProcessor.class, "managed")).accepts(this.runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onType(String.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(this.runtimeHints);

  }

  private void process(Class<?> beanClass) {
    this.registrar.registerRuntimeHints(this.runtimeHints, beanClass);
  }

  @Reflective
  @SuppressWarnings("unused")
  static class SampleTypeAnnotatedBean {

    private String notManaged;

    public void notManaged() {
    }
  }

  @SuppressWarnings("unused")
  static class SampleConstructorAnnotatedBean {

    @Reflective
    SampleConstructorAnnotatedBean(String name) {
    }

    SampleConstructorAnnotatedBean(Integer nameAsNumber) {
    }
  }

  @SuppressWarnings("unused")
  static class SampleFieldAnnotatedBean {

    @Reflective
    String managed;

    String notManaged;

  }

  @SuppressWarnings("unused")
  static class SampleMethodAnnotatedBean {

    @Reflective
    void managed() {
    }

    void notManaged() {
    }
  }

  @SuppressWarnings("unused")
  static class SampleMethodMetaAnnotatedBean {

    @SampleInvoker
    void invoke() {
    }

    void notManaged() {
    }
  }

  @SuppressWarnings("unused")
  static class SampleMethodMetaAnnotatedBeanWithAlias {

    @RetryInvoker
    void invoke() {
    }

    void notManaged() {
    }
  }

  static class SampleMethodAnnotatedBeanWithInterface implements SampleInterface {

    @Override
    public void managed() {
    }

    public void notManaged() {
    }
  }

  static class SampleMethodAnnotatedBeanWithInheritance extends SampleInheritedClass {

    @Override
    public void managed() {
    }

    public void notManaged() {
    }
  }

  @Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @Reflective
  @interface SampleInvoker {

    int retries() default 0;

  }

  @Target({ ElementType.METHOD })
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @SampleInvoker
  @interface RetryInvoker {

    @AliasFor(attribute = "retries", annotation = SampleInvoker.class)
    int value() default 1;

  }

  @Target({ ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @Reflective(TestTypeHintReflectiveProcessor.class)
  @interface ReflectiveWithCustomProcessor {
  }

  interface SampleInterface {

    @Reflective
    void managed();
  }

  static class SampleInheritedClass {

    @Reflective
    void managed() {
    }
  }

  static class SampleCustomProcessor {

    @Reflective(TestMethodHintReflectiveProcessor.class)
    public String managed() {
      return "test";
    }
  }

  @Reflective
  @ReflectiveWithCustomProcessor
  static class SampleMultipleCustomProcessors {

    public String managed() {
      return "test";
    }
  }

  private static class TestMethodHintReflectiveProcessor extends SimpleReflectiveProcessor {

    @Override
    protected void registerMethodHint(ReflectionHints hints, Method method) {
      super.registerMethodHint(hints, method);
      hints.registerType(method.getReturnType(), MemberCategory.INVOKE_DECLARED_METHODS);
    }
  }

  private static class TestTypeHintReflectiveProcessor extends SimpleReflectiveProcessor {

    @Override
    protected void registerTypeHint(ReflectionHints hints, Class<?> type) {
      super.registerTypeHint(hints, type);
      hints.registerType(type, MemberCategory.INVOKE_DECLARED_METHODS);
    }
  }

}
