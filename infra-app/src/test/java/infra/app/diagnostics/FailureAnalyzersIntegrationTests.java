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

package infra.app.diagnostics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import infra.app.ApplicationType;
import infra.app.builder.ApplicationBuilder;
import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;
import infra.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link FailureAnalyzers}.
 *
 * @author Andy Wilkinson
 */
@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(OutputCaptureExtension.class)
class FailureAnalyzersIntegrationTests {

  @Test
  void analysisIsPerformed(CapturedOutput output) {
    assertThatExceptionOfType(Exception.class)
            .isThrownBy(() -> ApplicationBuilder.forSources(TestConfiguration.class).type(ApplicationType.NORMAL).run());
    assertThat(output).contains("APPLICATION FAILED TO START");
  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfiguration {

    @PostConstruct
    void fail() {
      throw new IllegalStateException();
    }

  }

}
