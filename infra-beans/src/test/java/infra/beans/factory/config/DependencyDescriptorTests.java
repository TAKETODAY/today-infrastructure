/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.beans.factory.config;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import infra.beans.factory.annotation.NonOrdered;
import infra.core.MethodParameter;

import static infra.core.testfixture.io.SerializationTestUtils.serializeAndDeserialize;
import static org.assertj.core.api.Assertions.assertThat;

class DependencyDescriptorTests {

  @Test
  void exposesFieldAndParameterTypeDescriptors() throws Exception {
    var field = new DependencyDescriptor(InjectionPoints.class.getDeclaredField("names"), true);
    var method = InjectionPoints.class.getDeclaredMethod("configure", List.class);
    var parameter = new DependencyDescriptor(new MethodParameter(method, 0), true);

    assertThat(field.getTypeDescriptor().getType()).isEqualTo(List.class);
    assertThat(field.getTypeDescriptor().getResolvableType().getGeneric().resolve()).isEqualTo(String.class);
    assertThat(field.getTypeDescriptor()).isSameAs(field.getTypeDescriptor());
    assertThat(parameter.getTypeDescriptor().getResolvableType().getGeneric().resolve()).isEqualTo(Integer.class);
  }

  @Test
  void retainsLookupStrategyWhenAllowingFallback() throws Exception {
    var field = InjectionPoints.class.getDeclaredField("names");
    var standard = new DependencyDescriptor(field, true);
    var customized = new DependencyDescriptor(field, true) { };

    assertThat(standard.fallbackMatchAllowed()).isFalse();
    assertThat(standard.forFallbackMatch().fallbackMatchAllowed()).isTrue();
    assertThat(standard.forFallbackMatch().usesStandardBeanLookup()).isTrue();
    assertThat(customized.usesStandardBeanLookup()).isFalse();
    assertThat(customized.forFallbackMatch().usesStandardBeanLookup()).isTrue();
  }

  @Test
  void takesOptionalNullableAndOrderingAnnotationsIntoAccount() throws Exception {
    var names = new DependencyDescriptor(InjectionPoints.class.getDeclaredField("names"), true);
    var optional = new DependencyDescriptor(InjectionPoints.class.getDeclaredField("optional"), true);
    var nullable = new DependencyDescriptor(InjectionPoints.class.getDeclaredField("nullable"), true);

    assertThat(names.isRequired()).isTrue();
    assertThat(names.isOrdered()).isTrue();
    assertThat(optional.isRequired()).isFalse();
    assertThat(nullable.isRequired()).isFalse();
    assertThat(nullable.isOrdered()).isFalse();
    assertThat(new DependencyDescriptor(nullable).isOrdered()).isFalse();
  }

  @Test
  void comparesInjectionPointAndResolutionOptions() throws Exception {
    var field = InjectionPoints.class.getDeclaredField("names");
    var descriptor = new DependencyDescriptor(field, true);
    var copy = new DependencyDescriptor(descriptor);
    assertThat(descriptor.equals(descriptor)).isTrue();
    assertThat(descriptor).isEqualTo(copy).hasSameHashCodeAs(copy).isNotEqualTo("names")
            .isNotEqualTo(new DependencyDescriptor(field, false))
            .isNotEqualTo(new DependencyDescriptor(field, true, false));
    copy.increaseNestingLevel();
    assertThat(descriptor).isNotEqualTo(copy);
    var otherOwner = new DependencyDescriptor(descriptor);
    otherOwner.setContainingClass(Derived.class);
    assertThat(descriptor).isNotEqualTo(otherOwner);
  }

  @Test
  void restoresFieldAndNestedExecutableParametersAfterSerialization() throws Exception {
    var field = InjectionPoints.class.getDeclaredField("names");
    var method = InjectionPoints.class.getDeclaredMethod("configure", List.class);
    var constructor = InjectionPoints.class.getDeclaredConstructor(List.class);
    var fieldDescriptor = new DependencyDescriptor(field, true, false);
    var methodDescriptor = new DependencyDescriptor(new MethodParameter(method, 0), true);
    var constructorDescriptor = new DependencyDescriptor(new MethodParameter(constructor, 0), false);
    methodDescriptor.increaseNestingLevel();
    constructorDescriptor.increaseNestingLevel();

    var restoredField = serializeAndDeserialize(fieldDescriptor);
    assertThat(restoredField.getField()).isEqualTo(field);
    assertThat(restoredField.isEager()).isFalse();
    var restoredMethod = serializeAndDeserialize(methodDescriptor);
    assertThat(restoredMethod.getMethodParameter().getExecutable()).isEqualTo(method);
    assertThat(restoredMethod.getDependencyType()).isEqualTo(Integer.class);
    var restoredConstructor = serializeAndDeserialize(constructorDescriptor);
    assertThat(restoredConstructor.getMethodParameter().getExecutable()).isEqualTo(constructor);
    assertThat(restoredConstructor.getDependencyType()).isEqualTo(Long.class);
    assertThat(restoredConstructor.isRequired()).isFalse();
  }

  static class InjectionPoints {

    List<String> names;

    Optional<String> optional;

    @NonOrdered
    @Nullable String nullable;

    InjectionPoints(List<Long> identifiers) { }

    void configure(List<Integer> counts) { }

  }

  static class Derived extends InjectionPoints {

    Derived() {
      super(List.of());
    }

  }

}
