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

package infra.web.server.diagnostics;

import org.junit.jupiter.api.Test;

import infra.app.diagnostics.FailureAnalysis;
import infra.web.server.GenericWebServerFactory;
import infra.web.server.MissingWebServerFactoryBeanException;
import infra.web.server.PortInUseException;
import infra.web.server.context.GenericWebServerApplicationContext;
import infra.web.server.reactive.ReactiveWebServerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class WebServerFailureAnalyzerTests {

  @Test
  void analyzesPortInUseFailure() {
    PortInUseException cause = new PortInUseException(8080);

    FailureAnalysis analysis = new PortInUseFailureAnalyzer().analyze(cause);

    assertThat(analysis).isNotNull();
    assertThat(analysis.getDescription()).isEqualTo("Web server failed to start. Port 8080 was already in use.");
    assertThat(analysis.getAction()).isEqualTo(
            "Identify and stop the process that's listening on port 8080 or configure this application to listen on another port.");
    assertThat(analysis.getCause()).isSameAs(cause);
  }

  @Test
  void analyzesMissingServletWebServerFactory() {
    MissingWebServerFactoryBeanException cause = new MissingWebServerFactoryBeanException(
            GenericWebServerApplicationContext.class, GenericWebServerFactory.class);

    FailureAnalysis analysis = new MissingWebServerFactoryBeanFailureAnalyzer().analyze(cause);

    assertThat(analysis).isNotNull();
    assertThat(analysis.getDescription()).isEqualTo(
            "Web application could not be started as there was no infra.web.server.GenericWebServerFactory bean defined in the context.");
    assertThat(analysis.getAction()).isEqualTo(
            "Check your application's dependencies for a supported web server.\nCheck the configured web application type.");
    assertThat(analysis.getCause()).isSameAs(cause);
  }

  @Test
  void analyzesMissingReactiveWebServerFactory() {
    MissingWebServerFactoryBeanException cause = new MissingWebServerFactoryBeanException(
            GenericWebServerApplicationContext.class, ReactiveWebServerFactory.class);

    FailureAnalysis analysis = new MissingWebServerFactoryBeanFailureAnalyzer().analyze(cause);

    assertThat(analysis.getAction()).isEqualTo(
            "Check your application's dependencies for a supported reactive web server.\nCheck the configured web application type.");
  }

  @Test
  void missingFactoryAnalyzerHasDefaultOrder() {
    assertThat(new MissingWebServerFactoryBeanFailureAnalyzer().getOrder()).isZero();
  }

}
