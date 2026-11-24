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

package infra.test.context.bean.override;

import org.junit.jupiter.api.Test;

import infra.aot.hint.ReflectionHints;
import infra.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

class BeanOverrideReflectiveProcessorTests {

  @Test
  void registersOverrideProcessorConstructorsAndIgnoresUnannotatedElements() throws Exception {
    ReflectionHints hints = new ReflectionHints();
    var processor = new BeanOverrideReflectiveProcessor();
    processor.registerReflectionHints(hints, Fields.class.getDeclaredField("bean"));
    assertThat(hints.typeHints()).singleElement().satisfies(hint ->
            assertThat(hint.getMemberCategories()).contains(infra.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
    processor.registerReflectionHints(hints, Fields.class.getDeclaredField("plain"));
    assertThat(hints.typeHints()).hasSize(1);
  }

  static class Fields {
    @MockitoBean
    Runnable bean;

    String plain;
  }

}
