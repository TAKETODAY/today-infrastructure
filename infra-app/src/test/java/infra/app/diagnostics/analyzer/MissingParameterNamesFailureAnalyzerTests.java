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

package infra.app.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.app.diagnostics.FailureAnalysis;
import infra.core.MethodParameter;
import infra.web.handler.method.HandlerParameter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/12/17 16:10
 */
class MissingParameterNamesFailureAnalyzerTests {

  @Test
  void analyzeWhenMissingParametersExceptionReturnsFailure() throws Throwable {
    MissingParameterNamesFailureAnalyzer analyzer = new MissingParameterNamesFailureAnalyzer();
    FailureAnalysis analysis = analyzer.analyze(getInfraFrameworkMissingParameterException());
    assertThat(analysis.getDescription())
            .isEqualTo(String.format("Name for argument of type [java.lang.String] not specified, and parameter name "
                    + "information not available via reflection. Ensure that the compiler uses the '-parameters' flag.:%n"));
    assertThat(analysis.getAction()).isEqualTo(MissingParameterNamesFailureAnalyzer.ACTION);
  }

  @Test
  void analyzeForMissingParametersWhenMissingParametersExceptionReturnsFailure() throws Throwable {
    FailureAnalysis analysis = MissingParameterNamesFailureAnalyzer
            .analyzeForMissingParameters(getInfraFrameworkMissingParameterException());
    assertThat(analysis.getDescription())
            .isEqualTo(String.format("Name for argument of type [java.lang.String] not specified, and parameter name "
                    + "information not available via reflection. Ensure that the compiler uses the '-parameters' flag.:%n"));
    assertThat(analysis.getAction()).isEqualTo(MissingParameterNamesFailureAnalyzer.ACTION);
  }

  @Test
  void analyzeForMissingParametersWhenInCauseReturnsFailure() throws Throwable {
    RuntimeException exception = new RuntimeException("Badness", getInfraFrameworkMissingParameterException());
    FailureAnalysis analysis = MissingParameterNamesFailureAnalyzer.analyzeForMissingParameters(exception);
    assertThat(analysis.getDescription())
            .isEqualTo(String.format("Name for argument of type [java.lang.String] not specified, and parameter name "
                    + "information not available via reflection. Ensure that the compiler uses the '-parameters' flag.:%n%n"
                    + "    Resulting Failure: java.lang.RuntimeException: Badness"));
    assertThat(analysis.getAction()).isEqualTo(MissingParameterNamesFailureAnalyzer.ACTION);
  }

  @Test
  void analyzeForMissingParametersWhenInSuppressedReturnsFailure() throws Throwable {
    RuntimeException exception = new RuntimeException("Badness");
    exception.addSuppressed(getInfraFrameworkMissingParameterException());
    FailureAnalysis analysis = MissingParameterNamesFailureAnalyzer.analyzeForMissingParameters(exception);
    assertThat(analysis.getDescription())
            .isEqualTo(String.format("Name for argument of type [java.lang.String] not specified, and parameter name "
                    + "information not available via reflection. Ensure that the compiler uses the '-parameters' flag.:%n%n"
                    + "    Resulting Failure: java.lang.RuntimeException: Badness"));
    assertThat(analysis.getAction()).isEqualTo(MissingParameterNamesFailureAnalyzer.ACTION);
  }

  @Test
  void scansPastUnrelatedSuppressedExceptionsAndCycles() throws Throwable {
    var failure = new RuntimeException();
    var unrelated = new IllegalStateException(failure);
    failure.addSuppressed(unrelated);
    failure.addSuppressed(getInfraFrameworkMissingParameterException());

    FailureAnalysis analysis = MissingParameterNamesFailureAnalyzer.analyzeForMissingParameters(failure);

    assertThat(analysis).isNotNull();
    assertThat(analysis.getDescription()).endsWith("Resulting Failure: java.lang.RuntimeException");
    assertThat(analysis.getCause()).isSameAs(failure);
  }

  @Test
  void ignoresParameterMessagesWithoutAFrameworkStackFrame() {
    var failure = new IllegalArgumentException("Ensure that the compiler uses the '-parameters' flag");
    failure.setStackTrace(new StackTraceElement[0]);
    assertThat(MissingParameterNamesFailureAnalyzer.analyzeForMissingParameters(failure)).isNull();
    failure.setStackTrace(new StackTraceElement[] { new StackTraceElement("example.UserCode", "invoke", "UserCode.java", 1) });
    assertThat(MissingParameterNamesFailureAnalyzer.analyzeForMissingParameters(failure)).isNull();
  }

  @Test
  void analyzeForMissingParametersWhenNotPresentReturnsNull() {
    RuntimeException exception = new RuntimeException("Badness");
    FailureAnalysis analysis = MissingParameterNamesFailureAnalyzer.analyzeForMissingParameters(exception);
    assertThat(analysis).isNull();
  }

  private RuntimeException getInfraFrameworkMissingParameterException() throws Throwable {
    Method method = getClass().getDeclaredMethod("example", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    HandlerParameter handlerParameter = new HandlerParameter(parameter);
    try {
      handlerParameter.getParameterName();
    }
    catch (RuntimeException ex) {
      return ex;
    }
    throw new AssertionError("Did not throw");
  }

  void example(String name) {
  }

}
