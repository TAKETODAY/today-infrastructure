/*
 * Copyright 2012-present the original author or authors.
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

package infra.app;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import infra.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.withSettings;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 21:17
 */
class ExitCodeGeneratorsTests {

  @Test
  void addAllWhenGeneratorsIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> {
      List<ExitCodeGenerator> generators = null;
      new ExitCodeGenerators().addAll(generators);
    }).withMessageContaining("Generators is required");
  }

  @Test
  void addWhenGeneratorIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ExitCodeGenerators().add(null))
            .withMessageContaining("Generator is required");
  }

  @Test
  void getExitCodeWhenNoGeneratorsShouldReturnZero() {
    assertThat(new ExitCodeGenerators().getExitCode()).isEqualTo(0);
  }

  @Test
  void getExitCodeWhenGeneratorThrowsShouldReturnOne() {
    ExitCodeGenerator generator = Mockito.mock(ExitCodeGenerator.class);
    given(generator.getExitCode()).willThrow(new IllegalStateException());
    ExitCodeGenerators generators = new ExitCodeGenerators();
    generators.add(generator);
    assertThat(generators.getExitCode()).isEqualTo(1);
  }

  @Test
  void getExitCodeWithUnorderedGeneratorsReturnsFirstNonZeroExitCode() {
    ExitCodeGenerators generators = new ExitCodeGenerators();
    generators.add(mockGenerator(0));
    generators.add(mockGenerator(3));
    generators.add(mockGenerator(2));
    assertThat(generators.getExitCode()).isEqualTo(3);
  }

  @Test
  void getExitCodeWhenUsingExitCodeExceptionMapperShouldCallMapper() {
    ExitCodeGenerators generators = new ExitCodeGenerators();
    Exception e = new IOException();
    generators.add(e, mockMapper(IllegalStateException.class, 1));
    generators.add(e, mockMapper(IOException.class, 2));
    generators.add(e, mockMapper(UnsupportedOperationException.class, 3));
    assertThat(generators.getExitCode()).isEqualTo(2);
  }

  @Test
  void getExitCodeWithOrderedGeneratorsReturnsFirstNonZeroExitCode() {
    ExitCodeGenerators generators = new ExitCodeGenerators();
    generators.add(orderedMockGenerator(0, 1));
    generators.add(orderedMockGenerator(1, 3));
    generators.add(orderedMockGenerator(2, 2));
    generators.add(mockGenerator(3));
    assertThat(generators.getExitCode()).isEqualTo(2);
  }

  private ExitCodeGenerator mockGenerator(int exitCode) {
    ExitCodeGenerator generator = Mockito.mock(ExitCodeGenerator.class);
    given(generator.getExitCode()).willReturn(exitCode);
    return generator;
  }

  private ExitCodeGenerator orderedMockGenerator(int exitCode, int order) {
    ExitCodeGenerator generator = Mockito.mock(ExitCodeGenerator.class, withSettings().extraInterfaces(Ordered.class));
    given(generator.getExitCode()).willReturn(exitCode);
    given(((Ordered) generator).getOrder()).willReturn(order);
    return generator;
  }

  private ExitCodeExceptionMapper mockMapper(Class<?> exceptionType, int exitCode) {
    return (exception) -> {
      if (exceptionType.isInstance(exception)) {
        return exitCode;
      }
      return 0;
    };
  }

}
