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

package infra.test.context.bean.override.convention;

import org.junit.jupiter.api.Test;

import infra.aot.hint.ReflectionHints;
import infra.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TestBeanReflectiveProcessorTests {

  @Test
  void registersExternalFactoryMethodAndIgnoresLocalOrUnannotatedFields() throws Exception {
    ReflectionHints hints = new ReflectionHints();
    var processor = new TestBeanReflectiveProcessor();
    processor.registerReflectionHints(hints, Fields.class.getDeclaredField("external"));
    var type = hints.getTypeHint(TypeReference.of("example.TestFactories"));
    assertThat(type).isNotNull();
    assertThat(type.methods()).anySatisfy(method -> {
      assertThat(method.getName()).isEqualTo("create");
      assertThat(method.getParameterTypes()).isEmpty();
      assertThat(method.getMode()).isEqualTo(infra.aot.hint.ExecutableMode.INVOKE);
    });
    processor.registerReflectionHints(hints, Fields.class.getDeclaredField("local"));
    processor.registerReflectionHints(hints, Fields.class.getDeclaredField("plain"));
    assertThat(hints.typeHints()).hasSize(1);
  }

  @Test
  void validatesBothPartsOfExternalFactoryName() {
    var processor = new TestBeanReflectiveProcessor();
    assertThatIllegalArgumentException().isThrownBy(() -> processor.registerReflectionHints(
            new ReflectionHints(), Fields.class.getDeclaredField("missingClass")))
            .withMessageContaining("No class name");
    assertThatIllegalArgumentException().isThrownBy(() -> processor.registerReflectionHints(
            new ReflectionHints(), Fields.class.getDeclaredField("missingMethod")))
            .withMessageContaining("No method name");
  }

  static class Fields {
    @TestBean(methodName = " example.TestFactories # create ")
    String external;

    @TestBean(methodName = "create")
    String local;

    String plain;

    @TestBean(methodName = " # create")
    String missingClass;

    @TestBean(methodName = "example.TestFactories # ")
    String missingMethod;
  }

}
