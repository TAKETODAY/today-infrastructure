/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

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
  void analyzeForMissingParametersWhenNotPresentReturnsNull() {
    RuntimeException exception = new RuntimeException("Badness");
    FailureAnalysis analysis = MissingParameterNamesFailureAnalyzer.analyzeForMissingParameters(exception);
    assertThat(analysis).isNull();
  }

  private RuntimeException getInfraFrameworkMissingParameterException() throws Throwable {
    Method method = getClass().getDeclaredMethod("example", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    ResolvableMethodParameter resolvableMethodParameter = new ResolvableMethodParameter(parameter);
    try {
      resolvableMethodParameter.getParameterName();
    }
    catch (RuntimeException ex) {
      return ex;
    }
    throw new AssertionError("Did not throw");
  }

  void example(String name) {
  }

}