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
