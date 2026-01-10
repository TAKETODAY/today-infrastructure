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

package infra.aot.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/1 16:56
 */
class MethodReferenceTests {

  @Test
  void shouldCreateMethodReference() {
    MethodReference methodReference = MethodReference.of(String.class, "toString");
    assertThat(methodReference.getClassName()).isEqualTo("java.lang.String");
    assertThat(methodReference.getMethodName()).isEqualTo("toString");
  }

  @Test
  void shouldImplementEqualsAndHashCode() {
    MethodReference methodReference1 = MethodReference.of(String.class, "toString");
    MethodReference methodReference2 = MethodReference.of(String.class, "toString");
    MethodReference differentMethod = MethodReference.of(String.class, "equals");
    MethodReference differentClass = MethodReference.of(Integer.class, "toString");

    assertThat(methodReference1).isEqualTo(methodReference2);
    assertThat(methodReference1).hasSameHashCodeAs(methodReference2);
    assertThat(methodReference1).isNotEqualTo(differentMethod);
    assertThat(methodReference1).isNotEqualTo(differentClass);
  }

  @Test
  void shouldImplementToString() {
    MethodReference methodReference = MethodReference.of(String.class, "toString");
    assertThat(methodReference.toString()).isEqualTo("java.lang.String#toString");
  }

  @Test
  void shouldHandleNullEquals() {
    MethodReference methodReference = MethodReference.of(String.class, "toString");
    assertThat(methodReference).isNotEqualTo(null);
  }

  @Test
  void shouldHandleSameInstanceEquals() {
    MethodReference methodReference = MethodReference.of(String.class, "toString");
    assertThat(methodReference).isEqualTo(methodReference);
  }

}