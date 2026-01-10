/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aot.test;

import org.junit.jupiter.api.Test;

import infra.aot.hint.RuntimeHints;
import infra.aot.test.agent.EnabledIfRuntimeHintsAgent;
import infra.aot.test.agent.RuntimeHintsInvocations;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfRuntimeHintsAgent
class ReflectionInvocationsTests {

  @Test
  void sampleTest() {
    RuntimeHints hints = new RuntimeHints();
    hints.reflection().registerType(String.class);

    RuntimeHintsInvocations invocations = infra.aot.test.agent.RuntimeHintsRecorder.record(() -> {
      SampleReflection sample = new SampleReflection();
      sample.sample(); // does Method[] methods = String.class.getMethods();
    });
    assertThat(invocations).match(hints);
  }

  @Test
  void multipleCallsTest() {
    RuntimeHints hints = new RuntimeHints();
    hints.reflection().registerType(String.class);
    hints.reflection().registerType(Integer.class);
    RuntimeHintsInvocations invocations = infra.aot.test.agent.RuntimeHintsRecorder.record(() -> {
      SampleReflection sample = new SampleReflection();
      sample.multipleCalls(); // does Method[] methods = String.class.getMethods(); methods = Integer.class.getMethods();
    });
    assertThat(invocations).match(hints);
  }

}
