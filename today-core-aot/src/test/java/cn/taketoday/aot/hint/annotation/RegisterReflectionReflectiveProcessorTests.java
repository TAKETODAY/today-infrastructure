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

package cn.taketoday.aot.hint.annotation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.aot.hint.ExecutableHint;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeHint;
import cn.taketoday.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/7 12:08
 */
class RegisterReflectionReflectiveProcessorTests {

  private static final List<String> NO_METHODS = Collections.emptyList();

  private final RegisterReflectionReflectiveProcessor processor = new RegisterReflectionReflectiveProcessor();

  private final RuntimeHints hints = new RuntimeHints();

  @Nested
  class AnnotatedTypeTests {

    @Test
    void registerReflectionWithMemberCategory() {
      registerReflectionHints(RegistrationSimple.class);
      assertBasicTypeHint(SimplePojo.class, NO_METHODS, List.of(MemberCategory.INVOKE_PUBLIC_METHODS));
    }

    @Test
    void registerReflectionForMultipleTargets() {
      registerReflectionHints(RegistrationMultipleTargets.class);
      assertThat(hints.reflection().typeHints()).allSatisfy(
              hasOnlyMemberCategories(MemberCategory.INVOKE_PUBLIC_METHODS));
      assertThat(hints.reflection().typeHints().map(TypeHint::getType))
              .hasSameElementsAs(TypeReference.listOf(Number.class, Double.class, Integer.class, Float.class));
    }

    @Test
    void registerReflectionOnTargetClass() {
      registerReflectionHints(AnnotatedSimplePojo.class);
      assertBasicTypeHint(AnnotatedSimplePojo.class, NO_METHODS,
              List.of(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
    }
  }

  @Nested
  class AnnotatedMethodTests {

    @Test
    void registerReflectionForStaticField() throws NoSuchMethodException {
      Method method = RegistrationMethod.class.getDeclaredMethod("doReflection");
      registerReflectionHints(method);
      assertBasicTypeHint(SimplePojo.class, NO_METHODS, List.of(MemberCategory.INVOKE_DECLARED_METHODS));
    }

    @Test
    void registerReflectionWithoutTarget() throws NoSuchMethodException {
      Method method = RegistrationMethodWithoutTarget.class.getDeclaredMethod("doReflection");
      assertThatIllegalStateException()
              .isThrownBy(() -> registerReflectionHints(method))
              .withMessageContaining("At least one class must be specified, could not detect target from '")
              .withMessageContaining(method.toString());
    }
  }

  private void assertBasicTypeHint(Class<?> type, List<String> methodNames, List<MemberCategory> memberCategories) {
    TypeHint typeHint = getTypeHint(type);
    assertThat(typeHint.methods()).map(ExecutableHint::getName).hasSameElementsAs(methodNames);
    assertThat(typeHint.getMemberCategories()).hasSameElementsAs(memberCategories);
    assertThat(typeHint.fields()).isEmpty();
    assertThat(typeHint.constructors()).isEmpty();
  }

  private Consumer<TypeHint> hasOnlyMemberCategories(MemberCategory... categories) {
    return typeHint -> {
      assertThat(typeHint.fields()).isEmpty();
      assertThat(typeHint.methods()).isEmpty();
      assertThat(typeHint.constructors()).isEmpty();
      assertThat(typeHint.getMemberCategories()).containsOnly(categories);
    };
  }

  private TypeHint getTypeHint(Class<?> target) {
    TypeHint typeHint = hints.reflection().getTypeHint(target);
    assertThat(typeHint).isNotNull();
    return typeHint;
  }

  private void registerReflectionHints(AnnotatedElement annotatedElement) {
    this.processor.registerReflectionHints(this.hints.reflection(), annotatedElement);
  }

  @RegisterReflection(classes = SimplePojo.class, memberCategories = MemberCategory.INVOKE_PUBLIC_METHODS)
  static class RegistrationSimple { }

  @RegisterReflection(classes = { Number.class, Double.class },
          classNames = { "java.lang.Integer", "java.lang.Float" }, memberCategories = MemberCategory.INVOKE_PUBLIC_METHODS)
  static class RegistrationMultipleTargets {

  }

  static class RegistrationMethod {

    @RegisterReflection(classes = SimplePojo.class, memberCategories = MemberCategory.INVOKE_DECLARED_METHODS)
    private void doReflection() {

    }

  }

  static class RegistrationMethodWithoutTarget {

    @RegisterReflection(memberCategories = MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
    private void doReflection() {

    }

  }

  static class SimplePojo {

    private String name;

    private String description;

    public String getName() {
      return this.name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return this.description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  @RegisterReflection(memberCategories = MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
  static class AnnotatedSimplePojo {

    private String test;

    AnnotatedSimplePojo(String test) {
      this.test = test;
    }

  }

}