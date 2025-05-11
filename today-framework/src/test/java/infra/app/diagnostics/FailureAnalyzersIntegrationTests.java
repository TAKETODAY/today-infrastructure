/*
 * Copyright 2017 - 2025 the original author or authors.
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
import infra.web.server.PortInUseException;
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
      throw new PortInUseException(8080);
    }

  }

}
